package com.example.mahjongcoach.evaluator

import com.example.mahjongcoach.data.FinalPaipu
import com.example.mahjongcoach.data.PaipuEvent
import com.example.mahjongcoach.data.PaipuEventType
import com.example.mahjongcoach.data.PaipuHead
import com.example.mahjongcoach.data.PaipuPlayer
import com.example.mahjongcoach.data.PaipuRound
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FinalPaipuAnalyzerTest {
    @Test
    fun analyze_returnsMahjongRoundWithCandidateFeatures() {
        val paipu = FinalPaipu(
            uuid = "game-1",
            officialUrl = "https://example.test",
            head = PaipuHead(
                modeId = "16",
                startTime = 1,
                endTime = 2,
                players = listOf(PaipuPlayer(1, "me", 0, 32000), PaipuPlayer(2, "opponent", 1, 18000)),
                viewSeat = 0,
            ),
            rounds = listOf(
                PaipuRound(
                    roundIndex = 1,
                    events = listOf(
                        PaipuEvent(0, PaipuEventType.NewRound, null, null, mapOf("honba" to "2")),
                        PaipuEvent(1, PaipuEventType.DealTile, 0, "1m"),
                        PaipuEvent(2, PaipuEventType.DealTile, 0, "2m"),
                        PaipuEvent(3, PaipuEventType.DealTile, 0, "3m"),
                        PaipuEvent(4, PaipuEventType.DealTile, 0, "4p"),
                        PaipuEvent(5, PaipuEventType.DealTile, 0, "5p"),
                        PaipuEvent(6, PaipuEventType.DealTile, 0, "6p"),
                        PaipuEvent(7, PaipuEventType.DealTile, 0, "7s"),
                        PaipuEvent(8, PaipuEventType.DealTile, 0, "8s"),
                        PaipuEvent(9, PaipuEventType.DealTile, 0, "9s"),
                        PaipuEvent(10, PaipuEventType.DealTile, 0, "2z"),
                        PaipuEvent(11, PaipuEventType.DealTile, 0, "2z"),
                        PaipuEvent(12, PaipuEventType.DealTile, 0, "5m"),
                        PaipuEvent(13, PaipuEventType.DealTile, 0, "5m"),
                        PaipuEvent(14, PaipuEventType.DealTile, 0, "9p"),
                        PaipuEvent(15, PaipuEventType.DiscardTile, 0, "9p"),
                    ),
                ),
            ),
        )

        val round = FinalPaipuAnalyzer().analyze(paipu)

        assertEquals("game-1", round.id)
        assertEquals("王座之间", round.context["room_rank"])
        assertEquals("第1名 32000点 (+7000)", round.context["result"])
        assertEquals(1, round.turns.size)
        assertEquals("东二局", round.turns.first().roundLabel)
        assertEquals(2, round.turns.first().honba)
        assertTrue(round.turns.first().ukeireBest >= round.turns.first().ukeireChosen)
        assertTrue(round.turns.first().chosenDanger in 0.0..1.0)
    }

    @Test
    fun analyze_labelsModeTwelveAsJadeRoom() {
        val paipu = FinalPaipu(
            uuid = "game-1",
            officialUrl = "https://example.test",
            head = PaipuHead(
                modeId = "12",
                startTime = 1,
                endTime = 2,
                players = listOf(PaipuPlayer(1, "me", 0, 12900)),
                viewSeat = 0,
            ),
            rounds = emptyList(),
        )

        val round = FinalPaipuAnalyzer().analyze(paipu)

        assertEquals("玉之间", round.context["room_rank"])
        assertEquals("第1名 12900点 (-12100)", round.context["result"])
    }

    @Test
    fun analyze_keepsEastThreeLabelWhenDecisionComesFromInitialHand() {
        val paipu = FinalPaipu(
            uuid = "game-1",
            officialUrl = "https://example.test",
            head = PaipuHead(
                modeId = "12",
                startTime = 1,
                endTime = 2,
                players = listOf(
                    PaipuPlayer(1, "east", 0, 25000),
                    PaipuPlayer(2, "south", 1, 25000),
                    PaipuPlayer(3, "me", 2, 25000),
                    PaipuPlayer(4, "north", 3, 25000),
                ),
                viewSeat = 2,
            ),
            rounds = listOf(
                PaipuRound(
                    roundIndex = 3,
                    events = listOf(
                        PaipuEvent(
                            0,
                            PaipuEventType.NewRound,
                            null,
                            null,
                            mapOf(
                                "chang" to "0",
                                "ju" to "2",
                                "tiles2" to """["1m","2m","3m","4p","5p","6p","7s","8s","9s","2z","2z","5m","5m","9p"]""",
                            ),
                        ),
                        PaipuEvent(1, PaipuEventType.DiscardTile, 2, "9p"),
                    ),
                ),
            ),
        )

        val round = FinalPaipuAnalyzer().analyze(paipu)

        assertEquals("东三局", round.turns.first().roundLabel)
    }
}
