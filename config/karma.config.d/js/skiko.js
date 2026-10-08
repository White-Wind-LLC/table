// Loads Skiko for Kotlin/JS browser tests (Compose UI tests). Appended to the generated karma.conf.js,
// where `config` is in scope.
//
// Skiko's Kotlin/JS code calls the functions that skiko.mjs exports (malloc, org_jetbrains_skia_*, …)
// as globals, and an app gets them only after `onWasmReady`. Under karma neither happens: webpack keeps
// the exports module-local and the tests start before the wasm runtime is loaded. So map every export
// to its free identifier, and hold karma's start until `awaitSkiko` resolves.
;(function () {
    const fs = require('fs');
    const path = require('path');
    const webpack = require('webpack');

    const skiko = path.join(config.basePath, 'kotlin', 'skiko.mjs');
    if (!fs.existsSync(skiko)) return;

    const exported = {};
    for (const [, name] of fs.readFileSync(skiko, 'utf8').matchAll(/^export (?:let|const) (\w+)/gm)) {
        exported[name] = [skiko, name];
    }
    config.webpack.plugins.push(new webpack.ProvidePlugin(exported));

    const setup = path.join(config.basePath, 'kotlin', 'skiko-karma-setup.mjs');
    fs.writeFileSync(setup, `import { awaitSkiko } from ${JSON.stringify(skiko)};

const karma = window.__karma__;
const loaded = karma.loaded.bind(karma);
let ready = false;
let pending = false;
karma.loaded = () => { if (ready) loaded(); else pending = true; };
awaitSkiko.then(
    () => { ready = true; if (pending) loaded(); },
    (e) => karma.error('Skiko failed to load: ' + e),
);
`);
    // After kotlin-test-karma-runner.js, before the test bundle.
    config.files.splice(1, 0, setup);
    config.preprocessors[setup] = ['webpack'];
})();
