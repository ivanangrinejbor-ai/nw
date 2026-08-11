#!/usr/bin/env python3
"""Compare Android bricks vs DesktopScriptEngine parse/execute (read-only analysis)."""
import re
import pathlib
from collections import defaultdict

ROOT = pathlib.Path(__file__).resolve().parents[2]
ENGINE = ROOT / "desktop-runtime/src/main/java/org/catrobat/catroid/stage/DesktopScriptEngine.kt"
BRICKS_DIR = ROOT / "catroid/src/main/java/org/catrobat/catroid/content/bricks"
PHYS_DIR = ROOT / "catroid/src/main/java/org/catrobat/catroid/physics/content/bricks"
ACTIONS_DIR = ROOT / "catroid/src/main/java/org/catrobat/catroid/content/actions"
OUT = pathlib.Path(__file__).resolve().parent

engine = ENGINE.read_text(encoding="utf-8", errors="replace")

android_bricks = {p.stem for p in BRICKS_DIR.glob("*.java")} | {p.stem for p in BRICKS_DIR.glob("*.kt")}
if PHYS_DIR.exists():
    android_bricks |= {p.stem for p in PHYS_DIR.glob("*.java")}
android_bricks = sorted(android_bricks)

# parseBrickLeaf body
m = re.search(
    r"private fun parseBrickLeaf\(.*?\n(.*?)(?=\n    private fun |\n    fun |\Z)",
    engine,
    re.S,
)
parse_body = m.group(1) if m else ""
parsed = set(re.findall(r'"(\w+Brick)"\s*->', parse_body))
parsed |= set(re.findall(r'typeName\s*==\s*"(\w+Brick)"', parse_body))

all_in_engine = set(re.findall(r'"(\w+Brick)"', engine))
block_types_created = set(re.findall(r'Block\(\s*"([a-z0-9_]+)"', engine))

# Handler classification: "type" -> { body } one nesting level approx
stub_types = []
real_types = []
for em in re.finditer(
    r'"([a-z][a-z0-9_]*)"\s*->\s*\{([^{}]*(?:\{[^{}]*\}[^{}]*)*)\}',
    engine,
):
    t, body = em.group(1), em.group(2)
    stripped = re.sub(r"//.*?$", "", body, flags=re.M)
    stripped = re.sub(r"/\*.*?\*/", "", stripped, flags=re.S).strip()
    low = body.lower()
    if (
        stripped == ""
        or stripped in ("Unit", "null")
        or "not supported" in low
        or "stub" in low
        or "no-op" in low
        or "noop" in low
        or "android-only" in low
    ):
        stub_types.append(t)
    elif len(stripped) < 40 and ("Gdx.app" in stripped or "log" in low):
        stub_types.append(t)
    else:
        real_types.append(t)

stub_u = sorted(set(stub_types))
real_u = sorted(set(real_types) - set(stub_u))

missing_parse = sorted(set(android_bricks) - parsed)
boundary = {
    b
    for b in missing_parse
    if any(
        x in b
        for x in (
            "EndBrick",
            "ElseBrick",
            "CatchBrick",
            "FinallyBrick",
            "CaseBrick",
            "LoopEndless",
            "IfLogicElse",
            "Script",
        )
    )
}
scriptish = {
    b
    for b in missing_parse
    if b.startswith("When")
    or b.endswith("Script")
    or "Receiver" in b
    or b.endswith("DefinitionBrick")
    or b.startswith("UserDefined")
}
missing_meaningful = sorted(set(missing_parse) - boundary - scriptish)

# Category buckets
cats = defaultdict(list)
for b in missing_meaningful:
    low = b.lower()
    if any(x in low for x in ("admob", "banner", "interstitial", "rewarded", "adbrick")):
        cats["ads"].append(b)
    elif any(x in low for x in ("drone", "jumpingsumo", "ardrone", "parrot")):
        cats["drone"].append(b)
    elif any(
        x in low
        for x in (
            "nfc",
            "phiro",
            "arduino",
            "raspi",
            "lego",
            "nxt",
            "ev3",
            "chromecast",
            "cast",
        )
    ):
        cats["hardware"].append(b)
    elif any(x in low for x in ("voxel", "embroidery", "stitch", "plot")):
        cats["niche"].append(b)
    elif any(
        x in low
        for x in (
            "camera",
            "flash",
            "photo",
            "face",
            "mlkit",
            "torch",
            "pytorch",
            "onnx",
            "llm",
            "gemini",
            "openai",
            "askai",
            "neural",
        )
    ):
        cats["ai_camera"].append(b)
    elif any(
        x in low
        for x in (
            "3d",
            "raptor",
            "gltf",
            "mesh",
            "shader",
            "light",
            "skybox",
            "fog",
            "particle",
            "pbr",
            "threed",
        )
    ):
        cats["3d"].append(b)
    elif any(
        x in low
        for x in (
            "physics",
            "joint",
            "gravity",
            "velocity",
            "bounce",
            "friction",
            "mass",
            "force",
            "impulse",
            "torque",
            "ragdoll",
            "hitbox",
            "ray",
        )
    ):
        cats["physics"].append(b)
    elif any(
        x in low
        for x in (
            "web",
            "http",
            "socket",
            "server",
            "download",
            "upload",
            "firebase",
            "dns",
            "ping",
        )
    ):
        cats["web"].append(b)
    elif any(
        x in low
        for x in (
            "sound",
            "music",
            "note",
            "drum",
            "midi",
            "tone",
            "volume",
            "record",
            "speak",
            "tts",
        )
    ):
        cats["sound"].append(b)
    elif any(
        x in low
        for x in (
            "file",
            "folder",
            "zip",
            "path",
            "storage",
            "export",
            "import",
            "neoscript",
        )
    ):
        cats["file"].append(b)
    else:
        cats["other"].append(b)

# Map brick -> Block("type") near it in parseBrickLeaf for semantic samples
# Extract pairs: "XxxBrick" -> Block("yyy"
pairs = re.findall(r'"(\w+Brick)"\s*->\s*[^\n]*?Block\(\s*"([a-z0-9_]+)"', parse_body, re.S)
# Also multi-line: "XxxBrick" -> { ... Block("yyy"
pairs2 = re.findall(
    r'"(\w+Brick)"\s*->\s*\{.*?Block\(\s*"([a-z0-9_]+)"',
    parse_body,
    re.S,
)
brick_to_type = {}
for a, b in pairs + pairs2:
    brick_to_type.setdefault(a, set()).add(b)

# Write outputs
(OUT / "android_bricks.txt").write_text("\n".join(android_bricks), encoding="utf-8")
(OUT / "desktop_parsed.txt").write_text("\n".join(sorted(parsed)), encoding="utf-8")
(OUT / "missing_parse.txt").write_text("\n".join(missing_meaningful), encoding="utf-8")
(OUT / "missing_parse_all.txt").write_text("\n".join(missing_parse), encoding="utf-8")
(OUT / "stub_handlers.txt").write_text("\n".join(stub_u), encoding="utf-8")
(OUT / "real_handlers.txt").write_text("\n".join(real_u), encoding="utf-8")
(OUT / "block_types.txt").write_text("\n".join(sorted(block_types_created)), encoding="utf-8")

with (OUT / "missing_by_category.txt").open("w", encoding="utf-8") as f:
    for k, v in sorted(cats.items(), key=lambda x: -len(x[1])):
        f.write(f"## {k} ({len(v)})\n")
        for b in v:
            f.write(f"{b}\n")
        f.write("\n")

print("ANDROID_BRICKS", len(android_bricks))
print("PARSED_IN_parseBrickLeaf", len(parsed))
print("ALL_BRICK_STRINGS_IN_ENGINE", len(all_in_engine))
print("BLOCK_TYPES_CREATED", len(block_types_created))
print("STUB_HANDLERS", len(stub_u))
print("REAL_HANDLERS", len(real_u))
print("MISSING_FROM_PARSE_TOTAL", len(missing_parse))
print("MISSING_MEANINGFUL", len(missing_meaningful))
print("EXTRA_PARSED_NOT_IN_ANDROID", len(parsed - set(android_bricks)))
print("BOUNDARY_OR_SCRIPTISH_SKIPPED", len(boundary | scriptish))
for k, v in sorted(cats.items(), key=lambda x: -len(x[1])):
    print(f"CAT {k}: {len(v)}")
print("STUB_SAMPLE", ", ".join(stub_u[:40]))
print("MISSING_OTHER_SAMPLE", ", ".join(cats["other"][:40]))
