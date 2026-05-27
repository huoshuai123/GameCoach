package com.example.mahjongcoach.domain

import com.example.mahjongcoach.data.TurnContextSnapshot

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
    val roundLabel: String? = null,
    val honba: Int? = null,
    val contextSnapshot: TurnContextSnapshot? = null,
    val aiSource: String? = null,
    val aiRecommendedChoice: String? = null,
    val aiConfidence: Double? = null,
    val aiStatus: String? = null,
) {
    val priority: Priority
        get() = when {
            severity >= 8 -> Priority.High
            severity >= 5 -> Priority.Medium
            else -> Priority.Low
        }

    val aiDecisionId: String
        get() = listOf(
            roundLabel.orEmpty(),
            honba?.toString().orEmpty(),
            turn.toString(),
            currentChoice,
        ).joinToString("|")
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
    High("高优先级"),
    Medium("中优先级"),
    Low("低优先级"),
}

enum class ProblemType(val label: String, val schemaValue: String) {
    Efficiency("牌效率", "efficiency"),
    Risk("危险度", "risk"),
    AttackDefense("攻守判断", "attack_defense"),
}

interface GameAdapter<Input> {
    fun toSituation(input: Input): Situation
}
