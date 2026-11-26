package com.rsounds.provider.local

import android.net.Uri

/**
 * A media item indexed from Android MediaStore.
 *
 * The URI is a content URI owned by the platform media provider. RSounds stores
 * metadata for display but does not copy or modify the user's audio file.
 */
data class LocalAudioTrack(
    val mediaStoreId: Long,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val contentUri: Uri
)
