# springboot-practice — User Management REST API

A single-module Spring Boot practice project implementing a **User Management
REST API** with JWT authentication. It exercises a layered architecture
(controller / service / repository) together with a set of production-flavored
patterns: stateless JWT security with role-based method authorization, Bean
Validation, RFC 7807 `ProblemDetail` errors, JPA auditing, Redis caching with
**cache-stampede protection**, transactional-outbox-style cache invalidation,
and Bucket4j rate limiting.

- **Language / build:** Java 17, Maven (wrapper included)
- **Framework:** Spring Boot 4.1.0
- **Persistence:** PostgreSQL (Spring Data JPA, `ddl-auto=update`)
- **Cache:** Redis
- **Other libraries:** JJWT 0.12.6, Bucket4j 8.19.0, MapStruct, Lombok,
  springdoc-openapi (Swagger UI)

## Layout

```
springboot-practice/
├── pom.xml, mvnw, mvnw.cmd
├── Dockerfile, docker-compose.yml        # app + postgres + redis
└── src/main/java/com/example/usermanagement/
    ├── UserManagementApplication.java    # @EnableJpaAuditing, @EnableScheduling
    ├── controller/    AuthController, UserController
    ├── service/       UserServiceImpl, AuthService, RateLimitService
    ├── security/      JwtService, JwtAuthenticationFilter, RateLimitFilter, ...
    ├── cache/         LeaseAwareRedisCache(+Manager), CacheLeaseService,
    │                  CacheInvalidationOutboxProcessor
    ├── entity/        User, Role, CacheInvalidationEvent
    ├── repository/    UserRepository, CacheInvalidationEventRepository
    ├── dto/, mapper/, exception/, config/
```

## API

| Method | Path | Auth | Notes |
| --- | --- | --- | --- |
| `POST` | `/api/auth/register` | public | 201, creates `USER` |
| `POST` | `/api/auth/login` | public | 200, returns `{ accessToken }` |
| `POST` | `/api/users` | ADMIN | 201 + `Location` header |
| `GET` | `/api/users` | ADMIN | list users |
| `GET` | `/api/users/{id}` | ADMIN or self | `@Cacheable("users")` |
| `PUT` | `/api/users/{id}` | ADMIN or self | evicts cache, writes outbox event |
| `DELETE` | `/api/users/{id}` | ADMIN | 204 |
| `GET` | `/actuator/health` | public | only exposed actuator endpoint |

Public also: `/swagger-ui/**`, `/v3/api-docs/**`. Everything else requires
`Authorization: Bearer <accessToken>`.

### Sample flow

```bash
# 1. Register
curl -X POST http://localhost:8081/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"Ada","email":"ada@example.com","password":"password123"}'

# 2. Login
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"ada@example.com","password":"password123"}'
# -> { "accessToken": "eyJ..." }

# 3. Call the API
curl http://localhost:8081/api/users/1 -H "Authorization: Bearer eyJ..."
```

Interactive docs: **http://localhost:8081/swagger-ui/**.

## Running

### Docker (recommended)

```bash
docker compose up --build
```

Starts three containers:

| Service | Image | Port |
| --- | --- | --- |
| app | multi-stage Maven → JRE 17 | **8081** |
| postgres | `postgres:16` | host **5433** → 5432 |
| redis | `redis:7` | 6379 |

DB: `user_management` / `postgres` / `postgres`.

### Local

Requires a running PostgreSQL and Redis (no in-memory fallback):

```bash
./mvnw spring-boot:run            # or mvnw.cmd on Windows
./mvnw clean package -DskipTests  # -> target/demo-0.0.1-SNAPSHOT.jar
```

Defaults in `application.properties`: DB `jdbc:postgresql://localhost:5432/test`
(user `postgres` / `admin@123`), Redis `localhost:6379`, port **8081**. All
overridable via env vars (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`,
`JWT_EXPIRATION`, `RATE_LIMIT_*`, `REDIS_HOST/PORT`, `SERVER_PORT`, ...).

## Features worth knowing

- **JWT auth** — stateless sessions, HS-signed tokens (`sub` = email, `role`
  claim), BCrypt passwords, custom 401/403 handlers emitting
  `application/problem+json`.
- **Rate limiting** — Bucket4j filter: 100 requests / 60 s, keyed by IP for
  auth endpoints and by user id elsewhere; returns 429 `ProblemDetail`.
  Toggle with `rate-limit.enabled`.
- **Cache-stampede protection** — `LeaseAwareRedisCache` takes a Redis `SETNX`
  lease (5 s TTL, UUID token, Lua release) before loading a miss; waiters poll
  and re-acquire. Cache TTL is 10 minutes.
- **Async invalidation** — `PUT` writes `cache_invalidation_events` rows; a
  `@Scheduled` processor (~100 s) deletes the Redis keys and cleans up.

## Known limitations

- **No ADMIN bootstrap endpoint** — register/create always assign `Role.USER`;
  insert the first ADMIN directly in the DB to test admin-only routes.
- **Bad login returns 500** — `AuthService` throws a plain `RuntimeException`
  for wrong credentials/disabled accounts; the intended 401/403 fix is sketched
  in the commented block inside `GlobalExceptionHandler.java`.
- Only one context-load test exists; it needs live PostgreSQL + Redis.

Git remote: `github.com/aakashbsharma/java_springboot` (branch `main`).
