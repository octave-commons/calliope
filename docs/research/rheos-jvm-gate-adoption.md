# Adopt the upstream Rheos JVM build-gate extension

Date: 2026-10-25. License: GPL-3.0-or-later.
Status: observed engine evidence and bounded configuration adoption.

The studio goal encountered a real lifecycle refusal: FT-000A was reconciled
against its existing human approval sources and advanced through Rheos to
in_progress, but in_progress -> review ran the old default `pnpm build`.
Calliope is a JVM repository, so that command failed. The refusal was respected;
no card status or event history was hand-edited.

## Existing upstream repair

Current [Rheos configuration documentation](https://github.com/open-hax/rheos/blob/9043d1037519e9d0be640130b75dc9c4348444fc/docs/configuration.md)
defines the supported Promethean extension:

```json
{"fsm":{"extends":"promethean","buildGateCommands":["clojure -M:test"]}}
```

The owning implementation is
`src/rheos/backend/law/fsm.cljs::resolve-fsm`. It retains the upstream Promethean
FSM and replaces only the configured build commands/cwd; this repository neither
copies the FSM nor reimplements board checks, parsing or transitions.

The installed/published canonical CLI is `@eta-mu/rheos@0.2.0`. Npm metadata
observed integrity:
`sha512-spAAKcHZr+30XYOMGytGzUNy3/J0CdycLb8vxn83EfLDverxM4tlkkbZWh9Bcd1eIh16T8rJjEsv6JS+yCfT3Q==`.
The source tree's package version currently differs from the published CLI;
this is not a claim to have reproduced its build from that commit. Actual
installed behavior was tested directly before adoption.

## Behavioral evidence

A disposable board outside the repository's tasksDir was operated exclusively
through the canonical Rheos CLI:

- Configured the upstream extension with `clojure -M:test` and this repository
  as cwd; used normal incoming/accepted/breakdown/ready/todo/in_progress states.
- Rheos executed the command on in_progress -> review. The suite reported
  112 tests / 699 assertions, zero failures/errors under umask 0022. A separate
  `rheos read-task` confirmed review.
- Returned that fixture through Rheos to in_progress, configured `exit 7`,
  and attempted review. Rheos exited 3 and reported the command's exit code 7.
  This proves configured commands are enforced, not silently ignored.
- No production acceptance was inferred from the fixture.

## Consumer change

`openhax.kanban.json` now extends the upstream Promethean FSM and declares
the real JVM test/lint commands. Umask 0022 matches CI and the existing dataset
permission-test expectation; the observed failures under umask 0002 remain
documented rather than silently claimed repaired.

Repository Contracts pins the canonical Rheos CLI instead of eta-mu 1.1.1, whose
historical FSM/config behavior cannot consume this extension. CI reads the board
through Rheos and independently runs the Clojure suite and zero-warning lint.
This removes the old local gate mismatch without adding a package.json, local
Rheos validator, copied transition table or bypass.

Current main already contains a newer upstream design-frontmatter repair, but it
is not in the published CLI. Existing card bodies therefore link the proposed
studio contract; no unsupported frontmatter mutation is bypassed.
