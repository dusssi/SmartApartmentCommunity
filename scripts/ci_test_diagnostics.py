#!/usr/bin/env python3
"""Publish concise JUnit failure names and application stack lines as CI annotations."""
from pathlib import Path
import xml.etree.ElementTree as ET
import sys

root = Path(sys.argv[1])
server_mode = len(sys.argv) > 2 and sys.argv[2] == "--server"
reports = sorted(root.glob("*.xml"))
failures = []
emitted = 0

def annotate(text):
    global emitted
    if emitted >= 10 or not text.strip():
        return
    escaped = text.replace("%", "%25").replace("\r", "%0D").replace("\n", "%0A")
    print(f"::error::{escaped}")
    emitted += 1

if server_mode:
    for report in reports:
        try:
            suite = ET.parse(report).getroot()
        except (ET.ParseError, OSError):
            continue
        for output_name in ("system-out", "system-err"):
            output = suite.find(output_name)
            if output is None or not output.text:
                continue
            lines = output.text.splitlines()
            for index, line in enumerate(lines):
                if "Unhandled API error" in line:
                    for selected in lines[index:index + 5]:
                        annotate(selected)
    sys.exit(0)

for report in reports:
    try:
        suite = ET.parse(report).getroot()
    except (ET.ParseError, OSError):
        continue
    for case in suite.iter("testcase"):
        problems = list(case.findall("failure")) + list(case.findall("error"))
        if problems:
            name = f"{case.attrib.get('classname', '')}.{case.attrib.get('name', '')}"
            failures.append((name, problems))

if not reports:
    annotate(f"No JUnit XML reports found under {root}")
for name, problems in failures:
    annotate(f"FAILED TEST {name}")
for name, problems in failures:
    for problem in problems[:1]:
        annotate(problem.attrib.get("message", "Test failed"))
for name, problems in failures:
    for problem in problems[:1]:
        lines = (problem.text or "").splitlines()
        relevant = [line for line in lines if "com.apartment.community.server" in line
                    and ("IntegrationTest.java" in line or "OperationsTestSupport.java" in line)]
        for line in relevant[-3:]:
            annotate(line)
