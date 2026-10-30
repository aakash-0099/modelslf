# ModelSLF — Project Summaries

Three independent projects in one workspace: LLM fine-tuning, a GPT-2
visualization app, and a Spring Boot API practice project.

---

## springboot-practice

`springboot-practice` is a single-module Java 17 / Spring Boot 4.1.0 Maven
project implementing a User Management REST API with JWT authentication against
PostgreSQL and Redis. It practices a layered REST architecture
(controller/service/repository) with stateless Spring Security sessions,
JJWT-signed tokens carrying a role claim, `@PreAuthorize` role checks (ADMIN for
user administration, ADMIN-or-self for reads/updates), Bean Validation, RFC 7807
`ProblemDetail` error responses, JPA auditing with `AuditorAware`, and
MapStruct + Lombok DTO mapping. The API exposes registration and login under
`/api/auth` and full CRUD under `/api/users`, with Swagger UI and a health
endpoint as the only other public routes; a Bucket4j filter rate-limits requests
(100 per 60 s, keyed by IP or user) and returns 429 on overflow.

The project's headline feature is its caching architecture. `GET /api/users/{id}`
is `@Cacheable`, but reads go through a custom `LeaseAwareRedisCache` that takes
a Redis `SETNX` lease (5-second TTL, UUID token, Lua compare-and-delete) before
loading a cache miss from the database — protecting against the cache-stampede
problem where many concurrent requests simultaneously hit a cold key. Cache
invalidation is equally deliberate: updates evict locally, write rows into a
`cache_invalidation_events` outbox table, and a `@Scheduled` processor
(approximately every 100 seconds) deletes the corresponding Redis keys and
cleans up processed events, mimicking a transactional-outbox pattern.

Deployment is supported both ways: `mvnw spring-boot:run` against local
PostgreSQL and Redis (port 8081, all settings overridable via environment
variables), or `docker compose up --build` which orchestrates the app plus
`postgres:16` (host port 5433) and `redis:7` behind a multi-stage Dockerfile.
Known gaps worth noting: no endpoint creates the first ADMIN user (it must be
inserted directly in the database), failed logins currently surface as 500s
because `AuthService` throws a plain `RuntimeException` (a proper 401/403 fix is
sketched in comments inside `GlobalExceptionHandler`), and the only test is a
context-load test requiring live services.
