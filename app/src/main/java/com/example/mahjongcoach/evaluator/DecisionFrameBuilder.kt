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
            var roundLabel = roundLabel(round.roundIndex)
            var honba = 0

            round.events.forEach { event ->
                val seat = event.actorSeat
                when (event.type) {
                    PaipuEventType.NewRound -> {
                        event.payload["dora"]?.let { doraIndicators += it }
                        roundLabel = event.payload.roundLabelOrDefault(round.roundIndex)
                        honba = event.payload.intValue("honba", "ben", "ben_chang", "changbang").coerceAtLeast(0)
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
                                    roundLabel = roundLabel,
                                    honba = honba,
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
                            val tiles = parsePayloadTiles(event.payload["tiles"])
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

    private fun parsePayloadTiles(rawTiles: String?): List<String> {
        if (rawTiles.isNullOrBlank()) return emptyList()
        return rawTiles
            .removePrefix("[")
            .removeSuffix("]")
            .split(",")
            .map { tile -> tile.trim().trim('"', '\'') }
            .filter { it.isNotEmpty() }
    }

    private fun Map<String, String>.roundLabelOrDefault(roundIndex: Int): String {
        val explicit = firstValue("round", "round_label", "ju_label", "chang_ju")
        if (!explicit.isNullOrBlank()) return explicit

        val wind = firstValue("chang", "quan", "wind")?.toIntOrNull()?.let(::windLabel)
        val ju = firstValue("ju", "kyoku", "round_index", "roundIndex")?.toIntOrNull()
        if (wind != null && ju != null) return "$wind${numberLabel(ju + 1)}局"

        return roundLabel(roundIndex)
    }

    private fun Map<String, String>.intValue(vararg keys: String): Int {
        return firstValue(*keys)?.toIntOrNull() ?: 0
    }

    private fun Map<String, String>.firstValue(vararg keys: String): String? {
        keys.forEach { key ->
            this[key]?.let { return it }
            this[key.replaceFirstChar { it.uppercase() }]?.let { return it }
        }
        return null
    }

    private fun roundLabel(roundIndex: Int): String {
        val wind = windLabel(roundIndex / 4)
        val ju = roundIndex % 4 + 1
        return "$wind${numberLabel(ju)}局"
    }

    private fun windLabel(index: Int): String {
        return when (index) {
            0 -> "东"
            1 -> "南"
            2 -> "西"
            3 -> "北"
            else -> "第${index + 1}圈"
        }
    }

    private fun numberLabel(value: Int): String {
        return when (value) {
            1 -> "一"
            2 -> "二"
            3 -> "三"
            4 -> "四"
            else -> value.toString()
        }
    }
}
