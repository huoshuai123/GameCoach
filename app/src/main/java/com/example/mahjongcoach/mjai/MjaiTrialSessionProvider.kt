package com.example.mahjongcoach.mjai

import org.json.JSONObject

class MjaiTrialSessionProvider(
    private val client: MjaiHttpClient = DefaultMjaiHttpClient(),
    private val sessionStore: MjaiSessionStore = InMemoryMjaiSessionStore(),
    private val nowMillis: () -> Long = { System.currentTimeMillis() },
    private val logger: MjaiLogger = StdoutMjaiLogger,
) {
    private var cachedToken: String? = null

    fun fetchTrialSession(): Result<String> {
        cachedToken?.takeIf { it.isNotBlank() }?.let {
            saveSession(it)
            logger.info("Reusing in-memory trial session token.")
            return Result.success(it)
        }
        sessionStore.load()
            ?.takeIf { it.token.isNotBlank() && it.expiresAtMillis > nowMillis() }
            ?.let {
                cachedToken = it.token
                saveSession(it.token)
                logger.info("Reusing persisted trial session token.")
                return Result.success(it.token)
            }
        return runCatching {
            logger.info("Requesting MJAI trial session.")
            val response = client.post(
                "/user/trial",
                bearerToken = null,
                body = JSONObject().put("code", MjaiConstants.TrialCode).toString(),
            )
            if (response.code !in 200..299) {
                logger.warn("Trial session request failed: HTTP ${response.code}, body=${response.body.truncatedForLog()}")
                error("MJAI trial session failed with HTTP ${response.code}")
            }
            val json = JSONObject(response.body)
            val token = json.optString("id")
                .ifBlank { json.optString("token") }
                .ifBlank { json.optString("session_token") }
                .ifBlank { json.optString("sessionToken") }
            if (token.isBlank()) {
                logger.warn("Trial session response did not include a token: body=${response.body.truncatedForLog()}")
                error("MJAI trial session response did not include a token")
            }
            cachedToken = token
            saveSession(token)
            logger.info("Stored MJAI trial session token with local TTL.")
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
        logger.warn("Cleared cached MJAI trial session token.")
    }
}
