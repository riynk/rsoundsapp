package com.rsounds.core.lyrics

enum class LyricsSyncLevel { NONE, PLAIN, LINE, WORD }

data class LyricsDocument(
    val lines: List<LyricLine>,
    val metadata: Map<String, String> = emptyMap(),
    val offsetMs: Long = 0L,
    val source: String? = null
) {
    val syncLevel: LyricsSyncLevel
        get() = when {
            lines.isEmpty() -> LyricsSyncLevel.NONE
            lines.any { it.words.isNotEmpty() } -> LyricsSyncLevel.WORD
            lines.any { it.startMs >= 0L } -> LyricsSyncLevel.LINE
            else -> LyricsSyncLevel.PLAIN
        }
}

data class LyricLine(
    val startMs: Long,
    val text: String,
    val words: List<LyricWord> = emptyList(),
    val endMs: Long? = null
)

data class LyricWord(
    val startMs: Long,
    val text: String,
    val endMs: Long? = null
)
