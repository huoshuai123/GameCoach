package com.example.mahjongcoach.evaluator

data class DangerEstimate(
    val score: Double,
    val reasons: List<String>,
)

class DangerEstimator {
    fun estimate(discard: String, frame: DecisionFrame): DangerEstimate {
        val tile = Tile.parse(discard)
        val reasons = mutableListOf<String>()
        var score = 0.20

        if (frame.riichiSeats.isNotEmpty()) {
            score += 0.25
            reasons += "对手立直后基础危险度上升"
        }

        val isGenbutsu = frame.riichiSeats.any { seat ->
            frame.visibleDiscards[seat].orEmpty().any { Tile.parse(it).normalizedKey == tile.normalizedKey }
        }
        if (isGenbutsu) {
            score -= 0.40
            reasons += "现物"
        }

        if (tile.suit == TileSuit.Honor) {
            score += if (frame.visibleTiles.count { Tile.parse(it).normalizedKey == tile.normalizedKey } >= 2) {
                -0.10
            } else {
                0.10
            }
            reasons += "字牌按已见枚数调整"
        }

        if (tile.rank == 5 && tile.suit != TileSuit.Honor) {
            score += 0.10
            reasons += "中张牌危险度较高"
        }

        if (isDora(discard, frame.doraIndicators)) {
            score += 0.15
            reasons += "宝牌危险度和损失更高"
        }

        if (frame.turn >= 12 && frame.riichiSeats.isNotEmpty()) {
            score += 0.10
            reasons += "后巡面对立直压力更高"
        }

        return DangerEstimate(score = score.coerceIn(0.0, 1.0), reasons = reasons)
    }

    private fun isDora(tile: String, indicators: List<String>): Boolean {
        return indicators.any { nextDora(Tile.parse(it)).normalizedKey == Tile.parse(tile).normalizedKey }
    }

    private fun nextDora(indicator: Tile): Tile {
        val nextRank = when (indicator.suit) {
            TileSuit.Man, TileSuit.Pin, TileSuit.Sou -> if (indicator.rank == 9) 1 else indicator.rank + 1
            TileSuit.Honor -> when (indicator.rank) {
                4 -> 1
                7 -> 5
                else -> indicator.rank + 1
            }
        }
        return indicator.copy(rank = nextRank, display = "$nextRank${indicator.suit.code}", isRedFive = false)
    }
}
