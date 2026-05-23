package com.example.mahjongcoach.data

data class MahjongRound(
    val title: String,
    val source: String,
    val turns: List<MahjongTurn>,
)

data class MahjongTurn(
    val turn: Int,
    val chosenDiscard: String,
    val bestDiscard: String,
    val safestDiscard: String,
    val ukeireChosen: Double,
    val ukeireBest: Double,
    val chosenDanger: Double,
    val bestDanger: Double,
    val opponentPressure: Double,
    val shantenAfter: Double,
)
