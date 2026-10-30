# ModelSLF

Three independent projects in one workspace, spanning LLM fine-tuning,
model-interpreter tooling, and backend API engineering.

| Project | Stack | What it is |
| --- | --- | --- |
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

```

## Reading order

- **Want to run something?** Each project's own README has setup, commands,
  and usage:
  - [platform-api/README.md](platform-api/README.md)
- **Want the big picture?** [docs/SUMMARY.md](docs/SUMMARY.md) describes each
  project in three paragraphs — purpose and approach, key details, and how it
  fits together.

## Quick start pointers


# platform-api — app on :8081, Postgres on :5433, Redis on :6379
cd platform-api && docker compose up --build
```

## Notes

- A single root [`.gitignore`](.gitignore) covers Python, Node, Java/Maven,
  model weights/checkpoints, `.env` secrets, and OS junk for all three
  (individual per-project `.gitignore` files may still exist; they are
  redundant but harmless).