# ModelSLF — Project Summaries

Three independent projects in one workspace: LLM fine-tuning, a GPT-2
visualization app, and a Spring Boot API practice project.

---

## Finetuning

The `Finetuning` project is a compact, self-contained pipeline for adapting a
small language model to a custom domain. It fine-tunes
`unsloth/Qwen3-0.6B-unsloth-bnb-4bit` in 4-bit quantization using LoRA (rank 16,
alpha 16) via Unsloth and TRL's `SFTTrainer`, trained for 3 epochs over a
735-example instruction/response dataset of short tech glossary Q&A pairs. The
entire workflow lives in a single Google Colab notebook (`fiyntuning_qwen.ipynb`,
originally run on a Tesla T4), where the dataset is formatted through the Qwen3
chat template and only ~1.67% of parameters are trainable — a training loss of
about 0.72 after 276 steps in roughly 8 minutes.

A distinctive detail of the dataset is that every response begins with the
Devanagari prefix `संदर्भ—`, serving as a visible style marker: before training
the base model rambles in a "thinking" style, and after training it reliably
adopts the prefixed answer format, which the notebook's before/after inference
cells demonstrate directly. Beyond training, the project exports the merged
model to GGUF format in both `f16` (~1.2 GB) and `q4_k_m` (~379 MB)
quantizations together with an Ollama `Modelfile`, so the fine-tuned model can
run fully locally without any API dependency.

Deployment and evaluation complete the loop: the GGUF is imported into Ollama
(`ollama create my_model -f Modelfile`), after which `test_model.py` queries the
model with 8 knowledge-graph/Neo4j questions from `inputs.json` using the Python
Ollama client and prints per-question input/output blocks. The repository keeps
only the notebook, the two JSON files, the test script, and the Modelfile — the
large GGUF binaries must be regenerated in Colab, and no `requirements.txt` is
needed since `pip install -U unsloth` installs the full training stack.

---

## GPT2_Local

`GPT2_Local` (published as **gpt2-visualizer**) is a full-stack FastAPI + React
application that visualizes the real forward pass of a from-scratch GPT-2
implementation token by token. Its central design constraint is that nothing in
the UI is mocked: every number shown comes from a tensor recorded during an
actual forward pass. To make that possible, the hand-written `MultiHeadAttention`
in `backend/gpt2/layers.py` deliberately avoids PyTorch's fused
`F.scaled_dot_product_attention`, materializing the full (heads, T, T)
attention matrix so it can be rendered as heatmaps or arc diagrams per head.
The pure model package — layers, model, tracing, weight loading, streaming
generation — was extracted from `notebooks/gpt2.ipynb`, which follows Sebastian
Raschka's *Build a Large Language Model (From Scratch)*.

The React frontend offers two interchangeable modes writing into a single
Zustand store: **Live mode** streams one traced forward pass per generated token
over a WebSocket from FastAPI, while **Replay mode** plays back pre-recorded
JSON bundles and needs no backend at all, making a fully static build possible.
The UI exposes the animated pipeline (Tokens → LayerNorm → Attention → Add →
LayerNorm → MLP → Add → Sample), per-block internals (attention heatmaps/arcs,
LayerNorm outputs, residual stream via L2 norms and shared PCA projection, top-k
MLP neurons), a logit lens showing what the model predicts at each depth, and
the sampling funnel from raw logits through temperature and top-k filtering to
the final inverse-CDF draw — plus a side-by-side two-model comparison panel.

Two models are registered: `openai-124m` (real OpenAI GPT-2 weights, with
`355M`/`774M`/`1558M` selectable via `GPT2_MODEL_SIZE`) and `scratch-10ep`, a
13.3M-parameter model trained for 10 epochs on *The Verdict* in ~35 seconds on
CPU. The project runs either via `docker compose up --build` (backend on :8000,
nginx-proxied frontend on :8080) or as local dev terminals, and ships with ~168
pytest tests covering trace shape/numeric contracts, HuggingFace parity, API
schemas, and settings — plus frontend smoke checks that assert numeric
invariants like causal-mask zeros and attention rows summing to one.

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
