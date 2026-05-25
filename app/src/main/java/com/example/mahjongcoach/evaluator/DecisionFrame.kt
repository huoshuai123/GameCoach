package com.example.mahjongcoach.evaluator

data class DecisionFrame(
    val roundIndex: Int,
    val roundLabel: String,
    val honba: Int,
    val turn: Int,
    val viewSeat: Int,
    val hand: List<String>,
    val drawnTile: String?,
    val chosenDiscard: String,
    val visibleDiscards: Map<Int, List<String>>,
    val calls: Map<Int, List<List<String>>>,
    val doraIndicators: List<String>,
    val scores: List<Int>,
    val riichiSeats: Set<Int>,
    val visibleTiles: List<String>,
)
