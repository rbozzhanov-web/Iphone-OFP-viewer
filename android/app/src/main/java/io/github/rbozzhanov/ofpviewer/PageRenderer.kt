package io.github.rbozzhanov.ofpviewer

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * Draws one page of the plan as a picture, for the page's "original page" view.
 *
 * Safari draws a one-page PDF in an <img> by itself; Android's WebView cannot,
 * so the page hands the app the document once per plan and asks for pages by
 * number. The document goes no further than the app's own cache folder, where
 * the system's PDF renderer can open it.
 *
 * Calls come from the bridge thread and from the WebView's loading threads at
 * once, and PdfRenderer allows one open page at a time, so every use is under
 * the one lock.
 */
class PageRenderer(cacheDir: File) {
    private val file = File(cacheDir, "plan.pdf")
    private var fd: ParcelFileDescriptor? = null
    private var renderer: PdfRenderer? = null

    /** Replaces the document. False if the system cannot open it. */
    @Synchronized
    fun setDocument(bytes: ByteArray): Boolean {
        close()
        return try {
            file.writeBytes(bytes)
            val f = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            fd = f
            renderer = PdfRenderer(f)
            true
        } catch (e: Exception) {
            close()
            false
        }
    }

    /**
     * Page [index] (from 0) as a PNG, its longer side [LONG_SIDE] pixels: enough
     * detail for a chart to be zoomed into several times on an unfolded screen.
     * Null if there is no such page or no document.
     */
    @Synchronized
    fun pagePng(index: Int): ByteArray? {
        val r = renderer ?: return null
        if (index < 0 || index >= r.pageCount) return null
        r.openPage(index).use { page ->
            val k = LONG_SIDE.toFloat() / maxOf(page.width, page.height)
            val w = maxOf(1, (page.width * k).toInt())
            val h = maxOf(1, (page.height * k).toInt())
            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            try {
                // A PDF page has no background of its own; paper is white.
                bmp.eraseColor(Color.WHITE)
                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                val out = ByteArrayOutputStream(w * h / 4)
                bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
                return out.toByteArray()
            } finally {
                bmp.recycle()
            }
        }
    }

    @Synchronized
    fun close() {
        try { renderer?.close() } catch (_: Exception) {}
        try { fd?.close() } catch (_: Exception) {}
        renderer = null
        fd = null
    }

    private companion object {
        const val LONG_SIDE = 2400
    }
}
