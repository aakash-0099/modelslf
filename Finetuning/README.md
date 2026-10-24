# Finetuning — Qwen3-0.6B QLoRA Fine-Tuning → GGUF → Ollama

A small, self-contained project that fine-tunes a quantized
**Qwen3-0.6B** model on a custom instruction dataset using
[Unsloth](https://github.com/unslothai/unsloth) (LoRA/QLoRA), exports the result
as a **GGUF** file, and serves it locally with **Ollama** for testing.

The dataset is a 735-example collection of short tech glossary Q&A pairs
(programming concepts, tools, knowledge graphs, Neo4j, RAG, Cypher). Every
response starts with the Devanagari prefix `संदर्भ—` — a deliberate style marker
you can watch the model learn during training.

## Layout

```
Finetuning/
├── fiyntuning_qwen.ipynb    # main Colab notebook (51 cells, T4 GPU)
├── data.json                # 735 {instruction, response} pairs
├── inputs.json              # 8 knowledge-graph test questions
├── test_model.py            # Ollama inference test harness
└── model_gguf/
    └── Modelfile            # Ollama model definition (GGUF not committed)
```

> The notebook filename keeps the original `fiyntuning` spelling.

## Requirements

- **Training:** Google Colab (or any Linux GPU box) with a CUDA GPU — the
  notebook ran on a single **Tesla T4**. Install is a single line:
  `!pip install -U unsloth` (Unsloth pulls in torch, transformers, TRL,
  datasets).
- **Inference:** [Ollama](https://ollama.com) installed locally plus the
  Python client: `pip install ollama`.

## Workflow

### 1. Train (in the notebook)

Open `fiyntuning_qwen.ipynb` in Colab (GPU runtime) and run top to bottom:

1. Loads `unsloth/Qwen3-0.6B-unsloth-bnb-4bit` in 4-bit
   (`max_seq_length=1024`).
2. Uploads `data.json` and formats each pair through the Qwen3 chat template.
3. Attaches a LoRA adapter — `r=16`, `lora_alpha=16`, `lora_dropout=0`,
   gradient checkpointing (only **1.67%** of params trainable).
4. Trains with TRL's `SFTTrainer` for **3 epochs** (276 steps, training loss
   ~0.72, ~462 s on a T4).

Before/after inference cells show the model picking up the dataset style
(e.g. answering *"What is the capital of Japan?"* with
`संदर्भ— The capital of Japan is Tokyo.`).

### 2. Export to GGUF

```python
model.save_pretrained_gguf("model", tokenizer, quantization_method=["f16", "q4_k_m"])
```

Produces (in Colab's `/content/model_gguf/`):

| File | Size |
| --- | --- |
| `qwen3-0.6b.F16.gguf` | ~1.2 GB |
| `qwen3-0.6b.Q4_K_M.gguf` | ~379 MB |
| `Modelfile` | Ollama definition |

Download the zip (`model_gguf.zip`) at the end of the notebook. The GGUF
binaries are **not committed** to this repo — only the `Modelfile` is.

### 3. Serve with Ollama

Put `qwen3-0.6b.Q4_K_M.gguf` next to `model_gguf/Modelfile` (the `FROM`
directive references it by filename), then:

```bash
cd model_gguf
ollama create my_model -f Modelfile
ollama run my_model
```

### 4. Test

```bash
python test_model.py
```

Runs the 8 questions in `inputs.json` (knowledge-graph / Neo4j topics) against
the imported `my_model` via `ollama.chat()` and prints `TEST CASE / INPUT /
OUTPUT` blocks. Note: `inputs.json` is loaded by relative path, so run it from
the repo root.

## Hyperparameters

| Setting | Value |
| --- | --- |
| Base model | `unsloth/Qwen3-0.6B-unsloth-bnb-4bit` |
| Quantization | 4-bit (bitsandbytes) |
| LoRA rank / alpha | 16 / 16 |
| Sequence length | 1024 |
| Epochs | 3 (276 global steps) |
| Random seed | 3407 |
| Export | `f16`, `q4_k_m` |

## Repo

Git remote: `github.com/aakashbsharma/finetuning` (branch `main`).
