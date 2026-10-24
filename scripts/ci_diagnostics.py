#!/usr/bin/env python3
"""Expose CI failure text as annotations when external log downloads are unavailable."""
from pathlib import Path
import os
import sys

path = Path(sys.argv[1])
lines = path.read_text(errors="replace").splitlines() if path.exists() else []
needles = (
    "error:", "FAILURE:", "What went wrong:", "Execution failed", "Caused by:",
    "tests completed", "AssertionFailedError", "Exception", "BeanCreation",
    "Validation failed", "Migration", "SQL State", "Syntax error", "expected",
)
selected = [line for line in lines if any(word in line for word in needles)][-100:]
if not selected:
    selected = lines[-100:]
for line in selected:
    escaped = line.replace("%", "%25").replace("\r", "%0D").replace("\n", "%0A")
    print(f"::error::{escaped}")
summary = os.environ.get("GITHUB_STEP_SUMMARY")
if summary:
    with Path(summary).open("a") as output:
        output.write(f"\n### CI diagnostics: {path.name}\n\n```text\n")
        output.write("\n".join(lines[-220:]))
        output.write("\n```\n")
