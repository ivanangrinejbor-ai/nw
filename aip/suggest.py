#!/usr/bin/env python3
"""
suggest.py — Suggestion engine for v2 n-gram patterns.json.
Loads n-gram pattern database and suggests next bricks for a given context.
"""

import json
import sys
import os
from collections import Counter

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from code_xml_parser import parse_project


def load_patterns(path: str) -> dict:
    with open(path, 'r', encoding='utf-8') as f:
        return json.load(f)


def suggest_next(brick_sequence: list[str], patterns: dict, top_n: int = 3) -> list[dict]:
    """Suggest next brick using n-grams (5-gram → 4-gram → 3-gram → 2-gram fallback)."""
    ngrams = patterns.get('ngrams', {})
    suggestions = []
    seen = set()

    for order in range(5, 1, -1):
        key = str(order)
        if key not in ngrams:
            continue
        if len(brick_sequence) < order - 1:
            continue
        ctx = brick_sequence[-(order - 1):]
        ctx_key = '|'.join(ctx)
        if ctx_key in ngrams[key]:
            for next_type, prob in sorted(ngrams[key][ctx_key].items(), key=lambda x: -x[1]):
                if next_type not in seen:
                    seen.add(next_type)
                    suggestions.append({
                        'brick_type': next_type,
                        'confidence': int(prob * 100),
                        'reason': f'{order}-gram: after {" → ".join(ctx)}'
                    })
                    if len(suggestions) >= top_n:
                        return suggestions
    return suggestions


def suggest_for_script(script_type: str, patterns: dict, top_n: int = 5) -> list[dict]:
    """Suggest common bricks for a script type from first_bricks."""
    script_specific = patterns.get('script_specific', {})
    first_bricks = patterns.get('first_bricks', {})
    suggestions = []

    if script_type in script_specific:
        for order in range(5, 1, -1):
            key = str(order)
            order_data = script_specific[script_type].get(key, {})
            for ctx_key, next_map in order_data.items():
                for next_type, prob in sorted(next_map.items(), key=lambda x: -x[1]):
                    suggestions.append({
                        'brick_type': next_type,
                        'confidence': int(prob * 100),
                        'reason': f'Script {script_type} {order}-gram'
                    })
                    if len(suggestions) >= top_n:
                        return suggestions

    if first_bricks:
        for bt, count in sorted(first_bricks.items(), key=lambda x: -x[1])[:top_n]:
            if not any(s['brick_type'] == bt for s in suggestions):
                suggestions.append({
                    'brick_type': bt,
                    'confidence': count,
                    'reason': f'Common first brick'
                })

    return suggestions[:top_n]


def analyze_project(project_json_path: str, patterns_path: str):
    patterns = load_patterns(patterns_path)
    proj = parse_project(project_json_path)
    if not proj:
        print("Failed to parse project.")
        return

    print(f"Project: {proj.name}")
    print(f"Scenes: {len(proj.scenes)}")
    print()

    for scene in proj.scenes:
        for sprite in scene.sprites:
            for script in sprite.scripts:
                print(f"  [{script.script_type}] {sprite.name}")
                brick_list = [b.brick_type for b in script.bricks]
                for i, brick in enumerate(script.bricks):
                    print(f"    - {brick.brick_type}")
                    if i < len(script.bricks) - 1:
                        continue
                    ctx = brick_list[:i + 1]
                    suggestions = suggest_next(ctx, patterns)
                    for s in suggestions:
                        print(f"      -> {s['brick_type']} ({s['reason']}, conf={s['confidence']})")
                print()


# ---------- CLI ----------
if __name__ == '__main__':
    if len(sys.argv) < 2:
        print("Usage:")
        print("  python suggest.py <project_code.xml> [patterns.json]")
        print("  python suggest.py --list <brick1,brick2,...> [patterns.json]")
        sys.exit(1)

    patterns_path = 'model/patterns.json'
    for i, arg in enumerate(sys.argv):
        if arg.endswith('.json') and not arg.startswith('--'):
            patterns_path = arg
            break
    if not os.path.exists(patterns_path):
        patterns_path = 'model/patterns.json'

    if sys.argv[1] == '--list':
        bricks = sys.argv[2].split(',') if len(sys.argv) > 2 else []
        patterns = load_patterns(patterns_path)
        suggestions = suggest_next(bricks, patterns)
        for s in suggestions:
            print(f"{s['brick_type']} | {s['reason']} | conf={s['confidence']}")
    else:
        analyze_project(sys.argv[1], patterns_path)
