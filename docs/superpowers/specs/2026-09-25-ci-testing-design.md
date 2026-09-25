# CI testing on pull requests and main — Design

**Date:** 2026-09-25
**Status:** Approved
**Topic:** Add a GitHub Actions workflow that runs detekt, Spotless and the library test suites on
every pull request and every push to `main`.

## Problem

The repository has two workflows, `deploy-docs.yml` and `publish.yml`. Neither runs a test or a
lint. A pull request can merge with red `commonTest` suites, detekt findings or unformatted code, and
the first build to notice is a local one — or the tag-triggered publish.

## Goals

- Every pull request to `main` and every push to `main` gets independent pass/fail checks for:
  detekt, Spotless, the tests that run on Linux, and the tests that need macOS.
- The `commonTest` suites of `table-core`, `table-format` and `table-paging` run on JVM, wasmJs and
  iOS simulator.
- JS and Android library code is at least compiled.
- Failures leave enough behind to diagnose them without re-running: test and detekt reports as
  artifacts. Kover coverage is published as an artifact on green runs.
- Expensive macOS minutes are spent only on what needs macOS.

## Non-Goals

- `jsBrowserTest`. 27 of `table-core`'s tests fail there on a clean tree because Skiko's wasm
  binary does not load under karma; this is pre-existing and unrelated to CI. JS stays
  compile-only.
- Packaging the sample apps or running `./gradlew build` (20+ minutes, no library signal).
- Coverage thresholds, coverage PR comments, or any third-party reporting service.
- Branch-protection settings. Marking the new checks as required is a repository setting the
  maintainer applies after the workflow has run once.
- Changes to `deploy-docs.yml`, `publish.yml` or build-logic.

## Design

One new file: `.github/workflows/ci.yml`, with four jobs that run in parallel and share nothing.

### Triggers and shared settings

- `on`: `pull_request` (branches: `main`), `push` (branches: `main`), `workflow_dispatch`.
- `permissions: contents: read`.
- `concurrency`: group `ci-${{ github.ref }}`, `cancel-in-progress: true`, so a new push to a PR
  cancels its superseded run.
- Every job repeats the setup the existing workflows use: `actions/checkout@v5`,
  `actions/setup-java@v5` (temurin 17), `chmod +x ./gradlew`, `gradle/actions/setup-gradle@v6`.
  setup-gradle writes its cache only from the default branch; PR runs read it.
- Every Gradle invocation uses `--no-daemon --continue` with `CI: true` in the environment.
  `--continue` makes all failing tasks report, not just the first.

### Jobs

| Job          | Runner          | Gradle tasks                                                                                   | Extra flags                                  |
|--------------|-----------------|------------------------------------------------------------------------------------------------|----------------------------------------------|
| `detekt`     | `ubuntu-latest` | `detekt`                                                                                       | —                                            |
| `spotless`   | `ubuntu-latest` | `spotlessCheck`                                                                                | —                                            |
| `test-linux` | `ubuntu-latest` | yarn-lock upgrade; `jvmTest wasmJsBrowserTest`; `compileKotlinJs compileAndroidMain compileCommonMainKotlinMetadata`; `coverageReport` | `-PexcludeSamples=true -PenableIos=false` |
| `test-ios`   | `macos-latest`  | `iosSimulatorArm64Test`                                                                        | `-PexcludeSamples=true`                      |

**`detekt`** runs the root-level `detekt` name, which executes each subproject's `detekt` task; the
quality convention already makes that task depend on every per-source-set detekt task, so all source
sets are analysed. No type resolution is involved. On failure it uploads `**/build/reports/detekt/`
as the `detekt-reports` artifact.

**`spotless`** runs `spotlessCheck`, which covers both configured formats — `kotlin`
(`src/**/*.kt`) and `kotlinGradle` (build scripts, including `build-logic`) — in the root project and
all subprojects. Its log prints the offending diff, so it uploads nothing.

`detekt` and `spotless` pass no target or sample flags: they are source analysis with no npm install
and no klib resolution, so they lint the sample modules and `iosMain` exactly as a local
`./gradlew qualityCheck` does. If configuring the iOS targets on Linux turns out to fail, both jobs
fall back to `-PenableIos=false`; implementation verifies this before settling.

**`test-linux`** steps, in order:

1. `kotlinUpgradeYarnLock kotlinWasmUpgradeYarnLock`. `-PexcludeSamples=true` drops the samples'
   npm dependencies, so the committed locks no longer match and the lock-mismatch check would fail
   the build. `publish.yml` does the same for the same reason.
2. `jvmTest wasmJsBrowserTest`. wasmJs tests run in headless Chrome, which `ubuntu-latest` ships.
3. `compileKotlinJs compileAndroidMain compileCommonMainKotlinMetadata`.
4. `coverageReport` (merged Kover HTML + XML), only if the steps above succeeded.
5. Artifacts: `test-reports-linux` (`**/build/reports/tests/`) on failure; `coverage`
   (`build/reports/kover/`) on success.

**`test-ios`** runs `iosSimulatorArm64Test` and uploads `test-reports-ios`
(`**/build/reports/tests/`) on failure.

Artifacts use `actions/upload-artifact@v4` with `if-no-files-found: ignore` for the failure-only
uploads, so a failure before any report exists does not add a second error.

## Error handling

- A red step fails its job; the other three jobs are unaffected and still report.
- Tasks that do not exist (a typo, a target renamed upstream) fail loudly at Gradle's task
  selection, never silently pass.
- The known config-cache serialisation failure is confined to `table-sample` web tasks, which no
  job runs.

## Verification

1. Run each job's exact Gradle command line locally with the same flags (the iOS one included —
   the development machine is a Mac) and confirm all pass on the current `main`.
2. Confirm `detekt` and `spotlessCheck` configure without `-PenableIos=false` on Linux via the first
   CI run; apply the fallback if not.
3. Lint the workflow with `actionlint` when available.
4. Push the branch and open a draft PR: the workflow runs on its own PR. Acceptance is all four jobs
   green there, and a deliberately broken commit (temporary, reverted) turning the right job red.
