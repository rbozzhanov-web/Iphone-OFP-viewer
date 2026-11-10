# OFP Viewer for Android

A native Android app around the same reader as the web version. There is one
reader: `index.html` at the repository root is copied into the app when it is
built, so the app, the web page and the iOS app always read a plan the same way.

Made with folding phones in mind (it was built for an OPPO Find N6) and fine on
any phone running Android 8 or later.

## Installing

The newest build is always at
[releases/android-latest](https://github.com/rbozzhanov-web/Iphone-OFP-viewer/releases/tag/android-latest),
replaced on every change to `main`. Download `OFP-Viewer.apk` on the phone and
open it; the first time, allow installs from the browser or Files when Android
asks (on ColorOS: *Settings → Privacy → Install unknown apps*, or just follow
the prompt). Later builds install over it and keep the open plan.

Every push that touches the app also leaves the APK as an artifact on its
GitHub Actions run, and a `v*` tag attaches it to that release.

## What the app adds to the page

| | |
|---|---|
| **Folding** | Folding, unfolding and turning the phone are handled in place (`configChanges` in the manifest). Nothing reloads: the page is given the new width and lays itself out again, on the same tab, at the same place. Unfolded, the same screens use the wider screen — one tab at a time, as on the phone. |
| **Open with / Share** | PDFs can be sent to the app from Files, Mail, Telegram or a browser download. The app reads the file and the page collects it from `inbox.pdf`, the same arrangement as the iOS app. |
| **Original pages** | Android's WebView cannot draw a PDF in an `<img>` as Safari can, so the app draws "original page" views itself with the system PDF renderer (`PageRenderer`). The page hands the app the open document once per plan; it goes no further than the app's cache folder. |
| **System bars** | The status and navigation bars take the page's own light or dark background. |
| **Back** | Closes an open chart; otherwise sends the app to the background without closing it, so the plan is there on return. |

## Offline and private

The page is served from `https://appassets.androidplatform.net/`, an address
that never reaches the network: every request is answered from inside the APK,
and the app refuses anything addressed elsewhere. Links out of the reader (Air
Astana Trip Info) open in the browser. The service worker the web version uses
is not needed and not registered. Nothing is backed up or transferred off the
phone (`data_extraction_rules.xml`).

## Building it yourself

JDK 17+ and the Android SDK (platform 35):

```sh
cd android
ANDROID_HOME=/path/to/android-sdk ./gradlew assembleRelease
# → app/build/outputs/apk/release/app-release.apk
```

## The signing key

`app/sideload.keystore` is committed on purpose. Android installs an update only
if it is signed with the same key as the app already on the phone; with a key
kept in the repository, every build — CI or local — installs over the last one.
The key proves only that a build came from this project's public sources. It
would not do for a Play Store release, which needs a private key kept out of
the repository (and a new install, since the key would change).
