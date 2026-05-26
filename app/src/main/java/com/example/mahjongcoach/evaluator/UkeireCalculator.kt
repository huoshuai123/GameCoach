package com.example.mahjongcoach.evaluator

import mahjongutils.models.Tile as MahjongUtilsTile
import mahjongutils.shanten.ShantenWithGot
import mahjongutils.shanten.shanten

data class UkeireCandidate(
    val discard: String,
    val shantenAfter: Int,
    val ukeire: Int,
    val improvingTiles: List<String>,
)

class UkeireCalculator {
    fun evaluateCandidates(hand: List<String>, visibleTiles: List<String>): List<UkeireCandidate> {
        val result = shanten(hand.map(::toMahjongUtilsTile))
        val shantenInfo = result.shantenInfo as? ShantenWithGot ?: return emptyList()
        return shantenInfo.discardToAdvance.map { (discard, afterDiscard) ->
            val afterDiscardTiles = removeOne(hand, discard.toString())
            val knownTiles = visibleTiles + afterDiscardTiles
            val liveImprovingTiles = afterDiscard.advance
                .map { it.toString() }
                .filter { liveCount(it, knownTiles) > 0 }
            UkeireCandidate(
                discard = discard.toString(),
                shantenAfter = afterDiscard.shantenNum,
                ukeire = liveImprovingTiles.sumOf { liveCount(it, knownTiles) },
                improvingTiles = liveImprovingTiles,
            )
        }
    }

    private fun toMahjongUtilsTile(tile: String): MahjongUtilsTile = MahjongUtilsTile[tile]

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
}
