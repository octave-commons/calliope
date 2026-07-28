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
ALLOWED_CATEGORIES = {"epics", "stories", "chores"}
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
OWNER_OPTIONAL_STATUSES = {"icebox", "incoming"}


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


def dependency_cycles(graph: dict[str, list[str]]) -> list[list[str]]:
    """Return canonical directed dependency cycles discovered by DFS."""

    state: dict[str, int] = {}
    stack: list[str] = []
    positions: dict[str, int] = {}
    cycles: dict[tuple[str, ...], list[str]] = {}

    def visit(node: str) -> None:
        state[node] = 1
        positions[node] = len(stack)
        stack.append(node)
        for target in sorted(graph[node]):
            target_state = state.get(target, 0)
            if target_state == 0:
                visit(target)
            elif target_state == 1:
                members = stack[positions[target] :]
                rotations = [
                    tuple(members[index:] + members[:index])
                    for index in range(len(members))
                ]
                key = min(rotations)
                cycles[key] = [*key, key[0]]
        stack.pop()
        positions.pop(node)
        state[node] = 2

    for node in sorted(graph):
        if state.get(node, 0) == 0:
            visit(node)
    return [cycles[key] for key in sorted(cycles)]


def main() -> int:
    errors: list[str] = []
    paths: list[Path] = []

    for path in sorted(TASKS.rglob("*.md")):
        task_path = path.relative_to(TASKS)
        relative = path.relative_to(ROOT)
        if len(task_path.parts) == 1:
            errors.append(f"{relative}: prose/card at tasksDir root")
            continue
        if task_path.parts[0] not in ALLOWED_CATEGORIES:
            errors.append(f"{relative}: card outside an allowed category")
            continue
        paths.append(path)

    cards: dict[str, tuple[Path, dict[str, str]]] = {}

    for path in paths:
        relative = path.relative_to(ROOT)
        task_path = path.relative_to(TASKS)
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
        if status not in OWNER_OPTIONAL_STATUSES and not unquote(
            frontmatter.get("owner", "")
        ):
            errors.append(f"{relative}: missing owner outside icebox/incoming")

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
        expected_category = task_path.parts[0]
        if category and category != expected_category:
            errors.append(
                f"{relative}: category {category!r} does not match {expected_category!r}"
            )
        if card_type == "epic" and status not in {
            "breakdown",
            "icebox",
            "done",
            "rejected",
        }:
            errors.append(
                f"{relative}: decomposed epic should be breakdown or deliberately deferred"
            )

    graph: dict[str, list[str]] = {uuid: [] for uuid in cards}
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
            else:
                graph[uuid].append(target)

    for cycle in dependency_cycles(graph):
        errors.append(f"dependency cycle: {' -> '.join(cycle)}")

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

    counts = ", ".join(
        f"{status}={count}" for status, count in sorted(status_counts.items())
    )
    print(f"Rheos board valid: {len(cards)} cards ({counts})")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
