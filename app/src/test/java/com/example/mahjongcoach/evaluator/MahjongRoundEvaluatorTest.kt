package com.example.mahjongcoach.evaluator

import com.example.mahjongcoach.data.MahjongRound
import com.example.mahjongcoach.data.MahjongTurn
import org.junit.Assert.assertTrue
import org.junit.Test

class MahjongRoundEvaluatorTest {
    @Test
    fun evaluate_returnsCoachStyleReportWithDecisionPoints() {
        val report = MahjongRoundEvaluator().evaluate(sampleRound())

        assertTrue(report.situation.game == "Mahjong Soul")
        assertTrue(report.decisionPoints.size in 3..5)
        assertTrue(report.metrics.isNotEmpty())
        assertTrue(report.decisionPoints.all { it.turn > 0 })
        assertTrue(report.decisionPoints.all { it.trainingTip.isNotBlank() })
    }

    private fun sampleRound(): MahjongRound {
        return MahjongRound(
            title = "Mahjong Soul Review Demo - East 2",
            source = "unit-test",
            turns = listOf(
                MahjongTurn(5, "5m", "9p", "9p", 18.0, 26.0, 0.18, 0.10, 0.15, 2.0),
                MahjongTurn(9, "7s", "1m", "1m", 22.0, 24.0, 0.62, 0.18, 0.90, 1.0),
                MahjongTurn(12, "6p", "2z", "2z", 10.0, 11.0, 0.55, 0.12, 0.80, 1.0),
                MahjongTurn(15, "3m", "3m", "3m", 8.0, 8.0, 0.20, 0.20, 0.25, 0.0),
            ),
        )
    }
}
