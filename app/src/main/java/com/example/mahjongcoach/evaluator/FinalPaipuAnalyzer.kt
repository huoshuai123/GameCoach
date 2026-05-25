package com.example.mahjongcoach.evaluator

import com.example.mahjongcoach.data.FinalPaipu
import com.example.mahjongcoach.data.MahjongRound
import com.example.mahjongcoach.data.MahjongTurn

class FinalPaipuAnalyzer(
    private val frameBuilder: DecisionFrameBuilder = DecisionFrameBuilder(),
    private val ukeireCalculator: UkeireCalculator = UkeireCalculator(),
    private val dangerEstimator: DangerEstimator = DangerEstimator(),
) {
    fun analyze(paipu: FinalPaipu): MahjongRound {
        val frames = frameBuilder.build(paipu)
        val turns = frames.mapNotNull(::toTurn)
        val viewSeat = paipu.head.viewSeat ?: paipu.head.viewPlayer?.seat
        val playerName = paipu.head.viewPlayer?.nickname
            ?: paipu.head.players.firstOrNull { it.seat == viewSeat }?.nickname
            ?: "复盘玩家"
        return MahjongRound(
            id = paipu.uuid,
            title = "$playerName 的雀魂复盘",
            source = paipu.officialUrl,
            description = "从完整牌谱事件流生成的规则型复盘输入。",
            focus = "高置信牌效率与攻守判断",
            turns = turns,
        )
    }

    private fun toTurn(frame: DecisionFrame): MahjongTurn? {
        val candidates = ukeireCalculator.evaluateCandidates(frame.hand, frame.visibleTiles)
        if (candidates.isEmpty()) return null

        val chosen = candidates.firstOrNull { sameTile(it.discard, frame.chosenDiscard) }
            ?: candidates.first()
        val bestEfficiency = candidates.maxWithOrNull(
            compareBy<UkeireCandidate> { it.ukeire }.thenBy { -it.shantenAfter },
        ) ?: return null
        val dangerByDiscard = candidates.associate { candidate ->
            candidate.discard to dangerEstimator.estimate(candidate.discard, frame).score
        }
        val safest = dangerByDiscard.minByOrNull { it.value } ?: return null

        val opponentPressure = when {
            frame.riichiSeats.isNotEmpty() && frame.turn >= 12 -> 0.95
            frame.riichiSeats.isNotEmpty() -> 0.80
            frame.calls.any { it.key != frame.viewSeat && it.value.isNotEmpty() } -> 0.45
            else -> 0.20
        }

        return MahjongTurn(
            turn = frame.turn,
            chosenDiscard = frame.chosenDiscard,
            bestDiscard = bestEfficiency.discard,
            safestDiscard = safest.key,
            ukeireChosen = chosen.ukeire.toDouble(),
            ukeireBest = bestEfficiency.ukeire.toDouble(),
            chosenDanger = dangerEstimator.estimate(frame.chosenDiscard, frame).score,
            bestDanger = dangerByDiscard[bestEfficiency.discard] ?: 0.0,
            opponentPressure = opponentPressure,
            shantenAfter = chosen.shantenAfter.toDouble(),
        )
    }

    private fun sameTile(left: String, right: String): Boolean {
        return Tile.parse(left).normalizedKey == Tile.parse(right).normalizedKey
    }
}
