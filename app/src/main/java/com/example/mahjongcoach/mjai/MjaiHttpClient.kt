package com.example.mahjongcoach.mjai

import java.net.HttpURLConnection
import java.net.URL

interface MjaiHttpClient {
    fun get(path: String, bearerToken: String?): MjaiHttpResponse
    fun post(path: String, bearerToken: String?, body: String): MjaiHttpResponse
}

class DefaultMjaiHttpClient(
    private val baseUrl: String = MjaiConstants.BaseUrl,
    private val timeoutMillis: Int = MjaiConstants.TimeoutMillis,
) : MjaiHttpClient {
    override fun get(path: String, bearerToken: String?): MjaiHttpResponse {
        val connection = (URL(baseUrl.trimEnd('/') + path).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = timeoutMillis
            readTimeout = timeoutMillis
            bearerToken?.takeIf { it.isNotBlank() }?.let {
                setRequestProperty("Authorization", "Bearer $it")
            }
        }
        return connection.readResponse()
    }

    override fun post(path: String, bearerToken: String?, body: String): MjaiHttpResponse {
        val connection = (URL(baseUrl.trimEnd('/') + path).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = timeoutMillis
            readTimeout = timeoutMillis
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            bearerToken?.takeIf { it.isNotBlank() }?.let {
                setRequestProperty("Authorization", "Bearer $it")
            }
        }
        return connection.readResponse {
            outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
        }
    }

    private fun HttpURLConnection.readResponse(beforeRead: HttpURLConnection.() -> Unit = {}): MjaiHttpResponse {
        return try {
            beforeRead()
            val stream = if (responseCode in 200..299) {
                inputStream
            } else {
                errorStream ?: inputStream
            }
            MjaiHttpResponse(
                code = responseCode,
                body = stream.bufferedReader().use { it.readText() },
            )
        } finally {
            disconnect()
        }
    }
}
