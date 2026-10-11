package com.greninjaop.mailorganizer.data.http

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Minimal HTTP transport seam for Google API clients.
 *
 * Production code uses [UrlConnectionTransport] (plain [HttpURLConnection],
 * no extra dependencies). Unit tests inject a fake — this seam exists
 * because JVM loopback TCP is unavailable in some environments, so tests
 * cannot spin up a real local HTTP server.
 *
 * Implementations must throw [IOException] (never return a synthetic
 * response) when the request cannot reach the server.
 */
fun interface HttpTransport {
    /**
     * Executes one HTTP request.
     *
     * @param method e.g. "GET", "POST", "DELETE".
     * @param url fully-qualified URL.
     * @param headers request headers.
     * @param body request body, or null for bodiless requests.
     * @return the HTTP status code and response body (possibly empty).
     * @throws IOException when the request fails at the transport level.
     */
    @Throws(IOException::class)
    fun execute(
        method: String,
        url: String,
        headers: Map<String, String>,
        body: String?,
    ): TransportResponse
}

/** HTTP status code + body + headers returned by [HttpTransport.execute]. */
data class TransportResponse(
    val code: Int,
    val body: String,
    /** Response header names are lowercased. */
    val headers: Map<String, String> = emptyMap(),
)

/**
 * [HttpTransport] backed by [HttpURLConnection].
 *
 * Timeouts are constructor parameters so tests (and callers) can tune
 * them; defaults suit interactive use.
 */
class UrlConnectionTransport(
    private val connectTimeoutMs: Int = 15_000,
    private val readTimeoutMs: Int = 30_000,
) : HttpTransport {

    @Throws(IOException::class)
    override fun execute(
        method: String,
        url: String,
        headers: Map<String, String>,
        body: String?,
    ): TransportResponse {
        var connection: HttpURLConnection? = null
        try {
            connection = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = method
                connectTimeout = connectTimeoutMs
                readTimeout = readTimeoutMs
                // Don't follow redirects automatically: an unexpected
                // redirect to a login page must surface as an error, not
                // silently succeed.
                instanceFollowRedirects = false
                headers.forEach { (name, value) -> setRequestProperty(name, value) }
                if (body != null) {
                    doOutput = true
                    outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                }
            }
            val code = connection.responseCode
            val responseHeaders = connection.headerFields
                .filterKeys { it != null }
                .flatMap { (name, values) -> values.map { name.lowercase() to it } }
                .toMap()
            val responseBody = try {
                connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            } catch (_: IOException) {
                // Error stream (4xx/5xx) — may be absent.
                try {
                    connection.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                } catch (_: IOException) {
                    null
                } ?: ""
            }
            return TransportResponse(code, responseBody, responseHeaders)
        } finally {
            connection?.disconnect()
        }
    }
}
