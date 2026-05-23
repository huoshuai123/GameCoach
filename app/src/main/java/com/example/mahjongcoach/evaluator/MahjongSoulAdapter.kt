package com.example.mahjongcoach.evaluator

import com.example.mahjongcoach.data.MahjongRound
import com.example.mahjongcoach.domain.GameAdapter
import com.example.mahjongcoach.domain.Situation

class MahjongSoulAdapter : GameAdapter<MahjongRound> {
    override fun toSituation(input: MahjongRound): Situation {
        return Situation(
            game = "Mahjong Soul",
            title = input.title,
            context = mapOf(
                "source" to input.source,
                "focus" to input.focus,
                "turn_count" to input.turns.size.toString(),
            ),
        )
    }
}
