#!/usr/bin/env python3
from __future__ import annotations

import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
TASKS = ROOT / "docs" / "kanban"
DOCS = ROOT / "docs" / "kanban-docs"
CARD_DIRS = [TASKS / "epics", TASKS / "stories", TASKS / "chores"]

STATUS_OVERRIDES = {
    "docs/kanban/epics/ft-000-design-authority-and-studio-foundation.md": "breakdown",
    "docs/kanban/epics/ft-001-daily-driver-player.md": "breakdown",
    "docs/kanban/epics/ft-002-focused-curation.md": "breakdown",
    "docs/kanban/epics/ft-003-salvage-and-arrangement.md": "breakdown",
    "docs/kanban/epics/ft-004-release-and-publication.md": "icebox",
    "docs/kanban/stories/ft-000a-review-media-workbench-authority.md": "done",
    "docs/kanban/stories/ft-000b-define-studio-domain-laws.md": "ready",
    "docs/kanban/chores/ft-ops-002-review-authority-statuses.md": "done",
    "docs/kanban/chores/ft-ops-003-document-first-review-disposition.md": "done",
}

DEPENDENCY_OVERRIDES = {
    "docs/kanban/stories/ft-000b-define-studio-domain-laws.md": [
        "ft-000a-review-and-accept-or-revise-media-workbench-authority"
    ],
    "docs/kanban/stories/ft-000c-define-studio-events-and-projection.md": [
        "ft-000b-define-media-workbench-domain-laws",
        "ft-000d-decide-native-desktop-playback-read-model-and-application-topology",
    ],
    "docs/kanban/stories/ft-001a-index-playable-media.md": [
        "ft-000b-define-media-workbench-domain-laws",
        "ft-000c-define-append-only-studio-events-and-read-projection",
        "ft-000d-decide-native-desktop-playback-read-model-and-application-topology",
    ],
    "docs/kanban/stories/ft-001b-implement-playback-and-queue.md": [
        "ft-000d-decide-native-desktop-playback-read-model-and-application-topology",
        "ft-001a-index-playable-media-metadata-and-waveform-jobs",
    ],
    "docs/kanban/stories/ft-001c-build-player-shell-and-library.md": [
        "ft-000d-decide-native-desktop-playback-read-model-and-application-topology",
        "ft-001b-implement-playback-resolver-persistent-queue-and-resume",
    ],
}

ALLOWED_STATUSES = {
    "icebox", "incoming", "accepted", "breakdown", "ready", "todo",
    "in_progress", "blocked", "review", "document", "done", "rejected",
}


def rel(path: Path) -> str:
    return path.relative_to(ROOT).as_posix()


def slugify(value: str) -> str:
    return re.sub(r"[^a-z0-9]+", "-", value.strip().lower()).strip("-") or "task"


def unquote(value: str) -> str:
    value = value.strip()
    if len(value) >= 2 and value[0] == value[-1] and value[0] in {"'", '"'}:
        return value[1:-1]
    return value


def parse_list(value: str) -> list[str]:
    value = value.strip()
    if not value or value == "[]":
        return []
    if value.startswith("[") and value.endswith("]"):
        value = value[1:-1]
    return [unquote(part.strip()) for part in value.split(",") if unquote(part.strip())]


def quote_list(values: list[str]) -> str:
    return "[" + ", ".join(f'"{value}"' for value in values) + "]"


def split_card(path: Path) -> tuple[list[str], str]:
    text = path.read_text(encoding="utf-8")
    match = re.match(r"^---\n(.*?)\n---\n?(.*)$", text, re.S)
    if not match:
        raise RuntimeError(f"missing frontmatter: {path}")
    return match.group(1).splitlines(), match.group(2)


def fm_map(lines: list[str]) -> dict[str, str]:
    result: dict[str, str] = {}
    for line in lines:
        match = re.match(r"^([A-Za-z0-9_-]+):\s*(.*)$", line)
        if match:
            result[match.group(1)] = match.group(2).strip()
    return result


def card_paths() -> list[Path]:
    return [path for directory in CARD_DIRS for path in sorted(directory.glob("*.md"))]


def write_card(path: Path, lines: list[str], body: str) -> None:
    path.write_text("---\n" + "\n".join(lines) + "\n---\n\n" + body.lstrip().rstrip() + "\n", encoding="utf-8")


def migrate_cards() -> None:
    mapping: dict[str, str] = {}
    for path in card_paths():
        lines, _ = split_card(path)
        fm = fm_map(lines)
        title = unquote(fm.get("title", path.stem))
        uuid = unquote(fm.get("uuid", "")) or slugify(title)
        old_id = unquote(fm.get("id", ""))
        mapping[uuid] = uuid
        if old_id:
            mapping[old_id] = uuid

    for path in card_paths():
        lines, body = split_card(path)
        fm = fm_map(lines)
        title = unquote(fm.get("title", path.stem))
        uuid = unquote(fm.get("uuid", "")) or mapping.get(unquote(fm.get("id", "")), slugify(title))
        replacements: dict[str, str | None] = {
            "uuid": f'uuid: "{uuid}"',
            "id": None,
        }
        if "labels" in fm:
            replacements["labels"] = "labels: " + ", ".join(parse_list(fm["labels"]))
        for key in ("epic", "parent"):
            if key in fm:
                target = unquote(fm[key])
                replacements[key] = f'{key}: "{mapping.get(target, target)}"'

        override = DEPENDENCY_OVERRIDES.get(rel(path))
        dependencies = override if override is not None else [mapping.get(x, x) for x in parse_list(fm.get("dependency", ""))]
        replacements["dependency"] = f"dependency: {quote_list(dependencies)}" if dependencies else None
        if rel(path) in STATUS_OVERRIDES:
            replacements["status"] = f"status: {STATUS_OVERRIDES[rel(path)]}"

        emitted = {"uuid"}
        output = [f'uuid: "{uuid}"']
        for line in lines:
            match = re.match(r"^([A-Za-z0-9_-]+):\s*(.*)$", line)
            if not match:
                output.append(line)
                continue
            key = match.group(1)
            if key in {"id", "uuid"}:
                continue
            if key in replacements:
                value = replacements[key]
                if value is not None:
                    output.append(value)
                emitted.add(key)
            else:
                output.append(line)
                emitted.add(key)
        for key, value in replacements.items():
            if key not in emitted and value is not None:
                output.append(value)
        write_card(path, output, body)


def move_prose() -> None:
    DOCS.mkdir(parents=True, exist_ok=True)
    for name in ("AGENTS.md", "README.md", "BOARD-BREAKDOWN.md"):
        source = TASKS / name
        if source.exists():
            target = DOCS / name
            target.write_text(source.read_text(encoding="utf-8").rstrip() + "\n", encoding="utf-8")
            source.unlink()


def patch_ignore() -> None:
    path = ROOT / ".gitignore"
    text = path.read_text(encoding="utf-8").rstrip()
    text = text.replace("docs/kanban/.events/ledger.edn", "docs/kanban/.events/")
    if "docs/kanban/board.json" not in text:
        text += "\ndocs/kanban/board.json"
    path.write_text(text + "\n", encoding="utf-8")


def ensure_newlines() -> None:
    for path in (ROOT / "docs").rglob("*.md"):
        text = path.read_text(encoding="utf-8")
        if text and not text.endswith("\n"):
            path.write_text(text + "\n", encoding="utf-8")


def validate() -> None:
    errors: list[str] = []
    if list(TASKS.glob("*.md")):
        errors.append("tasksDir root still contains markdown")
    cards: dict[str, tuple[Path, dict[str, str]]] = {}
    for path in card_paths():
        lines, _ = split_card(path)
        fm = fm_map(lines)
        uuid = unquote(fm.get("uuid", ""))
        if not uuid:
            errors.append(f"{rel(path)} missing uuid")
            continue
        if uuid in cards:
            errors.append(f"duplicate uuid: {uuid}")
        cards[uuid] = (path, fm)
        if "id" in fm:
            errors.append(f"{rel(path)} still has id")
        status = unquote(fm.get("status", "incoming"))
        if status not in ALLOWED_STATUSES:
            errors.append(f"{rel(path)} invalid status {status}")
        if fm.get("dependency") == "[]":
            errors.append(f"{rel(path)} has empty dependency list")
        if fm.get("labels", "").startswith("["):
            errors.append(f"{rel(path)} labels remain flow-list encoded")
        try:
            points = int(unquote(fm.get("points", "0")))
            if status == "ready" and points > 5:
                errors.append(f"{rel(path)} ready with {points} points")
        except ValueError:
            errors.append(f"{rel(path)} invalid points")
    for uuid, (path, fm) in cards.items():
        for key in ("epic", "parent"):
            target = unquote(fm.get(key, ""))
            if target and target not in cards:
                errors.append(f"{rel(path)} unresolved {key}: {target}")
        for target in parse_list(fm.get("dependency", "")):
            if target not in cards:
                errors.append(f"{rel(path)} unresolved dependency: {target}")
            if target == uuid:
                errors.append(f"{rel(path)} self-dependency")
    if errors:
        raise SystemExit("\n".join(errors))
    print(f"migrated and validated {len(cards)} Rheos cards")


def main() -> None:
    move_prose()
    patch_ignore()
    migrate_cards()
    ensure_newlines()
    validate()
    Path(__file__).unlink()


if __name__ == "__main__":
    main()
