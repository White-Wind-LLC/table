# Documentation Site — Design

**Date:** 2026-06-17
**Status:** Approved
**Topic:** Split the large `README.md` (~1125 lines / 44 KB) into a dedicated documentation mini-site.

## Problem

`README.md` has grown to ~1125 lines covering 25+ sections. It is hard to navigate and discourages
discovery of features — e.g. issue #24 reports that the `embedded` parameter of `Table()` is
undocumented and had to be found by reading source. A dedicated documentation site with proper
navigation and an auto-generated API reference solves both the navigation problem and the
discoverability problem systematically.

## Goals

- A navigable documentation site replacing the monolithic README as the primary docs surface.
- Auto-generated API reference from KDoc (closes #24 and prevents the whole class of "undocumented
  parameter" issues).
- Live WASM demo remains available, co-located with the docs.
- Keep `README.md` as a short, welcoming landing page.

## Non-Goals

- Documentation versioning (latest-only; no `mike`). Can be added later if needed.
- Rewriting prose content — reuse existing README text, only restructure into pages.
- Unrelated refactoring of library code.

## Decisions (locked)

| Decision | Choice |
|---|---|
| Tooling | **MkDocs Material** (guides) + **Dokka** (API reference) |
| Hosting layout | Docs at Pages root, demo moved to `/demo` subpath |
| Content scope | README guides + Dokka API + CHANGELOG + live demo link |
| Versioning | Latest only |
| README after split | Short landing page linking to the site |
| Deploy trigger | `push` to `main` + tags `v*` + `workflow_dispatch` |

## Architecture

GitHub Pages allows a single deployment per `github-pages` environment. One CI job builds all three
parts and assembles them into one artifact:

```
site/                ← Pages root
├── index.html       ← MkDocs Material (guides)
├── guide/…          ← restructured README content
├── changelog/       ← CHANGELOG.md
├── api/             ← Dokka aggregated HTML  (closes #24)
└── demo/            ← table-sample WASM productionExecutable
```

MkDocs top navigation: **Guide · API Reference · Changelog · Live Demo**, where API Reference and
Live Demo are external links to the `api/` and `demo/` subpaths.

### Existing infrastructure leveraged

- **Dokka 2.2.0 is already wired** — applied to root and all subprojects in root `build.gradle.kts`
  (`allprojects { apply(plugin = "org.jetbrains.dokka") }`), with an aggregation hook already present
  in `convention/publishing.gradle.kts` (`dokkaGeneratePublicationHtml`). The site only needs to run
  the aggregation task and copy the HTML output into `site/api/`.
- **WASM demo build** already exists: `:table-sample:wasmJsBrowserDistribution` →
  `table-sample/build/dist/wasmJs/productionExecutable`.
- **GitHub Pages** is already configured (current `deploy-sample-pages.yml` deploys to the
  `github-pages` environment, concurrency group `pages`).

## Site content structure (`docs/`)

The current README sections are grouped into pages:

- **Getting Started:** Overview, Installation, Compatibility, Quick start
- **Guides:** Cell editing, Data grouping, Footer row, Filters, Fast Filters, Selection, Checkbox
  selection, Dynamic height & auto-width, Row reordering, Drag-to-scroll, Custom header icons
- **Modules:** `table-core`, `table-paging`, `table-format` (conditional formatting)
- **Reference:** manual Core API table + link to generated Dokka API
- **Project:** Supported targets, Third-party libraries, License, Changelog

Images in `docs/images/` stay in place and are reused. New Markdown pages live under `docs/` alongside
`docs/images/` and the existing `docs/superpowers/` specs directory (MkDocs `docs_dir` will point at a
content subfolder — e.g. `docs/site/` — so it does not pick up `images/` raw or the specs; exact
`docs_dir` layout finalized in the implementation plan to avoid collisions).

## README after split

Short landing page: badges, one-paragraph description, install snippet, minimal quick-start, and a
prominent link/button to the documentation site. All deep content moves to the site (single source of
truth — no duplication).

## Tooling & files

- `mkdocs.yml` — MkDocs Material configuration (nav, theme, repo links).
- `docs/requirements.txt` — `mkdocs-material` (+ any plugins used).
- Dokka — no new wiring; reuse the existing aggregation task, copy HTML to `site/api/`.
- Demo — reuse `:table-sample:wasmJsBrowserDistribution`, copy to `site/demo/`.

## CI / deploy

Replace `deploy-sample-pages.yml` with a single `deploy-docs.yml`:

- **Triggers:** `push` to `main`, tags `v*`, and `workflow_dispatch`.
- **Steps:**
  1. Checkout, setup JDK 17, setup Python.
  2. `./gradlew dokkaGenerate` (aggregated API HTML) and `:table-sample:wasmJsBrowserDistribution`.
  3. `pip install -r docs/requirements.txt`; `mkdocs build` → `site/`.
  4. Copy Dokka HTML → `site/api/`; copy WASM dist → `site/demo/`.
  5. `upload-pages-artifact` (`path: site`) → `deploy-pages`.
- Keep `permissions` (`pages: write`, `id-token: write`), `environment: github-pages`, concurrency
  group `pages`.
- Remove the old `deploy-sample-pages.yml` (its role is absorbed).

## Risks / open points

- Exact `docs_dir` layout must avoid clashing with `docs/images/` and `docs/superpowers/`. Resolved in
  the implementation plan (likely a dedicated content subfolder).
- WASM `productionExecutable` asset paths must resolve correctly under the `/demo/` subpath (relative
  paths expected to work; verify after first deploy).
- The exact Dokka aggregation task name for Dokka 2.x (`dokkaGenerate` vs the publication task) is
  confirmed during implementation.

## Success criteria

- Site builds locally via `mkdocs build` and the assembled `site/` contains `api/` and `demo/`.
- `embedded` (and other public `Table()` params) appear in the Dokka API reference (#24 closed).
- README is a concise landing page linking to the site.
- Push to `main` and tag push `v*` deploy the combined site to GitHub Pages.
