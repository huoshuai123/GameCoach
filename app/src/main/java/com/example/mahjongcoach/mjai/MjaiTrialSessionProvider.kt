package com.example.mahjongcoach.mjai

import org.json.JSONObject

class MjaiTrialSessionProvider(
    private val client: MjaiHttpClient = DefaultMjaiHttpClient(),
) {
    fun fetchTrialSession(): Result<String> {
        return runCatching {
            val response = client.post("/user/trial", bearerToken = null, body = "{}")
            if (response.code !in 200..299) {
                error("MJAI trial session failed with HTTP ${response.code}")
            }
            val json = JSONObject(response.body)
            val token = json.optString("token")
                .ifBlank { json.optString("session_token") }
                .ifBlank { json.optString("sessionToken") }
            if (token.isBlank()) {
                error("MJAI trial session response did not include a token")
            }
            token
        }
    }
}
