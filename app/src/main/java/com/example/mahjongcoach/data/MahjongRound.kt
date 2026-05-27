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
    val contextSnapshot: TurnContextSnapshot? = null,
)

data class TurnContextSnapshot(
    val hand: List<String>,
    val drawnTile: String?,
    val doraIndicators: List<String>,
    val scores: List<Int>,
    val riichiSeats: Set<Int>,
    val visibleDiscards: Map<Int, List<String>>,
    val calls: Map<Int, List<List<String>>>,
    val candidates: List<TurnCandidateSnapshot>,
)

data class TurnCandidateSnapshot(
    val discard: String,
    val shantenAfter: Int,
    val ukeire: Int,
    val danger: Double,
    val improvingTiles: List<String>,
)
