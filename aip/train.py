#!/usr/bin/env python3
"""
train.py — Full training pipeline for AI Project Assistant.

Steps:
  1. Scan datasets/ for .code.xml / code.xml files
  2. Parse all projects into structured JSON
  3. Mine n-gram patterns (sequences)
  4. Export pattern database for Android consumption

Usage:
  python train.py                          # uses datasets/ folder
  python train.py <path>                   # custom folder
  python train.py --out-dir ../catroid/src/main/assets  # write directly into app
"""

import argparse
import os
import sys
from pathlib import Path

# Add parent to path
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from code_xml_parser import scan_projects, export_to_json
from pattern_extractor import mine_patterns, export_patterns


def main():
    parser = argparse.ArgumentParser(description='Train AI Project Assistant on .code.xml projects')
    parser.add_argument('projects_dir', nargs='?', default='datasets',
                        help='Folder with .code.xml files (default: datasets/)')
    parser.add_argument('--out-dir', default='model', help='Output directory for patterns.json')
    parser.add_argument('--json', default='training_data/projects.json', help='Intermediate JSON path')
    args = parser.parse_args()

    os.makedirs(args.out_dir, exist_ok=True)
    os.makedirs(os.path.dirname(args.json), exist_ok=True)

    # Step 1-2: Scan & parse
    print("=" * 60)
    print("STEP 1: Scanning and parsing .code.xml files")
    print("=" * 60)
    projects = scan_projects(args.projects_dir)
    if not projects:
        print("No projects found. Exiting.")
        return

    # Step 3: Export to JSON
    print("\n" + "=" * 60)
    print("STEP 2: Exporting to JSON")
    print("=" * 60)
    export_to_json(projects, args.json)

    # Step 4: Mine patterns
    print("\n" + "=" * 60)
    print("STEP 3: Mining patterns")
    print("=" * 60)
    db = mine_patterns(args.json)

    # Step 5: Export patterns
    print("\n" + "=" * 60)
    print("STEP 4: Exporting patterns for Android")
    print("=" * 60)
    patterns_path = os.path.join(args.out_dir, 'patterns.json')
    export_patterns(db, patterns_path)

    print("\n✅ Training complete!")
    print(f"   Patterns → {patterns_path}")
    print(f"   Raw JSON → {args.json}")


if __name__ == '__main__':
    main()
