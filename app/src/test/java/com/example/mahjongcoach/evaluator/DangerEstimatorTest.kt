package com.example.mahjongcoach.evaluator

import org.junit.Assert.assertTrue
import org.junit.Test

class DangerEstimatorTest {
    @Test
    fun estimate_scoresGenbutsuLowerThanUnknownTileAgainstRiichi() {
        val estimator = DangerEstimator()
        val frame = dangerFrame(riichiSeats = setOf(1), visibleDiscards = mapOf(1 to listOf("9s")))

        val genbutsu = estimator.estimate("9s", frame)
        val unknown = estimator.estimate("5m", frame)

        assertTrue(genbutsu.score < unknown.score)
        assertTrue(genbutsu.reasons.any { it.contains("现物") })
    }

    @Test
    fun estimate_scoresDoraHigherUnderPressure() {
        val estimator = DangerEstimator()
        val frame = dangerFrame(riichiSeats = setOf(1), doraIndicators = listOf("4m"))

        val dora = estimator.estimate("5m", frame)
        val terminal = estimator.estimate("1m", frame)

        assertTrue(dora.score > terminal.score)
    }

    private fun dangerFrame(
        riichiSeats: Set<Int>,
        visibleDiscards: Map<Int, List<String>> = emptyMap(),
        doraIndicators: List<String> = emptyList(),
    ): DecisionFrame {
        return DecisionFrame(
            roundIndex = 0,
            roundLabel = "东一局",
            honba = 0,
            turn = 8,
            viewSeat = 0,
            hand = listOf("1m", "2m", "3m", "4p", "5p", "6p", "7s", "8s", "9s", "2z", "2z", "5m", "5m", "9p"),
            drawnTile = "9p",
            chosenDiscard = "9p",
            visibleDiscards = visibleDiscards,
            calls = emptyMap(),
            doraIndicators = doraIndicators,
            scores = listOf(25000, 25000, 25000, 25000),
            riichiSeats = riichiSeats,
            visibleTiles = visibleDiscards.values.flatten(),
        )
    }
}
