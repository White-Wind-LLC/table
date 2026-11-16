// Keeps skiko's Wasm calls out of JS tail position for Safari 27.2; see table-sample/webpack/skiko-no-tail-call.cjs.
// The check fails the build when minification folds a rewritten wrapper back into a tail call.
{
    const skikoLoaderPath = require('path').resolve(__dirname, '../../../../table-sample/webpack/skiko-no-tail-call.cjs');
    const { findTailCallWrappers } = require(skikoLoaderPath);

    config.module.rules.push({ test: /[\\/]skiko\.mjs$/, loader: skikoLoaderPath });

    config.plugins.push({
        apply(compiler) {
            const { Compilation, WebpackError } = compiler.webpack;
            compiler.hooks.thisCompilation.tap('SkikoNoTailCallCheck', (compilation) => {
                compilation.hooks.processAssets.tap(
                    { name: 'SkikoNoTailCallCheck', stage: Compilation.PROCESS_ASSETS_STAGE_ANALYSE },
                    (assets) => {
                        for (const [name, asset] of Object.entries(assets)) {
                            if (!/\.m?js$/.test(name)) continue;
                            const left = findTailCallWrappers(asset.source().toString());
                            if (left.length > 0) {
                                const message = `${name}: ${left.length} skiko wrapper(s) still tail-call Wasm`;
                                compilation.errors.push(new WebpackError(`${message}, e.g. ${left[0]}`));
                            }
                        }
                    },
                );
            });
        },
    });
}
