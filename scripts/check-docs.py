#!/usr/bin/env python3
"""Comprueba que los enlaces relativos de README y docs existan.

Falla ante un enlace roto: la documentación es parte del producto (RNF-49).
"""

from __future__ import annotations

import re
import sys
from pathlib import Path
from urllib.parse import urlparse

ROOT = Path(__file__).resolve().parent.parent
LINK_RE = re.compile(r"(?<!!)\[[^\]]*\]\(([^)\s]+)(?:\s+\"[^\"]*\")?\)")
SKIP_SCHEMES = {"http", "https", "mailto", "tel", "data"}


def markdown_files() -> list[Path]:
    files = [ROOT / "README.md"]
    files.extend(sorted((ROOT / "docs").rglob("*.md")))
    return [path for path in files if path.is_file()]


def main() -> int:
    errors: list[str] = []
    checked = 0

    for path in markdown_files():
        text = path.read_text(encoding="utf-8")
        for match in LINK_RE.finditer(text):
            target = match.group(1).strip()
            if not target or target.startswith("#"):
                continue
            parsed = urlparse(target)
            if parsed.scheme in SKIP_SCHEMES or target.startswith("//"):
                continue
            if parsed.scheme:
                continue

            relative = target.split("#", 1)[0]
            if not relative:
                continue
            candidate = (path.parent / relative).resolve()
            checked += 1
            if not candidate.exists():
                line = text.count("\n", 0, match.start()) + 1
                errors.append(f"{path.relative_to(ROOT)}:{line}: enlace roto -> {target}")

    if errors:
        print("Enlaces relativos roto(s):", file=sys.stderr)
        for error in errors:
            print(f"  {error}", file=sys.stderr)
        return 1

    print(f"Docs OK: {checked} enlaces relativos comprobados en {len(markdown_files())} ficheros.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
