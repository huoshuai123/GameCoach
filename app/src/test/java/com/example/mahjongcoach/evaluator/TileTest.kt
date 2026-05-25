package com.example.mahjongcoach.evaluator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TileTest {
    @Test
    fun parse_normalizesRedFiveButKeepsDisplay() {
        val tile = Tile.parse("0p")

        assertEquals(TileSuit.Pin, tile.suit)
        assertEquals(5, tile.rank)
        assertEquals("0p", tile.display)
        assertTrue(tile.isRedFive)
    }

    @Test
    fun countVisibleTiles_countsRedFiveAsFive() {
        val counts = TileCounts.from(listOf("5m", "0m", "1z"))

        assertEquals(2, counts.count(Tile.parse("5m")))
        assertEquals(1, counts.count(Tile.parse("1z")))
    }
}
