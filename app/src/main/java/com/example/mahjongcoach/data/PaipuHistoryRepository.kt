package com.example.mahjongcoach.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class PaipuHistoryEntry(
    val uuid: String,
    val title: String,
    val playerSummary: String,
    val savedAtMillis: Long,
)

data class PaipuHistoryRecord(
    val entry: PaipuHistoryEntry,
    val paipu: FinalPaipu,
)

class PaipuHistoryRepository(
    context: Context,
    private val nowMillis: () -> Long = { System.currentTimeMillis() },
) {
    private val preferences = context.getSharedPreferences("paipu_history", Context.MODE_PRIVATE)

    fun list(): List<PaipuHistoryEntry> {
        return preferences.getString(KEY_INDEX, null)
            ?.let { json -> parseIndex(JSONArray(json)) }
            ?: emptyList()
    }

    fun save(paipu: FinalPaipu): PaipuHistoryEntry {
        val entry = PaipuHistoryEntry(
            uuid = paipu.uuid,
            title = paipu.historyTitle(),
            playerSummary = paipu.head.players.joinToString(" / ") { it.nickname },
            savedAtMillis = nowMillis(),
        )
        val updatedIndex = (list().filterNot { it.uuid == entry.uuid } + entry)
            .sortedByDescending { it.savedAtMillis }

        preferences.edit()
            .putString(KEY_INDEX, updatedIndex.toJsonArray().toString())
            .putString(recordKey(paipu.uuid), FinalPaipuJsonCodec.toJson(paipu).toString())
            .apply()
        return entry
    }

    fun load(uuid: String): PaipuHistoryRecord? {
        val paipuJson = preferences.getString(recordKey(uuid), null) ?: return null
        val paipu = FinalPaipuJsonCodec.fromJson(JSONObject(paipuJson))
        val entry = list().firstOrNull { it.uuid == uuid }
            ?: PaipuHistoryEntry(
                uuid = paipu.uuid,
                title = paipu.historyTitle(),
                playerSummary = paipu.head.players.joinToString(" / ") { it.nickname },
                savedAtMillis = 0L,
            )
        return PaipuHistoryRecord(entry = entry, paipu = paipu)
    }

    private fun parseIndex(array: JSONArray): List<PaipuHistoryEntry> {
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val uuid = item.optString("uuid")
                if (uuid.isNotBlank()) {
                    add(
                        PaipuHistoryEntry(
                            uuid = uuid,
                            title = item.optString("title", uuid),
                            playerSummary = item.optString("playerSummary"),
                            savedAtMillis = item.optLong("savedAtMillis"),
                        )
                    )
                }
            }
        }
    }

    private fun List<PaipuHistoryEntry>.toJsonArray(): JSONArray {
        return JSONArray().also { array ->
            forEach { entry ->
                array.put(
                    JSONObject()
                        .put("uuid", entry.uuid)
                        .put("title", entry.title)
                        .put("playerSummary", entry.playerSummary)
                        .put("savedAtMillis", entry.savedAtMillis)
                )
            }
        }
    }

    private fun FinalPaipu.historyTitle(): String {
        val viewName = head.viewPlayer?.nickname
        val players = head.players.joinToString(" / ") { it.nickname }
        return when {
            !viewName.isNullOrBlank() -> "$viewName 的牌谱"
            players.isNotBlank() -> players
            else -> uuid
        }
    }

    private fun recordKey(uuid: String): String = "$KEY_RECORD_PREFIX$uuid"

    private companion object {
        const val KEY_INDEX = "index"
        const val KEY_RECORD_PREFIX = "record:"
    }
}

object FinalPaipuJsonCodec {
    fun toJson(paipu: FinalPaipu): JSONObject {
        return JSONObject()
            .put("uuid", paipu.uuid)
            .put("officialUrl", paipu.officialUrl)
            .put("head", paipu.head.toJson())
            .put("rounds", JSONArray().also { rounds ->
                paipu.rounds.forEach { round -> rounds.put(round.toJson()) }
            })
    }

    fun fromJson(json: JSONObject): FinalPaipu {
        return FinalPaipu(
            uuid = json.getString("uuid"),
            officialUrl = json.getString("officialUrl"),
            head = json.getJSONObject("head").toPaipuHead(),
            rounds = json.getJSONArray("rounds").toPaipuRounds(),
        )
    }

    private fun PaipuHead.toJson(): JSONObject {
        return JSONObject()
            .put("modeId", modeId)
            .put("startTime", startTime)
            .put("endTime", endTime)
            .put("viewSeat", viewSeat)
            .put("players", JSONArray().also { array ->
                players.forEach { array.put(it.toJson()) }
            })
    }

    private fun PaipuPlayer.toJson(): JSONObject {
        return JSONObject()
            .put("accountId", accountId)
            .put("nickname", nickname)
            .put("seat", seat)
            .put("score", score)
    }

    private fun PaipuRound.toJson(): JSONObject {
        return JSONObject()
            .put("roundIndex", roundIndex)
            .put("events", JSONArray().also { array ->
                events.forEach { array.put(it.toJson()) }
            })
    }

    private fun PaipuEvent.toJson(): JSONObject {
        return JSONObject()
            .put("index", index)
            .put("type", type.name)
            .put("actorSeat", actorSeat)
            .put("tile", tile)
            .put("payload", JSONObject(payload))
    }

    private fun JSONObject.toPaipuHead(): PaipuHead {
        val players = getJSONArray("players").toPaipuPlayers()
        val viewSeat = optIntOrNull("viewSeat")
        return PaipuHead(
            modeId = optStringOrNull("modeId"),
            startTime = optLongOrNull("startTime"),
            endTime = optLongOrNull("endTime"),
            players = players,
            viewSeat = viewSeat,
            viewPlayer = viewSeat?.let { seat -> players.firstOrNull { it.seat == seat } },
        )
    }

    private fun JSONArray.toPaipuPlayers(): List<PaipuPlayer> {
        return buildList {
            for (index in 0 until length()) {
                val item = optJSONObject(index) ?: continue
                add(
                    PaipuPlayer(
                        accountId = item.optLongOrNull("accountId"),
                        nickname = item.optString("nickname"),
                        seat = item.optInt("seat"),
                        score = item.optIntOrNull("score"),
                    )
                )
            }
        }
    }

    private fun JSONArray.toPaipuRounds(): List<PaipuRound> {
        return buildList {
            for (index in 0 until length()) {
                val item = optJSONObject(index) ?: continue
                add(
                    PaipuRound(
                        roundIndex = item.optInt("roundIndex"),
                        events = item.getJSONArray("events").toPaipuEvents(),
                    )
                )
            }
        }
    }

    private fun JSONArray.toPaipuEvents(): List<PaipuEvent> {
        return buildList {
            for (index in 0 until length()) {
                val item = optJSONObject(index) ?: continue
                add(
                    PaipuEvent(
                        index = item.optInt("index"),
                        type = runCatching { PaipuEventType.valueOf(item.optString("type")) }
                            .getOrDefault(PaipuEventType.Unknown),
                        actorSeat = item.optIntOrNull("actorSeat"),
                        tile = item.optStringOrNull("tile"),
                        payload = item.optJSONObject("payload").toStringMap(),
                    )
                )
            }
        }
    }

    private fun JSONObject?.toStringMap(): Map<String, String> {
        if (this == null) return emptyMap()
        return buildMap {
            val keys = keys()
            while (keys.hasNext()) {
                val key = keys.next()
                put(key, optString(key))
            }
        }
    }

    private fun JSONObject.optLongOrNull(name: String): Long? {
        return if (has(name) && !isNull(name)) optLong(name) else null
    }

    private fun JSONObject.optIntOrNull(name: String): Int? {
        return if (has(name) && !isNull(name)) optInt(name) else null
    }

    private fun JSONObject.optStringOrNull(name: String): String? {
        return if (has(name) && !isNull(name)) optString(name).ifBlank { null } else null
    }
}
