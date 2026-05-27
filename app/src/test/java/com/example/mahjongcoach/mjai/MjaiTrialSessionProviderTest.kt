package com.example.mahjongcoach.mjai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MjaiTrialSessionProviderTest {
    @Test
    fun fetchTrialSession_returnsTokenFromTrialEndpoint() {
        val client = RecordingMjaiHttpClient(
            MjaiHttpResponse(200, """{"token":"trial-token"}"""),
        )

        val result = MjaiTrialSessionProvider(client).fetchTrialSession()

        assertTrue(result.isSuccess)
        assertEquals("trial-token", result.getOrNull())
        assertEquals(listOf("/user/trial"), client.paths)
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
}

