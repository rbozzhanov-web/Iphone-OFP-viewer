import SwiftUI

/// OFP Viewer, as an app rather than a page on the home screen.
///
/// The reader itself is the same one file the web app is: it is carried in the
/// bundle and shown in a web view. That is deliberate. The parsing of these
/// plans — the column headings, the two lines to a waypoint, SR under the ground
/// speed — has been worked out against real documents and is the part that is
/// hard to get right; rewriting it in Swift to say the same things would be a
/// second thing to keep correct, not a better one.
///
/// What the app adds is what a page cannot have: an icon that is not a bookmark,
/// a place in the share sheet for a plan arriving by mail, and a build that can
/// be handed to another crew member through TestFlight.
@main
struct OFPViewerApp: App {
    var body: some Scene {
        WindowGroup {
            WebHost()
                // The page draws its own safe areas — it has to, being the same
                // file that runs in Safari — so the host gives it the whole screen.
                .ignoresSafeArea()
                .statusBarHidden(false)
        }
    }
}
