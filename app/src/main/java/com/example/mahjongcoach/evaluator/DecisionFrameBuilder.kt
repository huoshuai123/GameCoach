package com.example.mahjongcoach.evaluator

import com.example.mahjongcoach.data.FinalPaipu
import com.example.mahjongcoach.data.PaipuEventType

class DecisionFrameBuilder {
    fun build(paipu: FinalPaipu): List<DecisionFrame> {
        val viewSeat = paipu.head.viewSeat
            ?: paipu.head.viewPlayer?.seat
            ?: paipu.head.players.firstOrNull()?.seat
            ?: return emptyList()

        return paipu.rounds.flatMap { round ->
            val hands = mutableMapOf<Int, MutableList<String>>()
            val discards = mutableMapOf<Int, MutableList<String>>()
            val calls = mutableMapOf<Int, MutableList<List<String>>>()
            val riichiSeats = mutableSetOf<Int>()
            val doraIndicators = mutableListOf<String>()
            val frames = mutableListOf<DecisionFrame>()
            var viewTurn = 0
            var lastDraw: String? = null

            round.events.forEach { event ->
                val seat = event.actorSeat
                when (event.type) {
                    PaipuEventType.NewRound -> {
                        event.payload["dora"]?.let { doraIndicators += it }
                    }

                    PaipuEventType.DealTile -> {
                        if (seat != null && event.tile != null) {
                            hands.getOrPut(seat) { mutableListOf() }.add(event.tile)
                            if (seat == viewSeat) lastDraw = event.tile
                        }
                    }

                    PaipuEventType.DiscardTile -> {
                        if (seat != null && event.tile != null) {
                            if (seat == viewSeat) {
                                viewTurn++
                                val handBeforeDiscard = hands.getOrPut(seat) { mutableListOf() }.toList()
                                frames += DecisionFrame(
                                    roundIndex = round.roundIndex,
                                    turn = viewTurn,
                                    viewSeat = viewSeat,
                                    hand = handBeforeDiscard,
                                    drawnTile = lastDraw,
                                    chosenDiscard = event.tile,
                                    visibleDiscards = discards.mapValues { it.value.toList() },
                                    calls = calls.mapValues { it.value.toList() },
                                    doraIndicators = doraIndicators.toList(),
                                    scores = paipu.head.players.sortedBy { it.seat }.map { it.score ?: 25000 },
                                    riichiSeats = riichiSeats.toSet(),
                                    visibleTiles = discards.values.flatten() + calls.values.flatten().flatten() + doraIndicators,
                                )
                                removeOne(hands.getOrPut(seat) { mutableListOf() }, event.tile)
                                lastDraw = null
                            }
                            discards.getOrPut(seat) { mutableListOf() }.add(event.tile)
                        }
                    }

                    PaipuEventType.ChiPengGang -> {
                        if (seat != null) {
                            val tiles = event.payload["tiles"]
                                ?.split(",")
                                ?.map { it.trim() }
                                ?.filter { it.isNotEmpty() }
                                .orEmpty()
                            calls.getOrPut(seat) { mutableListOf() }.add(tiles)
                            tiles.forEach { removeOne(hands.getOrPut(seat) { mutableListOf() }, it) }
                        }
                    }

                    PaipuEventType.Riichi -> {
                        if (seat != null) riichiSeats += seat
                    }

                    PaipuEventType.Hule,
                    PaipuEventType.Liuju,
                    PaipuEventType.Unknown,
                    -> Unit
                }
            }
            frames
        }
    }

    private fun removeOne(hand: MutableList<String>, tile: String) {
        val key = Tile.parse(tile).normalizedKey
        val index = hand.indexOfFirst { Tile.parse(it).normalizedKey == key }
        if (index >= 0) hand.removeAt(index)
    }
}
