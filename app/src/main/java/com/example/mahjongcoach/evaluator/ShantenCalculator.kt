package com.example.mahjongcoach.evaluator

class ShantenCalculator {
    fun calculate(hand: List<String>): Int {
        val counts = to34Counts(hand)
        return minOf(
            normalShanten(counts),
            sevenPairsShanten(counts),
            thirteenOrphansShanten(counts),
        )
    }

    private fun normalShanten(counts: IntArray): Int {
        var best = 8

        fun search(index: Int, mutable: IntArray, melds: Int, taatsu: Int, pair: Int) {
            var i = index
            while (i < 34 && mutable[i] == 0) i++
            if (i >= 34) {
                val cappedTaatsu = minOf(taatsu, 4 - melds)
                val shanten = 8 - melds * 2 - cappedTaatsu - pair
                best = minOf(best, shanten)
                return
            }

            if (mutable[i] >= 3) {
                mutable[i] -= 3
                search(i, mutable, melds + 1, taatsu, pair)
                mutable[i] += 3
            }

            if (i < 27 && i % 9 <= 6 && mutable[i + 1] > 0 && mutable[i + 2] > 0) {
                mutable[i]--
                mutable[i + 1]--
                mutable[i + 2]--
                search(i, mutable, melds + 1, taatsu, pair)
                mutable[i]++
                mutable[i + 1]++
                mutable[i + 2]++
            }

            if (pair == 0 && mutable[i] >= 2) {
                mutable[i] -= 2
                search(i, mutable, melds, taatsu, 1)
                mutable[i] += 2
            }

            if (mutable[i] >= 2) {
                mutable[i] -= 2
                search(i, mutable, melds, taatsu + 1, pair)
                mutable[i] += 2
            }

            if (i < 27 && i % 9 <= 7 && mutable[i + 1] > 0) {
                mutable[i]--
                mutable[i + 1]--
                search(i, mutable, melds, taatsu + 1, pair)
                mutable[i]++
                mutable[i + 1]++
            }

            if (i < 27 && i % 9 <= 6 && mutable[i + 2] > 0) {
                mutable[i]--
                mutable[i + 2]--
                search(i, mutable, melds, taatsu + 1, pair)
                mutable[i]++
                mutable[i + 2]++
            }

            mutable[i]--
            search(i, mutable, melds, taatsu, pair)
            mutable[i]++
        }

        search(0, counts.copyOf(), 0, 0, 0)
        return best
    }

    private fun sevenPairsShanten(counts: IntArray): Int {
        val pairs = counts.count { it >= 2 }
        val unique = counts.count { it > 0 }
        return 6 - pairs + maxOf(0, 7 - unique)
    }

    private fun thirteenOrphansShanten(counts: IntArray): Int {
        val terminalAndHonorIndexes = intArrayOf(0, 8, 9, 17, 18, 26, 27, 28, 29, 30, 31, 32, 33)
        val unique = terminalAndHonorIndexes.count { counts[it] > 0 }
        val hasPair = terminalAndHonorIndexes.any { counts[it] >= 2 }
        return 13 - unique - if (hasPair) 1 else 0
    }
}

fun to34Counts(hand: List<String>): IntArray {
    val counts = IntArray(34)
    hand.map(Tile::parse).forEach { tile ->
        val index = when (tile.suit) {
            TileSuit.Man -> tile.rank - 1
            TileSuit.Pin -> 9 + tile.rank - 1
            TileSuit.Sou -> 18 + tile.rank - 1
            TileSuit.Honor -> 27 + tile.rank - 1
        }
        counts[index]++
    }
    return counts
}
