package com.example.mahjongcoach.evaluator

data class UkeireCandidate(
    val discard: String,
    val shantenAfter: Int,
    val ukeire: Int,
    val improvingTiles: List<String>,
)

class UkeireCalculator(
    private val shantenCalculator: ShantenCalculator = ShantenCalculator(),
) {
    fun evaluateCandidates(hand: List<String>, visibleTiles: List<String>): List<UkeireCandidate> {
        return hand
            .distinctBy { Tile.parse(it).normalizedKey }
            .map { discard ->
                val afterDiscard = removeOne(hand, discard)
                val shanten = shantenCalculator.calculate(afterDiscard)
                val knownTiles = visibleTiles + afterDiscard
                val improving = allTileTypes().filter { draw ->
                    liveCount(draw, knownTiles) > 0 &&
                        shantenCalculator.calculate(afterDiscard + draw) < shanten
                }
                UkeireCandidate(
                    discard = discard,
                    shantenAfter = shanten,
                    ukeire = improving.sumOf { liveCount(it, knownTiles) },
                    improvingTiles = improving,
                )
            }
    }

    private fun removeOne(hand: List<String>, discard: String): List<String> {
        val discardKey = Tile.parse(discard).normalizedKey
        var removed = false
        return hand.filter { tile ->
            if (!removed && Tile.parse(tile).normalizedKey == discardKey) {
                removed = true
                false
            } else {
                true
            }
        }
    }

    private fun liveCount(tile: String, visibleTiles: List<String>): Int {
        val key = Tile.parse(tile).normalizedKey
        val visible = visibleTiles.count { Tile.parse(it).normalizedKey == key }
        return (4 - visible).coerceAtLeast(0)
    }

    private fun allTileTypes(): List<String> {
        return buildList {
            for (suit in listOf("m", "p", "s")) {
                for (rank in 1..9) add("$rank$suit")
            }
            for (rank in 1..7) add("${rank}z")
        }
    }
}
