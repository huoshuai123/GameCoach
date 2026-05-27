package com.example.mahjongcoach.mjai

import com.example.mahjongcoach.data.FinalPaipu
import com.example.mahjongcoach.data.PaipuEvent
import com.example.mahjongcoach.data.PaipuEventType
import com.example.mahjongcoach.data.PaipuHead
import com.example.mahjongcoach.data.PaipuPlayer
import com.example.mahjongcoach.data.PaipuRound
import com.example.mahjongcoach.domain.DecisionPoint
import com.example.mahjongcoach.domain.ProblemType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FinalPaipuToMjaiConverterTest {
    @Test
    fun contexts_masksOpponentHandsAndKeepsViewHand() {
        val context = FinalPaipuToMjaiConverter().contexts(samplePaipu(), listOf(sampleDecision())).single()
        val start = context.events.getJSONObject(0)
        val tehais = start.getJSONArray("tehais")

        assertEquals("start_kyoku", start.getString("type"))
        assertEquals("1m", tehais.getJSONArray(0).getString(0))
        assertEquals("?", tehais.getJSONArray(1).getString(0))
        assertEquals("5mr", tehais.getJSONArray(0).getString(4))
    }

    @Test
    fun contexts_stopsBeforeTheDecisionDiscardAndEmitsVisibleEvents() {
        val context = FinalPaipuToMjaiConverter().contexts(samplePaipu(), listOf(sampleDecision())).single()
        val types = (0 until context.events.length()).map { context.events.getJSONObject(it).getString("type") }
        val discardTiles = (0 until context.events.length())
            .map { context.events.getJSONObject(it) }
            .filter { it.optString("type") == "dahai" }
            .map { it.getString("pai") }

        assertTrue(types.contains("tsumo"))
        assertTrue(discardTiles.contains("7z"))
        assertFalse("decision discard should not be sent before asking MJAI", discardTiles.contains("9p"))
    }

    private fun sampleDecision(): DecisionPoint {
        return DecisionPoint(
            label = "第 1 巡：牌效率损失",
            turn = 1,
            severity = 8,
            problemType = ProblemType.Efficiency,
            currentChoice = "打出 9p",
            recommendedChoice = "优先考虑 1m",
            reason = "reason",
            trainingTip = "tip",
            roundLabel = "东一局",
            honba = 0,
        )
    }

    private fun samplePaipu(): FinalPaipu {
        return FinalPaipu(
            uuid = "game-1",
            officialUrl = "https://example.test",
            head = PaipuHead(
                modeId = "12",
                startTime = 1,
                endTime = 2,
                players = listOf(
                    PaipuPlayer(1, "me", 0, 25000),
                    PaipuPlayer(2, "opponent", 1, 25000),
                ),
                viewSeat = 0,
            ),
            rounds = listOf(
                PaipuRound(
                    roundIndex = 0,
                    events = listOf(
                        PaipuEvent(
                            0,
                            PaipuEventType.NewRound,
                            null,
                            null,
                            mapOf(
                                "chang" to "0",
                                "ju" to "0",
                                "honba" to "0",
                                "dora" to "3s",
                                "tiles0" to """["1m","2m","3m","4m","0m","6p","7p","8p","1s","2s","3s","4z","4z"]""",
                                "tiles1" to """["9m","9m","9m","1p","1p","1p","9s","9s","9s","1z","2z","3z","5z"]""",
                            ),
                        ),
                        PaipuEvent(1, PaipuEventType.DealTile, 0, "9p"),
                        PaipuEvent(2, PaipuEventType.DiscardTile, 1, "7z"),
                        PaipuEvent(3, PaipuEventType.DiscardTile, 0, "9p"),
                    ),
                ),
            ),
        )
    }
}
