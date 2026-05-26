package com.example.mahjongcoach.evaluator

import com.example.mahjongcoach.data.FinalPaipu
import com.example.mahjongcoach.data.PaipuEvent
import com.example.mahjongcoach.data.PaipuEventType
import com.example.mahjongcoach.data.PaipuHead
import com.example.mahjongcoach.data.PaipuPlayer
import com.example.mahjongcoach.data.PaipuRound
import org.junit.Assert.assertEquals
import org.junit.Test

class DecisionFrameBuilderTest {
    @Test
    fun build_emitsFrameForViewPlayerDiscard() {
        val paipu = FinalPaipu(
            uuid = "game-1",
            officialUrl = "https://example.test",
            head = PaipuHead(
                modeId = "16",
                startTime = 1,
                endTime = 2,
                players = listOf(PaipuPlayer(1, "me", 0, 25000), PaipuPlayer(2, "opponent", 1, 25000)),
                viewSeat = 0,
            ),
            rounds = listOf(
                PaipuRound(
                    roundIndex = 0,
                    events = listOf(
                        PaipuEvent(0, PaipuEventType.DealTile, 0, "1m"),
                        PaipuEvent(1, PaipuEventType.DealTile, 0, "2m"),
                        PaipuEvent(2, PaipuEventType.DealTile, 0, "3m"),
                        PaipuEvent(3, PaipuEventType.DealTile, 0, "9p"),
                        PaipuEvent(4, PaipuEventType.DiscardTile, 0, "9p"),
                        PaipuEvent(5, PaipuEventType.DiscardTile, 1, "5m"),
                    ),
                ),
            ),
        )

        val frames = DecisionFrameBuilder().build(paipu)

        assertEquals(1, frames.size)
        assertEquals(0, frames.first().viewSeat)
        assertEquals("9p", frames.first().chosenDiscard)
        assertEquals(4, frames.first().hand.size)
    }

    @Test
    fun build_parsesCallTilesStoredAsJsonArrayPayload() {
        val paipu = FinalPaipu(
            uuid = "game-1",
            officialUrl = "https://example.test",
            head = PaipuHead(
                modeId = "16",
                startTime = 1,
                endTime = 2,
                players = listOf(PaipuPlayer(1, "me", 0, 25000), PaipuPlayer(2, "opponent", 1, 25000)),
                viewSeat = 0,
            ),
            rounds = listOf(
                PaipuRound(
                    roundIndex = 0,
                    events = listOf(
                        PaipuEvent(0, PaipuEventType.DealTile, 0, "1m"),
                        PaipuEvent(1, PaipuEventType.DealTile, 0, "2m"),
                        PaipuEvent(2, PaipuEventType.DealTile, 0, "3m"),
                        PaipuEvent(3, PaipuEventType.ChiPengGang, 1, null, mapOf("tiles" to """["3z","3z","3z"]""")),
                        PaipuEvent(4, PaipuEventType.DealTile, 0, "9p"),
                        PaipuEvent(5, PaipuEventType.DiscardTile, 0, "9p"),
                    ),
                ),
            ),
        )

        val frames = DecisionFrameBuilder().build(paipu)

        assertEquals(listOf("3z", "3z", "3z"), frames.first().calls[1]?.first())
        assertEquals(listOf("3z", "3z", "3z"), frames.first().visibleTiles.take(3))
    }

    @Test
    fun build_usesRoundObjectMetadataInsteadOfRoundArrayIndex() {
        val paipu = FinalPaipu(
            uuid = "game-1",
            officialUrl = "https://example.test",
            head = PaipuHead(
                modeId = "12",
                startTime = 1,
                endTime = 2,
                players = listOf(PaipuPlayer(1, "me", 0, 25000), PaipuPlayer(2, "opponent", 1, 25000)),
                viewSeat = 0,
            ),
            rounds = listOf(
                PaipuRound(
                    roundIndex = 3,
                    events = listOf(
                        PaipuEvent(0, PaipuEventType.NewRound, null, null, mapOf("chang" to "0", "ju" to "2", "ben" to "1")),
                        PaipuEvent(1, PaipuEventType.DealTile, 0, "1m"),
                        PaipuEvent(2, PaipuEventType.DealTile, 0, "2m"),
                        PaipuEvent(3, PaipuEventType.DealTile, 0, "3m"),
                        PaipuEvent(4, PaipuEventType.DealTile, 0, "9p"),
                        PaipuEvent(5, PaipuEventType.DiscardTile, 0, "9p"),
                    ),
                ),
            ),
        )

        val frame = DecisionFrameBuilder().build(paipu).first()

        assertEquals("东三局", frame.roundLabel)
        assertEquals(1, frame.honba)
    }

    @Test
    fun build_treatsMissingChangAndJuAsEastOneWhenHonbaIsPresent() {
        val paipu = FinalPaipu(
            uuid = "game-1",
            officialUrl = "https://example.test",
            head = PaipuHead(
                modeId = "12",
                startTime = 1,
                endTime = 2,
                players = listOf(PaipuPlayer(1, "me", 0, 25000), PaipuPlayer(2, "opponent", 1, 25000)),
                viewSeat = 0,
            ),
            rounds = listOf(
                PaipuRound(
                    roundIndex = 1,
                    events = listOf(
                        PaipuEvent(
                            0,
                            PaipuEventType.NewRound,
                            null,
                            null,
                            mapOf(
                                "ben" to "1",
                                "tiles0" to """["1m","2m","3m","4p","5p","6p","7s","8s","9s","2z","2z","5m","5m","9p"]""",
                            ),
                        ),
                        PaipuEvent(1, PaipuEventType.DiscardTile, 0, "9p"),
                    ),
                ),
            ),
        )

        val frame = DecisionFrameBuilder().build(paipu).first()

        assertEquals("东一局", frame.roundLabel)
        assertEquals(1, frame.honba)
    }

    @Test
    fun build_usesInitialRoundHandForFirstDiscardDecision() {
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

        val frame = DecisionFrameBuilder().build(paipu).first()

        assertEquals("东三局", frame.roundLabel)
        assertEquals(14, frame.hand.size)
        assertEquals("9p", frame.chosenDiscard)
    }
}
