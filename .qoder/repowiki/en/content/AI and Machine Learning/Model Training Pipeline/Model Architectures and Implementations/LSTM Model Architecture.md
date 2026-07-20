# LSTM Model Architecture

<cite>
**Referenced Files in This Document**
- [train_lstm.py](file://aip/train_lstm.py)
- [tokenizer.py](file://aip/tokenizer.py)
- [suggest.py](file://aip/suggest.py)
- [model_metadata.json](file://aip/model/model_metadata.json)
- [vocab.json](file://aip/vocab.json)
- [code_xml_parser.py](file://aip/code_xml_parser.py)
- [pattern_extractor.py](file://aip/pattern_extractor.py)
- [requirements.txt](file://aip/requirements.txt)
</cite>

## Table of Contents
1. [Introduction](#introduction)
2. [Project Structure](#project-structure)
3. [Core Components](#core-components)
4. [Architecture Overview](#architecture-overview)
5. [Detailed Component Analysis](#detailed-component-analysis)
6. [Dependency Analysis](#dependency-analysis)
7. [Performance Considerations](#performance-considerations)
8. [Troubleshooting Guide](#troubleshooting-guide)
9. [Conclusion](#conclusion)
10. [Appendices](#appendices)

## Introduction
This document explains the Long Short-Term Memory (LSTM) model architecture used for code pattern recognition and context-aware suggestions in NewCatroid. It covers network topology, sequence modeling via tokenization, attention mechanisms, training methodology, hyperparameters, memory requirements, performance characteristics, and practical usage patterns for configuration, training, and inference.

## Project Structure
The LSTM-related components are primarily located under the aip directory:
- Training pipeline and model definition: train_lstm.py
- Tokenization utilities: tokenizer.py
- Inference and suggestion service: suggest.py
- Data preparation helpers: code_xml_parser.py, pattern_extractor.py
- Model metadata and vocabulary: model/model_metadata.json, vocab.json
- Python dependencies: requirements.txt

```mermaid
graph TB
subgraph "Data Preparation"
XML["code_xml_parser.py"]
PAT["pattern_extractor.py"]
end
subgraph "Tokenization"
TOK["tokenizer.py"]
VOCAB["vocab.json"]
end
subgraph "Model"
META["model/model_metadata.json"]
LSTM["train_lstm.py"]
end
subgraph "Inference"
SUG["suggest.py"]
end
XML --> PAT
PAT --> TOK
TOK --> LSTM
VOCAB --> TOK
META --> LSTM
LSTM --> SUG
```

**Diagram sources**
- [code_xml_parser.py](file://aip/code_xml_parser.py)
- [pattern_extractor.py](file://aip/pattern_extractor.py)
- [tokenizer.py](file://aip/tokenizer.py)
- [vocab.json](file://aip/vocab.json)
- [model_metadata.json](file://aip/model/model_metadata.json)
- [train_lstm.py](file://aip/train_lstm.py)
- [suggest.py](file://aip/suggest.py)

**Section sources**
- [train_lstm.py](file://aip/train_lstm.py)
- [tokenizer.py](file://aip/tokenizer.py)
- [suggest.py](file://aip/suggest.py)
- [model_metadata.json](file://aip/model/model_metadata.json)
- [vocab.json](file://aip/vocab.json)
- [code_xml_parser.py](file://aip/code_xml_parser.py)
- [pattern_extractor.py](file://aip/pattern_extractor.py)
- [requirements.txt](file://aip/requirements.txt)

## Core Components
- Sequence tokenization: Converts programming blocks into token sequences using a fixed vocabulary.
- LSTM model: Encodes token sequences with an embedding layer, one or more LSTM layers, optional attention, and an output projection to predict next tokens.
- Training loop: Optimizes a cross-entropy loss over predicted next-token distributions with configurable optimizer and learning rate schedule.
- Inference: Generates suggestions by sampling or greedy decoding conditioned on the current token sequence.

Key responsibilities:
- Tokenizer maps block elements to integer IDs and handles padding/truncation.
- Metadata file stores model configuration (embedding size, hidden sizes, number of layers, dropout, etc.).
- Training script builds the model from metadata, prepares batches, and runs optimization steps.
- Suggestion script loads the trained model and vocabulary to produce contextual completions.

**Section sources**
- [train_lstm.py](file://aip/train_lstm.py)
- [tokenizer.py](file://aip/tokenizer.py)
- [model_metadata.json](file://aip/model/model_metadata.json)
- [suggest.py](file://aip/suggest.py)

## Architecture Overview
The LSTM-based sequence model follows a standard encoder-decoder style where the encoder is an LSTM that processes token embeddings and optionally applies attention to refine context before predicting the next token.

```mermaid
sequenceDiagram
participant Prep as "Data Prep<br/>code_xml_parser.py, pattern_extractor.py"
participant Tok as "Tokenizer<br/>tokenizer.py"
participant Meta as "Metadata<br/>model_metadata.json"
participant Train as "Training<br/>train_lstm.py"
participant Sug as "Suggestion<br/>suggest.py"
Prep->>Tok : "Extract patterns and build sequences"
Tok-->>Train : "Tokenized sequences + vocab"
Meta-->>Train : "Hyperparameters and config"
Train->>Train : "Build LSTM model (embeddings, LSTM layers, attention, output)"
Train->>Train : "Optimize cross-entropy loss"
Train-->>Sug : "Saved model weights and metadata"
Sug->>Tok : "Encode user input to tokens"
Sug->>Train : "Run inference with loaded model"
Sug-->>User : "Top-k suggestions"
```

**Diagram sources**
- [code_xml_parser.py](file://aip/code_xml_parser.py)
- [pattern_extractor.py](file://aip/pattern_extractor.py)
- [tokenizer.py](file://aip/tokenizer.py)
- [model_metadata.json](file://aip/model/model_metadata.json)
- [train_lstm.py](file://aip/train_lstm.py)
- [suggest.py](file://aip/suggest.py)

## Detailed Component Analysis

### Tokenization Pipeline
- Vocabulary management: A static vocabulary file defines token-to-ID mappings and special tokens (e.g., padding, unknown).
- Block parsing: Programming blocks are parsed from XML structures and converted into ordered token sequences.
- Sequence construction: Sequences are padded or truncated to a maximum length; batched inputs are prepared for training and inference.

```mermaid
flowchart TD
Start(["Start"]) --> Parse["Parse XML blocks"]
Parse --> Extract["Extract patterns"]
Extract --> MapTokens["Map tokens to IDs via vocab"]
MapTokens --> PadTrunc["Pad/Truncate to max_len"]
PadTrunc --> Batches["Create batches"]
Batches --> End(["End"])
```

**Diagram sources**
- [code_xml_parser.py](file://aip/code_xml_parser.py)
- [pattern_extractor.py](file://aip/pattern_extractor.py)
- [tokenizer.py](file://aip/tokenizer.py)
- [vocab.json](file://aip/vocab.json)

**Section sources**
- [tokenizer.py](file://aip/tokenizer.py)
- [vocab.json](file://aip/vocab.json)
- [code_xml_parser.py](file://aip/code_xml_parser.py)
- [pattern_extractor.py](file://aip/pattern_extractor.py)

### LSTM Model Topology
- Input embedding layer: Maps token IDs to dense vectors.
- LSTM cells: One or more stacked LSTM layers with specified hidden dimensions and dropout.
- Attention mechanism: Optional attention over LSTM outputs to weight relevant time steps for prediction.
- Output projection: Linear layer mapping final hidden states to logits over the vocabulary; softmax applied during inference.

```mermaid
classDiagram
class Embedding {
+input_dim
+output_dim
+forward(tokens)
}
class LSTMLayer {
+hidden_size
+num_layers
+dropout
+forward(embeds)
}
class Attention {
+attn_dim
+forward(hiddens)
}
class OutputProjection {
+vocab_size
+forward(context)
}
class LSTMModel {
+embedding
+lstm
+attention
+projection
+forward(tokens)
}
LSTMModel --> Embedding : "uses"
LSTMModel --> LSTMLayer : "stacked"
LSTMModel --> Attention : "optional"
LSTMModel --> OutputProjection : "maps to vocab"
```

**Diagram sources**
- [train_lstm.py](file://aip/train_lstm.py)
- [model_metadata.json](file://aip/model/model_metadata.json)

**Section sources**
- [train_lstm.py](file://aip/train_lstm.py)
- [model_metadata.json](file://aip/model/model_metadata.json)

### Training Methodology
- Loss function: Cross-entropy loss between predicted next-token distributions and ground-truth tokens.
- Optimization: Configurable optimizer (e.g., Adam) with learning rate scheduling and gradient clipping.
- Batch processing: Padded sequences are masked to ignore padding positions when computing loss.
- Checkpointing: Periodic saving of model weights and metadata for resuming training and deployment.

```mermaid
flowchart TD
TStart(["Training Start"]) --> LoadMeta["Load metadata and vocab"]
LoadMeta --> BuildModel["Build LSTM model"]
BuildModel --> PrepareData["Prepare tokenized batches"]
PrepareData --> Forward["Forward pass + attention"]
Forward --> ComputeLoss["Compute cross-entropy loss"]
ComputeLoss --> Backward["Backpropagation"]
Backward --> Update["Optimizer step + LR schedule"]
Update --> Checkpoint{"Checkpoint?"}
Checkpoint --> |Yes| Save["Save weights + metadata"]
Checkpoint --> |No| NextBatch["Next batch"]
Save --> NextBatch
NextBatch --> End(["Training End"])
```

**Diagram sources**
- [train_lstm.py](file://aip/train_lstm.py)
- [model_metadata.json](file://aip/model/model_metadata.json)

**Section sources**
- [train_lstm.py](file://aip/train_lstm.py)
- [model_metadata.json](file://aip/model/model_metadata.json)

### Inference and Suggestions
- Context encoding: Current token sequence is encoded through the same embedding and LSTM layers.
- Decoding strategies: Greedy selection or top-k sampling to generate next tokens iteratively.
- Attention usage: If enabled, attention weights highlight influential past tokens for better suggestions.
- Post-processing: Filter out special tokens and map IDs back to human-readable block names.

```mermaid
sequenceDiagram
participant User as "User"
participant Sug as "suggest.py"
participant Tok as "tokenizer.py"
participant Model as "train_lstm.py (loaded)"
User->>Sug : "Provide partial code"
Sug->>Tok : "Encode to token IDs"
Sug->>Model : "Run forward pass"
Model-->>Sug : "Logits / sampled tokens"
Sug->>Sug : "Apply decoding strategy"
Sug-->>User : "Return suggestions"
```

**Diagram sources**
- [suggest.py](file://aip/suggest.py)
- [tokenizer.py](file://aip/tokenizer.py)
- [train_lstm.py](file://aip/train_lstm.py)

**Section sources**
- [suggest.py](file://aip/suggest.py)
- [tokenizer.py](file://aip/tokenizer.py)
- [train_lstm.py](file://aip/train_lstm.py)

## Dependency Analysis
External dependencies include deep learning frameworks and utility libraries required for data processing, model building, and training. The dependency list is defined in the project’s requirements file.

```mermaid
graph TB
REQ["requirements.txt"]
DL["Deep Learning Framework"]
NP["Numerical Libraries"]
IO["I/O Utilities"]
TOK["tokenizer.py"]
TRAIN["train_lstm.py"]
SUG["suggest.py"]
REQ --> DL
REQ --> NP
REQ --> IO
TOK --> NP
TRAIN --> DL
TRAIN --> NP
SUG --> TOK
SUG --> TRAIN
```

**Diagram sources**
- [requirements.txt](file://aip/requirements.txt)
- [tokenizer.py](file://aip/tokenizer.py)
- [train_lstm.py](file://aip/train_lstm.py)
- [suggest.py](file://aip/suggest.py)

**Section sources**
- [requirements.txt](file://aip/requirements.txt)

## Performance Considerations
- Sequence length: Longer sequences increase memory and compute costs; consider truncation policies.
- Hidden dimensionality: Larger hidden sizes improve expressiveness but raise memory usage and training time.
- Number of layers: Stacking more LSTM layers increases capacity at the cost of slower training and inference.
- Attention overhead: Attention adds computation proportional to sequence length; beneficial for long-range dependencies.
- Batch size: Larger batches improve throughput but require more GPU memory; tune based on hardware constraints.
- Precision: Mixed precision can reduce memory footprint and speed up training if supported by the framework.

[No sources needed since this section provides general guidance]

## Troubleshooting Guide
Common issues and resolutions:
- Vocabulary mismatch: Ensure the same vocab.json used during training is available at inference time.
- Padding errors: Verify that sequences are consistently padded/truncated to the configured max length.
- Out-of-memory: Reduce batch size, hidden size, or sequence length; enable mixed precision if available.
- Slow training: Use larger batch sizes, optimize dataloader workers, and ensure efficient I/O.
- Poor suggestions: Increase model capacity (hidden size/layers), add attention, or expand training data diversity.

**Section sources**
- [tokenizer.py](file://aip/tokenizer.py)
- [train_lstm.py](file://aip/train_lstm.py)
- [suggest.py](file://aip/suggest.py)

## Conclusion
The LSTM architecture in NewCatroid provides a robust foundation for code pattern recognition and context-aware suggestions. By combining structured tokenization, configurable LSTM layers, optional attention, and a clear training/inference pipeline, it supports scalable development of intelligent coding assistance features. Proper tuning of hyperparameters and careful resource management are key to achieving desired performance.

[No sources needed since this section summarizes without analyzing specific files]

## Appendices

### Hyperparameter Specifications
Typical hyperparameters are stored in the model metadata file and may include:
- Embedding dimension
- LSTM hidden size
- Number of LSTM layers
- Dropout rates
- Maximum sequence length
- Vocabulary size
- Attention settings (enabled/disabled, attention dimension)
- Training options (optimizer type, learning rate, batch size, epochs)

**Section sources**
- [model_metadata.json](file://aip/model/model_metadata.json)

### Practical Examples

#### Configuration Example
- Define embedding size, hidden size, number of layers, and attention flags in the metadata file.
- Set vocabulary and max sequence length consistent across training and inference.

**Section sources**
- [model_metadata.json](file://aip/model/model_metadata.json)
- [vocab.json](file://aip/vocab.json)

#### Training Procedure
- Prepare tokenized sequences from XML blocks using the parser and extractor.
- Build the LSTM model from metadata and run the training loop with cross-entropy loss.
- Save checkpoints periodically for later evaluation and deployment.

**Section sources**
- [code_xml_parser.py](file://aip/code_xml_parser.py)
- [pattern_extractor.py](file://aip/pattern_extractor.py)
- [tokenizer.py](file://aip/tokenizer.py)
- [train_lstm.py](file://aip/train_lstm.py)

#### Inference Patterns
- Encode user input into token IDs using the tokenizer.
- Run the loaded model to obtain logits or sample next tokens.
- Apply decoding strategies (greedy or top-k) and map results back to block names.

**Section sources**
- [tokenizer.py](file://aip/tokenizer.py)
- [suggest.py](file://aip/suggest.py)
- [train_lstm.py](file://aip/train_lstm.py)