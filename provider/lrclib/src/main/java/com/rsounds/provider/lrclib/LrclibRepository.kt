package com.rsounds.provider.lrclib

import com.google.gson.Gson
import com.google.gson.JsonParseException
import com.rsounds.core.lyrics.EnhancedLrcParser
import com.rsounds.core.lyrics.LyricsDocument
import com.rsounds.core.lyrics.LyricsfileParser
import com.rsounds.core.lyrics.PlainLyricsParser
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.ThreadLocalRandom
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * LRCLIB client with explicit client identification, sequential request pacing,
 * Retry-After handling and word/line/plain lyrics fallback.
 *
 * No API key is required or embedded. Do not call this repository in a tight
 * library-scan loop; it intentionally serializes requests and applies a cooldown.
 */
class LrclibRepository(
    private val transport: LrclibHttpTransport = UrlConnectionLrclibTransport(),
    private val userAgent: String = "RSounds/0.1.0 (https://github.com/riynk/rsoundsapp)",
    private val baseUrl: String = "https://lrclib.net",
    private val monotonicTimeMs: () -> Long = { System.nanoTime() / 1_000_000L },
    private val wallTimeMs: () -> Long = { System.currentTimeMillis() },
    private val waitFor: suspend (Long) -> Unit = { delay(it) },
    private val jitterFor: (Long) -> Long = { maximum ->
        if (maximum <= 0L) 0L else ThreadLocalRandom.current().nextLong(maximum + 1L)
    },
    private val minRequestIntervalMs: Long = 300L
) {
    private val mutex = Mutex()
    private val gson = Gson()
    private var nextRequestAllowedAtMs = 0L

    suspend fun lookup(query: LrclibTrackQuery): LrclibLookupResult = mutex.withLock {
        validate(query)?.let { return@withLock LrclibLookupResult.InvalidResponse(it) }

        val beforeWait = monotonicTimeMs()
        val waitMs = (nextRequestAllowedAtMs - beforeWait).coerceAtLeast(0L)
        if (waitMs > 0L) waitFor(waitMs)

        val requestStartedAt = monotonicTimeMs()
        val response = try {
            withContext(Dispatchers.IO) {
                transport.get(buildLookupUrl(query), userAgent)
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            nextRequestAllowedAtMs = maxOf(
                nextRequestAllowedAtMs,
                saturatingAdd(requestStartedAt, minRequestIntervalMs)
            )
            return@withLock LrclibLookupResult.NetworkFailure(
                error.message ?: "Network request failed."
            )
        }

        val requestCompletedAt = monotonicTimeMs()
        nextRequestAllowedAtMs = maxOf(
            nextRequestAllowedAtMs,
            saturatingAdd(requestStartedAt, minRequestIntervalMs)
        )

        when (response.statusCode) {
            200 -> parseRecord(response.body, query)
            404 -> LrclibLookupResult.NotFound
            429 -> {
                val cooldown = retryDelayMs(response.retryAfter)
                val jitter = safeJitter(cooldown)
                nextRequestAllowedAtMs = maxOf(
                    nextRequestAllowedAtMs,
                    saturatingAdd(requestCompletedAt, saturatingAdd(cooldown, jitter))
                )
                LrclibLookupResult.RateLimited(cooldown + jitter)
            }
            503 -> {
                val cooldown = retryDelayMs(response.retryAfter)
                val jitter = safeJitter(cooldown)
                nextRequestAllowedAtMs = maxOf(
                    nextRequestAllowedAtMs,
                    saturatingAdd(requestCompletedAt, saturatingAdd(cooldown, jitter))
                )
                LrclibLookupResult.TemporarilyUnavailable(cooldown + jitter)
            }
            else -> LrclibLookupResult.HttpFailure(response.statusCode)
        }
    }

    private fun parseRecord(
        body: String,
        query: LrclibTrackQuery
    ): LrclibLookupResult {
        if (body.isBlank()) return LrclibLookupResult.InvalidResponse("LRCLIB returned an empty response.")
        val record = try {
            gson.fromJson(body, LrclibLyricsRecord::class.java)
        } catch (error: JsonParseException) {
            return LrclibLookupResult.InvalidResponse("LRCLIB returned malformed JSON.")
        } ?: return LrclibLookupResult.InvalidResponse("LRCLIB returned no track record.")

        if (record.instrumental) return LrclibLookupResult.Instrumental

        val document = buildLyricsDocument(record)
            ?: return LrclibLookupResult.NoLyricsAvailable

        return LrclibLookupResult.Found(
            record = record,
            document = document.copy(
                metadata = document.metadata + mapOf(
                    "lrclib_id" to record.id.toString(),
                    "track_name" to record.trackName.orEmpty(),
                    "artist_name" to record.artistName.orEmpty(),
                    "album_name" to record.albumName.orEmpty()
                ),
                source = "lrclib"
            ),
            signatureMatch = signatureMatch(query, record)
        )
    }

    private fun buildLyricsDocument(record: LrclibLyricsRecord): LyricsDocument? {
        if (record.hasWordSync && !record.lyricsfile.isNullOrBlank()) {
            LyricsfileParser.parseDocument(record.lyricsfile, source = "lrclib-word-sync")
                ?.let { return it }
        }

        if (!record.syncedLyrics.isNullOrBlank()) {
            EnhancedLrcParser.parse(record.syncedLyrics, source = "lrclib-line-sync")
                ?.let { return it }
        }

        return PlainLyricsParser.parse(record.plainLyrics, source = "lrclib-plain")
    }

    private fun signatureMatch(
        query: LrclibTrackQuery,
        record: LrclibLyricsRecord
    ): LyricsSignatureMatch {
        val titleMatches = normalize(query.trackName) == normalize(record.trackName.orEmpty())
        val artistMatches = normalize(query.artistName) == normalize(record.artistName.orEmpty())
        val durationMatches = query.durationMs?.let { requested ->
            record.durationSeconds?.let { actual ->
                kotlin.math.abs(actual - requested / 1_000.0) <= 2.0
            }
        }
        return LyricsSignatureMatch(
            titleMatches = titleMatches,
            artistMatches = artistMatches,
            durationMatches = durationMatches
        )
    }

    private fun buildLookupUrl(query: LrclibTrackQuery): String {
        val parameters = buildList {
            add("track_name=" + encode(query.trackName.trim()))
            add("artist_name=" + encode(query.artistName.trim()))
            query.albumName
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?.let { add("album_name=" + encode(it)) }
            query.durationMs
                ?.takeIf { it in 1_000L..3_600_000L }
                ?.let {
                    val seconds = String.format(Locale.US, "%.3f", it / 1_000.0)
                    add("duration=" + encode(seconds))
                }
        }
        return baseUrl.trimEnd('/') + "/api/get?" + parameters.joinToString("&")
    }

    private fun validate(query: LrclibTrackQuery): String? = when {
        query.trackName.isBlank() -> "Track title is required."
        query.artistName.isBlank() -> "Artist name is required."
        query.trackName.length > 300 -> "Track title is too long."
        query.artistName.length > 300 -> "Artist name is too long."
        query.albumName != null && query.albumName.length > 300 -> "Album name is too long."
        query.durationMs != null && query.durationMs !in 1_000L..3_600_000L ->
            "Track duration must be between 1 and 3600 seconds."
        userAgent.isBlank() -> "A descriptive User-Agent is required."
        minRequestIntervalMs < 200L -> "LRCLIB request pacing must be at least 200 ms."
        else -> null
    }

    private fun retryDelayMs(header: String?): Long {
        val maxCooldown = 24L * 60L * 60L * 1_000L
        val value = header?.trim().orEmpty()
        val seconds = value.toLongOrNull()
        if (seconds != null) {
            return saturatingMultiply(seconds.coerceAtLeast(0L), 1_000L).coerceAtMost(maxCooldown)
        }

        val dateMs = runCatching {
            ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME)
                .toInstant()
                .toEpochMilli()
        }.getOrNull()

        return if (dateMs != null) {
            (dateMs - wallTimeMs()).coerceAtLeast(0L).coerceAtMost(maxCooldown)
        } else {
            1_000L
        }
    }

    private fun safeJitter(cooldownMs: Long): Long {
        val maximum = (cooldownMs / 3L).coerceAtLeast(0L)
        return jitterFor(maximum).coerceIn(0L, maximum)
    }

    private fun normalize(value: String): String =
        value.lowercase(Locale.ROOT).filter { it.isLetterOrDigit() }

    private fun encode(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8.name())

    private fun saturatingAdd(left: Long, right: Long): Long {
        if (right > 0L && left > Long.MAX_VALUE - right) return Long.MAX_VALUE
        return left + right
    }

    private fun saturatingMultiply(left: Long, right: Long): Long {
        if (left <= 0L || right <= 0L) return 0L
        if (left > Long.MAX_VALUE / right) return Long.MAX_VALUE
        return left * right
    }
}
