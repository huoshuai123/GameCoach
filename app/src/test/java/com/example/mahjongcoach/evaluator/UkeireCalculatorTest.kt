package com.example.mahjongcoach.evaluator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UkeireCalculatorTest {
    @Test
    fun evaluateCandidates_keepsOnlyUniqueDiscardTiles() {
        val result = UkeireCalculator().evaluateCandidates(
            hand = listOf("1m", "2m", "3m", "4p", "5p", "6p", "7s", "8s", "9s", "2z", "2z", "5m", "5m", "9p"),
            visibleTiles = emptyList(),
        )

        assertEquals(result.map { it.discard }.distinct().size, result.size)
    }

    @Test
    fun evaluateCandidates_countsLiveImprovingTiles() {
        val result = UkeireCalculator().evaluateCandidates(
            hand = listOf("1m", "2m", "3m", "4p", "5p", "6p", "7s", "8s", "9s", "2z", "2z", "5m", "5m", "9p"),
            visibleTiles = listOf("5m", "5m"),
        )

        val best = result.maxBy { it.ukeire }

        assertTrue(best.ukeire > 0)
        assertTrue(result.all { it.shantenAfter >= -1 })
    }
}
