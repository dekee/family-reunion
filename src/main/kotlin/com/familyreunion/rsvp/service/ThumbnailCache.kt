package com.familyreunion.rsvp.service

/**
 * Byte-bounded LRU cache of rendered thumbnails, held in memory.
 *
 * Bounded by total bytes rather than entry count: thumbnails vary in size, so a count-based limit
 * gives no real control over the heap this occupies.
 *
 * Only thumbnails belong here. Gallery originals average ~2 MB and reach 8 MB (phone photos), so
 * the full set is ~740 MB — far too much to hold — and they are viewed one at a time in the
 * lightbox, where the hit rate would not justify it. Thumbnails are ~25 KB, so a 354-photo gallery
 * is roughly 10 MB and sits comfortably inside the default cap.
 */
class ThumbnailCache(private val maxBytes: Long = 64L * 1024 * 1024) {

    // accessOrder = true makes the iteration order least-recently-used first.
    private val entries = LinkedHashMap<String, ByteArray>(64, 0.75f, true)
    private var currentBytes = 0L
    private val lock = Any()

    fun get(key: String): ByteArray? = synchronized(lock) { entries[key] }

    fun put(key: String, bytes: ByteArray) {
        // An entry larger than the whole budget would evict everything and still not fit.
        if (bytes.size > maxBytes) return

        synchronized(lock) {
            entries.put(key, bytes)?.let { currentBytes -= it.size }
            currentBytes += bytes.size

            val iterator = entries.entries.iterator()
            while (currentBytes > maxBytes && iterator.hasNext()) {
                currentBytes -= iterator.next().value.size
                iterator.remove()
            }
        }
    }

    fun clear() = synchronized(lock) {
        entries.clear()
        currentBytes = 0
    }

    /** Current size in bytes — for tests and for logging cache growth. */
    fun sizeBytes(): Long = synchronized(lock) { currentBytes }

    fun count(): Int = synchronized(lock) { entries.size }
}
