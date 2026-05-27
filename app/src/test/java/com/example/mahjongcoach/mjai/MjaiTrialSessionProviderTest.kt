package com.example.mahjongcoach.mjai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MjaiTrialSessionProviderTest {
    @Test
    fun fetchTrialSession_returnsTokenFromTrialEndpoint() {
        val client = RecordingMjaiHttpClient(
            MjaiHttpResponse(200, """{"id":"trial-token"}"""),
        )

        val result = MjaiTrialSessionProvider(client).fetchTrialSession()

        assertTrue(result.isSuccess)
        assertEquals("trial-token", result.getOrNull())
        assertEquals(listOf("/user/trial"), client.paths)
        assertTrue(client.bodies.single().contains("FREE_TRIAL_SPONSORED_BY_MJAPI_DiscordID_9ns4esyx"))
    }

    @Test
    fun fetchTrialSession_reusesTokenInMemory() {
        val client = RecordingMjaiHttpClient(
            MjaiHttpResponse(200, """{"id":"trial-token"}"""),
            MjaiHttpResponse(403, """{"error":"active session exists"}"""),
        )
        val provider = MjaiTrialSessionProvider(client)

        val first = provider.fetchTrialSession()
        val second = provider.fetchTrialSession()

        assertTrue(first.isSuccess)
        assertTrue(second.isSuccess)
        assertEquals("trial-token", second.getOrNull())
        assertEquals("trial endpoint should only be called once while cached", 1, client.paths.size)
    }

    @Test
    fun fetchTrialSession_reusesPersistedTokenBeforeExpiry() {
        val store = FakeMjaiSessionStore("persisted-token", 10_000L)
        val client = RecordingMjaiHttpClient(
            MjaiHttpResponse(403, """{"error":"active session exists"}"""),
        )

        val result = MjaiTrialSessionProvider(
            client = client,
            sessionStore = store,
            nowMillis = { 1_000L },
        ).fetchTrialSession()

        assertTrue(result.isSuccess)
        assertEquals("persisted-token", result.getOrNull())
        assertEquals("trial endpoint should not be called while persisted token is fresh", 0, client.paths.size)
        assertEquals(1_000L + MjaiConstants.SessionTtlMillis, store.expiresAtMillis)
    }

    @Test
    fun fetchTrialSession_replacesExpiredPersistedToken() {
        val store = FakeMjaiSessionStore("old-token", 999L)
        val client = RecordingMjaiHttpClient(
            MjaiHttpResponse(200, """{"id":"new-token"}"""),
        )

        val result = MjaiTrialSessionProvider(
            client = client,
            sessionStore = store,
            nowMillis = { 1_000L },
        ).fetchTrialSession()

        assertTrue(result.isSuccess)
        assertEquals("new-token", result.getOrNull())
        assertEquals("new-token", store.token)
        assertEquals(1_000L + MjaiConstants.SessionTtlMillis, store.expiresAtMillis)
    }

    @Test
    fun fetchTrialSession_rejectsEmptyToken() {
        val client = RecordingMjaiHttpClient(
            MjaiHttpResponse(200, """{"token":""}"""),
        )

        val result = MjaiTrialSessionProvider(client).fetchTrialSession()

        assertTrue(result.isFailure)
    }

    @Test
    fun fetchTrialSession_mapsUnauthorizedAndRateLimitToFailures() {
        val unauthorized = MjaiTrialSessionProvider(
            RecordingMjaiHttpClient(MjaiHttpResponse(401, """{"message":"no"}""")),
        ).fetchTrialSession()
        val limited = MjaiTrialSessionProvider(
            RecordingMjaiHttpClient(MjaiHttpResponse(429, """{"message":"slow down"}""")),
        ).fetchTrialSession()

        assertTrue(unauthorized.isFailure)
        assertTrue(limited.isFailure)
    }

    private class FakeMjaiSessionStore(
        var token: String? = null,
        var expiresAtMillis: Long = 0L,
    ) : MjaiSessionStore {
        override fun load(): MjaiStoredSession? {
            return token?.let { MjaiStoredSession(it, expiresAtMillis) }
        }

        override fun save(session: MjaiStoredSession) {
            token = session.token
            expiresAtMillis = session.expiresAtMillis
        }

        override fun clear() {
            token = null
            expiresAtMillis = 0L
        }
    }
}
