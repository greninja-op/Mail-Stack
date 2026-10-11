package com.greninjaop.mailorganizer.testutil

import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.Executors
import kotlin.concurrent.thread

/**
 * Minimal HTTP/1.1 fake server for unit tests.
 *
 * Exists because `com.sun.net.httpserver` (jdk.httpserver module) is not
 * on the unit-test compile/runtime classpath, and adding module flags to
 * the Kotlin Gradle plugin proved unreliable. This uses only
 * `java.net.ServerSocket` (java.base) — zero module issues.
 *
 * Supports: request line, headers (case-insensitive names), Content-Length
 * bodies, and simple text responses. Not a general HTTP server — just
 * enough for API-client tests.
 */
class FakeHttpServer(
    private val handler: (Request) -> Response,
) {
    data class Request(
        val method: String,
        val path: String,
        val query: String?,
        /** Header names are lowercased. */
        val headers: Map<String, String>,
        val body: String,
    )

    data class Response(val code: Int, val body: String)

    /**
     * Throw from a handler to close the connection without any response
     * (simulates a lost response / network failure mid-request).
     */
    class HangUp : Exception()

    private val serverSocket = ServerSocket(0)
    val port: Int = serverSocket.localPort

    private val executor = Executors.newCachedThreadPool()
    @Volatile
    private var running = true

    init {
        thread(isDaemon = true, name = "fake-http-server") {
            while (running) {
                val client: Socket = try {
                    serverSocket.accept()
                } catch (_: Exception) {
                    break
                }
                executor.submit { handle(client) }
            }
        }
    }

    private fun handle(client: Socket) {
        try {
            client.use { sock ->
                // Note: BufferedReader is char-based; all test traffic is
                // UTF-8 text, which is all this fake supports.
                val input = BufferedReader(InputStreamReader(sock.getInputStream(), Charsets.UTF_8))
                val requestLine = input.readLine() ?: return
                val parts = requestLine.split(" ")
                if (parts.size < 2) return
                val method = parts[0]
                val target = parts[1]
                val path = target.substringBefore("?")
                val query = target.substringAfter("?", "").ifEmpty { null }
                val headers = mutableMapOf<String, String>()
                while (true) {
                    val line = input.readLine() ?: break
                    if (line.isEmpty()) break
                    val idx = line.indexOf(':')
                    if (idx > 0) {
                        headers[line.substring(0, idx).trim().lowercase()] =
                            line.substring(idx + 1).trim()
                    }
                }
                val contentLength = headers["content-length"]?.toIntOrNull() ?: 0
                val body = if (contentLength > 0) {
                    val chars = CharArray(contentLength)
                    var read = 0
                    while (read < contentLength) {
                        val n = input.read(chars, read, contentLength - read)
                        if (n < 0) break
                        read += n
                    }
                    String(chars, 0, read)
                } else {
                    ""
                }
                val response = try {
                    handler(Request(method, path, query, headers, body))
                } catch (e: HangUp) {
                    return // Close without responding.
                } catch (t: Throwable) {
                    Response(500, "fake handler failed: ${t.message}")
                }
                val bytes = response.body.toByteArray(Charsets.UTF_8)
                val statusText = STATUS_TEXTS[response.code] ?: "Status"
                val head = "HTTP/1.1 ${response.code} $statusText\r\n" +
                    "Content-Type: application/json\r\n" +
                    "Content-Length: ${bytes.size}\r\n" +
                    "Connection: close\r\n\r\n"
                val out = sock.getOutputStream()
                out.write(head.toByteArray(Charsets.UTF_8))
                out.write(bytes)
                out.flush()
            }
        } catch (_: Exception) {
            // Client went away; nothing to do.
        }
    }

    fun stop() {
        running = false
        try {
            serverSocket.close()
        } catch (_: Exception) {
            // Already closed.
        }
        executor.shutdownNow()
    }

    private companion object {
        val STATUS_TEXTS = mapOf(
            200 to "OK",
            201 to "Created",
            204 to "No Content",
            400 to "Bad Request",
            401 to "Unauthorized",
            403 to "Forbidden",
            404 to "Not Found",
            409 to "Conflict",
            429 to "Too Many Requests",
            500 to "Internal Server Error",
        )
    }
}
