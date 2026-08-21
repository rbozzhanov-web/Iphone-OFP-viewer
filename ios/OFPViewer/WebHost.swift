import SwiftUI
import WebKit
import UniformTypeIdentifiers

/// The one page, served to the web view from the app's own scheme.
///
/// Not a file:// URL: a page loaded from one has no origin worth the name, and
/// the viewer keeps the loaded plan in IndexedDB and the light-or-dark choice in
/// localStorage. Both are wrapped in try/catch on the page and their loss is
/// survivable — the plan simply has to be picked again after a cold start — but
/// they are worth keeping, and a scheme of the app's own has a real origin where
/// a file URL does not.
private let appScheme = "ofpviewer"
private let appOrigin = "\(appScheme)://app/"

struct WebHost: UIViewRepresentable {
    func makeCoordinator() -> Coordinator { Coordinator() }

    func makeUIView(context: Context) -> WKWebView {
        let cfg = WKWebViewConfiguration()
        cfg.setURLSchemeHandler(BundleScheme(), forURLScheme: appScheme)
        cfg.websiteDataStore = .default()          // storage that outlives the launch
        cfg.allowsInlineMediaPlayback = true

        let web = WKWebView(frame: .zero, configuration: cfg)
        web.navigationDelegate = context.coordinator
        web.isOpaque = false
        web.backgroundColor = .clear
        web.scrollView.backgroundColor = .clear
        // The page is written against the visual viewport and lays out its own
        // insets; letting the scroll view add its own would inset them twice.
        web.scrollView.contentInsetAdjustmentBehavior = .never
        web.allowsBackForwardNavigationGestures = false
        web.allowsLinkPreview = false
        if #available(iOS 16.4, *) { web.isInspectable = true }

        if let url = URL(string: appOrigin + "index.html") {
            web.load(URLRequest(url: url))
        }
        return web
    }

    func updateUIView(_ web: WKWebView, context: Context) {}

    /// Keeps the app on its own page. There is nowhere else to go — the viewer
    /// links to nothing and fetches nothing — so anything else is a mistake or a
    /// tap on something in a NOTAM that looks like a link, and belongs in Safari.
    final class Coordinator: NSObject, WKNavigationDelegate {
        func webView(_ web: WKWebView,
                     decidePolicyFor action: WKNavigationAction,
                     decisionHandler: @escaping (WKNavigationActionPolicy) -> Void) {
            guard let url = action.request.url else { return decisionHandler(.cancel) }
            if url.scheme == appScheme { return decisionHandler(.allow) }
            if action.navigationType == .linkActivated, UIApplication.shared.canOpenURL(url) {
                UIApplication.shared.open(url)
            }
            decisionHandler(.cancel)
        }
    }
}

/// Hands the web view files out of the app bundle under the app's own scheme.
private final class BundleScheme: NSObject, WKURLSchemeHandler {
    func webView(_ web: WKWebView, start task: WKURLSchemeTask) {
        guard let url = task.request.url else { return }
        let name = url.path.isEmpty || url.path == "/" ? "/index.html" : url.path
        let file = (name as NSString).lastPathComponent

        guard let path = Bundle.main.path(forResource: (file as NSString).deletingPathExtension,
                                          ofType: (file as NSString).pathExtension),
              let data = FileManager.default.contents(atPath: path) else {
            task.didFailWithError(URLError(.fileDoesNotExist))
            return
        }
        let type = UTType(filenameExtension: (file as NSString).pathExtension)?
            .preferredMIMEType ?? "application/octet-stream"
        let head = URLResponse(url: url, mimeType: type,
                               expectedContentLength: data.count, textEncodingName: "utf-8")
        task.didReceive(head)
        task.didReceive(data)
        task.didFinish()
    }

    func webView(_ web: WKWebView, stop task: WKURLSchemeTask) {}
}
