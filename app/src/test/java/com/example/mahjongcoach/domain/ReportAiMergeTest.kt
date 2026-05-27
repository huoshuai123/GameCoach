package com.example.mahjongcoach.domain

import com.example.mahjongcoach.mjai.MjaiAssessment
import com.example.mahjongcoach.mjai.MjaiAssessmentStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReportAiMergeTest {
    @Test
    fun withMjaiAssessments_addsAiFieldsForSuccessfulDecision() {
        val report = sampleReport()
        val assessment = MjaiAssessment(
            decisionId = report.decisionPoints.single().aiDecisionId,
            recommendedDiscard = "1m",
            recommendedWeight = 0.7,
            chosenWeight = 0.2,
            model = "mini",
            status = MjaiAssessmentStatus.Success,
        )

        val merged = report.withMjaiAssessments(listOf(assessment))

        val decision = merged.decisionPoints.single()
        assertEquals("MJAI mini", decision.aiSource)
        assertEquals("MJAI 推荐打出 1m", decision.aiRecommendedChoice)
        assertEquals(0.7, decision.aiConfidence ?: 0.0, 0.001)
        assertEquals("success", decision.aiStatus)
    }

    @Test
    fun withMjaiAssessments_keepsRuleReportWhenAssessmentFails() {
        val report = sampleReport()
        val assessment = MjaiAssessment(
            decisionId = report.decisionPoints.single().aiDecisionId,
            recommendedDiscard = null,
            recommendedWeight = null,
            chosenWeight = null,
            model = "mini",
            status = MjaiAssessmentStatus.NetworkError,
        )

        val merged = report.withMjaiAssessments(listOf(assessment))

        val decision = merged.decisionPoints.single()
        assertNull(decision.aiRecommendedChoice)
        assertEquals("network_error", decision.aiStatus)
        assertEquals(report.decisionPoints.single().recommendedChoice, decision.recommendedChoice)
    }

    private fun sampleReport(): EvaluationReport {
        return EvaluationReport(
            situation = Situation(game = "Mahjong Soul", title = "test"),
            summary = "summary",
            metrics = emptyList(),
            decisionPoints = listOf(
                DecisionPoint(
                    label = "第 1 巡：牌效率损失",
                    turn = 1,
                    severity = 8,
                    problemType = ProblemType.Efficiency,
                    currentChoice = "打出 9p",
                    recommendedChoice = "优先考虑 1m",
                    reason = "rule reason",
                    trainingTip = "rule tip",
                    roundLabel = "东一局",
                    honba = 0,
                )
            ),
            trainingFocus = TrainingFocus("theme", "next", "evidence"),
        )
    }
}
