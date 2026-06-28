"""
Pattern Extractor — mines n-gram brick sequences from parsed NewCatroid projects.
Produces variable-order Markov model for Android AI assistant.
"""

import json
from collections import Counter, defaultdict
from pathlib import Path


class PatternDatabase:
    def __init__(self):
        self.ngrams: dict[int, dict[str, dict[str, float]]] = {}
        self.script_specific_ngrams: dict[str, dict[int, dict[str, dict[str, float]]]] = {}
        self.first_bricks: Counter = Counter()
        self.total_scripts = 0
        self.total_bricks = 0
        self.unique_brick_types: set = set()

    def to_dict(self) -> dict:
        return {
            'model_version': 2,
            'ngrams': {
                str(n): data for n, data in self.ngrams.items()
            },
            'script_specific': {
                stype: {str(n): data for n, data in ndata.items()}
                for stype, ndata in self.script_specific_ngrams.items()
            },
            'first_bricks': dict(self.first_bricks.most_common(20)),
            'stats': {
                'total_scripts': self.total_scripts,
                'total_bricks': self.total_bricks,
                'unique_brick_types': len(self.unique_brick_types),
            }
        }


def _normalize_counts(counts: dict[str, int]) -> dict[str, float]:
    total = sum(counts.values())
    if total == 0:
        return {}
    return {k: round(v / total, 4) for k, v in sorted(counts.items(), key=lambda x: -x[1])}


def _build_ngram_counts(bricks: list[str], max_n: int = 5) -> dict[int, dict[str, dict[str, int]]]:
    ngram_counts: dict[int, dict[str, dict[str, int]]] = {}
    for n in range(2, max_n + 1):
        ngram_counts[n] = defaultdict(lambda: defaultdict(int))

    for i in range(len(bricks)):
        for n in range(2, max_n + 1):
            if i + n > len(bricks):
                continue
            context = tuple(bricks[i:i + n - 1])
            target = bricks[i + n - 1]
            context_key = '|'.join(context)
            ngram_counts[n][context_key][target] += 1

    return ngram_counts


def mine_patterns(json_path: str) -> PatternDatabase:
    with open(json_path, 'r', encoding='utf-8') as f:
        projects = json.load(f)

    db = PatternDatabase()
    all_ngram_counts: dict[int, dict[str, dict[str, int]]] = {}
    for n in range(2, 6):
        all_ngram_counts[n] = defaultdict(lambda: defaultdict(int))

    script_ngram_counts: dict[str, dict[int, dict[str, dict[str, int]]]] = defaultdict(
        lambda: {n: defaultdict(lambda: defaultdict(int)) for n in [2, 3, 4, 5]}
    )

    for proj in projects:
        for scene in proj.get('scenes', []):
            for sprite in scene.get('sprites', []):
                for script in sprite.get('scripts', []):
                    script_type = script.get('type', 'Unknown')
                    bricks = script.get('bricks', [])
                    if not bricks:
                        continue

                    db.total_scripts += 1
                    db.total_bricks += len(bricks)

                    brick_types = [b.get('type', '') for b in bricks if b.get('type', '')]
                    db.unique_brick_types.update(brick_types)

                    if not brick_types:
                        continue

                    db.first_bricks[brick_types[0]] += 1

                    for i in range(len(brick_types)):
                        for n in range(2, 6):
                            if i + n > len(brick_types):
                                continue
                            context = tuple(brick_types[i:i + n - 1])
                            target = brick_types[i + n - 1]
                            context_key = '|'.join(context)
                            all_ngram_counts[n][context_key][target] += 1
                            script_ngram_counts[script_type][n][context_key][target] += 1

    for n in range(2, 6):
        raw = all_ngram_counts[n]
        db.ngrams[n] = {
            ctx: _normalize_counts(counts)
            for ctx, counts in raw.items()
        }

    for stype, ndata in script_ngram_counts.items():
        db.script_specific_ngrams[stype] = {}
        for n in range(2, 6):
            raw = ndata[n]
            if raw:
                db.script_specific_ngrams[stype][n] = {
                    ctx: _normalize_counts(counts)
                    for ctx, counts in raw.items()
                }

    return db


def export_patterns(db: PatternDatabase, output_path: str):
    data = db.to_dict()
    with open(output_path, 'w', encoding='utf-8') as f:
        json.dump(data, f, indent=2, ensure_ascii=False)
    print(f"Patterns exported to {output_path}")
    print(f"  Scripts: {db.total_scripts}, Bricks: {db.total_bricks}")
    print(f"  Unique brick types: {len(db.unique_brick_types)}")
    for n in [2, 3, 4, 5]:
        print(f"  {n}-gram contexts: {len(db.ngrams.get(n, {}))}")


def export_combined(db: PatternDatabase, extra_patterns_path: str, output_path: str):
    import copy
    with open(extra_patterns_path, 'r', encoding='utf-8') as f:
        extra = json.load(f)
    combined = copy.deepcopy(extra)
    combined.update(db.to_dict())
    with open(output_path, 'w', encoding='utf-8') as f:
        json.dump(combined, f, indent=2, ensure_ascii=False)
    print(f"Combined patterns exported to {output_path}")


if __name__ == '__main__':
    import sys
    input_json = sys.argv[1] if len(sys.argv) > 1 else 'training_data/projects.json'
    output_json = sys.argv[2] if len(sys.argv) > 2 else 'model/patterns.json'
    print(f"Mining n-gram patterns from {input_json}...")
    db = mine_patterns(input_json)
    export_patterns(db, output_json)
    print("Done.")
