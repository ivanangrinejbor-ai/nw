#!/usr/bin/env python3
"""train_transformer.py — Transformer brick predictor (optimized).

Architecture: Decoder-only Transformer (GPT-style)
Оптимизации:
  - causal mask создаётся один раз в __init__
  - mixed precision (torch.cuda.amp)
  - DataLoader с num_workers > 0
  - torch.compile (если PyTorch >= 2.0)

Usage:
  python train_transformer.py
  python train_transformer.py --window 768 --epochs 20 --batch-size 4
"""

import argparse
import json
import math
import os
import sys
import time
from dataclasses import dataclass

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

import numpy as np
import torch
import torch.nn as nn
import torch.nn.functional as F
from torch.utils.data import Dataset, DataLoader

from code_xml_parser import scan_projects, export_to_json
from tokenizer import BrickTokenizer


# ---------- Transformer Model ----------

@dataclass
class TransformerConfig:
    vocab_size: int = 1000
    d_model: int = 128
    n_heads: int = 4
    n_layers: int = 4
    d_ff: int = 512
    dropout: float = 0.1
    max_seq_len: int = 768
    pad_id: int = 0


class PositionalEncoding(nn.Module):
    def __init__(self, d_model: int, max_len: int = 768):
        super().__init__()
        pe = torch.zeros(max_len, d_model)
        position = torch.arange(0, max_len, dtype=torch.float).unsqueeze(1)
        div_term = torch.exp(torch.arange(0, d_model, 2).float() * -(math.log(10000.0) / d_model))
        pe[:, 0::2] = torch.sin(position * div_term)
        pe[:, 1::2] = torch.cos(position * div_term)
        self.register_buffer('pe', pe.unsqueeze(0))

    def forward(self, x):
        return x + self.pe[:, :x.size(1), :]


class TransformerDecoderBlock(nn.Module):
    def __init__(self, d_model, n_heads, d_ff, dropout):
        super().__init__()
        self.self_attn = nn.MultiheadAttention(d_model, n_heads, dropout=dropout, batch_first=True)
        self.norm1 = nn.LayerNorm(d_model)
        self.norm2 = nn.LayerNorm(d_model)
        self.ff = nn.Sequential(
            nn.Linear(d_model, d_ff),
            nn.GELU(),
            nn.Dropout(dropout),
            nn.Linear(d_ff, d_model),
            nn.Dropout(dropout),
        )
        self.dropout = nn.Dropout(dropout)

    def forward(self, x, causal_mask):
        attn_out, _ = self.self_attn(x, x, x, attn_mask=causal_mask, is_causal=False)
        x = self.norm1(x + self.dropout(attn_out))
        ff_out = self.ff(x)
        x = self.norm2(x + ff_out)
        return x


class BrickTransformer(nn.Module):
    def __init__(self, config: TransformerConfig):
        super().__init__()
        self.config = config
        self.token_embedding = nn.Embedding(config.vocab_size, config.d_model, padding_idx=config.pad_id)
        self.pos_encoding = PositionalEncoding(config.d_model, config.max_seq_len)
        self.dropout = nn.Dropout(config.dropout)

        self.blocks = nn.ModuleList([
            TransformerDecoderBlock(config.d_model, config.n_heads, config.d_ff, config.dropout)
            for _ in range(config.n_layers)
        ])

        self.ln_f = nn.LayerNorm(config.d_model)
        self.head = nn.Linear(config.d_model, config.vocab_size)

        self.register_buffer('causal_mask',
            torch.triu(torch.full((config.max_seq_len, config.max_seq_len), float('-inf')), diagonal=1))

        self._init_weights()

    def _init_weights(self):
        for p in self.parameters():
            if p.dim() > 1:
                nn.init.normal_(p, mean=0.0, std=0.02)

    def forward(self, x):
        mask = self.causal_mask[:x.size(1), :x.size(1)]
        x = self.token_embedding(x)
        x = self.pos_encoding(x)
        x = self.dropout(x)
        for block in self.blocks:
            x = block(x, mask)
        x = self.ln_f(x)
        return self.head(x)


# ---------- Dataset ----------

class BrickDataset(Dataset):
    def __init__(self, projects_json_path, tokenizer, window=768):
        with open(projects_json_path, 'r', encoding='utf-8') as f:
            projects = json.load(f)
        self.inputs = []
        self.targets = []
        for proj in projects:
            seq = tokenizer.build_project_sequence(proj, window * 3)
            if len(seq) <= window:
                continue
            for i in range(window, len(seq)):
                ctx = seq[i - window:i]
                tgt = seq[i]
                if tgt in (0, 3):
                    continue
                self.inputs.append(ctx)
                self.targets.append(seq[i - window + 1 : i + 1])

    def __len__(self):
        return len(self.inputs)

    def __getitem__(self, idx):
        return torch.tensor(self.inputs[idx], dtype=torch.long), torch.tensor(self.targets[idx], dtype=torch.long)


# ---------- Training ----------

def train_epoch(model, dataloader, optimizer, scheduler, device, scaler):
    model.train()
    total_loss = 0.0
    total_correct = 0
    total_tokens = 0
    for x, y in dataloader:
        x, y = x.to(device), y.to(device)
        optimizer.zero_grad()
        with torch.amp.autocast('cuda', enabled=(device.type == 'cuda')):
            logits = model(x)
            loss = F.cross_entropy(logits.view(-1, logits.size(-1)), y.view(-1), ignore_index=0)
        scaler.scale(loss).backward()
        scaler.unscale_(optimizer)
        torch.nn.utils.clip_grad_norm_(model.parameters(), 1.0)
        scaler.step(optimizer)
        scaler.update()
        scheduler.step()
        valid = y != 0
        total_loss += loss.item() * valid.sum().item()
        preds = logits.argmax(-1)
        total_correct += (preds[valid] == y[valid]).sum().item()
        total_tokens += valid.sum().item()
    return total_loss / max(total_tokens, 1), total_correct / max(total_tokens, 1)


def main():
    parser = argparse.ArgumentParser(description='Train Transformer brick predictor')
    parser.add_argument('projects_dir', nargs='?', default='datasets')
    parser.add_argument('--window', type=int, default=768, help='Context window size')
    parser.add_argument('--epochs', type=int, default=20)
    parser.add_argument('--batch-size', type=int, default=4)
    parser.add_argument('--lr', type=float, default=3e-4)
    parser.add_argument('--d-model', type=int, default=128)
    parser.add_argument('--n-layers', type=int, default=4)
    parser.add_argument('--n-heads', type=int, default=4)
    parser.add_argument('--d-ff', type=int, default=512)
    parser.add_argument('--out-dir', default='model')
    parser.add_argument('--json', default='training_data/projects.json')
    parser.add_argument('--compile', action='store_true', help='Use torch.compile')
    args = parser.parse_args()

    device = torch.device('cuda' if torch.cuda.is_available() else 'cpu')
    print(f'Device: {device}')
    if device.type == 'cuda':
        print(f'GPU: {torch.cuda.get_device_name(0)}')
        print(f'Memory: {torch.cuda.get_device_properties(0).total_memory / 1024**3:.1f} GB')

    os.makedirs(args.out_dir, exist_ok=True)
    os.makedirs(os.path.dirname(args.json), exist_ok=True)

    # Step 1: Parse projects
    print('=' * 60)
    print('STEP 1: Loading project data')
    print('=' * 60)
    if os.path.exists(args.json):
        print(f'  Using existing {args.json}')
    else:
        print(f'  Scanning {args.projects_dir} for .code.xml files...')
        projects = scan_projects(args.projects_dir)
        if not projects:
            print('No projects found. Exiting.')
            return
        export_to_json(projects, args.json)

    with open(args.json, 'r', encoding='utf-8') as f:
        num_projects = len(json.load(f))
    print(f'  Projects: {num_projects}')

    # Step 2: Build vocabulary
    print('\n' + '=' * 60)
    print('STEP 2: Building vocabulary')
    print('=' * 60)
    tokenizer = BrickTokenizer()
    tokenizer.build_vocab(args.json, min_freq=1)
    tokenizer.save(os.path.join(args.out_dir, 'vocab.json'))
    vocab_size = tokenizer.vocab_size
    print(f'  Vocabulary size: {vocab_size}')

    # Step 3: Create dataset
    print('\n' + '=' * 60)
    print(f'STEP 3: Creating dataset (window={args.window})')
    print('=' * 60)
    t0 = time.time()
    dataset = BrickDataset(args.json, tokenizer, window=args.window)
    if len(dataset) == 0:
        print('No training data generated!')
        return
    print(f'  Training pairs: {len(dataset)}')
    print(f'  Dataset build time: {time.time() - t0:.0f}s')

    nw = min(4, os.cpu_count() or 1)
    dataloader = DataLoader(dataset, batch_size=args.batch_size, shuffle=True,
                            num_workers=nw, pin_memory=True,
                            persistent_workers=(nw > 0), prefetch_factor=2 if nw > 0 else None)
    print(f'  Batches: {len(dataloader)}, workers: {nw}')

    # Step 4: Build model
    print('\n' + '=' * 60)
    print('STEP 4: Building Transformer')
    print('=' * 60)
    config = TransformerConfig(
        vocab_size=vocab_size,
        d_model=args.d_model,
        n_heads=args.n_heads,
        n_layers=args.n_layers,
        d_ff=args.d_ff,
        max_seq_len=args.window,
    )
    model = BrickTransformer(config).to(device)
    _model_raw = model
    if args.compile and hasattr(torch, 'compile'):
        try:
            model = torch.compile(model)
            print('  Using torch.compile')
        except Exception as e:
            print(f'  torch.compile skipped: {e}')
    n_params = sum(p.numel() for p in model.parameters() if p.requires_grad)
    print(f'  Parameters: {n_params:,}')
    print(f'  Architecture: {args.n_layers} layers, {args.n_heads} heads, {args.d_model} dim')

    # Step 5: Train
    print('\n' + '=' * 60)
    print('STEP 5: Training')
    print('=' * 60)
    optimizer = torch.optim.AdamW(model.parameters(), lr=args.lr, weight_decay=0.1)
    total_steps = len(dataloader) * args.epochs
    scheduler = torch.optim.lr_scheduler.OneCycleLR(
        optimizer, max_lr=args.lr, total_steps=total_steps, pct_start=0.1
    )
    scaler = torch.amp.GradScaler('cuda', enabled=(device.type == 'cuda'))

    t0 = time.time()
    best_loss = float('inf')
    for epoch in range(args.epochs):
        loss, acc = train_epoch(model, dataloader, optimizer, scheduler, device, scaler)
        elapsed = time.time() - t0
        lr_now = scheduler.get_last_lr()[0]
        print(f'  Epoch {epoch+1:2d}/{args.epochs} | loss={loss:.4f} | acc={acc:.4f} | time={elapsed:.0f}s | lr={lr_now:.2e}')

        if loss < best_loss:
            best_loss = loss
            torch.save(model.state_dict(), os.path.join(args.out_dir, 'transformer_model.pt'))
            print(f'    -> saved checkpoint')

    train_time = time.time() - t0

    # Step 6: Save final model
    print('\n' + '=' * 60)
    print('STEP 6: Saving model')
    print('=' * 60)
    model_path = os.path.join(args.out_dir, 'transformer_model.pt')
    torch.save(model.state_dict(), model_path)
    model_size_mb = os.path.getsize(model_path) / (1024 * 1024)
    print(f'  Model: {model_path} ({model_size_mb:.2f} MB)')

    # Step 7: Save stats
    stats = {
        'model_type': 'transformer',
        'vocab_size': vocab_size,
        'window': args.window,
        'd_model': args.d_model,
        'n_layers': args.n_layers,
        'n_heads': args.n_heads,
        'd_ff': args.d_ff,
        'n_params': n_params,
        'training_pairs': len(dataset),
        'epochs': args.epochs,
        'batch_size': args.batch_size,
        'train_time_seconds': round(train_time, 1),
        'best_loss': round(best_loss, 4),
        'final_loss': round(loss, 4),
        'final_accuracy': round(acc, 4),
        'compile': args.compile,
        'mixed_precision': True,
    }
    with open(os.path.join(args.out_dir, 'training_stats.json'), 'w', encoding='utf-8') as f:
        json.dump(stats, f, indent=2)

    metadata = {
        'model_version': 4,
        'model_type': 'transformer',
        'vocab_size': vocab_size,
        'window': args.window,
        'structural_tokens': type(tokenizer).STRUCTURAL_TOKENS,
        'brick_types': list(tokenizer.word2id.keys()),
    }
    with open(os.path.join(args.out_dir, 'model_metadata.json'), 'w', encoding='utf-8') as f:
        json.dump(metadata, f, indent=2)

    print(f'\n{"=" * 60}')
    print('  TRAINING COMPLETE')
    print(f'{"=" * 60}')
    print(f'  Model: {model_path} ({model_size_mb:.2f} MB)')
    print(f'  Stats: loss={best_loss:.4f}, acc={acc:.4f}')

    # Export to ONNX (wrapper: float->long cast + last-token-only output)
    try:
        _model_raw.eval()

        class OnnxWrapper(nn.Module):
            def __init__(self, m):
                super().__init__()
                self.inner = m
            def forward(self, x):
                return self.inner(x.to(torch.long))[:, -1, :]

        wrapper = OnnxWrapper(_model_raw)
        example = torch.zeros((1, args.window), dtype=torch.float32, device=device)
        onnx_path = os.path.join(args.out_dir, 'transformer_model.onnx')
        torch.onnx.export(
            wrapper, example, onnx_path,
            input_names=['input_ids'],
            output_names=['logits'],
            opset_version=14, dynamo=False,
        )
        print(f'  ONNX: {onnx_path} ({os.path.getsize(onnx_path) / 1024 / 1024:.2f} MB)')
    except Exception as e:
        print(f'  ONNX export skipped: {e}')

    print(f'\n  Для Android: трансформер_model.onnx + vocab.json в assets/')
    print(f'  ONNX Runtime уже встроен — используй блоки LoadNN/PredictNN')


if __name__ == '__main__':
    main()
