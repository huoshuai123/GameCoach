package com.example.mahjongcoach.evaluator

import com.example.mahjongcoach.data.MahjongRound
import com.example.mahjongcoach.data.MahjongTurn
import com.example.mahjongcoach.domain.ReportSchemaExporter
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

    @Test
    fun exportedJson_matchesReviewReportSchemaShape() {
        val report = MahjongRoundEvaluator().evaluate(sampleRound())
        val schema = ReportSchemaExporter.toMap(report)
        val decisions = schema["decision_points"] as List<*>
        val decision = decisions.first() as Map<*, *>

        assertTrue(schema.containsKey("situation"))
        assertTrue(schema.containsKey("summary"))
        assertTrue(schema.containsKey("metrics"))
        assertTrue(schema.containsKey("decision_points"))
        assertTrue(schema.containsKey("training_focus"))
        assertTrue(decision.containsKey("turn"))
        assertTrue(decision.containsKey("choice"))
        assertTrue(decision.containsKey("recommendation"))
        assertTrue(decision.containsKey("problem_type"))
        assertTrue(decision.containsKey("reason"))
        assertTrue(decision.containsKey("training_tip"))
        assertTrue(decision.containsKey("priority"))
    }

    private fun sampleRound(): MahjongRound {
        return MahjongRound(
            id = "unit-test",
            title = "东二局：效率损失复盘",
            source = "unit-test",
            description = "unit test sample",
            focus = "牌效率",
            turns = listOf(
                MahjongTurn(5, "5m", "9p", "9p", 18.0, 26.0, 0.18, 0.10, 0.15, 2.0),
                MahjongTurn(9, "7s", "1m", "1m", 22.0, 24.0, 0.62, 0.18, 0.90, 1.0),
                MahjongTurn(12, "6p", "2z", "2z", 10.0, 11.0, 0.55, 0.12, 0.80, 1.0),
                MahjongTurn(15, "3m", "3m", "3m", 8.0, 8.0, 0.20, 0.20, 0.25, 0.0),
            ),
        )
    }
}
