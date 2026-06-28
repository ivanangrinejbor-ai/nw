"""
Tokenizer for LSTM training — converts brick types to/from token IDs.
Supports 500-token context windows across all objects in a project.
"""

import json
from collections import Counter

SPECIAL_TOKENS = ["[PAD]", "[UNK]", "[SEP]", "[START]", "[END]"]
PAD_ID, UNK_ID, SEP_ID, START_ID, END_ID = 0, 1, 2, 3, 4


class BrickTokenizer:
    def __init__(self):
        self.word2id = {}
        self.id2word = {}
        self.vocab_size = 0

    def build_vocab(self, projects_json_path: str, min_freq: int = 1):
        """Build vocabulary from all parsed projects."""
        with open(projects_json_path, 'r', encoding='utf-8') as f:
            projects = json.load(f)

        counter = Counter()
        for proj in projects:
            for scene in proj.get('scenes', []):
                for sprite in scene.get('sprites', []):
                    for script in sprite.get('scripts', []):
                        for brick in script.get('bricks', []):
                            bt = brick.get('type', '')
                            if bt:
                                counter[bt] += 1

        # Build vocab with special tokens first
        self.word2id = {tok: i for i, tok in enumerate(SPECIAL_TOKENS)}
        for word, freq in counter.items():
            if freq >= min_freq and word not in self.word2id:
                self.word2id[word] = len(self.word2id)

        self.id2word = {v: k for k, v in self.word2id.items()}
        self.vocab_size = len(self.word2id)

    def save(self, path: str):
        """Save vocabulary to JSON."""
        data = {
            'word2id': self.word2id,
            'id2word': {str(k): v for k, v in self.id2word.items()},
            'vocab_size': self.vocab_size
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
        """Build a single token sequence for the entire project.
        Concatenates all scripts from all objects with [SEP] tokens.
        Truncates to last `max_len` tokens."""
        tokens = [START_ID]
        for scene in project.get('scenes', []):
            for sprite in scene.get('sprites', []):
                for script in sprite.get('scripts', []):
                    for brick in script.get('bricks', []):
                        bt = brick.get('type', '')
                        if bt:
                            tokens.append(self.encode(bt))
                    tokens.append(SEP_ID)
                tokens.append(SEP_ID)
            tokens.append(SEP_ID)
        tokens.append(END_ID)
        # Take last max_len tokens
        return tokens[-max_len:]

    def generate_training_pairs(self, projects_json_path: str, window: int = 500):
        """Generate (context, target) pairs for next-token prediction.
        Yields (input_sequence, target_token_id) for every position."""
        with open(projects_json_path, 'r', encoding='utf-8') as f:
            projects = json.load(f)

        for proj in projects:
            all_tokens = self.build_project_sequence(proj, window * 3)
            for i in range(window, len(all_tokens)):
                context = all_tokens[i - window:i]
                target = all_tokens[i]
                if target in (PAD_ID, START_ID):
                    continue
                yield context, target
