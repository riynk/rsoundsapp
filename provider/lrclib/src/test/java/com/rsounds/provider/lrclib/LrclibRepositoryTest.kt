package com.rsounds.provider.lrclib

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.coroutines.runBlocking
import com.rsounds.core.lyrics.LyricsfileParser
import com.rsounds.core.lyrics.LyricsSyncLevel

class LrclibRepositoryTest {
    @Test
    fun prefersWordSyncedLyricsfileWhenAdvertised() = runBlocking {
        val body = """
            {
              "id": 123,
              "trackName": "Hello World",
              "artistName": "RSounds Band",
              "albumName": "First Album",
              "duration": 180,
              "instrumental": false,
              "hasWordSync": true,
              "plainLyrics": "Hello world",
              "syncedLyrics": "[00:01.00]Hello world",
              "lyricsfile": "version: '1.0'\nlines:\n  - text: 'Hello world'\n    start_ms: 1000\n    words:\n      - text: 'Hello '\n        start_ms: 1000\n      - text: 'world'\n        start_ms: 1500\n"
            }
        """.trimIndent()
        val transport = QueueTransport(LrclibHttpResponse(200, body))
        val repository = testRepository(transport)

        val result = repository.lookup(
            LrclibTrackQuery("Hello World", "RSounds Band", "First Album", 180_000L)
        )

        assertTrue(result is LrclibLookupResult.Found)
        val found = result as LrclibLookupResult.Found
        assertTrue("JSON hasWordSync flag was not decoded", found.record.hasWordSync)
        assertNotNull("Lyricsfile field was not decoded", found.record.lyricsfile)
        val parsedLyricsfile = LyricsfileParser.parseDocument(found.record.lyricsfile)
        assertNotNull("Lyricsfile did not parse", parsedLyricsfile)
        assertEquals(
            "Lyricsfile must retain word timestamps",
            LyricsSyncLevel.WORD,
            parsedLyricsfile?.syncLevel
        )
        assertEquals(LyricsSyncLevel.WORD, found.document.syncLevel)
        assertEquals("lrclib-word-sync", found.document.source)
        assertTrue(found.signatureMatch.isStrong)
    }

    @Test
    fun fallsBackToLineSyncThenPlainText() = runBlocking {
        val lineJson = """
            {
              "id": 5,
              "trackName": "Line Track",
              "artistName": "Artist",
              "duration": 100,
              "hasWordSync": false,
              "syncedLyrics": "[00:01.00]First line\n[00:03.00]Second line",
              "plainLyrics": "First line\nSecond line"
            }
        """.trimIndent()
        val plainJson = """
            {
              "id": 6,
              "trackName": "Plain Track",
              "artistName": "Artist",
              "duration": 100,
              "hasWordSync": false,
              "plainLyrics": "First line\nSecond line"
            }
        """.trimIndent()
        val transport = QueueTransport(
            LrclibHttpResponse(200, lineJson),
            LrclibHttpResponse(200, plainJson)
        )
        val repository = testRepository(transport)

        val lineResult = repository.lookup(LrclibTrackQuery("Line Track", "Artist"))
        val plainResult = repository.lookup(LrclibTrackQuery("Plain Track", "Artist"))

        assertEquals("LINE", (lineResult as LrclibLookupResult.Found).document.syncLevel.name)
        assertEquals("PLAIN", (plainResult as LrclibLookupResult.Found).document.syncLevel.name)
    }

    @Test
    fun instrumentalAndNotFoundAreNotTreatedAsEmptyLyrics() = runBlocking {
        val instrumentalJson = """{"id":7,"instrumental":true}"""
        val transport = QueueTransport(
            LrclibHttpResponse(200, instrumentalJson),
            LrclibHttpResponse(404, """{"name":"TrackNotFound"}""")
        )
        val repository = testRepository(transport)

        assertEquals(LrclibLookupResult.Instrumental, repository.lookup(LrclibTrackQuery("Instrumental", "Artist")))
        assertEquals(LrclibLookupResult.NotFound, repository.lookup(LrclibTrackQuery("Missing", "Artist")))
    }

    @Test
    fun rateLimitHonorsRetryAfterBeforeTheNextRequest() = runBlocking {
        var clock = 10_000L
        val transport = QueueTransport(
            LrclibHttpResponse(429, "{}", retryAfter = "2"),
            LrclibHttpResponse(404, "{}")
        )
        val repository = LrclibRepository(
            transport = transport,
            monotonicTimeMs = { clock },
            waitFor = { delayMs -> clock += delayMs },
            jitterFor = { 0L }
        )

        val first = repository.lookup(LrclibTrackQuery("Track", "Artist"))
        assertEquals(LrclibLookupResult.RateLimited(2_000L), first)

        val second = repository.lookup(LrclibTrackQuery("Track", "Artist"))
        assertEquals(LrclibLookupResult.NotFound, second)
        assertTrue(transport.requestCount == 2)
        assertTrue(clock >= 12_000L)
    }

    @Test
    fun sendsIdentifyingUserAgentAndEncodedSignatureParameters() = runBlocking {
        val transport = QueueTransport(LrclibHttpResponse(404, "{}"))
        val repository = testRepository(transport)

        repository.lookup(
            LrclibTrackQuery("A Song & More", "An Artist", "An Album", 205_250L)
        )

        assertTrue(transport.lastUserAgent.contains("RSounds"))
        assertTrue(transport.lastUrl.contains("track_name=A+Song+%26+More"))
        assertTrue(transport.lastUrl.contains("artist_name=An+Artist"))
        assertTrue(transport.lastUrl.contains("duration=205.250"))
    }

    @Test
    fun rejectsInvalidSignatureBeforeMakingNetworkRequest() = runBlocking {
        val transport = QueueTransport()
        val repository = testRepository(transport)

        val result = repository.lookup(LrclibTrackQuery("", "Artist"))

        assertTrue(result is LrclibLookupResult.InvalidResponse)
        assertFalse(transport.requestCount > 0)
    }

    private fun testRepository(transport: QueueTransport) = LrclibRepository(
        transport = transport,
        monotonicTimeMs = { 10_000L },
        waitFor = {},
        jitterFor = { 0L }
    )

    private class QueueTransport(
        private vararg val responses: LrclibHttpResponse
    ) : LrclibHttpTransport {
        var requestCount: Int = 0
            private set
        var lastUrl: String = ""
            private set
        var lastUserAgent: String = ""
            private set

        override fun get(url: String, userAgent: String): LrclibHttpResponse {
            lastUrl = url
            lastUserAgent = userAgent
            val response = responses.getOrNull(requestCount) ?: LrclibHttpResponse(404, "{}")
            requestCount += 1
            return response
        }
    }
}
