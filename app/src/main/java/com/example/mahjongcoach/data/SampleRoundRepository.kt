package com.example.mahjongcoach.data

import android.content.Context

class SampleRoundRepository(
    private val context: Context,
) {
    fun loadSampleRound(): MahjongRound {
        val json = context.assets.open("mahjong_round.json")
            .bufferedReader()
            .use { it.readText() }
        return MahjongRoundParser.parse(json)
    }
}
