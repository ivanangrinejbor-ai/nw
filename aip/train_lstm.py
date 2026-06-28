#!/usr/bin/env python3
"""
train_lstm.py — Train an LSTM model for brick prediction with 500-token context.
Understands full project context by reading all objects' scripts.

Output:
  model/model.tflite     — quantized TFLite model (~500KB-2MB)
  model/vocab.json       — tokenizer vocabulary
  model/training_stats.json
"""

import argparse
import json
import os
import sys
import time
from pathlib import Path

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
os.environ['TF_CPP_MIN_LOG_LEVEL'] = '3'  # suppress TF warnings

from code_xml_parser import scan_projects, export_to_json
from tokenizer import BrickTokenizer

import numpy as np

# Lazy TF import (after pip install)
def import_tf():
    import tensorflow as tf
    return tf


def main():
    parser = argparse.ArgumentParser(description='Train LSTM brick predictor')
    parser.add_argument('projects_dir', nargs='?', default='datasets',
                        help='Folder with .code.xml files (default: datasets/)')
    parser.add_argument('--window', type=int, default=500, help='Context window size (default: 500)')
    parser.add_argument('--epochs', type=int, default=10, help='Training epochs (default: 10)')
    parser.add_argument('--batch-size', type=int, default=64)
    parser.add_argument('--embed-dim', type=int, default=64)
    parser.add_argument('--lstm-units', type=int, default=128)
    parser.add_argument('--out-dir', default='model', help='Output directory')
    parser.add_argument('--json', default='training_data/projects.json')
    args = parser.parse_args()

    tf = import_tf()
    os.makedirs(args.out_dir, exist_ok=True)
    os.makedirs(os.path.dirname(args.json), exist_ok=True)

    # Step 1: Parse projects (or load existing)
    print("=" * 60)
    print("STEP 1: Loading project data")
    print("=" * 60)
    if os.path.exists(args.json):
        print(f"  Using existing {args.json}")
        with open(args.json, 'r', encoding='utf-8') as f:
            projects = json.load(f)
        print(f"  Loaded {len(projects)} projects")
    else:
        print(f"  Scanning {args.projects_dir} for .code.xml files...")
        projects = scan_projects(args.projects_dir)
        if not projects:
            print("No projects found. Exiting.")
            return
        export_to_json(projects, args.json)

    # Step 2: Build vocabulary
    print("\n" + "=" * 60)
    print("STEP 2: Building vocabulary")
    print("=" * 60)
    tokenizer = BrickTokenizer()
    tokenizer.build_vocab(args.json, min_freq=1)
    tokenizer.save(os.path.join(args.out_dir, 'vocab.json'))
    vocab_size = tokenizer.vocab_size
    print(f"  Vocabulary size: {vocab_size}")

    # Step 3: Generate training data
    print("\n" + "=" * 60)
    print("STEP 3: Generating training sequences")
    print("=" * 60)
    window = args.window
    inputs, targets = [], []
    pair_count = 0
    for ctx, tgt in tokenizer.generate_training_pairs(args.json, window):
        inputs.append(ctx)
        targets.append(tgt)
        pair_count += 1
        if pair_count % 10000 == 0:
            print(f"  Generated {pair_count} pairs...")

    if not inputs:
        print("No training data generated!")
        return

    X = np.array(inputs, dtype=np.int32)
    y = np.array(targets, dtype=np.int32)
    print(f"  Total training pairs: {len(X)}")
    print(f"  Input shape: {X.shape}")
    print(f"  Vocabulary: {vocab_size} types")

    # Step 4: Build model
    print("\n" + "=" * 60)
    print("STEP 4: Building LSTM model")
    print("=" * 60)
    model = tf.keras.Sequential([
        tf.keras.layers.Embedding(vocab_size, args.embed_dim, input_length=window,
                                  mask_zero=True),
        tf.keras.layers.LSTM(args.lstm_units, return_sequences=False, dropout=0.2),
        tf.keras.layers.Dense(vocab_size, activation='softmax')
    ])
    model.compile(
        optimizer='adam',
        loss='sparse_categorical_crossentropy',
        metrics=['accuracy']
    )
    model.summary()

    # Step 5: Train
    print("\n" + "=" * 60)
    print("STEP 5: Training")
    print("=" * 60)
    t0 = time.time()
    history = model.fit(
        X, y,
        epochs=args.epochs,
        batch_size=args.batch_size,
        validation_split=0.1,
        verbose=1
    )
    train_time = time.time() - t0
    print(f"  Training time: {train_time:.1f}s")

    # Step 6: Save stats
    stats = {
        'vocab_size': vocab_size,
        'window': window,
        'training_pairs': len(X),
        'epochs': args.epochs,
        'train_time_seconds': round(train_time, 1),
        'final_accuracy': round(float(history.history['accuracy'][-1]), 4),
        'final_loss': round(float(history.history['loss'][-1]), 4)
    }
    with open(os.path.join(args.out_dir, 'training_stats.json'), 'w') as f:
        json.dump(stats, f, indent=2)
    print(f"\n  Stats: acc={stats['final_accuracy']}, loss={stats['final_loss']}")

    # Step 7: Convert to TFLite (dynamic range quantization for size)
    print("\n" + "=" * 60)
    print("STEP 6: Converting to TensorFlow Lite")
    print("=" * 60)
    converter = tf.lite.TFLiteConverter.from_keras_model(model)
    converter.optimizations = [tf.lite.Optimize.DEFAULT]
    tflite_model = converter.convert()

    tflite_path = os.path.join(args.out_dir, 'model.tflite')
    with open(tflite_path, 'wb') as f:
        f.write(tflite_model)
    model_size_mb = len(tflite_model) / (1024 * 1024)
    print(f"  TFLite model: {tflite_path} ({model_size_mb:.2f} MB)")

    # Step 8: Save a combined metadata file for Android
    metadata = {
        'vocab_size': vocab_size,
        'window': window,
        'model_version': 3,
        'brick_types': list(tokenizer.word2id.keys())
    }
    with open(os.path.join(args.out_dir, 'model_metadata.json'), 'w') as f:
        json.dump(metadata, f, indent=2)

    print(f"\n{'=' * 60}")
    print("  TRAINING COMPLETE")
    print(f"{'=' * 60}")
    print(f"  Model: {tflite_path} ({model_size_mb:.2f} MB)")
    print(f"  Vocab: {os.path.join(args.out_dir, 'vocab.json')}")
    print(f"  Stats: {os.path.join(args.out_dir, 'training_stats.json')}")
    print(f"\n  To deploy: copy {tflite_path} and vocab.json to Android assets/")


if __name__ == '__main__':
    main()
