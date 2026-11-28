#!/usr/bin/env python3
"""Valida docs/seed-ejercicios.csv y su migración Flyway V6 (M2)."""

from __future__ import annotations

import csv
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
CSV_PATH = ROOT / "docs" / "seed-ejercicios.csv"
SQL_PATH = ROOT / "backend" / "src" / "main" / "resources" / "db" / "migration" / "V6__catalog_seed.sql"

MUSCLES = {
    "CHEST", "BACK", "SHOULDERS", "BICEPS", "TRICEPS", "QUADS", "HAMSTRINGS",
    "GLUTES", "ADDUCTORS", "CALVES", "CORE", "FOREARMS", "NECK", "FULL_BODY",
}
EQUIPMENT = {"BARBELL", "DUMBBELL", "MACHINE", "CABLE", "BODYWEIGHT", "KETTLEBELL", "BAND", "OTHER"}
MECHANICS = {"COMPOUND", "ISOLATION"}
EXPECTED_HEADER = ["name_es", "name_en", "muscle_group", "secondary", "equipment", "mechanics", "bodyweight"]


def main() -> int:
    errors: list[str] = []
    with CSV_PATH.open(encoding="utf-8", newline="") as handle:
        reader = csv.DictReader(handle)
        if reader.fieldnames != EXPECTED_HEADER:
            errors.append(f"cabecera inesperada: {reader.fieldnames}")
        rows = list(reader)

    if len(rows) < 150:
        errors.append(f"solo hay {len(rows)} ejercicios; el mínimo es 150")

    names_es: set[str] = set()
    names_en: set[str] = set()
    for line, row in enumerate(rows, start=2):
        name_es = (row["name_es"] or "").strip()
        name_en = (row["name_en"] or "").strip()
        if not name_es or not name_en:
            errors.append(f"línea {line}: nombre vacío")
            continue
        if name_es in names_es or name_en in names_en:
            errors.append(f"línea {line}: nombre duplicado ({name_es} / {name_en})")
        names_es.add(name_es)
        names_en.add(name_en)
        if row["muscle_group"] not in MUSCLES:
            errors.append(f"línea {line}: muscle_group inválido {row['muscle_group']}")
        if row["equipment"] not in EQUIPMENT:
            errors.append(f"línea {line}: equipment inválido {row['equipment']}")
        if row["mechanics"] not in MECHANICS:
            errors.append(f"línea {line}: mechanics inválido {row['mechanics']}")
        if row["bodyweight"] not in {"true", "false"}:
            errors.append(f"línea {line}: bodyweight debe ser true/false")
        for secondary in filter(None, (row["secondary"] or "").split(";")):
            if secondary not in MUSCLES:
                errors.append(f"línea {line}: secundario inválido {secondary}")

    sql = SQL_PATH.read_text(encoding="utf-8")
    for name in names_es:
        if name.replace("'", "''") not in sql:
            errors.append(f"la migración V6 no contiene '{name}'")

    if errors:
        for error in errors:
            print(f"FAIL  {error}", file=sys.stderr)
        return 1
    print(f"Seed OK: {len(rows)} ejercicios, únicos y presentes en V6.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
