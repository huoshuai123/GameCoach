package com.example.mahjongcoach.mjai

import com.example.mahjongcoach.data.FinalPaipu
import com.example.mahjongcoach.data.PaipuEvent
import com.example.mahjongcoach.data.PaipuEventType
import com.example.mahjongcoach.domain.DecisionPoint
import com.example.mahjongcoach.evaluator.DecisionFrameBuilder
import com.example.mahjongcoach.evaluator.Tile
import org.json.JSONArray
import org.json.JSONObject

class FinalPaipuToMjaiConverter(
    private val frameBuilder: DecisionFrameBuilder = DecisionFrameBuilder(),
) {
    fun contexts(paipu: FinalPaipu, decisions: List<DecisionPoint>): List<MjaiDecisionContext> {
        val frames = frameBuilder.build(paipu)
        return decisions.take(MjaiConstants.MaxDecisionPoints).mapNotNull { decision ->
            val frame = frames.firstOrNull {
                it.turn == decision.turn &&
                    it.roundLabel == decision.roundLabel &&
                    it.honba == (decision.honba ?: it.honba)
            } ?: return@mapNotNull null
            val round = paipu.rounds.firstOrNull { it.roundIndex == frame.roundIndex } ?: return@mapNotNull null
            val events = JSONArray()
            val viewSeat = frame.viewSeat

            events.put(startKyoku(round.roundIndex, round.events.firstOrNull(), viewSeat, paipu))
            for (event in round.events) {
                if (event.type == PaipuEventType.DiscardTile &&
                    event.actorSeat == viewSeat &&
                    sameTile(event.tile, frame.chosenDiscard)
                ) {
                    break
                }
                toMjaiEvent(event, viewSeat)?.let(events::put)
            }

            MjaiDecisionContext(
                decisionId = decision.aiDecisionId,
                roundIndex = frame.roundIndex,
                viewSeat = frame.viewSeat,
                chosenDiscard = frame.chosenDiscard,
                events = events,
            )
        }
    }

    private fun startKyoku(roundIndex: Int, newRound: PaipuEvent?, viewSeat: Int, paipu: FinalPaipu): JSONObject {
        val payload = newRound?.payload.orEmpty()
        val chang = payload.firstInt("chang", "quan", "wind") ?: (roundIndex / 4)
        val ju = payload.firstInt("ju", "kyoku", "round_index", "roundIndex") ?: (roundIndex % 4)
        val honba = payload.firstInt("honba", "ben", "ben_chang", "changbang") ?: 0
        val dora = payload.firstValue("dora", "doras")
            ?.let(::parseTiles)
            ?.firstOrNull()
            ?: "5z"

        return JSONObject()
            .put("type", "start_kyoku")
            .put("bakaze", listOf("E", "S", "W", "N").getOrElse(chang) { "E" })
            .put("kyoku", ju + 1)
            .put("honba", honba)
            .put("kyotaku", payload.firstInt("kyotaku", "liqibang") ?: 0)
            .put("oya", ju.coerceIn(0, 3))
            .put("dora_marker", dora.toMjaiTile())
            .put("scores", JSONArray((0..3).map { seat -> paipu.head.players.firstOrNull { it.seat == seat }?.score ?: 25000 }))
            .put("tehais", maskedInitialHands(payload, viewSeat))
    }

    private fun maskedInitialHands(payload: Map<String, String>, viewSeat: Int): JSONArray {
        val result = JSONArray()
        for (seat in 0..3) {
            val hand = parseTiles(payload["tiles$seat"])
            val visible = if (seat == viewSeat) {
                hand.map { it.toMjaiTile() }
            } else {
                List(hand.size.coerceAtLeast(13)) { "?" }
            }
            result.put(JSONArray(visible))
        }
        return result
    }

    private fun toMjaiEvent(event: PaipuEvent, viewSeat: Int): JSONObject? {
        val actor = event.actorSeat ?: return null
        return when (event.type) {
            PaipuEventType.DealTile -> JSONObject()
                .put("type", "tsumo")
                .put("actor", actor)
                .put("pai", if (actor == viewSeat) event.tile.orEmpty().toMjaiTile() else "?")

            PaipuEventType.DiscardTile -> JSONObject()
                .put("type", "dahai")
                .put("actor", actor)
                .put("pai", event.tile.orEmpty().toMjaiTile())
                .put("tsumogiri", event.payload["tsumogiri"]?.toBooleanStrictOrNull() ?: false)

            PaipuEventType.Riichi -> JSONObject()
                .put("type", "reach")
                .put("actor", actor)

            PaipuEventType.ChiPengGang -> {
                val tiles = parseTiles(event.payload["tiles"]).map { it.toMjaiTile() }
                JSONObject()
                    .put("type", "fulou")
                    .put("actor", actor)
                    .put("pais", JSONArray(tiles))
            }

            else -> null
        }
    }

    private fun sameTile(left: String?, right: String): Boolean {
        if (left == null) return false
        return Tile.parse(left).normalizedKey == Tile.parse(right).normalizedKey
    }

    private fun parseTiles(rawTiles: String?): List<String> {
        if (rawTiles.isNullOrBlank()) return emptyList()
        return rawTiles
            .removePrefix("[")
            .removeSuffix("]")
            .split(",")
            .map { it.trim().trim('"', '\'') }
            .filter { it.isNotBlank() }
    }

    private fun String.toMjaiTile(): String {
        if (this == "?") return this
        val tile = Tile.parse(this)
        val suit = when (tile.suit.code) {
            'm' -> "m"
            'p' -> "p"
            's' -> "s"
            else -> "z"
        }
        return "${tile.rank}$suit${if (tile.isRedFive) "r" else ""}"
    }

    private fun Map<String, String>.firstInt(vararg keys: String): Int? {
        return firstValue(*keys)?.toIntOrNull()
    }

    private fun Map<String, String>.firstValue(vararg keys: String): String? {
        keys.forEach { key ->
            this[key]?.let { return it }
            this[key.replaceFirstChar { it.uppercase() }]?.let { return it }
        }
        return null
    }
}
