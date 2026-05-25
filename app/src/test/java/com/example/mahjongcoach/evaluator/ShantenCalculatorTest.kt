package com.example.mahjongcoach.evaluator

import org.junit.Assert.assertEquals
import org.junit.Test

class ShantenCalculatorTest {
    private val calculator = ShantenCalculator()

    @Test
    fun calculate_returnsZeroForTenpaiNormalHand() {
        val hand = listOf("1m", "2m", "3m", "4p", "5p", "6p", "7s", "8s", "9s", "2z", "2z", "5m", "5m")

        assertEquals(0, calculator.calculate(hand))
    }

    @Test
    fun calculate_returnsMinusOneForCompleteNormalHand() {
        val hand = listOf("1m", "2m", "3m", "4p", "5p", "6p", "7s", "8s", "9s", "2z", "2z", "5m", "5m", "5m")

        assertEquals(-1, calculator.calculate(hand))
    }

    @Test
    fun calculate_handlesSevenPairsTenpai() {
        val hand = listOf("1m", "1m", "2m", "2m", "3p", "3p", "4p", "4p", "5s", "5s", "6s", "6s", "7z")

        assertEquals(0, calculator.calculate(hand))
    }
}
