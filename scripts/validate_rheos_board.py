#!/usr/bin/env python3
"""Validate Fork Tales' repository-specific Rheos card contract.

Rheos intentionally exposes a small task shape. This validator checks the richer
Markdown relationships and readiness rules that remain authoritative in Git.
"""

from __future__ import annotations

import re
import sys
from collections import Counter
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
TASKS = ROOT / "docs" / "kanban"
CARD_DIRS = tuple(TASKS / name for name in ("epics", "stories", "chores"))
# The full state vocabulary of the `promethean` FSM shipped by eta-mu 1.1.1.
VALID_STATUSES = {
    "icebox",
    "incoming",
    "accepted",
    "breakdown",
    "blocked",
    "ready",
    "todo",
    "in_progress",
    "testing",
    "review",
    "document",
    "done",
    "rejected",
    "archived",
}


def unquote(value: str) -> str:
    value = value.strip()
    if len(value) >= 2 and value[0] == value[-1] and value[0] in {'"', "'"}:
        return value[1:-1]
    return value


def parse_list(value: str) -> list[str]:
    value = value.strip()
    if not value:
        return []
    if value == "[]":
        return [""]
    if value.startswith("[") and value.endswith("]"):
        value = value[1:-1]
    return [unquote(part) for part in value.split(",") if unquote(part)]


def parse_card(path: Path) -> dict[str, str]:
    text = path.read_text(encoding="utf-8")
    if text and not text.endswith("\n"):
        raise ValueError("missing trailing newline")
    match = re.match(r"^---\n(.*?)\n---\n", text, re.S)
    if not match:
        raise ValueError("missing YAML frontmatter")
    frontmatter: dict[str, str] = {}
    for line in match.group(1).splitlines():
        item = re.match(r"^([A-Za-z0-9_-]+):\s*(.*)$", line)
        if item:
            frontmatter[item.group(1)] = item.group(2).strip()
    return frontmatter


def main() -> int:
    errors: list[str] = []

    root_markdown = sorted(TASKS.glob("*.md"))
    for path in root_markdown:
        errors.append(f"{path.relative_to(ROOT)}: prose/card at tasksDir root")

    paths = [path for directory in CARD_DIRS for path in sorted(directory.glob("*.md"))]
    cards: dict[str, tuple[Path, dict[str, str]]] = {}

    for path in paths:
        relative = path.relative_to(ROOT)
        try:
            frontmatter = parse_card(path)
        except (OSError, UnicodeError, ValueError) as exc:
            errors.append(f"{relative}: {exc}")
            continue

        uuid = unquote(frontmatter.get("uuid", ""))
        if not uuid:
            errors.append(f"{relative}: missing explicit uuid")
            continue
        if uuid in cards:
            errors.append(f"{relative}: duplicate uuid {uuid}")
        cards[uuid] = (path, frontmatter)

        if "id" in frontmatter:
            errors.append(f"{relative}: inert id field present; use uuid")

        status = unquote(frontmatter.get("status", "incoming"))
        if status not in VALID_STATUSES:
            errors.append(f"{relative}: invalid status {status!r}")

        try:
            points = int(unquote(frontmatter.get("points", "0")))
        except ValueError:
            errors.append(f"{relative}: points must be an integer")
            points = 0
        if status == "ready" and points > 5:
            errors.append(f"{relative}: ready card exceeds 5 points")

        labels = frontmatter.get("labels", "")
        if labels.startswith("["):
            errors.append(f"{relative}: labels must be a comma-separated scalar")

        if frontmatter.get("dependency") == "[]":
            errors.append(f"{relative}: omit empty dependency instead of []")

        card_type = unquote(frontmatter.get("type", ""))
        category = unquote(frontmatter.get("category", ""))
        expected_category = path.parent.name
        if category and category != expected_category:
            errors.append(
                f"{relative}: category {category!r} does not match {expected_category!r}"
            )
        if card_type == "epic" and status not in {"breakdown", "icebox", "done", "rejected"}:
            errors.append(f"{relative}: decomposed epic should be breakdown or deliberately deferred")

    for uuid, (path, frontmatter) in cards.items():
        relative = path.relative_to(ROOT)
        for key in ("epic", "parent"):
            target = unquote(frontmatter.get(key, ""))
            if target and target not in cards:
                errors.append(f"{relative}: unresolved {key} {target!r}")
        for target in parse_list(frontmatter.get("dependency", "")):
            if not target:
                errors.append(f"{relative}: phantom empty dependency")
            elif target not in cards:
                errors.append(f"{relative}: unresolved dependency {target!r}")
            elif target == uuid:
                errors.append(f"{relative}: self-dependency")

    status_counts = Counter(
        unquote(frontmatter.get("status", "incoming"))
        for _, frontmatter in cards.values()
    )
    if status_counts["in_progress"] > 2:
        errors.append("board exceeds WIP limit: more than 2 in_progress")
    if status_counts["review"] > 1:
        errors.append("board exceeds WIP limit: more than 1 review")

    if errors:
        print("Rheos board validation failed:", file=sys.stderr)
        for error in errors:
            print(f"- {error}", file=sys.stderr)
        return 1

    counts = ", ".join(f"{status}={count}" for status, count in sorted(status_counts.items()))
    print(f"Rheos board valid: {len(cards)} cards ({counts})")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
