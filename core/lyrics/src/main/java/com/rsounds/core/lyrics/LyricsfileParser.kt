/*
 * Adapted from PixelMusic by Anshul Yadav and contributors.
 * Source: https://github.com/ianshulyadav/PixelMusic
 * Original path: app/src/main/java/com/unshoo/pixelmusic/data/network/lyrics/LyricsfileParser.kt
 * Upstream project license: GPL-3.0-or-later.
 *
 * RSounds changes: package/API adaptation and three-digit millisecond timestamps.
 * See docs/PROVENANCE.md and retain upstream license/copyright attributions.
 */
package com.rsounds.core.lyrics

import java.util.Locale
import org.yaml.snakeyaml.LoaderOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.SafeConstructor

/** Converts LRCLIB Lyricsfile YAML into enhanced LRC with bounded safe parsing. */
object LyricsfileParser {
    private const val MAX_LYRICSFILE_CODE_POINTS = 1_000_000
    private const val MAX_LINES = 10_000
    private const val MAX_WORDS_PER_LINE = 2_000

    private fun newYaml(): Yaml {
        val options = LoaderOptions().apply {
            codePointLimit = MAX_LYRICSFILE_CODE_POINTS
            maxAliasesForCollections = 0
            nestingDepthLimit = 20
            isAllowDuplicateKeys = false
        }
        return Yaml(SafeConstructor(options))
    }

    fun toEnhancedLrc(rawLyricsfile: String?): String? {
        val source = rawLyricsfile?.takeIf { it.isNotBlank() } ?: return null
        val root = runCatching { newYaml().load<Any?>(source) }.getOrNull() as? Map<*, *> ?: return null
        if (root["version"]?.toString() != "1.0") return null
        val lines = (root["lines"] as? List<*>)?.take(MAX_LINES).orEmpty()
        return lines.mapNotNull(::parseLine).takeIf { it.isNotEmpty() }?.joinToString("\n")
    }

    /** Parses Lyricsfile directly into RSounds' normalized lyrics model. */
    fun parseDocument(rawLyricsfile: String?, source: String = "lrclib"): LyricsDocument? {
        return EnhancedLrcParser.parse(toEnhancedLrc(rawLyricsfile), source)
    }

    private fun parseLine(rawLine: Any?): String? {
        val line = rawLine as? Map<*, *> ?: return null
        val text = line["text"]?.toString()?.sanitizeLineText().orEmpty()
        val rawWords = (line["words"] as? List<*>)?.take(MAX_WORDS_PER_LINE).orEmpty()
        val words = rawWords.mapNotNull(::parseWord)
        val startMs = line["start_ms"].asMilliseconds() ?: words.firstOrNull()?.startMs ?: return null
        val content = if (words.isNotEmpty()) buildWordSyncedContent(text, words) else text
        if (content.isBlank()) return null
        return "[${formatTimestamp(startMs)}]$content"
    }

    private fun buildWordSyncedContent(text: String, words: List<TimedWord>): String {
        if (text.isNotBlank()) {
            val aligned = StringBuilder()
            var cursor = 0
            var allWordsAligned = true
            for (word in words) {
                val token = word.text.sanitizeLineText().trim()
                if (token.isEmpty()) continue
                val position = text.indexOf(token, startIndex = cursor)
                if (position < cursor) {
                    allWordsAligned = false
                    break
                }
                aligned.append(text, cursor, position)
                aligned.append('<').append(formatTimestamp(word.startMs)).append('>')
                aligned.append(token)
                cursor = position + token.length
            }
            if (allWordsAligned && aligned.isNotEmpty()) {
                aligned.append(text, cursor, text.length)
                return aligned.toString()
            }
        }
        return words.joinToString("") { word ->
            "<${formatTimestamp(word.startMs)}>${word.text.sanitizeLineText()}"
        }
    }

    private fun parseWord(rawWord: Any?): TimedWord? {
        val word = rawWord as? Map<*, *> ?: return null
        val text = word["text"]?.toString()?.takeIf { it.isNotEmpty() } ?: return null
        val startMs = word["start_ms"].asMilliseconds() ?: return null
        return TimedWord(startMs, text)
    }

    private fun Any?.asMilliseconds(): Int? {
        val value = when (this) {
            is Number -> toLong()
            is String -> toLongOrNull()
            else -> null
        } ?: return null
        return value.takeIf { it in 0..Int.MAX_VALUE.toLong() }?.toInt()
    }

    private fun String.sanitizeLineText(): String = replace('\r', ' ').replace('\n', ' ')

    private fun formatTimestamp(timeMs: Int): String {
        val totalSeconds = timeMs / 1_000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.US, "%02d:%02d.%03d", minutes, seconds, timeMs % 1_000)
    }

    private data class TimedWord(val startMs: Int, val text: String)
}
