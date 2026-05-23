package com.example.mahjongcoach.evaluator

import com.example.mahjongcoach.data.MahjongRound
import com.example.mahjongcoach.domain.DecisionPoint
import com.example.mahjongcoach.domain.EvaluationReport
import com.example.mahjongcoach.domain.Metric
import com.example.mahjongcoach.domain.ProblemType
import com.example.mahjongcoach.domain.TrainingFocus
import kotlin.math.roundToInt

class MahjongRoundEvaluator(
    private val adapter: MahjongSoulAdapter = MahjongSoulAdapter(),
) {
    fun evaluate(round: MahjongRound): EvaluationReport {
        val decisions = round.turns.flatMap { turn ->
            val efficiencyLoss = turn.ukeireBest - turn.ukeireChosen
            val dangerGap = turn.chosenDanger - turn.bestDanger
            val pushRisk = turn.chosenDanger * turn.opponentPressure
            buildList {
                if (efficiencyLoss >= 4.0) {
                    add(
                        DecisionPoint(
                            label = "Turn ${turn.turn}: tile efficiency loss",
                            turn = turn.turn,
                            severity = (4 + efficiencyLoss / 2 + dangerGap).roundToInt().coerceAtMost(10),
                            problemType = ProblemType.Efficiency,
                            currentChoice = "Discarded ${turn.chosenDiscard}",
                            recommendedChoice = "Prefer ${turn.bestDiscard}",
                            reason = "The chosen discard loses ${efficiencyLoss.roundToInt()} effective tiles compared with the best candidate, slowing the hand before tenpai.",
                            trainingTip = "Check shanten and ukeire before choosing a safe-looking discard.",
                        )
                    )
                }

                if (pushRisk >= 0.45 && turn.shantenAfter >= 1.0) {
                    add(
                        DecisionPoint(
                            label = "Turn ${turn.turn}: over-push risk",
                            turn = turn.turn,
                            severity = (5 + pushRisk * 6 + turn.shantenAfter).roundToInt().coerceAtMost(10),
                            problemType = ProblemType.AttackDefense,
                            currentChoice = "Discarded ${turn.chosenDiscard}",
                            recommendedChoice = "Consider ${turn.safestDiscard}",
                            reason = "Opponent pressure is high while the hand is not ready. The chosen tile carries too much deal-in risk for the current hand speed.",
                            trainingTip = "Practice push/fold thresholds when one shanten or worse under riichi pressure.",
                        )
                    )
                }

                if (dangerGap >= 0.35 && efficiencyLoss <= 2.0) {
                    add(
                        DecisionPoint(
                            label = "Turn ${turn.turn}: unnecessary danger",
                            turn = turn.turn,
                            severity = (5 + dangerGap * 8).roundToInt().coerceAtMost(10),
                            problemType = ProblemType.Risk,
                            currentChoice = "Discarded ${turn.chosenDiscard}",
                            recommendedChoice = "Prefer safer ${turn.bestDiscard}",
                            reason = "The selected tile is much more dangerous without buying meaningful speed or value.",
                            trainingTip = "When candidates have similar efficiency, downgrade clearly dangerous tiles.",
                        )
                    )
                }
            }
        }
            .sortedByDescending { it.severity }
            .take(5)

        val metrics = listOf(
            Metric("Average ukeire loss", averageUkeireLoss(round), "Lower is better."),
            Metric("Average chosen danger", average(round) { it.chosenDanger }, "Estimated deal-in risk proxy."),
            Metric("Pressure exposure", average(round) { it.chosenDanger * it.opponentPressure }, "Risk taken under opponent pressure."),
        )

        return EvaluationReport(
            situation = adapter.toSituation(round),
            summary = "This report focuses on tile efficiency, danger, and push/fold discipline. It found ${decisions.size} review-worthy decision point(s).",
            metrics = metrics,
            decisionPoints = decisions,
            trainingFocus = decisions.firstOrNull()?.let {
                TrainingFocus(
                    theme = it.problemType.label,
                    nextAction = it.trainingTip,
                    evidence = "${it.label} has ${it.priority.label.lowercase()} priority.",
                )
            } ?: TrainingFocus(
                theme = "Keep reviewing decision quality",
                nextAction = "Compare each discard against speed, danger, and hand readiness.",
                evidence = "No high-priority decision point was found in this sample.",
            ),
        )
    }

    private fun averageUkeireLoss(round: MahjongRound): Double {
        return average(round) { it.ukeireBest - it.ukeireChosen }
    }

    private fun average(round: MahjongRound, selector: (com.example.mahjongcoach.data.MahjongTurn) -> Double): Double {
        if (round.turns.isEmpty()) return 0.0
        return round.turns.sumOf(selector) / round.turns.size
    }
}
