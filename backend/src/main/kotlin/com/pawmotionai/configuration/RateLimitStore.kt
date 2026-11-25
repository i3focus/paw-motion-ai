package com.pawmotionai.configuration

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Fixed-window counters with a hard memory ceiling (N13). Expired windows are purged when the map reaches
 * [maxKeys] (at most once per [purgeIntervalMillis]). If it is still full, new keys share one of [shards] counters
 * instead of growing the map, so the size never exceeds `maxKeys + shards`. Sharing only makes limiting stricter for
 * the overflow keys; it never lets a caller reset or escape a bucket.
 */
class RateLimitStore(
    private val windowMillis: Long,
    private val maxKeys: Int,
    private val shards: Int = 64,
    private val purgeIntervalMillis: Long = 1_000,
) {
    private val windows = ConcurrentHashMap<String, Window>()
    private val lastPurge = AtomicLong(0)

    /** Counts one hit for [key] in its current window and returns the new count. */
    fun hit(key: String, now: Long): Int {
        var effective = key
        if (windows.size >= maxKeys && !windows.containsKey(key)) {
            purgeExpired(now)
            if (windows.size >= maxKeys) {
                effective = "shard:" + Math.floorMod(key.hashCode(), shards)
            }
        }
        return windows.compute(effective) { _, existing ->
            if (existing == null || now - existing.startedAtEpochMilli >= windowMillis) {
                Window(now, 1)
            } else {
                Window(existing.startedAtEpochMilli, existing.count + 1)
            }
        }!!.count
    }

    val size: Int get() = windows.size

    private fun purgeExpired(now: Long) {
        val last = lastPurge.get()
        if (now - last < purgeIntervalMillis && last != 0L) return
        if (!lastPurge.compareAndSet(last, now)) return
        windows.entries.removeIf { now - it.value.startedAtEpochMilli >= windowMillis }
    }

    private data class Window(val startedAtEpochMilli: Long, val count: Int)
}
