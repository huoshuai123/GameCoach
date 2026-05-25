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
                            label = "第 ${turn.turn} 巡：牌效率损失",
                            turn = turn.turn,
                            severity = (4 + efficiencyLoss / 2 + dangerGap).roundToInt().coerceAtMost(10),
                            problemType = ProblemType.Efficiency,
                            currentChoice = "打出 ${turn.chosenDiscard}",
                            recommendedChoice = "优先考虑 ${turn.bestDiscard}",
                            reason = "这一手的主要问题是速度变慢。和推荐选择相比，当前弃牌少了约 ${efficiencyLoss.roundToInt()} 张有效牌，听牌前的手牌推进会被明显拖住。",
                            trainingTip = "下一局遇到相似局面时，先数向听和有效牌，再决定是否为了看起来安全而牺牲速度。",
                            roundLabel = turn.roundLabel,
                            honba = turn.honba,
                        )
                    )
                }

                if (pushRisk >= 0.45 && turn.shantenAfter >= 1.0) {
                    add(
                        DecisionPoint(
                            label = "第 ${turn.turn} 巡：高压下继续推进",
                            turn = turn.turn,
                            severity = (5 + pushRisk * 6 + turn.shantenAfter).roundToInt().coerceAtMost(10),
                            problemType = ProblemType.AttackDefense,
                            currentChoice = "打出 ${turn.chosenDiscard}",
                            recommendedChoice = "考虑转打 ${turn.safestDiscard}",
                            reason = "这一手更像是该收手的节点。对手压力已经很高，而自己仍未听牌，当前弃牌承担的放铳风险和手牌速度不匹配。",
                            trainingTip = "练习“一向听或更慢时遇到强压力”的押引阈值：没有足够打点或安全改良时，优先保命。",
                            roundLabel = turn.roundLabel,
                            honba = turn.honba,
                        )
                    )
                }

                if (dangerGap >= 0.35 && efficiencyLoss <= 2.0) {
                    add(
                        DecisionPoint(
                            label = "第 ${turn.turn} 巡：没有必要的危险牌",
                            turn = turn.turn,
                            severity = (5 + dangerGap * 8).roundToInt().coerceAtMost(10),
                            problemType = ProblemType.Risk,
                            currentChoice = "打出 ${turn.chosenDiscard}",
                            recommendedChoice = "优先选择更安全的 ${turn.bestDiscard}",
                            reason = "这一手没有用风险换到足够收益。两个候选的效率差距很小，但当前选择明显更危险。",
                            trainingTip = "当两个候选牌效率接近时，把危险度作为第一排序条件，主动降级明显危险的牌。",
                            roundLabel = turn.roundLabel,
                            honba = turn.honba,
                        )
                    )
                }
            }
        }
            .sortedByDescending { it.severity }
            .take(5)

        val metrics = listOf(
            Metric("平均有效牌损失", averageUkeireLoss(round), "越低越好，用来观察弃牌是否拖慢手牌速度。"),
            Metric("平均弃牌危险度", average(round) { it.chosenDanger }, "当前选择的放铳风险估计值。"),
            Metric("压力暴露", average(round) { it.chosenDanger * it.opponentPressure }, "在对手高压下仍承担的风险。"),
        )

        return EvaluationReport(
            situation = adapter.toSituation(round),
            summary = "本局重点看${round.focus}、危险度和攻守切换。系统从结构化牌谱中找到了 ${decisions.size} 个值得复盘的关键决策点。",
            metrics = metrics,
            decisionPoints = decisions,
            trainingFocus = decisions.firstOrNull()?.let {
                TrainingFocus(
                    theme = it.problemType.label,
                    nextAction = it.trainingTip,
                    evidence = "${it.label} 被评为${it.priority.label}，建议优先复盘。",
                )
            } ?: TrainingFocus(
                theme = "保持决策质量",
                nextAction = "继续用速度、危险度和听牌进度三件事检查每一次弃牌。",
                evidence = "这局样例没有发现高优先级决策点。",
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
