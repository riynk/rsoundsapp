package com.rsounds.provider.lrclib

import com.google.gson.annotations.SerializedName
import com.rsounds.core.lyrics.LyricsDocument

data class LrclibTrackQuery(
    val trackName: String,
    val artistName: String,
    val albumName: String? = null,
    val durationMs: Long? = null
)

data class LrclibLyricsRecord(
    @SerializedName("id") val id: Long = 0L,
    @SerializedName("trackName") val trackName: String? = null,
    @SerializedName("artistName") val artistName: String? = null,
    @SerializedName("albumName") val albumName: String? = null,
    @SerializedName("duration") val durationSeconds: Double? = null,
    @SerializedName("instrumental") val instrumental: Boolean = false,
    @SerializedName("hasWordSync") val hasWordSync: Boolean = false,
    @SerializedName("plainLyrics") val plainLyrics: String? = null,
    @SerializedName("syncedLyrics") val syncedLyrics: String? = null,
    @SerializedName("lyricsfile") val lyricsfile: String? = null
)

data class LyricsSignatureMatch(
    val titleMatches: Boolean,
    val artistMatches: Boolean,
    val durationMatches: Boolean?
) {
    val isStrong: Boolean
        get() = titleMatches && artistMatches && durationMatches != false
}

sealed interface LrclibLookupResult {
    data class Found(
        val record: LrclibLyricsRecord,
        val document: LyricsDocument,
        val signatureMatch: LyricsSignatureMatch
    ) : LrclibLookupResult

    data object Instrumental : LrclibLookupResult
    data object NoLyricsAvailable : LrclibLookupResult
    data object NotFound : LrclibLookupResult
    data class RateLimited(val retryAfterMs: Long) : LrclibLookupResult
    data class TemporarilyUnavailable(val retryAfterMs: Long) : LrclibLookupResult
    data class HttpFailure(val statusCode: Int) : LrclibLookupResult
    data class InvalidResponse(val reason: String) : LrclibLookupResult
    data class NetworkFailure(val reason: String) : LrclibLookupResult
}
