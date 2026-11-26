package com.rsounds.core.lyrics

/**
 * Parses LRC and enhanced-LRC timestamps without discarding millisecond precision.
 * Supports [mm:ss], [mm:ss.xx], [mm:ss.xxx] and word-level <mm:ss.xxx> tags.
 */
object EnhancedLrcParser {
    private val lineTimestamp = Regex("""^\[(\d{1,3}):(\d{2})(?:[.,](\d{1,3}))?]""")
    private val wordTimestamp = Regex("""<(\d{1,3}):(\d{2})(?:[.,](\d{1,3}))?>""")
    private val metadataLine = Regex("""^\[([a-zA-Z]+):(.*)]$""")

    fun parse(content: String?, source: String? = null): LyricsDocument? {
        if (content.isNullOrBlank()) return null
        val metadata = linkedMapOf<String, String>()
        val parsedLines = mutableListOf<LyricLine>()
        var offsetMs = 0L

        content.lineSequence().forEach { rawLine ->
            val raw = rawLine.trimEnd('\r')
            if (raw.isBlank()) return@forEach
            val meta = metadataLine.matchEntire(raw)
            if (meta != null && !meta.groupValues[1].all(Char::isDigit)) {
                val key = meta.groupValues[1].lowercase()
                val value = meta.groupValues[2].trim()
                metadata[key] = value
                if (key == "offset") offsetMs = value.toLongOrNull() ?: 0L
                return@forEach
            }

            var remainder = raw
            val lineStarts = mutableListOf<Long>()
            while (true) {
                val match = lineTimestamp.find(remainder) ?: break
                if (match.range.first != 0) break
                parseTimestamp(match.groupValues[1], match.groupValues[2], match.groupValues[3])
                    ?.let(lineStarts::add)
                remainder = remainder.substring(match.range.last + 1)
            }
            if (lineStarts.isEmpty()) return@forEach

            val words = parseWords(remainder, lineStarts.first(), offsetMs)
            val plainText = (if (words.isEmpty()) remainder else words.joinToString("") { it.text }).trim()
            if (plainText.isBlank()) return@forEach

            lineStarts.forEach { lineStart ->
                parsedLines += LyricLine(
                    startMs = (lineStart + offsetMs).coerceAtLeast(0L),
                    text = plainText,
                    words = words
                )
            }
        }

        if (parsedLines.isEmpty()) return null
        val timestampedStarts = parsedLines.mapNotNull { it.startMs }.distinct().sorted()
        val ordered = parsedLines.sortedBy { it.startMs ?: Long.MAX_VALUE }.map { line ->
            val nextLineStart = line.startMs?.let { start ->
                timestampedStarts.firstOrNull { it > start }
            }
            val wordsWithEnds = line.words.mapIndexed { wordIndex, word ->
                word.copy(endMs = word.endMs ?: line.words.getOrNull(wordIndex + 1)?.startMs)
            }
            line.copy(words = wordsWithEnds, endMs = line.endMs ?: nextLineStart)
        }
        return LyricsDocument(
            lines = ordered,
            metadata = metadata,
            offsetMs = offsetMs,
            source = source ?: "enhanced-lrc"
        )
    }

    private fun parseWords(text: String, fallbackStartMs: Long, offsetMs: Long): List<LyricWord> {
        val matches = wordTimestamp.findAll(text).toList()
        if (matches.isEmpty()) return emptyList()
        val words = mutableListOf<LyricWord>()
        val prefix = text.substring(0, matches.first().range.first)
        if (prefix.isNotBlank()) {
            words += LyricWord((fallbackStartMs + offsetMs).coerceAtLeast(0L), prefix)
        }
        matches.forEachIndexed { index, match ->
            val segmentStart = match.range.last + 1
            val segmentEnd = matches.getOrNull(index + 1)?.range?.first ?: text.length
            val wordText = text.substring(segmentStart, segmentEnd)
            val time = parseTimestamp(match.groupValues[1], match.groupValues[2], match.groupValues[3])
                ?: return@forEachIndexed
            if (wordText.isNotEmpty()) {
                words += LyricWord((time + offsetMs).coerceAtLeast(0L), wordText)
            }
        }
        return words
    }

    private fun parseTimestamp(minutes: String, seconds: String, fraction: String): Long? {
        val minuteValue = minutes.toLongOrNull() ?: return null
        val secondValue = seconds.toLongOrNull() ?: return null
        if (secondValue !in 0L..59L) return null
        val millis = when (fraction.length) {
            0 -> 0L
            1 -> fraction.toLongOrNull()?.times(100L) ?: return null
            2 -> fraction.toLongOrNull()?.times(10L) ?: return null
            else -> fraction.take(3).toLongOrNull() ?: return null
        }
        return minuteValue * 60_000L + secondValue * 1_000L + millis
    }
}
