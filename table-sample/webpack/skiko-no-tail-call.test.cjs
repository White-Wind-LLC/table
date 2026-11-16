// Run with: node --test table-sample/webpack/skiko-no-tail-call.test.cjs
const assert = require('node:assert/strict');
const test = require('node:test');
const vm = require('node:vm');

const loader = require('./skiko-no-tail-call.cjs');
const { rewriteTailCallWrappers, findTailCallWrappers } = loader;

// Lines as skiko.mjs (skiko 0.150.1) ships them.
const SKIKO_MALLOC = 'export let malloc = (...a) => (malloc = loadedWasm._["malloc"])(...a)';
const SKIKO_FREE = 'export let free = (...a) => (free = loadedWasm._["free"])(...a)';
// The same lines as the loader has to write them.
const PATCHED_MALLOC =
    'export let malloc = (...a) => { try { return (malloc = loadedWasm._["malloc"])(...a) } catch (e) { throw e } }';
const PATCHED_FREE =
    'export let free = (...a) => { try { return (free = loadedWasm._["free"])(...a) } catch (e) { throw e } }';

// Runs a rewritten wrapper as a script, against a fake skiko instance.
function runWrapper(exportName, exportFn) {
    const source = `let ${exportName} = (...a) => (${exportName} = loadedWasm._["${exportName}"])(...a)`;
    const { code } = rewriteTailCallWrappers(source);
    const context = vm.createContext({ loadedWasm: { _: { [exportName]: exportFn } } });
    vm.runInContext(`${code}; globalThis.wrapper = ${exportName};`, context);
    return {
        call: (...args) => vm.runInContext(`wrapper(...${JSON.stringify(args)})`, context),
        binding: () => vm.runInContext(exportName, context),
    };
}

test('rewrites a skiko export wrapper so the Wasm call sits inside try', () => {
    const { code, count } = rewriteTailCallWrappers(SKIKO_MALLOC);

    assert.equal(count, 1);
    assert.equal(code, PATCHED_MALLOC);
});

test('rewrites every wrapper and leaves the rest of the module as it was', () => {
    const source = ['export const loadedWasm = {', '    _: {}', '}', SKIKO_MALLOC, SKIKO_FREE, ''].join('\n');

    const { code, count } = rewriteTailCallWrappers(source);

    assert.equal(count, 2);
    assert.equal(code, ['export const loadedWasm = {', '    _: {}', '}', PATCHED_MALLOC, PATCHED_FREE, ''].join('\n'));
});

test('a rewritten wrapper passes every argument and returns what the export returns', () => {
    const wrapper = runWrapper('skia_add', (x, y, z) => x * 100 + y * 10 + z);

    assert.equal(wrapper.call(1, 2, 3), 123);
});

test('a rewritten wrapper still binds the raw export after its first call', () => {
    const raw = () => 7;
    const wrapper = runWrapper('skia_seven', raw);

    wrapper.call();

    assert.equal(wrapper.binding(), raw);
});

test('a rewritten wrapper rethrows the export error unchanged', () => {
    const failure = new Error('Wasm trap');
    const wrapper = runWrapper('skia_fail', () => {
        throw failure;
    });

    assert.throws(() => wrapper.call(), (thrown) => thrown === failure);
});

test('the loader refuses a skiko.mjs without wrappers instead of shipping it unpatched', () => {
    assert.throws(() => loader('export const loadedWasm = { _: {} }'), /no skiko export wrapper/);
});

test('the loader returns the rewritten module', () => {
    assert.equal(loader(SKIKO_FREE), PATCHED_FREE);
});

test('the bundle check finds a tail-call wrapper in minified output', () => {
    // As terser emits it in the deployed bundle.
    const bundle =
        'let y=(...e)=>(y=m._.org_jetbrains_skia_impl_Managed__invokeFinalizer)(...e),' +
        'w=(...e)=>(w=m._.malloc)(...e);';

    assert.deepEqual(findTailCallWrappers(bundle), [
        '(...e)=>(y=m._.org_jetbrains_skia_impl_Managed__invokeFinalizer)(...e)',
        '(...e)=>(w=m._.malloc)(...e)',
    ]);
});

test('the bundle check finds a tail-call wrapper in unminified output', () => {
    assert.deepEqual(findTailCallWrappers(SKIKO_MALLOC), ['(...a) => (malloc = loadedWasm._["malloc"])(...a)']);
});

test('the bundle check accepts rewritten wrappers, minified or not', () => {
    const minified = 'let w=(...e)=>{try{return(w=m._.malloc)(...e)}catch(e){throw e}};';

    assert.deepEqual(findTailCallWrappers(minified), []);
    assert.deepEqual(findTailCallWrappers(rewriteTailCallWrappers(SKIKO_MALLOC).code), []);
});
