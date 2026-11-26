package com.rsounds.core.lyrics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class LyricsfileParserTest {
    @Test
    fun wordTimestampsAreKeptAtMillisecondPrecision() {
        val lyricsfile = """
            version: '1.0'
            lines:
              - text: 'Hello world'
                start_ms: 1000
                end_ms: 2400
                words:
                  - text: 'Hello '
                    start_ms: 1000
                    end_ms: 1500
                  - text: 'world'
                    start_ms: 1500
                    end_ms: 2400
        """.trimIndent()
        val enhanced = LyricsfileParser.toEnhancedLrc(lyricsfile)
        assertNotNull(enhanced)
        val parsed = EnhancedLrcParser.parse(enhanced)
        assertEquals(LyricsSyncLevel.WORD, parsed?.syncLevel)
        assertEquals(1_000L, parsed?.lines?.single()?.words?.get(0)?.startMs)
        assertEquals(1_500L, parsed?.lines?.single()?.words?.get(1)?.startMs)
        assertEquals("Hello world", parsed?.lines?.single()?.text)
    }

    @Test
    fun lineTimestampIsFallbackWhenWordDataIsMissing() {
        val lyricsfile = """
            version: '1.0'
            lines:
              - text: "A line without word timing"
                start_ms: 2500
                end_ms: 4000
        """.trimIndent()
        val parsed = EnhancedLrcParser.parse(LyricsfileParser.toEnhancedLrc(lyricsfile))
        assertEquals(LyricsSyncLevel.LINE, parsed?.syncLevel)
        assertEquals(2_500L, parsed?.lines?.single()?.startMs)
        assertEquals("A line without word timing", parsed?.lines?.single()?.text)
    }

    @Test
    fun malformedAndUnknownVersionsAreRejected() {
        assertNull(LyricsfileParser.toEnhancedLrc("not: [valid"))
        assertNull(LyricsfileParser.toEnhancedLrc("version: '2.0'\nlines:\n  - text: future\n    start_ms: 0"))
        assertNull(LyricsfileParser.toEnhancedLrc("version: '1.0'\nlines: []"))
    }

    @Test
    fun enhancedLrcSupportsOffsetsAndMultipleTimestamps() {
        val parsed = EnhancedLrcParser.parse(
            "[ar:Example Artist]\n[offset:-50]\n[00:01.234][00:05.678]<00:01.234>Hello"
        )
        assertNotNull(parsed)
        assertEquals("Example Artist", parsed?.metadata?.get("ar"))
        assertEquals(1_184L, parsed?.lines?.first()?.startMs)
        assertEquals(5_628L, parsed?.lines?.last()?.startMs)
        assertEquals(1_184L, parsed?.lines?.first()?.words?.first()?.startMs)
    }
}
