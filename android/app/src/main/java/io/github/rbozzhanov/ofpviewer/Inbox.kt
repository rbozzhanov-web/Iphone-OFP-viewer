package io.github.rbozzhanov.ofpviewer

/**
 * A document handed to the app from outside — "Open with" or "Share" from
 * Files, Mail, Telegram — waiting for the page to collect it.
 *
 * The page is told only the file's name; it then fetches the bytes itself from
 * `inbox.pdf`, which the activity answers from here. Passing several megabytes
 * as a script argument would mean encoding them and a pause on the thread that
 * draws the page; a fetch costs neither. Same arrangement as the iOS app.
 */
class Inbox {
    private var name: String? = null
    private var bytes: ByteArray? = null
    private var announced = false

    /** A new document has arrived; it replaces any not yet collected. */
    @Synchronized
    fun offer(name: String, bytes: ByteArray) {
        this.name = name
        this.bytes = bytes
        announced = false
    }

    /** The name to tell the page about, once per document, or null if there is nothing new. */
    @Synchronized
    fun announce(): String? {
        if (bytes == null || announced) return null
        announced = true
        return name
    }

    /** The page collecting the document. It is handed over once and not kept. */
    @Synchronized
    fun take(): ByteArray? {
        val b = bytes
        bytes = null
        name = null
        return b
    }
}
