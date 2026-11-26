package com.rsounds.core.lyrics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LyricsTimelineTest {
    @Test
    fun selectsLineAtExactTimestampBoundary() {
        val document = LyricsDocument(
            lines = listOf(
                LyricLine(1_000L, "First"),
                LyricLine(2_000L, "Second")
            )
        )

        assertNull(LyricsTimeline.locate(document, 999L))
        assertEquals(LyricsCursor(0, null), LyricsTimeline.locate(document, 1_000L))
        assertEquals(LyricsCursor(1, null), LyricsTimeline.locate(document, 2_000L))
    }

    @Test
    fun selectsWordAndAppliesUserCorrection() {
        val document = LyricsDocument(
            lines = listOf(
                LyricLine(
                    startMs = 1_000L,
                    text = "Alpha beta gamma",
                    words = listOf(
                        LyricWord(1_000L, "Alpha "),
                        LyricWord(1_500L, "beta "),
                        LyricWord(2_000L, "gamma")
                    )
                )
            )
        )

        assertEquals(LyricsCursor(0, 1), LyricsTimeline.locate(document, 1_750L))
        assertEquals(LyricsCursor(0, 0), LyricsTimeline.locate(document, 1_750L, correctionMs = 400L))
        assertEquals(LyricsCursor(0, 1), LyricsTimeline.locate(document, 1_350L, correctionMs = -150L))
    }

    @Test
    fun returnsNoLineDuringGapBetweenTimedLines() {
        val document = LyricsDocument(
            lines = listOf(
                LyricLine(startMs = 1_000L, text = "First", endMs = 1_500L),
                LyricLine(startMs = 2_000L, text = "Second")
            )
        )

        assertEquals(LyricsCursor(0, null), LyricsTimeline.locate(document, 1_499L))
        assertNull(LyricsTimeline.locate(document, 1_500L))
        assertNull(LyricsTimeline.locate(document, 1_750L))
        assertEquals(LyricsCursor(1, null), LyricsTimeline.locate(document, 2_000L))
    }

    @Test
    fun plainLyricsHaveNoActiveTimelineCursor() {
        val plain = PlainLyricsParser.parse("First line\n\nSecond line")
        assertEquals(LyricsSyncLevel.PLAIN, plain?.syncLevel)
        assertEquals(2, plain?.lines?.size)
        assertNull(LyricsTimeline.locate(plain!!, 10_000L))
    }

    @Test
    fun linesWithoutWordTimingRemainLineSynchronized() {
        val document = LyricsDocument(
            lines = listOf(LyricLine(startMs = 500L, text = "Line sync only"))
        )

        assertEquals(LyricsSyncLevel.LINE, document.syncLevel)
        assertEquals(LyricsCursor(0, null), LyricsTimeline.locate(document, 900L))
    }

    @Test
    fun negativePlaybackPositionAndExtremeCorrectionAreSafe() {
        val document = LyricsDocument(lines = listOf(LyricLine(0L, "Start")))
        assertNull(LyricsTimeline.locate(document, 0L, correctionMs = 1L))
        assertNull(LyricsTimeline.locate(document, Long.MIN_VALUE, correctionMs = Long.MAX_VALUE))
    }
}
