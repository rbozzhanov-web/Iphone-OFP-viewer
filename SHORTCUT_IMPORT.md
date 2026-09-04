# Share an OFP straight into OFP Viewer

iOS does not expose a web app as a native Share Sheet destination. This Shortcut is
the local bridge: it accepts a PDF in the Share Sheet, puts it on the device-only
clipboard, then OFP Viewer imports it with one deliberate tap. No file is saved
to Files, and no byte is uploaded.

## Make the Shortcut once

In **Shortcuts**, create a shortcut named **Import to OFP Viewer**.

1. Open the shortcut's details. Enable **Show in Share Sheet**.
2. Under *Share Sheet Types*, select **PDFs** only.
3. Add **Base64 Encode**. Feed it the *Shortcut Input* and set *Line Breaks* to
   **None**.
4. Add a **Text** action. Its text must be exactly:
   `OFPVIEWER-PDF-v1:` followed immediately by the magic variable from the
   Base64 action — no spaces and no new line.
5. Add **Copy to Clipboard**. Feed it the Text output and enable **Local Only**.
6. Add **Show Notification** with: `Open OFP Viewer and tap Paste OFP from
   Shortcut.`

## Use it

1. In Mail, Messages, Files, or another app, use **Share** on an OFP PDF.
2. Tap **Import to OFP Viewer**.
3. Open the installed **OFP Viewer** PWA from its Home Screen icon.
4. Tap **Paste OFP from Shortcut** and allow paste when iOS asks.

The app accepts PDFs up to **3 MB** by this route. After a successful import it
tries to clear the clipboard; the PDF itself is then held only in OFP Viewer's
local IndexedDB storage. The normal PDF picker remains available for larger
documents.
