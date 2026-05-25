package com.example.mahjongcoach.evaluator

enum class TileSuit(val code: Char) {
    Man('m'),
    Pin('p'),
    Sou('s'),
    Honor('z'),
}

data class Tile(
    val suit: TileSuit,
    val rank: Int,
    val display: String,
    val isRedFive: Boolean,
) {
    val normalizedKey: String = "$rank${suit.code}"

    companion object {
        fun parse(raw: String): Tile {
            require(raw.length == 2) { "Tile must use two-character notation: $raw" }
            val suit = when (raw[1]) {
                'm' -> TileSuit.Man
                'p' -> TileSuit.Pin
                's' -> TileSuit.Sou
                'z' -> TileSuit.Honor
                else -> error("Unsupported tile suit: $raw")
            }
            val rawRank = raw[0].digitToIntOrNull() ?: error("Unsupported tile rank: $raw")
            val rank = if (rawRank == 0 && suit != TileSuit.Honor) 5 else rawRank
            val validRank = if (suit == TileSuit.Honor) rank in 1..7 else rank in 1..9
            require(validRank) { "Invalid tile rank: $raw" }
            require(rawRank != 0 || suit != TileSuit.Honor) { "Honor tiles cannot be red fives: $raw" }
            return Tile(suit = suit, rank = rank, display = raw, isRedFive = rawRank == 0)
        }
    }
}

class TileCounts private constructor(
    private val counts: Map<String, Int>,
) {
    fun count(tile: Tile): Int = counts[tile.normalizedKey] ?: 0

    companion object {
        fun from(rawTiles: List<String>): TileCounts {
            val counts = rawTiles
                .map(Tile::parse)
                .groupingBy { it.normalizedKey }
                .eachCount()
            return TileCounts(counts)
        }
    }
}
