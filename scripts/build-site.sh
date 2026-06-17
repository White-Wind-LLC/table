#!/usr/bin/env bash
set -euo pipefail

# 1. Guides → site/
mkdocs build --strict

# 2. API reference → site/api/
./gradlew --no-daemon dokkaGenerateHtml
rm -rf site/api
cp -r build/dokka/html site/api

# 3. WASM demo → site/demo/
# --no-configuration-cache: KotlinWebpack has a known incompatibility with Gradle config cache
./gradlew --no-daemon --no-configuration-cache :table-sample:wasmJsBrowserDistribution
rm -rf site/demo
cp -r table-sample/build/dist/wasmJs/productionExecutable site/demo

echo "Site assembled at ./site (root=guides, /api=Dokka, /demo=WASM)"
