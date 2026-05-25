package com.example.mahjongcoach.data

data class MahjongRound(
    val id: String,
    val title: String,
    val source: String,
    val description: String,
    val focus: String,
    val turns: List<MahjongTurn>,
    val context: Map<String, String> = emptyMap(),
)

data class SampleRound(
    val id: String,
    val title: String,
    val description: String,
    val focus: String,
    val assetName: String,
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
    val roundLabel: String? = null,
    val honba: Int? = null,
)
