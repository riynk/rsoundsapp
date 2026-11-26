package com.rsounds.provider.local

import android.content.ContentResolver
import android.content.ContentUris
import android.provider.MediaStore

/**
 * Read-only access to audio files visible through Android's MediaStore.
 *
 * Call [getTracks] off the main thread and request READ_MEDIA_AUDIO on Android 13+
 * or READ_EXTERNAL_STORAGE on older Android releases before querying.
 */
class MediaStoreAudioRepository(
    private val contentResolver: ContentResolver
) {
    fun getTracks(): List<LocalAudioTrack> {
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION
        )
        val selection = MediaStore.Audio.Media.IS_MUSIC + " != 0 AND " +
            MediaStore.Audio.Media.DURATION + " > 0"
        val sortOrder = MediaStore.Audio.Media.TITLE + " COLLATE NOCASE ASC"

        val cursor = contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            sortOrder
        ) ?: return emptyList()

        return cursor.use { result ->
            val idColumn = result.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = result.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = result.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumColumn = result.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durationColumn = result.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

            buildList {
                while (result.moveToNext()) {
                    val id = result.getLong(idColumn)
                    val title = result.getString(titleColumn)?.trim().orEmpty()
                    val artist = result.getString(artistColumn)?.trim().orEmpty()
                    val album = result.getString(albumColumn)?.trim().orEmpty()
                    val duration = result.getLong(durationColumn)

                    if (title.isNotEmpty() && duration > 0L) {
                        add(
                            LocalAudioTrack(
                                mediaStoreId = id,
                                title = title,
                                artist = artist.ifBlank { "Unknown artist" },
                                album = album.ifBlank { "Unknown album" },
                                durationMs = duration,
                                contentUri = ContentUris.withAppendedId(
                                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                                    id
                                )
                            )
                        )
                    }
                }
            }
        }
    }
}
