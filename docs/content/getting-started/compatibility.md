# Compatibility

The following table lists compatibility information for released library versions.

| Version | Kotlin | Compose Multiplatform |
|---------|-------:|----------------------:|
| 3.8.0   | 2.4.20 |                1.12.0 |
| 3.7.0   | 2.4.20 |                1.12.0 |
| 3.6.0   | 2.4.20 |                1.12.0 |
| 3.5.0   | 2.4.20 |                1.12.0 |
| 3.4.0   | 2.4.20 |                1.12.0 |
| 3.3.0   | 2.4.20 |                1.12.0 |
| 3.2.0   | 2.4.20 |                1.12.0 |
| 3.1.1   | 2.4.20 |                1.12.0 |
| 3.1.0   | 2.4.20 |                1.12.0 |
| 3.0.0   | 2.4.20 |                1.12.0 |
| 2.4.1   | 2.4.20 |                1.12.0 |
| 2.4.0   | 2.4.20 |                1.12.0 |
| 2.3.2   | 2.4.10 |                1.11.1 |
| 2.3.1   | 2.4.10 |                1.11.1 |
| 2.3.0   | 2.4.10 |                1.11.1 |
| 2.2.0   | 2.4.10 |                1.11.1 |
| 2.1.1   | 2.4.10 |                1.11.1 |
| 2.1.0   | 2.4.10 |                1.11.1 |
| 2.0.0   | 2.4.10 |                1.11.1 |
| 1.11.0  | 2.4.10 |                1.11.1 |
| 1.10.0  |  2.4.0 |                1.11.1 |
| 1.9.0   | 2.3.21 |                1.11.0 |
| 1.8.3   | 2.3.21 |                1.10.3 |
| 1.8.2   | 2.3.20 |                1.10.3 |
| 1.8.0   | 2.3.10 |                1.10.3 |
| 1.7.15  | 2.3.10 |                1.10.2 |
| 1.7.13  | 2.3.10 |                1.10.1 |
| 1.7.4   |  2.3.0 |                 1.9.3 |
| 1.4.0   | 2.2.21 |                 1.9.3 |
| 1.3.1   | 2.2.21 |                 1.9.2 |
| 1.2.1   | 2.2.10 |                 1.9.0 |

## Safari 27.2 on web (wasm and JS)

Safari 27.2 on macOS and iOS shows a Compose web app as a white page, or with its layout collapsing into the
top left corner a few seconds after start. Once the calling Wasm is optimised, a JS tail call into a Wasm function
receives `NaN` for an `f32`/`f64` argument of 0 or 1
([WebKit bug 329421](https://bugs.webkit.org/show_bug.cgi?id=329421),
[CMP-10986](https://youtrack.jetbrains.com/issue/CMP-10986)), and Skiko's `skiko.mjs` calls every Skia function
that way: `(...a) => (X = loadedWasm._["X"])(...a)`. The bug is not in this library and cannot be fixed from it:
the wrappers end up in your app's bundle.

The sample works around it with a webpack loader that moves each of those calls into a `try` block, which is never
a tail position. Copy both files into your web app module to do the same:

- [`table-sample/webpack/skiko-no-tail-call.cjs`](https://github.com/White-Wind-LLC/table/blob/main/table-sample/webpack/skiko-no-tail-call.cjs),
  the loader;
- [`table-sample/webpack.config.d/skiko-no-tail-call.js`](https://github.com/White-Wind-LLC/table/blob/main/table-sample/webpack.config.d/skiko-no-tail-call.js),
  which applies it and fails the build when the bundle still holds a tail-call wrapper. Fix the module name in its
  `require` path.

Drop them once the Safari versions your users run carry the WebKit fix
([PR 79391](https://github.com/WebKit/WebKit/pull/79391)).
