package com.example.mahjongcoach.data

import org.junit.Assert.assertEquals
import org.junit.Test

class FinalPaipuJsonCodecTest {
    @Test
    fun roundTrip_preservesDownloadedPaipuContent() {
        val paipu = FinalPaipu(
            uuid = "260522-2793b6c3-992e-424b-9205-11a121e9ac00",
            officialUrl = "https://mahjongsoul.game.yo-star.com/?paipu=260522-test",
            head = PaipuHead(
                modeId = "16",
                startTime = 1000L,
                endTime = 2000L,
                players = listOf(
                    PaipuPlayer(accountId = 1L, nickname = "东家", seat = 0, score = 32000),
                    PaipuPlayer(accountId = 2L, nickname = "南家", seat = 1, score = 18000),
                ),
                viewSeat = 1,
                viewPlayer = PaipuPlayer(accountId = 2L, nickname = "南家", seat = 1, score = 18000),
            ),
            rounds = listOf(
                PaipuRound(
                    roundIndex = 0,
                    events = listOf(
                        PaipuEvent(
                            index = 0,
                            type = PaipuEventType.NewRound,
                            actorSeat = null,
                            tile = null,
                            payload = mapOf("chang" to "0", "ju" to "0"),
                        ),
                        PaipuEvent(
                            index = 1,
                            type = PaipuEventType.DiscardTile,
                            actorSeat = 1,
                            tile = "5m",
                            payload = mapOf("isRiichi" to "false"),
                        ),
                    ),
                )
            ),
        )

        val restored = FinalPaipuJsonCodec.fromJson(FinalPaipuJsonCodec.toJson(paipu))

        assertEquals(paipu.uuid, restored.uuid)
        assertEquals(paipu.officialUrl, restored.officialUrl)
        assertEquals(paipu.head.players, restored.head.players)
        assertEquals(paipu.head.viewSeat, restored.head.viewSeat)
        assertEquals(paipu.head.viewPlayer, restored.head.viewPlayer)
        assertEquals(paipu.rounds, restored.rounds)
    }
}
