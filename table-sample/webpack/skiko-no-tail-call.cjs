// Webpack loader for skiko.mjs. Its export wrappers end in a JS tail call into skiko's Wasm, and Safari 27.2
// delivers an f32/f64 argument of 0 or 1 through such a call as NaN once the calling Wasm is optimized
// (WebKit bug 329421): Compose then measures garbage and shows a white page. A call inside try is never a tail call.
'use strict';

// `(...a) => (X = loadedWasm._["X"])(...a)` as skiko ships it, and its minified form `(...e)=>(y=m._.X)(...e)`.
const TAIL_CALL_WRAPPER =
    /\(\.\.\.([\w$]+)\)\s*=>\s*\(\s*([\w$]+)\s*=\s*([\w$]+)\._(\.[\w$]+|\[(["'])[\w$]+\5\])\s*\)\s*\(\s*\.\.\.\1\s*\)/g;

function rewriteTailCallWrappers(source) {
    let count = 0;
    const code = source.replace(TAIL_CALL_WRAPPER, (match, rest, binding, holder, member) => {
        count++;
        const call = `(${binding} = ${holder}._${member})(...${rest})`;
        return `(...${rest}) => { try { return ${call} } catch (e) { throw e } }`;
    });
    return { code, count };
}

function findTailCallWrappers(code) {
    return Array.from(code.matchAll(TAIL_CALL_WRAPPER), (match) => match[0]);
}

function skikoNoTailCallLoader(source) {
    const { code, count } = rewriteTailCallWrappers(source);
    if (count === 0) {
        const file = (this && this.resourcePath) || 'skiko.mjs';
        throw new Error(`${file}: no skiko export wrapper matched, so the Safari 27.2 workaround would not apply`);
    }
    return code;
}

module.exports = skikoNoTailCallLoader;
module.exports.rewriteTailCallWrappers = rewriteTailCallWrappers;
module.exports.findTailCallWrappers = findTailCallWrappers;
