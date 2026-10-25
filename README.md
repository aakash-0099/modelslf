# ModelSLF

Three independent projects in one workspace, spanning LLM fine-tuning,
model-interpreter tooling, and backend API engineering.

| Project | Stack | What it is |
| --- | --- | --- |
| [finetune-service](services/finetune-service/README.md) | Python · Unsloth · Colab · Ollama | QLoRA fine-tune of Qwen3-0.6B on a custom dataset → GGUF export → local Ollama serving |
| [gpt2-visualizer](docs/gpt2/OVERVIEW.md) | Python · FastAPI · PyTorch · React | token-by-token visualization of a real GPT-2 forward pass (attention heatmaps, logit lens, sampling funnel) |
| [platform-api](platform-api/README.md) | Java 25 · Spring Boot · PostgreSQL · Redis | User Management REST API with JWT auth, Bucket4j rate limiting, and stampede-safe Redis caching |

## Layout

```
ModelSLF/
├── .gitignore              unified ignore rules for all three projects
├── README.md               this file
├── tree.txt                full file listing of the workspace
├── docs/
│   ├── SUMMARY.md          three-paragraph summary of each project
│   └── gpt2/               gpt2-visualizer docs: OVERVIEW, architecture, trace-schema
├── frontend/               gpt2-visualizer React app (Vite + TypeScript)
├── infra/
│   └── docker-compose.gpt2.yml   gpt2-visualizer backend + frontend
├── platform-api/           Maven project (app + Docker Compose)
└── services/
    ├── finetune-service/   Colab notebook + dataset + Ollama test harness
    └── gpt2-service/       FastAPI backend + gpt2 model package + tests
```

## Reading order

- **Want to run something?** Each project's own README has setup, commands,
  and usage:
  - [services/finetune-service/README.md](services/finetune-service/README.md)
  - [docs/gpt2/OVERVIEW.md](docs/gpt2/OVERVIEW.md)
  - [platform-api/README.md](platform-api/README.md)
- **Want the big picture?** [docs/SUMMARY.md](docs/SUMMARY.md) describes each
  project in three paragraphs — purpose and approach, key details, and how it
  fits together.

## Quick start pointers

```bash
# finetune-service — open services/finetune-service/fiyntuning_qwen.ipynb in
# Colab (T4 GPU); locally, with the exported .gguf next to model_gguf/Modelfile:
cd services/finetune-service
(cd model_gguf && ollama create my_model -f Modelfile) && python test_model.py

# gpt2-visualizer — http://localhost:8080
docker compose -f infra/docker-compose.gpt2.yml up --build

# platform-api — app on :8081, Postgres on :5433, Redis on :6379
cd platform-api && docker compose up --build
```

## Notes

- The three projects are **fully independent** — no shared build tooling or
  code. `finetune-service` and `gpt2-service` live under `services/`; the
  gpt2-visualizer frontend, its docs and its compose file sit beside them at
  the repo root. Only gpt2-visualizer is orchestrated from the root (via
  `infra/`); the other two run from their own directories.
- A single root [`.gitignore`](.gitignore) covers Python, Node, Java/Maven,
  model weights/checkpoints, `.env` secrets, and OS junk for all three
  (individual per-project `.gitignore` files may still exist; they are
  redundant but harmless).
- Large binaries (GGUF exports, GPT-2 checkpoints, replay trace bundles) are
  intentionally ignored — regenerate them via each project's README.
