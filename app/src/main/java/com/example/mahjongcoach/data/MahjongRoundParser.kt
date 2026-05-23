package com.example.mahjongcoach.data

import org.json.JSONObject

object MahjongRoundParser {
    fun parse(json: String): MahjongRound {
        val root = JSONObject(json)
        val turns = root.getJSONArray("turns")
        val parsedTurns = buildList {
            for (index in 0 until turns.length()) {
                val turn = turns.getJSONObject(index)
                add(
                    MahjongTurn(
                        turn = turn.optInt("turn"),
                        chosenDiscard = turn.optString("chosen_discard"),
                        bestDiscard = turn.optString("best_discard"),
                        safestDiscard = turn.optString("safest_discard", turn.optString("best_discard")),
                        ukeireChosen = turn.optDouble("ukeire_chosen"),
                        ukeireBest = turn.optDouble("ukeire_best"),
                        chosenDanger = turn.optDouble("chosen_danger"),
                        bestDanger = turn.optDouble("best_danger"),
                        opponentPressure = turn.optDouble("opponent_pressure"),
                        shantenAfter = turn.optDouble("shanten_after"),
                    )
                )
            }
        }
        return MahjongRound(
            id = root.optString("id", "sample"),
            title = root.optString("title", "Mahjong Soul Review Demo"),
            source = root.optString("source", "structured-demo"),
            description = root.optString("description", "结构化样例牌谱"),
            focus = root.optString("focus", "综合复盘"),
            turns = parsedTurns,
        )
    }
}
