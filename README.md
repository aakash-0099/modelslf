# ModelGPT

Three independent projects in one workspace, spanning LLM fine-tuning,
model-interpreter tooling, and backend API engineering.

| Project | Stack | What it is |
| --- | --- | --- |
| [Finetuning](Finetuning/README.md) | Python · Unsloth · Colab · Ollama | QLoRA fine-tune of Qwen3-0.6B on a custom dataset → GGUF export → local Ollama serving |
| [GPT2_Local](GPT2_Local/README.md) | Python · FastAPI · PyTorch · React | **gpt2-visualizer** — token-by-token visualization of a real GPT-2 forward pass (attention heatmaps, logit lens, sampling funnel) |
| [springboot-practice](springboot-practice/README.md) | Java 17 · Spring Boot · PostgreSQL · Redis | User Management REST API with JWT auth, Bucket4j rate limiting, and stampede-safe Redis caching |

## Layout

```
ModelGPT/
├── .gitignore              unified ignore rules for all three projects
├── README.md               this file
├── SUMMARY.md              three-paragraph summary of each project
├── Finetuning/             Colab notebook + dataset + Ollama test harness
├── GPT2_Local/             FastAPI backend + React frontend + model package
└── springboot-practice/    Maven project (app + Docker Compose)
```

## Reading order

- **Want to run something?** Each project's own README has setup, commands,
  and usage:
  - [Finetuning/README.md](Finetuning/README.md)
  - [GPT2_Local/README.md](GPT2_Local/README.md)
  - [springboot-practice/README.md](springboot-practice/README.md)
- **Want the big picture?** [SUMMARY.md](SUMMARY.md) describes each project in
  three paragraphs — purpose and approach, key details, and how it fits
  together.

## Quick start pointers

```bash
# Finetuning — open Finetuning/fiyntuning_qwen.ipynb in Colab (T4 GPU),
# then locally: ollama create my_model -f Modelfile && python test_model.py

# GPT2_Local — http://localhost:8080
cd GPT2_Local && docker compose up --build

# springboot-practice — app on :8081, Postgres on :5433, Redis on :6379
cd springboot-practice && docker compose up --build
```

## Notes

- The three projects are **fully independent** — no shared build tooling or
  code. Run each from its own directory.
- A single root [`.gitignore`](.gitignore) covers Python, Node, Java/Maven,
  model weights/checkpoints, `.env` secrets, and OS junk for all three
  (individual per-project `.gitignore` files may still exist; they are
  redundant but harmless).
- Large binaries (GGUF exports, GPT-2 checkpoints, replay trace bundles) are
  intentionally ignored — regenerate them via each project's README.
