package com.example.mahjongcoach.mjai

import java.net.HttpURLConnection
import java.net.URL

interface MjaiHttpClient {
    fun post(path: String, bearerToken: String?, body: String): MjaiHttpResponse
}

class DefaultMjaiHttpClient(
    private val baseUrl: String = MjaiConstants.BaseUrl,
    private val timeoutMillis: Int = MjaiConstants.TimeoutMillis,
) : MjaiHttpClient {
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

        return try {
            connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val stream = if (connection.responseCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream ?: connection.inputStream
            }
            MjaiHttpResponse(
                code = connection.responseCode,
                body = stream.bufferedReader().use { it.readText() },
            )
        } finally {
            connection.disconnect()
        }
    }
}
