package com.example.mahjongcoach.mjai

import android.content.Context

data class MjaiStoredSession(
    val token: String,
    val expiresAtMillis: Long,
)

interface MjaiSessionStore {
    fun load(): MjaiStoredSession?
    fun save(session: MjaiStoredSession)
    fun clear()
}

class InMemoryMjaiSessionStore : MjaiSessionStore {
    private var session: MjaiStoredSession? = null

    override fun load(): MjaiStoredSession? = session

    override fun save(session: MjaiStoredSession) {
        this.session = session
    }

    override fun clear() {
        session = null
    }
}

class SharedPreferencesMjaiSessionStore(context: Context) : MjaiSessionStore {
    private val preferences = context.getSharedPreferences("mjai_session", Context.MODE_PRIVATE)

    override fun load(): MjaiStoredSession? {
        val token = preferences.getString(KEY_TOKEN, null)?.takeIf { it.isNotBlank() } ?: return null
        val expiresAtMillis = preferences.getLong(KEY_EXPIRES_AT, 0L)
        return MjaiStoredSession(token = token, expiresAtMillis = expiresAtMillis)
    }

    override fun save(session: MjaiStoredSession) {
        preferences.edit()
            .putString(KEY_TOKEN, session.token)
            .putLong(KEY_EXPIRES_AT, session.expiresAtMillis)
            .apply()
    }

    override fun clear() {
        preferences.edit().clear().apply()
    }

    private companion object {
        const val KEY_TOKEN = "token"
        const val KEY_EXPIRES_AT = "expires_at_millis"
    }
}
