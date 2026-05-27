package com.example.mahjongcoach.mjai

import com.example.mahjongcoach.data.FinalPaipu
import com.example.mahjongcoach.data.PaipuEvent
import com.example.mahjongcoach.data.PaipuEventType
import com.example.mahjongcoach.data.PaipuHead
import com.example.mahjongcoach.data.PaipuPlayer
import com.example.mahjongcoach.data.PaipuRound
import com.example.mahjongcoach.domain.DecisionPoint
import com.example.mahjongcoach.domain.EvaluationReport
import com.example.mahjongcoach.domain.ProblemType
import com.example.mahjongcoach.domain.Situation
import com.example.mahjongcoach.domain.TrainingFocus
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MjaiReviewProviderTest {
    @Test
    fun assess_usesTrialTokenMiniModelAndStopsSession() = runBlocking {
        val client = RecordingMjaiHttpClient(
            MjaiHttpResponse(200, """{"id":"trial-token"}"""),
            MjaiHttpResponse(200, """{"status":"ok"}"""),
            MjaiHttpResponse(200, """{"act":{"type":"dahai","pai":"1m","meta":{"q_values":{"1m":0.72,"9p":0.12}}}}"""),
            MjaiHttpResponse(200, """{"status":"ok"}"""),
        )

        val result = MjaiReviewProvider(client = client).assess(samplePaipu(), sampleReport())

        assertEquals(listOf("/user/trial", "/mjai/start", "/mjai/batch", "/mjai/stop"), client.paths)
        assertTrue(client.bodies[1].contains(""""model":"mini""""))
        assertTrue(client.bodies[2].trim().startsWith("["))
        assertTrue(client.bodies[2].contains(""""seq":0"""))
        assertTrue(client.bodies[2].contains(""""data""""))
        assertTrue(client.bodies[2].contains(""""type":"start_kyoku""""))
        assertEquals(MjaiAssessmentStatus.Success, result.single().status)
        assertEquals("1m", result.single().recommendedDiscard)
        assertEquals(0.72, result.single().recommendedWeight ?: 0.0, 0.001)
        assertEquals(0.12, result.single().chosenWeight ?: 0.0, 0.001)
    }

    @Test
    fun assess_returnsTrialUnavailableWhenTrialFails() = runBlocking {
        val client = RecordingMjaiHttpClient(MjaiHttpResponse(429, """{"message":"quota"}"""))

        val result = MjaiReviewProvider(client = client).assess(samplePaipu(), sampleReport())

        assertEquals(MjaiAssessmentStatus.TrialUnavailable, result.single().status)
        assertEquals(listOf("/user/trial"), client.paths)
    }

    private fun sampleReport(): EvaluationReport {
        val decision = DecisionPoint(
            label = "第 1 巡：牌效率损失",
            turn = 1,
            severity = 8,
            problemType = ProblemType.Efficiency,
            currentChoice = "打出 9p",
            recommendedChoice = "优先考虑 1m",
            reason = "reason",
            trainingTip = "tip",
            roundLabel = "东一局",
            honba = 0,
        )
        return EvaluationReport(
            situation = Situation(game = "Mahjong Soul", title = "test"),
            summary = "summary",
            metrics = emptyList(),
            decisionPoints = listOf(decision),
            trainingFocus = TrainingFocus("theme", "next", "evidence"),
        )
    }

    private fun samplePaipu(): FinalPaipu {
        return FinalPaipu(
            uuid = "game-1",
            officialUrl = "https://example.test",
            head = PaipuHead(
                modeId = "12",
                startTime = 1,
                endTime = 2,
                players = listOf(PaipuPlayer(1, "me", 0, 25000), PaipuPlayer(2, "opponent", 1, 25000)),
                viewSeat = 0,
            ),
            rounds = listOf(
                PaipuRound(
                    roundIndex = 0,
                    events = listOf(
                        PaipuEvent(0, PaipuEventType.NewRound, null, null, mapOf("tiles0" to """["1m","2m","3m","4m","5m","6p","7p","8p","1s","2s","3s","4z","4z"]""")),
                        PaipuEvent(1, PaipuEventType.DealTile, 0, "9p"),
                        PaipuEvent(2, PaipuEventType.DiscardTile, 0, "9p"),
                    ),
                ),
            ),
        )
    }
}
