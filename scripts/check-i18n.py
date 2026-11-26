#!/usr/bin/env python3
"""Falla si los catálogos ES/EN no tienen exactamente las mismas claves."""
import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
FILES = {
    "es": ROOT / "frontend/public/assets/i18n/es.json",
    "en": ROOT / "frontend/public/assets/i18n/en.json",
}


def flatten(value, prefix=""):
    keys = {}
    for key, item in value.items():
        full = f"{prefix}.{key}" if prefix else key
        if isinstance(item, dict):
            keys.update(flatten(item, full))
        else:
            keys[full] = item
    return keys


def main():
    catalogs = {lang: flatten(json.loads(path.read_text(encoding="utf-8"))) for lang, path in FILES.items()}
    failed = False
    for lang, keys in catalogs.items():
        empty = sorted(key for key, value in keys.items() if isinstance(value, str) and not value.strip())
        if empty:
            print(f"FAIL  {lang}: claves vacías: {', '.join(empty)}", file=sys.stderr)
            failed = True
    for left, right in (("es", "en"), ("en", "es")):
        missing = sorted(set(catalogs[left]) - set(catalogs[right]))
        if missing:
            print(f"FAIL  {left} tiene claves que faltan en {right}: {', '.join(missing)}", file=sys.stderr)
            failed = True
    if failed:
        return 1
    print(f"i18n OK: {len(catalogs['es'])} claves en ES y EN")
    return 0


if __name__ == "__main__":
    sys.exit(main())
