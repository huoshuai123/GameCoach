package com.example.mahjongcoach.mjai

class RecordingMjaiHttpClient(
    private vararg val responses: MjaiHttpResponse,
) : MjaiHttpClient {
    val paths = mutableListOf<String>()
    val bodies = mutableListOf<String>()
    private var index = 0

    override fun post(path: String, bearerToken: String?, body: String): MjaiHttpResponse {
        paths += path
        bodies += body
        return responses.getOrElse(index++) { responses.last() }
    }
}

