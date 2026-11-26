package com.rsounds.provider.lrclib

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

data class LrclibHttpResponse(
    val statusCode: Int,
    val body: String,
    val retryAfter: String? = null
)

interface LrclibHttpTransport {
    @Throws(IOException::class)
    fun get(url: String, userAgent: String): LrclibHttpResponse
}

/**
 * Small GET-only transport. It does not follow arbitrary redirects, send cookies,
 * or persist server responses. Network calls are made from Dispatchers.IO.
 */
class UrlConnectionLrclibTransport : LrclibHttpTransport {
    override fun get(url: String, userAgent: String): LrclibHttpResponse {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 10_000
            connection.readTimeout = 15_000
            connection.instanceFollowRedirects = false
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("User-Agent", userAgent)

            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader(Charsets.UTF_8)?.use { reader ->
                val result = StringBuilder()
                val buffer = CharArray(8_192)
                while (true) {
                    val count = reader.read(buffer)
                    if (count < 0) break
                    if (result.length + count > MAX_RESPONSE_CHARS) {
                        throw IOException("LRCLIB response exceeded the configured size limit.")
                    }
                    result.append(buffer, 0, count)
                }
                result.toString()
            }.orEmpty()

            return LrclibHttpResponse(
                statusCode = status,
                body = body,
                retryAfter = connection.getHeaderField("Retry-After")
            )
        } finally {
            connection.disconnect()
        }
    }

    private companion object {
        const val MAX_RESPONSE_CHARS = 2_000_000
    }
}
