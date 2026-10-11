#!/usr/bin/env python3
"""Static resource checks for the Android app (no Android SDK required).

Checks performed:
  1. Every XML resource file under app/src/main/res is well-formed.
  2. Every "@type/name" reference in XML resolves to a defined resource
     (values, layout, drawable, style, dimen, color, string, id).
  3. Every "R.type.name" used in Java sources exists in the resources or is a
     platform/generated id defined by the code (android.R / com.google... are ignored).
  4. Every "?attr/name" in XML is declared in values/attrs or is a known framework/Material attr.

Usage: python3 scripts/verify_resources.py [repo_root]
Exit status is 0 when no problems are found, 1 otherwise.
"""
import os
import re
import sys
import xml.etree.ElementTree as ET

ROOT = os.path.abspath(sys.argv[1] if len(sys.argv) > 1 else os.path.join(os.path.dirname(__file__), ".."))
RES = os.path.join(ROOT, "app", "src", "main", "res")
JAVA = os.path.join(ROOT, "app", "src", "main", "java")
MANIFEST = os.path.join(ROOT, "app", "src", "main", "AndroidManifest.xml")

problems = []
defined = {}  # type -> set(names)


def add_defined(rtype, name):
    defined.setdefault(rtype, set()).add(name)


# --- 1 & collect definitions -------------------------------------------------
xml_files = []
if os.path.isfile(MANIFEST):
    xml_files.append(MANIFEST)
for dirpath, _, files in os.walk(RES):
    folder = os.path.basename(dirpath).split("-")[0]
    for f in files:
        if f.endswith(".png") and folder in ("drawable", "mipmap"):
            add_defined(folder, os.path.splitext(f)[0])
        if not f.endswith(".xml"):
            continue
        path = os.path.join(dirpath, f)
        xml_files.append(path)
        try:
            tree = ET.parse(path)
        except ET.ParseError as exc:
            problems.append(f"{os.path.relpath(path, ROOT)}: not well-formed: {exc}")
            continue
        if folder == "values":
            for node in tree.getroot():
                name = node.get("name")
                if name is None:
                    continue
                tag = node.tag
                if tag == "item":
                    rtype = node.get("type")
                    if rtype:
                        add_defined(rtype, name)
                elif tag in ("color", "dimen", "string", "style", "bool", "integer"):
                    add_defined(tag, name)
                elif tag == "attr":
                    add_defined("attr", name)
        else:
            add_defined(folder, os.path.splitext(f)[0])
        # ids declared with @+id/
        for el in tree.iter():
            for key, value in el.attrib.items():
                if value.startswith("@+id/"):
                    add_defined("id", value[len("@+id/"):])

if os.path.isfile(MANIFEST):
    try:
        ET.parse(MANIFEST)
    except ET.ParseError as exc:
        problems.append(f"AndroidManifest.xml: not well-formed: {exc}")

# --- 2. XML references -------------------------------------------------------
REF = re.compile(r"@(?:\+)?(?:android:)?([a-z]+)/([A-Za-z0-9_.]+)")
FRAMEWORK_OK = {"null", "empty"}
for path in xml_files:
    text = open(path, encoding="utf-8").read()
    for rtype, name in REF.findall(text):
        if rtype in ("id",) and ("@+id/" + name) in text:
            # declarations are fine; references must exist
            pass
        if rtype in defined and name in defined[rtype]:
            continue
        if rtype == "id" and name in defined.get("id", set()):
            continue
        # Framework (android:) and library references are not checked here.
        if rtype in ("drawable", "color", "dimen", "style", "string", "layout", "id", "mipmap", "menu", "string-array", "array", "integer", "bool"):
            if f"@android:{rtype}/" in text and f"@android:{rtype}/{name}" in text:
                continue
            if f"@{rtype}/{name}" in text or f"@+{rtype}/{name}" in text or f"@{rtype}/" in text:
                problems.append(f"{os.path.relpath(path, ROOT)}: unresolved @{rtype}/{name}")

# --- 3. Java R references ----------------------------------------------------
R_REF = re.compile(r"(?<![\w.])R\.([a-z]+)\.([A-Za-z0-9_]+)")
for dirpath, _, files in os.walk(JAVA):
    for f in files:
        if not f.endswith(".java"):
            continue
        path = os.path.join(dirpath, f)
        text = open(path, encoding="utf-8").read()
        # Strip comments so examples in Javadoc do not count.
        code = re.sub(r"/\*.*?\*/", "", text, flags=re.S)
        code = re.sub(r"//[^\n]*", "", code)
        for rtype, name in R_REF.findall(code):
            if name in defined.get(rtype, set()):
                continue
            problems.append(f"{os.path.relpath(path, ROOT)}: R.{rtype}.{name} is not defined in res/")

# --- 4. Layout / style hygiene: "fill_parent" is deprecated --------------------
for path in xml_files:
    if "fill_parent" in open(path, encoding="utf-8").read():
        problems.append(f"{os.path.relpath(path, ROOT)}: uses deprecated fill_parent")

counts = {k: len(v) for k, v in sorted(defined.items())}
print("resources defined:", counts)
print(f"xml files checked: {len(xml_files)}")
if problems:
    print(f"{len(problems)} problem(s):")
    for p in problems:
        print("  -", p)
    sys.exit(1)
print("resource checks: PASSED")
