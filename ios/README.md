# OFP Viewer as an app

The viewer is a web page and does not need to be an app: opened in Safari and
added to the home screen it runs offline, keeps the plan, and updates itself.
Everything below is for the two things a page cannot do — being handed to another
crew member through TestFlight, and standing in the App Store.

The app is a SwiftUI shell around the same `index.html` the web version is. The
reader is not rewritten in Swift: the part of this that is hard to get right is
the reading of the form — the column headings, the two lines to a waypoint, SR
under the ground speed — and it has been worked out against real plans. A second
implementation would be a second thing to keep correct, not a better one.

The page is served to the web view from a scheme of the app's own rather than
from a `file://` URL, so it has a real origin and keeps the loaded plan and the
light-or-dark choice between launches, as it does in Safari.

---

## Building it

There is no Mac in this repository's tooling, so the build runs on GitHub's
macOS runners: **Actions → iOS app → Run workflow**. Every push that touches
`ios/**` or `index.html` builds it too.

What comes out is `OFP-Viewer.ipa`, attached to the run as an artifact, and the
job checks that the `index.html` inside the bundle is byte-for-byte the one in
the commit — a shell that ships the wrong reader is the one failure a compiler
cannot catch.

To make a release with a stable address instead of an artifact that expires, run
the workflow with a version in the box (`v1.0.0`), or push a `v*` tag.

On a Mac with Xcode, `ios/OFPViewer.xcodeproj` opens and runs as it stands.

## The build is unsigned

Apple will not install an unsigned app on a device. Signing needs a certificate,
and a certificate needs an Apple ID, which the build has none of — so the `.ipa`
comes out unsigned and is signed on your own machine by whatever puts it on the
phone:

- **Sideloadly** or **AltStore** — sign with a free Apple ID. The app then runs
  for seven days and has to be refreshed; with a paid developer account, a year.
- **Apple Configurator**, for a device you administer.

## TestFlight and the App Store

Both need a paid **Apple Developer Program** membership and three things this
repository does not have:

1. A real bundle identifier. It is `com.example.ofpviewer` here, which is a
   placeholder — change `PRODUCT_BUNDLE_IDENTIFIER` in the project to something
   under a domain you own, and register it in App Store Connect.
2. A signing identity: a distribution certificate and provisioning profile, or
   `-allowProvisioningUpdates` with an App Store Connect API key. The key,
   its ID and the issuer ID go in as repository secrets, and the archive step
   drops `CODE_SIGNING_ALLOWED=NO` and gains `DEVELOPMENT_TEAM`.
3. An app record in App Store Connect for the build to be uploaded to, with
   `xcrun altool` or `xcrun notarytool`'s App Store equivalent.

Worth knowing before you spend the fee: **App Review guideline 4.2** turns down
apps that are a web site in a wrapper. TestFlight is a lighter gate than the
store — builds for internal testers on your own team go out with no review at
all — so it is a reasonable way to put this in another pilot's hands. A public
App Store listing for a shell around a page is a different proposition, and the
honest answer is that it would want a native reader behind it.

## What the app adds over the page

- An icon that is an app rather than a bookmark, and a place in the app switcher.
- A plan can be sent to it from Files or Mail through the share sheet: the app
  declares itself a viewer of PDFs, and asks for the document rather than
  claiming to own it.
- Nothing to fetch. The page is in the bundle, so there is no first launch that
  needs a network and no cache to be cold.
