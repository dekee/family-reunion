package com.familyreunion.rsvp.security

import jakarta.servlet.http.HttpServletRequest
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/**
 * Simple in-memory sliding-window rate limiter keyed by an arbitrary string (typically
 * "<action>:<clientIp>"). Used to blunt automated abuse of public, unauthenticated
 * endpoints such as slogan voting and event registration.
 *
 * Notes / limitations:
 *  - Per-pod state: in a multi-replica deployment each pod limits independently, and counters
 *    reset on restart. For this app's scale that is acceptable; move to a shared store (Redis)
 *    if stronger guarantees are needed.
 *  - Memory is bounded: once the number of tracked keys exceeds [maxKeys], stale buckets are
 *    pruned so a flood of unique keys cannot exhaust the heap.
 */
@Component
class IpRateLimiter {

    private val buckets = ConcurrentHashMap<String, MutableList<Instant>>()
    private val maxKeys = 50_000

    /** Returns true if the request is allowed, false once the limit for [key] is exceeded. */
    fun tryAcquire(key: String, maxRequests: Int, windowSeconds: Long): Boolean {
        val now = Instant.now()
        if (buckets.size > maxKeys) {
            buckets.entries.removeIf { (_, ts) ->
                synchronized(ts) {
                    ts.removeAll { it.isBefore(now.minusSeconds(windowSeconds)) }
                    ts.isEmpty()
                }
            }
        }
        val timestamps = buckets.getOrPut(key) { mutableListOf() }
        synchronized(timestamps) {
            timestamps.removeAll { it.isBefore(now.minusSeconds(windowSeconds)) }
            if (timestamps.size >= maxRequests) return false
            timestamps.add(now)
            return true
        }
    }

    companion object {
        /** Best-effort client IP, honoring the ingress's X-Forwarded-For header. */
        fun clientIp(request: HttpServletRequest): String {
            val xff = request.getHeader("X-Forwarded-For")
            return if (!xff.isNullOrBlank()) xff.split(",")[0].trim() else request.remoteAddr ?: "unknown"
        }
    }
}
