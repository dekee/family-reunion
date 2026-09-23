package com.familyreunion.rsvp.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ThumbnailCacheTest {

    private fun bytes(n: Int) = ByteArray(n) { 1 }

    @Test
    fun `returns what was stored`() {
        val cache = ThumbnailCache()
        cache.put("a", bytes(10))
        assertThat(cache.get("a")).hasSize(10)
        assertThat(cache.get("missing")).isNull()
    }

    @Test
    fun `evicts oldest entries once the byte budget is exceeded`() {
        val cache = ThumbnailCache(maxBytes = 100)
        cache.put("a", bytes(50))
        cache.put("b", bytes(50))
        assertThat(cache.sizeBytes()).isEqualTo(100)

        cache.put("c", bytes(50))   // pushes total to 150, over the cap

        assertThat(cache.sizeBytes()).isLessThanOrEqualTo(100)
        assertThat(cache.get("a")).isNull()          // least recently used, evicted
        assertThat(cache.get("b")).isNotNull()
        assertThat(cache.get("c")).isNotNull()
    }

    @Test
    fun `eviction is by recency of access, not insertion`() {
        val cache = ThumbnailCache(maxBytes = 100)
        cache.put("a", bytes(50))
        cache.put("b", bytes(50))

        cache.get("a")              // "a" is now the most recently used
        cache.put("c", bytes(50))

        assertThat(cache.get("a")).isNotNull()
        assertThat(cache.get("b")).isNull()          // "b" went instead
    }

    @Test
    fun `replacing a key does not double-count its bytes`() {
        val cache = ThumbnailCache(maxBytes = 100)
        cache.put("a", bytes(40))
        cache.put("a", bytes(30))

        assertThat(cache.count()).isEqualTo(1)
        assertThat(cache.sizeBytes()).isEqualTo(30)
    }

    @Test
    fun `an entry larger than the whole budget is not stored`() {
        val cache = ThumbnailCache(maxBytes = 100)
        cache.put("small", bytes(40))
        cache.put("huge", bytes(500))

        // Caching it would evict everything else and still not fit.
        assertThat(cache.get("huge")).isNull()
        assertThat(cache.get("small")).isNotNull()
    }

    @Test
    fun `clear empties the cache and resets the byte count`() {
        val cache = ThumbnailCache(maxBytes = 100)
        cache.put("a", bytes(50))
        cache.clear()

        assertThat(cache.count()).isZero()
        assertThat(cache.sizeBytes()).isZero()
        assertThat(cache.get("a")).isNull()
    }
}
