package com.example.mahjongcoach.mjai

import org.json.JSONObject

class MjaiTrialSessionProvider(
    private val client: MjaiHttpClient = DefaultMjaiHttpClient(),
    private val sessionStore: MjaiSessionStore = InMemoryMjaiSessionStore(),
    private val nowMillis: () -> Long = { System.currentTimeMillis() },
) {
    private var cachedToken: String? = null

    fun fetchTrialSession(): Result<String> {
        cachedToken?.takeIf { it.isNotBlank() }?.let {
            saveSession(it)
            return Result.success(it)
        }
        sessionStore.load()
            ?.takeIf { it.token.isNotBlank() && it.expiresAtMillis > nowMillis() }
            ?.let {
                cachedToken = it.token
                saveSession(it.token)
                return Result.success(it.token)
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
            saveSession(token)
            token
        }
    }

    private fun saveSession(token: String) {
        sessionStore.save(
            MjaiStoredSession(
                token = token,
                expiresAtMillis = nowMillis() + MjaiConstants.SessionTtlMillis,
            )
        )
    }

    fun clearCachedSession() {
        cachedToken = null
        sessionStore.clear()
    }
}
