#!/usr/bin/env python3
"""Publish JUnit XML failure names and relevant server errors as CI annotations."""
from pathlib import Path
import xml.etree.ElementTree as ET
import sys

root = Path(sys.argv[1])
emitted = 0

def annotate(text):
    global emitted
    if emitted >= 240 or not text.strip():
        return
    escaped = text.replace("%", "%25").replace("\r", "%0D").replace("\n", "%0A")
    print(f"::error::{escaped}")
    emitted += 1

reports = sorted(root.glob("*.xml"))
annotate(f"JUnit diagnostic scan: {root} contains {len(reports)} XML reports")
for report in reports:
    before = emitted
    try:
        suite = ET.parse(report).getroot()
    except (ET.ParseError, OSError):
        continue
    for case in suite.iter("testcase"):
        failures = list(case.findall("failure")) + list(case.findall("error"))
        if not failures:
            continue
        name = f"{case.attrib.get('classname', '')}.{case.attrib.get('name', '')}"
        annotate(f"FAILED TEST {name}")
        for failure in failures:
            annotate(failure.attrib.get("message", "Test failed"))
            for line in (failure.text or "").splitlines()[:80]:
                annotate(line)
    for output_name in ("system-out", "system-err"):
        output = suite.find(output_name)
        if output is None or not output.text:
            continue
        lines = output.text.splitlines()
        matches = [i for i, line in enumerate(lines) if any(word in line for word in (
            "Unhandled API error", "ERROR", "Exception", "NullPointer", "LazyInitialization",
            "TransientObject", "detached entity", "could not initialize proxy"))]
        selected = []
        for index in matches[-12:]:
            selected.extend(lines[max(0, index - 2):min(len(lines), index + 12)])
        if selected:
            annotate(f"{report.name} {output_name} diagnostics")
            for line in selected[-120:]:
                annotate(line)
    if emitted == before:
        raw = report.read_text(errors="replace")
        if "<failure" in raw or "<error" in raw:
            annotate(f"Raw JUnit failure from {report.name}")
            lines = raw.replace("]]>", "").splitlines()
            matches = [i for i, line in enumerate(lines) if "<failure" in line or "<error" in line]
            for index in matches[:2]:
                for line in lines[index:index + 100]:
                    annotate(line)
