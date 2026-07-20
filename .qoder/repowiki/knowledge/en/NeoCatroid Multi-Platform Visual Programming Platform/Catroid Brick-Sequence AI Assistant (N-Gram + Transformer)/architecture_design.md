Flat script-oriented package with no Python packages — each `.py` file is an independent CLI entry point sharing two core modules:

- `code_xml_parser.py` — XML parser for NewCatroid `.code.xml` files; defines dataclasses `Project/Scene/Sprite/Script/Brick/FormulaElement` and exposes `scan_projects()` + `export_to_json()` used by every training pipeline.
- `tokenizer.py` — `BrickTokenizer` mapping brick types to token IDs plus structural boundary tokens (`<project_start>`, `<scene_start>`, …) for LoRA-compatible sequences; provides `build_project_sequence()` and `generate_training_pairs()`.

Two parallel prediction backends coexist:
1. **N-gram Markov model** — `pattern_extractor.py` scans parsed projects to build variable-order (2–5 gram) counts per-script and globally, emitting `model/patterns.json`; `suggest.py` loads this JSON and falls back from 5-gram down to 2-gram at inference time.
2. **Transformer** — `train_transformer.py` builds a decoder-only GPT-style `BrickTransformer` (positional encoding, causal mask precomputed in `__init__`, mixed precision via `torch.amp.GradScaler`, optional `torch.compile`) over a `BrickDataset` sliding window; saves `transformer_model.pt`, `model_metadata.json`, and an ONNX wrapper exporting only the last-token logits for Android's built-in ONNX Runtime.

Supporting scripts: `train_lstm.py` (TensorFlow placeholder), `train.py` (legacy), batch helpers `run_training.bat`, `deploy.bat`, `prepare_colab.bat`, Colab notebooks `train_colab.ipynb` / `train_colab_transformer.ipynb`, and a frozen `colab_pack.zip`. Dependency manifest is `requirements.txt` (numpy, lxml, json5, torch ≥ 2.0; TF/transformers commented out).