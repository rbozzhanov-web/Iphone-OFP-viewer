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
/// Where the page comes to collect a plan another app has handed over. Under
/// the app's own scheme, so it is same-origin to the page and needs no leave.
private let inboxPath = "/inbox.pdf"

/// A plan handed to the app from Files, Mail or anything else with a share
/// sheet, held until the page is there to take it.
///
/// The bytes are handed over through the app's own scheme rather than passed
/// into a call: a briefing package runs to several megabytes, and encoding one
/// into a string of JavaScript makes a copy a third larger again, plus the
/// pause on the main thread to build it. The page is only told a plan is
/// waiting; it comes and fetches it.
final class Inbox {
    static let shared = Inbox()

    private let lock = NSLock()
    private var pending: (name: String, data: Data)?
    /// Whether the page has already been told about the document now waiting.
    /// Telling it twice would send it for a document the first telling has
    /// already had, and the empty-handed second trip would put an error over a
    /// plan that had in fact opened perfectly well.
    private var announced = false

    /// Set by the web host once its page is loaded and able to take a document.
    /// Held here rather than passed about because a document can arrive before
    /// there is any page to give it to — a cold launch straight from Files —
    /// and must then wait rather than be dropped.
    var deliver: ((String) -> Void)?

    /// Reads the document now, while the leave to read it is still good: the
    /// URL is only ours for the length of this call, and the page may not come
    /// for it until the web view has finished loading.
    func receive(_ url: URL) {
        let scoped = url.startAccessingSecurityScopedResource()
        defer { if scoped { url.stopAccessingSecurityScopedResource() } }
        guard let data = try? Data(contentsOf: url), !data.isEmpty else { return }

        lock.lock()
        pending = (url.lastPathComponent, data)
        announced = false
        lock.unlock()
        offer()
    }

    /// Tells the page a plan is waiting, if there is a page, there is a plan,
    /// and it has not been told already.
    func offer() {
        guard let deliver = deliver else { return }
        lock.lock()
        let name = announced ? nil : pending?.name
        if name != nil { announced = true }
        lock.unlock()
        guard let name = name else { return }
        DispatchQueue.main.async { deliver(name) }
    }

    /// Handed over once and then let go — the page has it from here, and the
    /// copy in memory is only ever a doorstep.
    func take() -> Data? {
        lock.lock()
        defer { lock.unlock() }
        let data = pending?.data
        pending = nil
        return data
    }
}

/// The file's name as a JavaScript string literal. It is written by whatever
/// app sent the document and can hold quotes, backslashes or newlines.
private func jsLiteral(_ s: String) -> String {
    var out = "\""
    for u in s.unicodeScalars {
        switch u {
        case "\"":  out += "\\\""
        case "\\":  out += "\\\\"
        case "\n":  out += "\\n"
        case "\r":  out += "\\r"
        default:
            // U+2028/9 end a line in JavaScript source though not in JSON.
            if u.value < 0x20 || u.value == 0x2028 || u.value == 0x2029 {
                out += String(format: "\\u%04x", u.value)
            } else {
                out.unicodeScalars.append(u)
            }
        }
    }
    return out + "\""
}

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

        /// The page is only now able to be handed anything. A plan that arrived
        /// before this — a cold launch straight out of Files, which is the
        /// ordinary way in — has been waiting in the inbox for exactly this.
        func webView(_ web: WKWebView, didFinish navigation: WKNavigation!) {
            Inbox.shared.deliver = { [weak web] name in
                web?.evaluateJavaScript("window.__ofpOpen && window.__ofpOpen(\(jsLiteral(name)))")
            }
            Inbox.shared.offer()
        }
    }
}

/// Hands the web view files out of the app bundle under the app's own scheme,
/// and the one document that is not in the bundle: the plan another app has
/// just handed over.
private final class BundleScheme: NSObject, WKURLSchemeHandler {
    func webView(_ web: WKWebView, start task: WKURLSchemeTask) {
        guard let url = task.request.url else { return }

        if url.path == inboxPath {
            guard let data = Inbox.shared.take() else {
                // Nothing waiting: the page asked twice, or the document was
                // already collected. Not an error worth a message on screen.
                task.didFailWithError(URLError(.resourceUnavailable))
                return
            }
            let head = URLResponse(url: url, mimeType: "application/pdf",
                                   expectedContentLength: data.count, textEncodingName: nil)
            task.didReceive(head)
            task.didReceive(data)
            task.didFinish()
            return
        }

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
