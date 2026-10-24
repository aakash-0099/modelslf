# gpt2-visualizer

A FastAPI + React app that visualizes the **real** forward pass of a from-scratch
GPT-2 (124M) implementation, token by token. Nothing in the UI is mocked: every
number comes from a tensor recorded during an actual forward pass.

> **Demo GIF not yet recorded.** [docs/README.md](docs/README.md) explains how
> to capture `docs/demo.gif` (short recording of replay mode: pipeline
> animation → attention arcs → logit lens, then `ffmpeg` downscale).

## Status

| Phase | Scope | State |
| --- | --- | --- |
| 1 | `backend/gpt2` pure model package + docs | done |
| 2 | Tracing: `return_trace`, logit lens, PCA reduction, `generate_stream` | done |
| 3 | Test suite (shape, numeric contract, HF parity) | done |
| 4 | FastAPI app, training + export scripts, compose | done |
| 5 | `frontend` React app | done |
| 6 | Docs, Docker, static build | done |

## Quick start

### Docker (recommended)

```bash
cd GPT2_Local
docker compose up --build
```

Then open **http://localhost:8080**.

First start takes ~3 minutes: the 500 MB GPT-2 124M checkpoint is downloaded and
loaded. Subsequent starts are ~30 s. Replay mode works as soon as the frontend is
up, before the backend finishes loading — the exported traces need no model.

To skip the download and run without the real weights:

```bash
GPT2_LOAD_OPENAI_MODEL=0 docker compose up --build
```

`openai-124m` will be listed with `ready: false` and the UI will say why.

### Choosing the model size

Which OpenAI checkpoint is downloaded is configurable, and the architecture
config is selected to match it, so the model and the weights can never disagree:

```bash
# local dev: a .env next to docker-compose.yml (or backend/.env) is read by the backend
echo "GPT2_MODEL_SIZE=355M" >> .env

# docker: compose interpolates that same .env file, or use the shell
GPT2_MODEL_SIZE=355M docker compose up --build
```

Valid sizes are `124M` (default), `355M`, `774M` and `1558M`. The registry id
follows the size — `openai-355m` — and so does the download, which grows fast:
~500 MB for 124M, ~1.5 GB for 355M, ~3 GB for 774M, ~6 GB for 1558M. Bigger
models are also slow to run per token; the traces were designed around 124M.

Copy `backend/.env.example` to `backend/.env` (or a `.env` next to
`docker-compose.yml`) for the full list of backend settings
(`GPT2_DEVICE`, `GPT2_CHECKPOINT_DIR`, `GPT2_LOAD_OPENAI_MODEL`, ...). Values
already set in the environment always override what is in the file.

### Local development

Two terminals. Backend:

```bash
cd GPT2_Local/backend
python -m venv .venv && source .venv/Scripts/activate   # Windows
pip install -r requirements.txt
python -m scripts.export_traces     # optional, enables replay mode
uvicorn app.main:app --reload --port 8000
```

Frontend:

```bash
cd GPT2_Local/frontend
npm install
npm run dev            # http://localhost:5173, proxies /api to :8000
```

### Optional: train the scratch model

`scratch-10ep` is a 13.3 M-parameter GPT-2 trained for 10 epochs on *The
Verdict*, ~35 s on CPU:

```bash
cd GPT2_Local/backend
python -m scripts.train_small
```

Until it runs, that model is listed with `ready: false` rather than being hidden,
so the UI can explain the absence instead of silently showing one model.

## Two ways to run it

### Live mode

Needs the backend. Generates over a WebSocket and streams one traced forward pass
per token:

```bash
docker compose up --build      # or the local dev setup above
```

Open the app, pick **Live**, set a prompt, press **Generate**. Each new token
appends to the context and the whole pipeline animates again over the longer
sequence. **Stop** closes the socket; the traces already received stay on screen.

### Replay mode

Needs **no backend at all** — this is the static-hosting path. Plays the
pre-recorded traces in `frontend/public/traces/*.json`:

```bash
cd GPT2_Local/frontend
npm run dev            # Replay is the default mode
```

The bundles hold the same `WSStepMessage` payloads the live socket sends, so
`ModeToggle` swaps the transport and no component changes.

To regenerate them:

```bash
cd GPT2_Local/backend
python -m scripts.export_traces          # writes frontend/public/traces/*.json
```

> The traces are **gitignored** — they are 7.8 MB of generated JSON and the
> exporter needs the backend's dependencies and the 124M checkpoint. Run it once
> after cloning, or the replay buttons 404. `npm run build:static` checks for
> them and tells you this if they are missing.

### Building for static hosting

```bash
cd GPT2_Local/frontend
npm run build:static         # -> dist-static/
npm run preview:static       # serve it locally at :4173
```

Upload `dist-static/` to any static host. There is no server component: the
build has no WebSocket code and makes no `/api` calls, verified by grepping the
output. Only the mode toggle's **Replay** option is offered, and the comparison
panel is hidden because it needs two models from the registry.

> `preview:static` runs with proxying explicitly disabled. `vite preview`
> otherwise falls back to `server.proxy`, so with a backend running on `:8000`
> it would silently proxy `/api` and make a bundle that is *not* standalone look
> like it works. If you check `/api/models` while previewing and get the SPA
> fallback page back rather than JSON, that is the expected result.

For a subpath deploy (GitHub Pages project sites), set the base path in
`frontend/.env.static` — see `frontend/.env.static.example`.

## Architecture

```
                    ┌─────────────────────────────────────────────┐
                    │            React app (Vite)                  │
                    │                                              │
                    │   ModeToggle ──┬── Live     ──▶ WebSocket     │
                    │                │                              │
                    │                └── Replay   ──▶ fetch        │
                    │                                     │        │
                    │                        useTraceStore (Zustand) │
                    │                                     │        │
                    │   ┌─────────────┬───────────────────┤        │
                    │   ▼             ▼                   ▼        │
                    │ Pipeline   BlockDetail        LogitLens       │
                    │ Tokens→…→Sample  LN→Attn→Add→LN→MLP→Add         │
                    │   │             │                                │
                    │   │        AttentionHeatmap / Arcs / HeadSelector│
                    │   │             │                                │
                    │   │        TopKChart ──▶ TokenStrip ──▶ loop     │
                    │   └─────────────┴────────────────────────────────┘
                    └──────────────┬──────────────────────────┬────────┘
                     /api/ws/generate                  /traces/*.json
                                   │                          │
                    ┌──────────────▼──────────┐    ┌──────────▼─────────┐
                    │  FastAPI (:8000)        │    │ exported bundles    │
                    │  registry → gpt2 model  │    │ (gitignored, 7.8MB) │
                    └──────────────┬──────────┘    └────────────────────┘
                                   │
                    ┌──────────────▼──────────────────────────────┐
                    │  backend/gpt2 — pure model package           │
                    │  hand-written attention (no F.sdpa, so the    │
                    │  T×T matrix actually exists to visualize)     │
                    │  forward(return_trace=True) → Trace           │
                    └─────────────────────────────────────────────┘
```

Data flows one way: both sources write `WSStepMessage`s into `useTraceStore`, and
every component reads the store. Neither component knows which mode is active,
which is why `ModeToggle` is the only place that changes.

Read [docs/architecture.md](docs/architecture.md) for the model and tracing
detail, and [docs/trace-schema.md](docs/trace-schema.md) for the JSON contract.

## Layout

```
GPT2_Local/                (original repo name: gpt2-visualizer)
├── docs/                 architecture.md, trace-schema.md, README.md
├── notebooks/            the original notebook, untouched
├── docker-compose.yml    backend + frontend
├── backend/
│   ├── gpt2/             pure model package (no web code)
│   ├── app/              FastAPI: settings, registry, routes
│   ├── scripts/          train_small.py, export_traces.py
│   ├── tests/            parity, trace-shape, API route tests
│   └── checkpoints/      gitignored weights
└── frontend/
    ├── src/types/        the JSON contract, mirrored from docs/trace-schema.md
    ├── src/store/        useTraceStore: steps, current step, selected block/head
    ├── src/hooks/        useLiveGeneration (WebSocket) and useReplay (bundles)
    ├── src/components/   Pipeline, BlockDetail, Attention*, LogitLens, TopK, Compare
    ├── public/traces/    gitignored exported bundles
    └── Dockerfile        node build -> nginx, proxies /api
```

## Endpoints

| Method | Path | Purpose |
| --- | --- | --- |
| `GET` | `/api/health` | Liveness, device, models ready |
| `GET` | `/api/models` | Registry listing with `ready` flags |
| `POST` | `/api/tokenize` | GPT-2 BPE encoding |
| `POST` | `/api/trace` | One traced forward pass |
| `WS` | `/api/ws/generate` | Stream one trace per generated token, then `done` |

Two models are registered: `openai-124m` (real OpenAI weights; the id becomes
`openai-355m` etc. when `GPT2_MODEL_SIZE` is set) and `scratch-10ep`
(from `scripts/train_small.py`, listed as not-ready when its checkpoint is absent).

Interactive reference at http://localhost:8000/docs.

## Tests

```bash
cd GPT2_Local/backend
pytest                # 168 tests, ~35 s
pytest -m "not slow"   # 154 tests, ~11 s, no checkpoint needed
pytest -m slow         # real 124M weights + HuggingFace comparison
```

```bash
cd GPT2_Local/frontend
npm run smoke           # renders the real components against the real traces
npm run smoke:static    # same, against the static-build module graph
```

`smoke` asserts numeric invariants on all four recorded traces — the causal
upper triangle is exactly `0.0`, the deepest logit-lens entry equals
`top_tokens[0]`, sampling candidates sum to 1 within float16 tolerance — then
renders each component and checks the output. It is not a browser test; see
[frontend/README.md](frontend/README.md) for what it does and does not cover.

## Using the model directly

```python
from gpt2 import GPTModel, GPT_CONFIG_124M, get_tokenizer

tok = get_tokenizer()
model = GPTModel(GPT_CONFIG_124M).eval()
trace = model.forward(
    torch.tensor([tok.encode("Life is full of")]),
    return_trace=True, tokenizer=tok, model_id="scratch",
)
print(trace.top_pieces[:3])
```

Load the real OpenAI weights:

```python
from gpt2 import download_and_load_gpt2, load_weights_into_gpt

model = GPTModel(GPT_CONFIG_124M).eval()
_, params = download_and_load_gpt2("124M", "checkpoints/gpt2", reuse_existing=True)
load_weights_into_gpt(model, params)
```

Stream a generation with a trace per token:

```python
from gpt2 import generate_stream

for step in generate_stream(model, prompt="Every effort moves you",
                            max_new_tokens=5, temperature=0.9, top_k=50):
    print(step.step, repr(step.piece), step.trace.logit_lens[-1].top_pieces[0])
```

## Notes

- **Model size override:** `backend/.env` in this checkout currently pins
  `GPT2_MODEL_SIZE=1558M` (~6 GB download, slow CPU inference). Delete the file
  or set `GPT2_MODEL_SIZE=124M` to get the documented default — the traces and
  docs were designed around 124M.
- **Replay bundles are generated:** `frontend/public/traces/` is gitignored and
  absent after cloning. Run `python -m scripts.export_traces` once before using
  Replay mode or `npm run build:static` (which checks for them).
- **Folder vs project name:** this directory is `GPT2_Local`, but the project
  (and its `package.json` name) is `gpt2-visualizer`.

## Source

Derived from the from-scratch implementation in `notebooks/gpt2.ipynb`, itself
following *Build a Large Language Model (From Scratch)* by Sebastian Raschka.
