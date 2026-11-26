package com.rsounds.core.lyrics

/**
 * Converts plain-text lyrics into readable lines while keeping their sync level honest.
 * Blank lines are discarded; non-empty lines have no invented timestamps.
 */
object PlainLyricsParser {
    fun parse(content: String?, source: String? = null): LyricsDocument? {
        if (content.isNullOrBlank()) return null
        val lines = content
            .lineSequence()
            .map { it.trimEnd('\r').trim() }
            .filter { it.isNotEmpty() }
            .map { LyricLine(startMs = null, text = it) }
            .toList()
        if (lines.isEmpty()) return null
        return LyricsDocument(lines = lines, source = source ?: "plain-text")
    }
}
