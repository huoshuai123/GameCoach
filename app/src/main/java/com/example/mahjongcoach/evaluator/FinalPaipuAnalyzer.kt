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
            context = mapOf(
                "room_rank" to roomRankLabel(paipu.head.modeId),
                "result" to resultLabel(paipu),
            ),
        )
    }

    private fun toTurn(frame: DecisionFrame): MahjongTurn? {
        val candidates = ukeireCalculator.evaluateCandidates(frame.hand, frame.visibleTiles)
        if (candidates.isEmpty()) return null

        val chosen = candidates.firstOrNull { sameTile(it.discard, frame.chosenDiscard) }
            ?: candidates.first()
        val bestEfficiency = candidates.minWithOrNull(
            compareBy<UkeireCandidate> { it.shantenAfter }.thenByDescending { it.ukeire },
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
            roundLabel = frame.roundLabel,
            honba = frame.honba,
        )
    }

    private fun sameTile(left: String, right: String): Boolean {
        return Tile.parse(left).normalizedKey == Tile.parse(right).normalizedKey
    }

    private fun roomRankLabel(modeId: String?): String {
        if (modeId.isNullOrBlank()) return "未知房间"
        return when (modeId) {
            "1" -> "友人场"
            "2" -> "段位场"
            "8" -> "铜之间"
            "9" -> "银之间"
            "12" -> "玉之间"
            "15" -> "玉之间"
            "16" -> "王座之间"
            else -> "模式 $modeId"
        }
    }

    private fun resultLabel(paipu: FinalPaipu): String {
        val viewSeat = paipu.head.viewSeat ?: paipu.head.viewPlayer?.seat
        val viewPlayer = paipu.head.viewPlayer ?: paipu.head.players.firstOrNull { it.seat == viewSeat }
        if (viewPlayer?.score == null) return "结果未知"

        val ranked = paipu.head.players
            .filter { it.score != null }
            .sortedWith(compareByDescending<com.example.mahjongcoach.data.PaipuPlayer> { it.score }.thenBy { it.seat })
        val rank = ranked.indexOfFirst { it.seat == viewPlayer.seat }.takeIf { it >= 0 }?.plus(1)
        val scoreDelta = viewPlayer.score - 25000
        val deltaText = if (scoreDelta >= 0) "+$scoreDelta" else scoreDelta.toString()
        return if (rank != null) {
            "第${rank}名 ${viewPlayer.score}点 ($deltaText)"
        } else {
            "${viewPlayer.score}点 ($deltaText)"
        }
    }
}
