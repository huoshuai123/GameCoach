package com.example.mahjongcoach.mjai

import org.json.JSONObject

class MjaiTrialSessionProvider(
    private val client: MjaiHttpClient = DefaultMjaiHttpClient(),
) {
    private var cachedToken: String? = null

    fun fetchTrialSession(): Result<String> {
        cachedToken?.takeIf { it.isNotBlank() }?.let {
            return Result.success(it)
        }
        return runCatching {
            val response = client.post(
                "/user/trial",
                bearerToken = null,
                body = JSONObject().put("code", MjaiConstants.TrialCode).toString(),
            )
            if (response.code !in 200..299) {
                error("MJAI trial session failed with HTTP ${response.code}")
            }
            val json = JSONObject(response.body)
            val token = json.optString("id")
                .ifBlank { json.optString("token") }
                .ifBlank { json.optString("session_token") }
                .ifBlank { json.optString("sessionToken") }
            if (token.isBlank()) {
                error("MJAI trial session response did not include a token")
            }
            cachedToken = token
            token
        }
    }

    fun clearCachedSession() {
        cachedToken = null
    }
}
