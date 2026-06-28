"""
Parser for NewCatroid .code.xml project files.
Extracts structured data: sprites, scripts, bricks, formulas.
"""

import xml.etree.ElementTree as ET
from dataclasses import dataclass, field
from typing import Optional
from pathlib import Path
import glob
import json
import os


# ---------- Data Classes ----------

@dataclass
class FormulaElement:
    el_type: str          # NUMBER, STRING, OPERATOR, FUNCTION, USER_VARIABLE, etc.
    value: str
    left: Optional['FormulaElement'] = None
    right: Optional['FormulaElement'] = None
    children: list['FormulaElement'] = field(default_factory=list)


@dataclass
class Brick:
    brick_type: str       # e.g. "SetXBrick", "WaitBrick", "PlaySoundBrick"
    formulas: dict = field(default_factory=dict)   # category -> FormulaElement
    # For bricks with spinners / string fields:
    sprite_ref: Optional[str] = None
    string_value: Optional[str] = None


@dataclass
class Script:
    script_type: str      # e.g. "StartScript", "WhenScript", "BroadcastScript"
    bricks: list[Brick] = field(default_factory=list)


@dataclass
class Sprite:
    name: str
    looks: list[str] = field(default_factory=list)
    scripts: list[Script] = field(default_factory=list)


@dataclass
class Scene:
    name: str
    sprites: list[Sprite] = field(default_factory=list)


@dataclass
class Project:
    name: str
    scenes: list[Scene] = field(default_factory=list)
    variables: list[str] = field(default_factory=list)
    lists: list[str] = field(default_factory=list)


# ---------- Parsing Logic ----------

def parse_formula_element(el: ET.Element) -> Optional[FormulaElement]:
    if el is None:
        return None
    el_type = el.findtext('type', '').strip()
    value = el.findtext('value', '').strip()
    fe = FormulaElement(el_type=el_type, value=value)
    left = el.find('leftChild')
    right = el.find('rightChild')
    if left is not None:
        fe.left = parse_formula_element(left)
    if right is not None:
        fe.right = parse_formula_element(right)
    children = el.find('additionalChildren')
    if children is not None:
        for child in children:
            parsed = parse_formula_element(child)
            if parsed:
                fe.children.append(parsed)
    return fe


def parse_formulas(formula_list_el: ET.Element) -> dict:
    formulas = {}
    if formula_list_el is None:
        return formulas
    for formula_el in formula_list_el.findall('formula'):
        category = formula_el.get('category', '')
        fe = parse_formula_element(formula_el)
        if fe:
            formulas[category] = fe
    return formulas


def parse_brick(brick_el: ET.Element) -> Brick:
    brick_type = brick_el.get('type', 'UnknownBrick')
    brick = Brick(brick_type=brick_type)
    formula_list = brick_el.find('formulaList')
    if formula_list is not None:
        brick.formulas = parse_formulas(formula_list)
    return brick


def parse_script(script_el: ET.Element) -> Script:
    script_type = script_el.get('type', 'UnknownScript')
    script = Script(script_type=script_type)
    brick_list = script_el.find('brickList')
    if brick_list is not None:
        for brick_el in brick_list.findall('brick'):
            script.bricks.append(parse_brick(brick_el))
    return script


def parse_sprite(object_el: ET.Element) -> Sprite:
    name = object_el.get('name', 'Unnamed')
    sprite = Sprite(name=name)
    look_list = object_el.find('lookList')
    if look_list is not None:
        for look in look_list.findall('look'):
            look_name = look.get('name', '')
            if look_name:
                sprite.looks.append(look_name)
    script_list = object_el.find('scriptList')
    if script_list is not None:
        for script_el in script_list.findall('script'):
            sprite.scripts.append(parse_script(script_el))
    return sprite


def parse_project(xml_path: str) -> Optional[Project]:
    """Parse a .code.xml file into a Project data structure."""
    try:
        tree = ET.parse(xml_path)
        root = tree.getroot()
    except Exception as e:
        print(f"  [ERROR] Failed to parse {xml_path}: {e}")
        return None

    if root.tag != 'program':
        print(f"  [WARN] Root tag is '{root.tag}', expected 'program'")
        return None

    header = root.find('header')
    program_name = 'Unnamed'
    if header is not None:
        program_name = header.findtext('programName', 'Unnamed')

    project = Project(name=program_name)

    # Global variables
    var_list = root.find('programVariableList')
    if var_list is not None:
        for var in var_list.findall('userVariable'):
            vname = var.findtext('name', '')
            if vname:
                project.variables.append(vname)

    # Global lists
    list_list = root.find('programListOfLists')
    if list_list is not None:
        for lst in list_list.findall('userList'):
            lname = lst.findtext('name', '')
            if lname:
                project.lists.append(lname)

    # Scenes
    scenes = root.find('scenes')
    if scenes is not None:
        for scene_el in scenes.findall('scene'):
            scene_name = scene_el.findtext('name', 'Unnamed Scene')
            scene = Scene(name=scene_name)
            obj_list = scene_el.find('objectList')
            if obj_list is not None:
                for obj in obj_list.findall('object'):
                    scene.sprites.append(parse_sprite(obj))
            project.scenes.append(scene)
    else:
        print(f"  [WARN] No <scenes> found in {xml_path}")

    return project


def format_formula_summary(fe: FormulaElement, max_depth: int = 2) -> str:
    """Return a short string summary of a formula element."""
    if fe is None:
        return ""
    if fe.el_type == 'NUMBER':
        return fe.value
    elif fe.el_type == 'STRING':
        return f'"{fe.value}"'
    elif fe.el_type == 'USER_VARIABLE':
        return f'${fe.value}'
    elif fe.el_type == 'USER_LIST':
        return f'@{fe.value}'
    elif fe.el_type == 'FUNCTION':
        args = []
        if fe.left:
            args.append(format_formula_summary(fe.left, max_depth - 1))
        if fe.right:
            args.append(format_formula_summary(fe.right, max_depth - 1))
        for c in fe.children:
            args.append(format_formula_summary(c, max_depth - 1))
        return f"{fe.value}({', '.join(args)})" if args else fe.value
    elif fe.el_type == 'OPERATOR':
        l = format_formula_summary(fe.left, max_depth - 1) if fe.left else ''
        r = format_formula_summary(fe.right, max_depth - 1) if fe.right else ''
        return f"({l} {fe.value} {r})" if l and r else fe.value
    else:
        return f"[{fe.el_type}:{fe.value}]"


def project_to_dict(proj: Project) -> dict:
    """Convert a Project object to a serializable dict (for JSON export)."""
    scenes_data = []
    for scene in proj.scenes:
        sprites_data = []
        for sprite in scene.sprites:
            scripts_data = []
            for script in sprite.scripts:
                bricks_data = []
                for brick in script.bricks:
                    formulas_data = {}
                    for cat, fe in brick.formulas.items():
                        formulas_data[cat] = format_formula_summary(fe)
                    bricks_data.append({
                        'type': brick.brick_type,
                        'formulas': formulas_data
                    })
                scripts_data.append({
                    'type': script.script_type,
                    'bricks': bricks_data
                })
            sprites_data.append({
                'name': sprite.name,
                'looks': sprite.looks,
                'scripts': scripts_data
            })
        scenes_data.append({
            'name': scene.name,
            'sprites': sprites_data
        })
    return {
        'name': proj.name,
        'scenes': scenes_data,
        'variables': proj.variables,
        'lists': proj.lists
    }


def scan_projects(directory: str) -> list[Project]:
    """Scan a directory recursively for Catroid .code.xml files and parse them.
    Accepts any .xml file but only parses those with root tag <program>."""
    pattern = os.path.join(directory, '**', '*.xml')
    xml_files = glob.glob(pattern, recursive=True)
    projects = []
    for fpath in xml_files:
        try:
            with open(fpath, 'rb') as f:
                header = f.read(200)
                if b'<program' not in header and b'program' not in header[:50]:
                    continue
        except Exception:
            continue
        print(f"  Parsing: {fpath}")
        proj = parse_project(fpath)
        if proj:
            projects.append(proj)
            print(f"    -> {proj.name} ({len(proj.scenes)} scenes)")
    return projects


def export_to_json(projects: list[Project], output_path: str):
    """Export parsed projects to a JSON file for training."""
    data = [project_to_dict(p) for p in projects]
    with open(output_path, 'w', encoding='utf-8') as f:
        json.dump(data, f, indent=2, ensure_ascii=False)
    print(f"Exported {len(projects)} projects to {output_path}")


# ---------- CLI ----------
if __name__ == '__main__':
    import sys
    if len(sys.argv) < 2:
        print("Usage: python code_xml_parser.py <projects_directory> [output_json]")
        sys.exit(1)
    proj_dir = sys.argv[1]
    out_json = sys.argv[2] if len(sys.argv) > 2 else 'training_data/projects.json'
    print(f"Scanning {proj_dir} for .code.xml files...")
    projects = scan_projects(proj_dir)
    export_to_json(projects, out_json)
    print("Done.")
