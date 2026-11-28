#!/usr/bin/env python3
"""Pipeline de imágenes de ejercicios.

1. Descarga el catálogo RepDB free tier.
2. Genera una propuesta para cada ejercicio de docs/seed-ejercicios.csv.
3. Separa coincidencias exactas de las que requieren revisión.
4. Con `--apply`, descarga únicamente las aprobadas y actualiza los WebP locales.

Uso:
  python3 scripts/exercise-images.py             # genera CSV + HTML de revisión
  python3 scripts/exercise-images.py --apply     # aplica auto + approved=yes
"""
from __future__ import annotations

import argparse
import csv
import html
import json
import re
import subprocess
import sys
import unicodedata
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SEED = ROOT / "docs/seed-ejercicios.csv"
REVIEW = ROOT / "docs/exercise-image-review.csv"
REVIEW_HTML = ROOT / "docs/exercise-image-review.html"
IMAGES = ROOT / "frontend/public/exercises"
REPDB_URL = "https://raw.githubusercontent.com/RepDB/exercise-dataset/main/exercises.json"
IMAGE_BASE = "https://raw.githubusercontent.com/RepDB/exercise-dataset/main/"
STOP = {"de", "del", "con", "en", "la", "el", "los", "las", "and", "with", "the", "of", "a", "para"}


def norm(value: str) -> str:
    value = unicodedata.normalize("NFKD", value).encode("ascii", "ignore").decode().lower()
    return " ".join(token for token in re.sub(r"[^a-z0-9]+", " ", value).split() if token not in STOP)


def tokens(value: str) -> set[str]:
    return set(norm(value).split())


def score(source: dict, candidate: dict) -> tuple[float, int, int]:
    source_tokens = tokens(source["name_es"]) | tokens(source["name_en"])
    candidate_tokens = tokens(candidate["name_es"]) | tokens(candidate["name_en"])
    inter = len(source_tokens & candidate_tokens)
    union = len(source_tokens | candidate_tokens) or 1
    name_score = inter / union
    equipment = 1 if source["equipment"].lower() in str(candidate.get("equipment", "")).lower() else 0
    muscle = 1 if source["muscle_group"].lower() in str(candidate.get("primary_muscles", [])).lower() else 0
    return (name_score * 0.7 + equipment * 0.2 + muscle * 0.1, equipment, muscle)


def image_path(candidate: dict) -> str | None:
    flat = candidate.get("images", {}).get("flat", {})
    return flat.get("main") or flat.get("start") or flat.get("peak")


def download(candidate: dict, target: Path) -> bool:
    path = image_path(candidate)
    if not path:
        return False
    try:
        urllib.request.urlretrieve(IMAGE_BASE + path, target)
        return target.stat().st_size > 0
    except Exception as exception:
        print(f"No se pudo descargar {path}: {exception}", file=sys.stderr)
        return False


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--apply", action="store_true")
    args = parser.parse_args()

    with urllib.request.urlopen(REPDB_URL, timeout=30) as response:
        candidates = json.load(response)["exercises"]
    rows = list(csv.DictReader(SEED.open(encoding="utf-8")))
    IMAGES.mkdir(parents=True, exist_ok=True)

    auto: list[dict] = []
    review: list[dict] = []
    for index, row in enumerate(rows, start=1):
        ranked = sorted(
            ((score(row, candidate), candidate) for candidate in candidates),
            key=lambda item: item[0][0],
            reverse=True,
        )
        exact = next(
            (
                candidate
                for candidate in candidates
                if norm(row["name_es"]) == norm(candidate["name_es"])
                or norm(row["name_en"]) == norm(candidate["name_en"])
            ),
            None,
        )
        chosen = exact or ranked[0][1]
        exact_match = exact is not None
        entry = {
            **row,
            "index": index,
            "file": f"ex-{index:03d}.webp",
            "candidate_id": chosen["id"],
            "candidate_name": chosen["name_es"],
            "candidate_en": chosen["name_en"],
            "score": f"{ranked[0][0][0]:.3f}",
            "equipment_match": str(ranked[0][0][1]),
            "muscle_match": str(ranked[0][0][2]),
            "status": "auto" if exact_match else "review",
            "approved": "yes" if exact_match else "",
        }
        (auto if exact_match else review).append(entry)

    with REVIEW.open("w", encoding="utf-8", newline="") as handle:
        fields = [
            "index", "name_es", "name_en", "muscle_group", "equipment",
            "candidate_id", "candidate_name", "candidate_en", "score",
            "equipment_match", "muscle_match", "status", "approved", "file",
        ]
        writer = csv.DictWriter(handle, fieldnames=fields, extrasaction='ignore', lineterminator='\n')
        writer.writeheader()
        writer.writerows(auto + review)

    cards = []
    by_id = {candidate["id"]: candidate for candidate in candidates}
    for entry in review:
        candidate = by_id[entry["candidate_id"]]
        path = image_path(candidate)
        image = f'<img src="{IMAGE_BASE + path}" alt="">' if path else ""
        cards.append(
            f"<article><h3>{html.escape(entry['name_es'])}</h3>"
            f"<p>{html.escape(entry['name_en'])} · {html.escape(entry['muscle_group'])} · "
            f"{html.escape(entry['equipment'])}</p><div class='pair'>{image}<div>"
            f"<strong>{html.escape(entry['candidate_name'])}</strong><br>"
            f"<small>score {entry['score']} · eq {entry['equipment_match']} · "
            f"músculo {entry['muscle_match']}</small></div></div></article>"
        )
    REVIEW_HTML.write_text(
        "<!doctype html><meta charset='utf-8'><title>Revisión de imágenes</title>"
        "<style>body{font:14px system-ui;background:#111;color:#eee;padding:24px}"
        "main{display:grid;grid-template-columns:repeat(auto-fill,minmax(280px,1fr));gap:16px}"
        "article{border:1px solid #333;padding:12px;border-radius:8px}"
        "img{width:128px;height:128px;object-fit:cover;border-radius:6px}"
        ".pair{display:flex;gap:12px;align-items:center}</style>"
        "<h1>Revisión de imágenes de ejercicios</h1>"
        "<p>Edita <code>docs/exercise-image-review.csv</code>: pon <code>yes</code> o "
        "<code>no</code> en <code>approved</code>. Después ejecuta "
        "<code>python3 scripts/exercise-images.py --apply</code>.</p>"
        f"<main>{''.join(cards)}</main>",
        encoding="utf-8",
    )

    if args.apply:
        applied = 0
        for entry in auto + review:
            if entry["approved"].lower() not in {"yes", "true", "1"}:
                continue
            candidate = by_id[entry["candidate_id"]]
            if download(candidate, IMAGES / entry["file"]):
                applied += 1
        print(f"Imágenes aplicadas: {applied}")

    print(f"Auto: {len(auto)} · revisión: {len(review)} · total: {len(rows)}")
    print(f"CSV: {REVIEW.relative_to(ROOT)}")
    print(f"HTML: {REVIEW_HTML.relative_to(ROOT)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
