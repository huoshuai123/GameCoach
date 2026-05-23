package com.example.mahjongcoach.domain

object ReportSchemaExporter {
    fun toMap(report: EvaluationReport): Map<String, Any> {
        return mapOf(
            "situation" to mapOf(
                "game" to report.situation.game,
                "title" to report.situation.title,
                "player" to report.situation.player,
                "context" to report.situation.context,
            ),
            "summary" to report.summary,
            "metrics" to report.metrics.map {
                mapOf(
                    "name" to it.name,
                    "value" to it.value,
                    "explanation" to it.explanation,
                )
            },
            "decision_points" to report.decisionPoints.map {
                mapOf(
                    "turn" to it.turn,
                    "choice" to it.currentChoice,
                    "recommendation" to it.recommendedChoice,
                    "problem_type" to it.problemType.schemaValue,
                    "reason" to it.reason,
                    "training_tip" to it.trainingTip,
                    "priority" to it.priority.name.lowercase(),
                )
            },
            "training_focus" to mapOf(
                "theme" to report.trainingFocus.theme,
                "next_action" to report.trainingFocus.nextAction,
                "evidence" to report.trainingFocus.evidence,
            ),
        )
    }
}
