#!/usr/bin/env python3
"""Android bricks vs DesktopScriptEngine: presence + stub vs real handlers."""
import re
import pathlib
from collections import defaultdict

ROOT = pathlib.Path(r"C:/Users/ivanp/NewCatroid")
ENGINE = ROOT / "desktop-runtime/src/main/java/org/catrobat/catroid/stage/DesktopScriptEngine.kt"
BRICKS_DIR = ROOT / "catroid/src/main/java/org/catrobat/catroid/content/bricks"
PHYS_DIR = ROOT / "catroid/src/main/java/org/catrobat/catroid/physics/content/bricks"
OUT = ROOT / ".omo/runtime-parity"

engine = ENGINE.read_text(encoding="utf-8", errors="replace")
lines = engine.splitlines()

android_bricks = {p.stem for p in BRICKS_DIR.glob("*.java")} | {p.stem for p in BRICKS_DIR.glob("*.kt")}
if PHYS_DIR.exists():
    android_bricks |= {p.stem for p in PHYS_DIR.glob("*.java")}
# Exclude abstract/base helpers that are not runnable bricks
EXCLUDE = {
    "Brick",
    "BrickBaseType",
    "FormulaBrick",
    "CompositeBrick",
    "ConcurrentFormulaHashMap",
    "UserVariableBrick",
    "UserListBrick",
    "UserVariableBrickWithFormula",
    "UserListBrickWithFormula",
    "ScriptBrick",
    "ScriptBrickBaseType",
    "AnimatedBrick",
    "VisualPlacementBrick",
}
android_bricks = sorted(android_bricks - EXCLUDE)

# All "XxxBrick" -> arms anywhere in engine
parsed = set(re.findall(r'"(\w+Brick)"\s*->', engine))
# Also type == "XxxBrick" checks for scripts
parsed |= set(re.findall(r'(?:type|stype|scriptType|brickType)\s*==\s*"(\w+Brick)"', engine))
parsed |= set(re.findall(r'getAttribute\("type"\)\s*==\s*"(\w+Brick)"', engine))

# Internal opcodes: listOf("opcode"
opcodes = set(re.findall(r'listOf\(\s*"([a-z][a-z0-9_]*)"', engine))

# Brick -> opcodes near each other (within ~8 lines after brick arm)
brick_opcodes = defaultdict(set)
for i, line in enumerate(lines):
    m = re.search(r'"(\w+Brick)"\s*->', line)
    if not m:
        continue
    brick = m.group(1)
    window = "\n".join(lines[i : i + 12])
    for op in re.findall(r'listOf\(\s*"([a-z][a-z0-9_]*)"', window):
        brick_opcodes[brick].add(op)

# Handler classification on opcodes in execute* when branches
# Find execute function regions
exec_regions = []
for m in re.finditer(r"private fun (execute\w+)\(", engine):
    exec_regions.append((m.group(1), m.start()))

stub_ops = set()
real_ops = set()
# Simpler: for each "op" -> { body } after line containing execute
for em in re.finditer(
    r'"([a-z][a-z0-9_]*)"\s*->\s*\{([^{}]*(?:\{[^{}]*\}[^{}]*)*)\}',
    engine,
):
    t, body = em.group(1), em.group(2)
    stripped = re.sub(r"//.*?$", "", body, flags=re.M)
    stripped = re.sub(r"/\*.*?\*/", "", stripped, flags=re.S).strip()
    low = body.lower()
    is_stub = (
        stripped == ""
        or stripped in ("Unit", "null")
        or any(
            s in low
            for s in (
                "not supported",
                "stub",
                "no-op",
                "noop",
                "android-only",
                "not available",
                "not implemented",
            )
        )
    )
    if is_stub:
        stub_ops.add(t)
    else:
        real_ops.add(t)
real_ops -= stub_ops

# Classify bricks
def is_boundary(b: str) -> bool:
    return any(
        x in b
        for x in (
            "EndBrick",
            "ElseBrick",
            "CatchBrick",
            "FinallyBrick",
            "CaseBrick",
            "LoopEndless",
            "IfLogicElse",
        )
    )


def is_scriptish(b: str) -> bool:
    return (
        b.startswith("When")
        or b.endswith("Script")
        or "Receiver" in b
        or b.endswith("DefinitionBrick")
        or b.startswith("UserDefinedBrick")
    )


missing = sorted(set(android_bricks) - parsed)
present = sorted(set(android_bricks) & parsed)

# Present but only maps to stub opcodes
present_stub = []
present_real = []
present_unknown = []
for b in present:
    ops = brick_opcodes.get(b, set())
    if not ops:
        # container / script handled without listOf opcode in window
        present_unknown.append(b)
        continue
    if ops & stub_ops and not (ops - stub_ops):
        present_stub.append(b)
    elif ops & real_ops:
        present_real.append(b)
    else:
        present_unknown.append(b)

cats = defaultdict(list)
for b in missing:
    if is_boundary(b) or is_scriptish(b):
        cats["boundary_or_script"].append(b)
        continue
    low = b.lower()
    if any(x in low for x in ("admob", "banner", "interstitial", "rewarded")):
        cats["ads"].append(b)
    elif any(x in low for x in ("drone", "jumpingsumo", "ardrone", "parrot")):
        cats["drone"].append(b)
    elif any(x in low for x in ("nfc", "phiro", "arduino", "raspi", "lego", "nxt", "ev3", "chromecast", "cast")):
        cats["hardware"].append(b)
    elif any(x in low for x in ("voxel", "embroidery", "stitch", "plot")):
        cats["niche"].append(b)
    elif any(x in low for x in ("camera", "flash", "photo", "face", "mlkit", "torch", "pytorch", "onnx", "llm", "gemini", "openai", "askai", "neural")):
        cats["ai_camera"].append(b)
    elif any(x in low for x in ("3d", "raptor", "gltf", "mesh", "shader", "light", "skybox", "fog", "particle", "pbr", "threed")):
        cats["3d"].append(b)
    elif any(x in low for x in ("physics", "joint", "gravity", "velocity", "bounce", "friction", "mass", "force", "impulse", "torque", "ragdoll", "hitbox", "ray")):
        cats["physics"].append(b)
    elif any(x in low for x in ("web", "http", "socket", "server", "download", "upload", "firebase", "dns", "ping")):
        cats["web"].append(b)
    elif any(x in low for x in ("sound", "music", "note", "drum", "midi", "tone", "volume", "record", "speak", "tts")):
        cats["sound"].append(b)
    elif any(x in low for x in ("file", "folder", "zip", "path", "storage", "export", "import", "neoscript")):
        cats["file"].append(b)
    elif any(x in low for x in ("pen", "draw", "stamp", "fill")):
        cats["pen"].append(b)
    elif any(x in low for x in ("list", "variable", "formula")):
        cats["data"].append(b)
    elif any(x in low for x in ("motion", "move", "turn", "glide", "place", "point", "goto", "setx", "sety")):
        cats["motion"].append(b)
    else:
        cats["other"].append(b)

OUT.mkdir(parents=True, exist_ok=True)
(OUT / "android_bricks.txt").write_text("\n".join(android_bricks), encoding="utf-8")
(OUT / "desktop_parsed.txt").write_text("\n".join(sorted(parsed)), encoding="utf-8")
(OUT / "present_real.txt").write_text("\n".join(sorted(present_real)), encoding="utf-8")
(OUT / "present_stub.txt").write_text("\n".join(sorted(present_stub)), encoding="utf-8")
(OUT / "present_unknown_handler.txt").write_text("\n".join(sorted(present_unknown)), encoding="utf-8")
(OUT / "stub_opcodes.txt").write_text("\n".join(sorted(stub_ops)), encoding="utf-8")
(OUT / "real_opcodes.txt").write_text("\n".join(sorted(real_ops)), encoding="utf-8")
(OUT / "opcodes.txt").write_text("\n".join(sorted(opcodes)), encoding="utf-8")

with (OUT / "missing_by_category.txt").open("w", encoding="utf-8") as f:
    for k, v in sorted(cats.items(), key=lambda x: -len(x[1])):
        f.write(f"## {k} ({len(v)})\n")
        for b in sorted(v):
            ops = ",".join(sorted(brick_opcodes.get(b, [])))
            f.write(f"{b}\n" if not ops else f"{b}  ops={ops}\n")
        f.write("\n")

with (OUT / "brick_opcode_map.txt").open("w", encoding="utf-8") as f:
    for b in sorted(brick_opcodes):
        f.write(f"{b}: {', '.join(sorted(brick_opcodes[b]))}\n")

print("ANDROID_BRICKS", len(android_bricks))
print("PARSED_ANYWHERE", len(parsed & set(android_bricks)), "/", len(android_bricks))
print("MISSING", len(missing))
print("PRESENT_REAL", len(present_real))
print("PRESENT_STUB_ONLY", len(present_stub))
print("PRESENT_UNKNOWN", len(present_unknown))
print("OPCODES", len(opcodes), "REAL_OP", len(real_ops), "STUB_OP", len(stub_ops))
for k, v in sorted(cats.items(), key=lambda x: -len(x[1])):
    print(f"CAT {k}: {len(v)}")
print("STUB_BRICKS:", ", ".join(sorted(present_stub)[:50]))
print("STUB_OPS:", ", ".join(sorted(stub_ops)))
