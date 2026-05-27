package com.example.mahjongcoach.domain

import com.example.mahjongcoach.mjai.MjaiAssessment
import com.example.mahjongcoach.mjai.MjaiAssessmentStatus

fun EvaluationReport.withMjaiAssessments(assessments: List<MjaiAssessment>): EvaluationReport {
    if (assessments.isEmpty()) return this
    val byDecisionId = assessments.associateBy { it.decisionId }
    return copy(
        decisionPoints = decisionPoints.map { decision ->
            val assessment = byDecisionId[decision.aiDecisionId] ?: return@map decision
            if (assessment.status == MjaiAssessmentStatus.Success && !assessment.recommendedDiscard.isNullOrBlank()) {
                decision.copy(
                    aiSource = "MJAI ${assessment.model}",
                    aiRecommendedChoice = "MJAI 推荐打出 ${assessment.recommendedDiscard}",
                    aiConfidence = assessment.recommendedWeight,
                    aiStatus = assessment.status.schemaValue,
                )
            } else {
                decision.copy(aiStatus = assessment.status.schemaValue)
            }
        },
    )
}
