"""
Tokenizer for LSTM/LoRA Transformer training — converts brick types to/from token IDs.
Supports dynamic context windows (500–50000 tokens) across all objects in a project.
Includes structural boundary tokens for LoRA fine-tuning:
  <project_start>, <project_end>, <scene_start>, <scene_end>,
  <object_start>, <object_end>, <script_start>, <script_end>,
  <global_var>, <global_list>, <broadcast>, <signal>
"""

import json
from collections import Counter

SPECIAL_TOKENS = ["[PAD]", "[UNK]", "[SEP]", "[START]", "[END]"]
STRUCTURAL_TOKENS = [
    "<project_start>", "<project_end>",
    "<scene_start>", "<scene_end>",
    "<object_start>", "<object_end>",
    "<script_start>", "<script_end>",
    "<global_var>", "<global_list>",
    "<broadcast>", "<signal>"
]
PAD_ID, UNK_ID, SEP_ID, START_ID, END_ID = 0, 1, 2, 3, 4
# Structural token IDs start at 5
STRUCT_IDS = {tok: i + 5 for i, tok in enumerate(STRUCTURAL_TOKENS)}


class BrickTokenizer:
    def __init__(self):
        self.word2id = {}
        self.id2word = {}
        self.vocab_size = 0

    def build_vocab(self, projects_json_path: str, min_freq: int = 1):
        """Build vocabulary from all parsed projects including structural tokens."""
        with open(projects_json_path, 'r', encoding='utf-8') as f:
            projects = json.load(f)

        counter = Counter()
        for proj in projects:
            # Add global variable/list/broadcast names to vocabulary
            for var in proj.get('variables', []):
                name = var.get('name', '')
                if name:
                    counter[f"var:{name}"] += 1
            for lst in proj.get('lists', []):
                name = lst.get('name', '')
                if name:
                    counter[f"list:{name}"] += 1
            for msg in proj.get('broadcasts', []):
                if msg:
                    counter[f"msg:{msg}"] += 1
            for scene in proj.get('scenes', []):
                for sprite in scene.get('sprites', []):
                    for script in sprite.get('scripts', []):
                        for brick in script.get('bricks', []):
                            bt = brick.get('type', '')
                            if bt:
                                counter[bt] += 1

        # Special tokens first
        self.word2id = {tok: i for i, tok in enumerate(SPECIAL_TOKENS)}
        # Structural boundary tokens (LoRA-compatible)
        for tok in STRUCTURAL_TOKENS:
            self.word2id[tok] = len(self.word2id)
        # Brick types
        for word, freq in counter.items():
            if freq >= min_freq and word not in self.word2id:
                self.word2id[word] = len(self.word2id)

        self.id2word = {v: k for k, v in self.word2id.items()}
        self.vocab_size = len(self.word2id)

    def save(self, path: str):
        data = {
            'word2id': self.word2id,
            'id2word': {str(k): v for k, v in self.id2word.items()},
            'vocab_size': self.vocab_size,
            'structural_tokens': STRUCTURAL_TOKENS
        }
        with open(path, 'w', encoding='utf-8') as f:
            json.dump(data, f, indent=2)

    def load(self, path: str):
        with open(path, 'r', encoding='utf-8') as f:
            data = json.load(f)
        self.word2id = data['word2id']
        self.id2word = {int(k): v for k, v in data['id2word'].items()}
        self.vocab_size = data['vocab_size']

    def encode(self, brick_type: str) -> int:
        return self.word2id.get(brick_type, UNK_ID)

    def decode(self, token_id: int) -> str:
        return self.id2word.get(token_id, '[UNK]')

    def build_project_sequence(self, project: dict, max_len: int = 500) -> list[int]:
        """
        Build token sequence with LoRA-compatible structural boundaries.
        Format: <project_start> <scene> ... <scene> <project_end>
        Each scene: <scene_start> <obj> ... <obj> <scene_end>
        Each obj: <object_start> <script> ... <script> <object_end>
        Each script: <script_start> brick1 brick2 ... <script_end>
        """
        tokens = [self.encode("<project_start>")]
        for scene in project.get('scenes', []):
            tokens.append(self.encode("<scene_start>"))
            for sprite in scene.get('sprites', []):
                tokens.append(self.encode("<object_start>"))
                for script in sprite.get('scripts', []):
                    tokens.append(self.encode("<script_start>"))
                    for brick in script.get('bricks', []):
                        bt = brick.get('type', '')
                        if bt:
                            tokens.append(self.encode(bt))
                    tokens.append(self.encode("<script_end>"))
                tokens.append(self.encode("<object_end>"))
            tokens.append(self.encode("<scene_end>"))
        tokens.append(self.encode("<project_end>"))
        return tokens[-max_len:] if len(tokens) > max_len else tokens

    def generate_training_pairs(self, projects_json_path: str, window: int = 500):
        """Generate (context, target) pairs for next-token prediction."""
        with open(projects_json_path, 'r', encoding='utf-8') as f:
            projects = json.load(f)

        for proj in projects:
            all_tokens = self.build_project_sequence(proj, window * 6)
            if len(all_tokens) <= window:
                continue
            for i in range(window, len(all_tokens)):
                context = all_tokens[i - window:i]
                target = all_tokens[i]
                if target in (PAD_ID, START_ID):
                    continue
                yield context, target

    def build_project_sequence_with_globals(self, project: dict, max_len: int = 500) -> list[int]:
        """
        Extended context for high-token mode: includes global variables, lists, broadcasts.
        """
        tokens = [self.encode("<project_start>")]
        # Global scope
        tokens.append(self.encode("<global_var>"))
        for var in project.get('variables', []):
            tokens.append(self.encode(f"var:{var.get('name','')}"))
        tokens.append(self.encode("<global_list>"))
        for lst in project.get('lists', []):
            tokens.append(self.encode(f"list:{lst.get('name','')}"))
        tokens.append(self.encode("<broadcast>"))
        for msg in project.get('broadcasts', []):
            tokens.append(self.encode(f"msg:{msg}"))
        # Scenes
        for scene in project.get('scenes', []):
            tokens.append(self.encode("<scene_start>"))
            for sprite in scene.get('sprites', []):
                tokens.append(self.encode("<object_start>"))
                for script in sprite.get('scripts', []):
                    tokens.append(self.encode("<script_start>"))
                    for brick in script.get('bricks', []):
                        bt = brick.get('type', '')
                        if bt:
                            tokens.append(self.encode(bt))
                    tokens.append(self.encode("<script_end>"))
                tokens.append(self.encode("<object_end>"))
            tokens.append(self.encode("<scene_end>"))
        tokens.append(self.encode("<project_end>"))
        return tokens[-max_len:] if len(tokens) > max_len else tokens

