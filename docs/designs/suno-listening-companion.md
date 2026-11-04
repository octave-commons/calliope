# Suno listening companion

This bounded implementation follows Err's 2026-10-03 request: use Suno to
listen, generate, edit and separate stems; use Calliope to keep metadata and
repeatable curation judgments. It adds a native companion to the approved
workbench direction without claiming the local-player milestone, publication
features, or knowledge-graph implementation are complete.

## Use it

From Calliope, run `scripts/listen`, or `clojure -M:companion`. A native desktop
display is required. No server or browser runtime is embedded.

1. Listen in Suno. Search Calliope by title, lyric, style, label or project.
   Paste a Suno song URL into the search field to find that exact render.
2. Read Lyrics, Styles and Metadata. Generation controls are recorded intent;
   they are separate from your ratings of what you hear. Missing controls stay
   unknown. Metadata preserves unfamiliar fields such as V6 `aug_creativity`.
3. Write a thought, comma-separated labels, and as many project/list memberships
   as useful. Use names such as `motif/choir`, `world/river`, `salvage/intro`,
   and `stems-shortlist`. These are human-authored annotations, not Rheos cards.
4. Choose keep, salvage, reject, or unreviewed. Optional ratings are enjoyment,
   publishability, weirdness, salvageability, and technical quality, each 0–5.
   An absent rating is different from zero.
5. Add an optional point or range in seconds, `m:ss`, or `h:mm:ss`. Edits save
   automatically after a 600 ms pause, when changing songs, and before closing.
   Save now (`Ctrl+Enter`) flushes immediately. These are revisit/salvage markers,
   not accepted cuts or stems. Partial marker text is retained until it forms a
   valid range; incomplete input never becomes a valid span.

`Ctrl+L` focuses search. Search and disposition filters work while editing;
pending edits stay attached to their exact render. Clicking a column heading
sorts the table. Horizontal scrolling
keeps the title and metadata columns readable. The filter selects dispositions;
search includes historical note text and current labels/project memberships. Open in Suno
opens the selected song using the system browser. It does not start playback.

Returning to a render restores its note, labels, projects, ratings, and marker
fields, including pending edits. Disk writes run serially without disabling the
library or ordinary editing. Save callbacks update their own render and never
replace the form for another render. Table cells and dropdown choices explicitly
render text against the palette in selected and focused states.

Undo (`Ctrl+Z`) and Redo (`Ctrl+Shift+Z` or `Ctrl+Y`) move through the selected
render's saved editing batches. Unsaved edits are flushed before a history move,
so Undo can undo the batch just typed. The History tree shows every branch;
select a revision to inspect it, then Restore selected revision to make it the
active form. Editing after Undo creates a sibling branch and keeps the old
future. Redo prefers the child last traversed by Undo, or the latest child when
there is no preference. The tree and active revision replay after restart.

```text
A → B          Undo B, then edit C:
               A ┬→ B   (retained)
                 └→ C   (active)
```

A failed disk write retains drafts in the current window, displays an error,
and cancels closing. Reload reads the latest durable heads, then retries retained
forms as new revisions. Save now retries against the currently loaded heads.
Concurrent writers are checked against an expected head, so stale forms never
silently overwrite another writer's revision. In-memory drafts survive navigation;
only flushed events survive process loss. No-op flushes append nothing.

## Metadata intake

```bash
clojure -M:suno-meta import "$HOME/Downloads/Suno Downloads"
clojure -M:suno-meta import path/to/new-export.json
clojure -M:suno-meta project
clojure -M:suno-meta stats
```

The UI's Import metadata button accepts a file or folder. Inputs are a raw Suno
clip object, an array of clip objects, a JSON object with a `clips` array, or
legacy Suno text with `Title:`, `ID:`, `Tags:` and `Prompt:` headers. Unsupported
JSON envelopes fail visibly. Valid files can be imported even when other files
are rejected; the result lists rejected names and errors and CLI exits nonzero.
Missing source paths fail. Only JSON/TXT files are read. No audio or artwork is
downloaded, copied, opened or required by the importer.

The [Suno Manager listing](https://chromewebstore.google.com/detail/suno-manager/bhfnejgfonhlaalgnhcphpjbbifidpki)
was read on 2026-10-03. The publisher lists v3.5.7, updated 2026-09-06, and says
the old bulk downloader was removed after Suno's download change. Its replacement
archives songs as they are scrolled past and exports lyrics/styles/settings as
JSON, CSV or a readable document. Use its JSON export for future intake. Capture
coverage depends on which songs it has seen; an export is not proof that every
workspace, trashed song or hidden render was captured. The exact v3.5.7 export
envelope and installed extension version were not available for verification.

No replacement extension is needed for this slice. We do not store account
credentials or bypass audio download restrictions. Download quotas reported by
the publisher are not independently verified against this account.

## Durable boundaries

- `ledgers/ingest.edn`: append-only `:suno/metadata-observed` events, with a full
  source SHA-256, source basename/format, normalized clip and sanitized original
  renderer object. Account/credential fields are removed recursively. URLs are
  references, never downloaded. Source filename variation is recorded separately
  and is not a musical variance measurement.
- Exact observation identity is schema + Suno UUID + SHA-256 of canonical
  sanitized clip data. A new export wrapper/time does not duplicate every clip;
  changed clip evidence appends. A sparse TXT observation does not overwrite
  richer JSON. Same titles never merge clip IDs or establish work identity.
- `ledgers/projections/suno-meta-v1.edn`: a deterministic rebuild from canonical
  events, without a volatile generated timestamp. The latest imported JSON
  observation is chosen when available; legacy TXT is the fallback. Source
  deletion does not delete historical observations.
- `ledgers/studio.edn`: append-only listening revisions and history moves with
  user authorship and exact render scope. New `:listening/review-recorded` v2
  events contain a parent event ID and only changed fields. Legacy v1 full
  review snapshots replay as revisions without rewriting any bytes.
  `:listening/history-moved` events record from/to revision IDs and
  undo/redo/checkout action. Cursor moves and branches are durable; no prior
  event is deleted. Appends take a lock and sync to disk. Saving does not change
  provider likes, playlists or workspace membership.
- Mutable UI/read models remain projections. No similarity inference, classifier
  suggestion or graph traversal accepts a relation automatically.

Filesystem writes take a lock per ledger within this adapter. Other legacy
writers are not automatically coordinated by that lock; avoid concurrent corpus
ingestion with a companion import. This slice records render-scoped judgments;
work/clip/arrangement scopes, structured smart-list queries and stem jobs remain
future integration work.

## Explicit maintenance and Drive boundary

There is no automatic retention cutoff or history squashing. The 2026-10-04
direction is to keep the small events and introduce maintenance deliberately
when needed. These operations are future work, not implemented commands:

- **Snapshot**: a versioned replay checkpoint anchored to a ledger prefix hash,
  with all retained event IDs, branches, heads and redo preferences. Replaying
  the suffix must reproduce the complete view exactly.
- **Trim**: a reviewed retention selection, with a reachability report, archive
  manifest and verified original event bytes before retiring local history.
- **Flatten/squash**: an explicitly chosen compact representation. State whether
  it preserves branch history or intentionally removes undo reachability, and
  keep links to original event IDs and archive hashes. A current-state export is
  a derived view, not a silent replacement for the event ledger.
- **Drive archive**: upload/download immutable batches and manifests; verify
  hashes before marking a remote copy usable or removing any local source.

Axxium's public `/api/auth/config` reported `googleEnabled: true` on 2026-10-04.
Its existing Google adapter requests `openid email profile` and supports OIDC
code + PKCE login. That establishes identity. It does not authorize Drive access.
Future Drive integration should use Axxium's named external adapter boundary and
request per-file `drive.file` access for chosen or app-created archives, with
separate consent and credential storage. The Google Drive scope authority is
[Google's API-specific authorization guide](https://developers.google.com/workspace/drive/api/guides/api-specific-auth).
No Google sign-in, Drive consent, OAuth client changes, or remote uploads were
performed during this companion repair.

## Design evidence

The [Claude Calliope Muse session and workbench](https://claude.ai/design/p/f180f278-a28f-44ae-8e8d-a1fb48ac8c39?file=Fork+Tales+Workbench+v2.html&via=share)
were accessible through the user's Chrome session. The design exposes a render
library, inspector, scoped ratings, user/proposed labels, smart lists, extractor
and claim screens, graph search and Muse. The session explicitly describes
seeded labels, limited graph scale and human acceptance. Its displayed results
are design/demo evidence, not imported corpus judgments.

The [ChatGPT Calliope Studio](https://calliope-studio.enso-err.chatgpt.site/)
supplies the compact disposition/listening-note/marker flow and the separation
between a style prompt and lyric intent. The companion follows those interaction
choices; it does not copy mock transport or synthesis into a metadata tool.

The native palette is a small adapter from `uxx/tokens/src/colors.ts` Monokai.
Uxx's React components are not claimed to run in Swing. Palette values have one
resource authority here, `resources/companion/monokai.edn`.

Software and process documentation: GPL-3.0-or-later. Creative corpus sources
retain their existing licenses.
