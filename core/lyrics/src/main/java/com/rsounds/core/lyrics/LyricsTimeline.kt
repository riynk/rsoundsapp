package com.rsounds.core.lyrics

data class LyricsCursor(
    val lineIndex: Int,
    val wordIndex: Int?
)

/**
 * Resolves the active lyric line/word from the real media playback position.
 *
 * A positive correctionMs delays the lyric highlight; a negative value advances it.
 * The calculation is saturating so extreme user-provided offsets cannot overflow.
 */
object LyricsTimeline {
    fun locate(
        document: LyricsDocument,
        playbackPositionMs: Long,
        correctionMs: Long = 0L
    ): LyricsCursor? {
        if (document.lines.isEmpty()) return null
        val lyricPosition = subtractSaturated(playbackPositionMs, correctionMs)
        if (lyricPosition < 0L) return null

        val lineIndex = document.lines.indexOfLast { line ->
            val start = line.startMs
            start != null && start <= lyricPosition
        }
        if (lineIndex < 0) return null

        val line = document.lines[lineIndex]
        val nextTimestampedStart = document.lines
            .drop(lineIndex + 1)
            .firstNotNullOfOrNull { it.startMs }
        val effectiveLineEnd = line.endMs ?: nextTimestampedStart
        if (effectiveLineEnd != null && lyricPosition >= effectiveLineEnd) return null

        val wordIndex = line.words.indexOfLast { it.startMs <= lyricPosition }
        if (wordIndex < 0) return LyricsCursor(lineIndex = lineIndex, wordIndex = null)

        val word = line.words[wordIndex]
        val nextWordStart = line.words.getOrNull(wordIndex + 1)?.startMs
        val effectiveWordEnd = word.endMs ?: nextWordStart
        val activeWordIndex = if (effectiveWordEnd != null && lyricPosition >= effectiveWordEnd) {
            null
        } else {
            wordIndex
        }

        return LyricsCursor(lineIndex = lineIndex, wordIndex = activeWordIndex)
    }

    private fun subtractSaturated(value: Long, correction: Long): Long = when {
        correction > 0L && value < Long.MIN_VALUE + correction -> Long.MIN_VALUE
        correction < 0L && value > Long.MAX_VALUE + correction -> Long.MAX_VALUE
        else -> value - correction
    }
}
