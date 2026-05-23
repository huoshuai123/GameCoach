package com.example.mahjongcoach.domain

data class Metric(
    val name: String,
    val value: Double,
    val explanation: String,
)

data class DecisionPoint(
    val label: String,
    val turn: Int,
    val severity: Int,
    val problemType: ProblemType,
    val currentChoice: String,
    val recommendedChoice: String,
    val reason: String,
    val trainingTip: String,
) {
    val priority: Priority
        get() = when {
            severity >= 8 -> Priority.High
            severity >= 5 -> Priority.Medium
            else -> Priority.Low
        }
}

data class Situation(
    val game: String,
    val title: String,
    val player: String = "Demo player",
    val context: Map<String, String> = emptyMap(),
)

data class EvaluationReport(
    val situation: Situation,
    val summary: String,
    val metrics: List<Metric>,
    val decisionPoints: List<DecisionPoint>,
    val trainingFocus: TrainingFocus,
)

data class TrainingFocus(
    val theme: String,
    val nextAction: String,
    val evidence: String,
)

enum class Priority(val label: String) {
    High("High"),
    Medium("Medium"),
    Low("Low"),
}

enum class ProblemType(val label: String) {
    Efficiency("Efficiency"),
    Risk("Risk"),
    AttackDefense("Attack / defense"),
}

interface GameAdapter<Input> {
    fun toSituation(input: Input): Situation
}
