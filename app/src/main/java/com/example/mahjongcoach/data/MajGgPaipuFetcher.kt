package com.example.mahjongcoach.data

import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONArray
import org.json.JSONObject

interface PublicPaipuFetcher {
    fun fetch(request: PublicPaipuFetchRequest): FinalPaipu
}

class MajGgPaipuFetcher : PublicPaipuFetcher {
    override fun fetch(request: PublicPaipuFetchRequest): FinalPaipu {
        val connection = (URL(request.majGgUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 8000
            readTimeout = 8000
        }
        return try {
            val code = connection.responseCode
            if (code !in 200..299) {
                throw IllegalStateException("公开解析源返回 $code，暂时无法提取牌谱。")
            }
            parseHtml(request, connection.inputStream.bufferedReader().use { it.readText() })
        } finally {
            connection.disconnect()
        }
    }

    fun parseHtml(request: PublicPaipuFetchRequest, html: String): FinalPaipu {
        val state = extractFreshState(html)
        val game = findGameObject(state)
            ?: throw IllegalStateException("公开解析源结构变化，未找到牌谱数据。")
        val accounts = game.findArray("accounts")
            ?: throw IllegalStateException("公开解析源结构变化，未找到玩家信息。")
        val rounds = game.findArray("Rounds")
            ?: game.findArray("rounds")
            ?: throw IllegalStateException("公开解析源结构变化，未找到牌谱局数据。")

        val finalScores = game.resultScores()
            ?: game.findIntArray("finalScores")
            ?: game.findIntArray("final_scores")
        val players = parsePlayers(accounts, finalScores)
        val viewPlayer = request.viewAccountId?.let { accountId ->
            players.firstOrNull { it.accountId == accountId }
        }
        if (request.viewAccountId != null && viewPlayer == null) {
            throw IllegalStateException("公开牌谱中未找到视角账号，无法定位主视角。")
        }
        val viewSeat = viewPlayer?.seat

        val head = PaipuHead(
            modeId = game.optStringOrNull("modeId")
                ?: game.optStringOrNull("mode_id")
                ?: game.optStringOrNull("mode")
                ?: game.optJSONObject("config")
                    ?.optJSONObject("meta")
                    ?.optStringOrNull("modeId"),
            startTime = null,
            endTime = null,
            players = players,
            viewSeat = viewSeat,
            viewPlayer = viewPlayer,
        )

        return FinalPaipu(
            uuid = request.uuid,
            officialUrl = request.officialUrl,
            head = head,
            rounds = parseRounds(rounds),
        )
    }

    private fun extractFreshState(html: String): JSONObject {
        val scriptContent = findFreshStateScriptContent(html)
        val json = when {
            scriptContent != null -> scriptContent.trim()
            else -> findAssignedFreshStateJson(html)
        }
            ?.replace("&quot;", "\"")
            ?: throw IllegalStateException("公开解析源结构变化，未找到 Fresh state。")
        return JSONObject(json).also { it.resolveFreshReferences() }
    }

    private fun findFreshStateScriptContent(html: String): String? {
        val markerIndex = html.indexOf("__FRSH_STATE", ignoreCase = true)
        if (markerIndex < 0) return null

        val scriptStart = html.lastIndexOf("<script", markerIndex, ignoreCase = true)
        if (scriptStart < 0) return null

        val openTagEnd = html.indexOf('>', scriptStart)
        if (openTagEnd < 0 || openTagEnd < markerIndex) return null

        val scriptEnd = html.indexOf("</script>", openTagEnd, ignoreCase = true)
        if (scriptEnd < 0) return null

        val openTag = html.substring(scriptStart, openTagEnd)
        if (!openTag.contains("id=", ignoreCase = true)) return null

        return html.substring(openTagEnd + 1, scriptEnd)
    }

    private fun findAssignedFreshStateJson(html: String): String? {
        val markerIndex = html.indexOf("__FRSH_STATE", ignoreCase = true)
        if (markerIndex < 0) return null

        val assignmentIndex = html.indexOf('=', markerIndex)
        if (assignmentIndex < 0) return null

        val objectStart = html.indexOf('{', assignmentIndex)
        if (objectStart < 0) return null

        return extractBalancedJsonObject(html, objectStart)
    }

    private fun extractBalancedJsonObject(text: String, objectStart: Int): String? {
        var depth = 0
        var inString = false
        var escaping = false

        for (index in objectStart until text.length) {
            val char = text[index]
            if (inString) {
                when {
                    escaping -> escaping = false
                    char == '\\' -> escaping = true
                    char == '"' -> inString = false
                }
                continue
            }

            when (char) {
                '"' -> inString = true
                '{' -> depth += 1
                '}' -> {
                    depth -= 1
                    if (depth == 0) {
                        return text.substring(objectStart, index + 1)
                    }
                }
            }
        }

        return null
    }

    private fun findGameObject(value: Any?): JSONObject? {
        return when (value) {
            is JSONObject -> {
                if (value.findArray("accounts") != null && (value.findArray("Rounds") != null || value.findArray("rounds") != null)) {
                    value
                } else {
                    val keys = value.keys()
                    while (keys.hasNext()) {
                        val found = findGameObject(value.opt(keys.next()))
                        if (found != null) return found
                    }
                    null
                }
            }
            is JSONArray -> {
                for (index in 0 until value.length()) {
                    val found = findGameObject(value.opt(index))
                    if (found != null) return found
                }
                null
            }
            else -> null
        }
    }

    private fun JSONObject.resolveFreshReferences() {
        val values = optJSONArray("v") ?: return
        val references = optJSONArray("r") ?: return
        for (index in 0 until references.length()) {
            val reference = references.optJSONArray(index) ?: continue
            val sourcePath = reference.optJSONArray(0) ?: continue
            val targetPath = reference.optJSONArray(1) ?: continue
            val sourceValue = values.valueAt(sourcePath) ?: continue
            values.setValueAt(targetPath, sourceValue)
        }
    }

    private fun JSONArray.valueAt(path: JSONArray): Any? {
        var current: Any? = this
        for (index in 0 until path.length()) {
            val key = path.optString(index)
            current = when (current) {
                is JSONArray -> current.opt(key.toIntOrNull() ?: return null)
                is JSONObject -> current.opt(key)
                else -> return null
            }
        }
        return current
    }

    private fun JSONArray.setValueAt(path: JSONArray, value: Any) {
        if (path.length() == 0) return
        var current: Any? = this
        for (index in 0 until path.length() - 1) {
            val key = path.optString(index)
            current = when (current) {
                is JSONArray -> current.opt(key.toIntOrNull() ?: return)
                is JSONObject -> current.opt(key)
                else -> return
            }
        }

        val lastKey = path.optString(path.length() - 1)
        when (current) {
            is JSONArray -> current.put(lastKey.toIntOrNull() ?: return, value)
            is JSONObject -> current.put(lastKey, value)
        }
    }

    private fun parsePlayers(accounts: JSONArray, finalScores: List<Int>? = null): List<PaipuPlayer> {
        return buildList {
            for (index in 0 until accounts.length()) {
                val account = accounts.optJSONObject(index) ?: continue
                val seat = account.optIntOrNull("seat") ?: index
                add(
                    PaipuPlayer(
                        accountId = account.optLongOrNull("accountId") ?: account.optLongOrNull("account_id"),
                        nickname = account.optString("nickname", account.optString("name", "玩家${seat + 1}")),
                        seat = seat,
                        score = account.optIntOrNull("score") ?: finalScores?.getOrNull(seat),
                    )
                )
            }
        }
    }

    private fun parseRounds(rounds: JSONArray): List<PaipuRound> {
        return buildList {
            for (roundIndex in 0 until rounds.length()) {
                val round = rounds.optJSONObject(roundIndex) ?: continue
                val newRoundPayload = round.toRoundPayload()
                val tiles = round.findArray("Tile") ?: round.findArray("tiles") ?: JSONArray()
                val events = mutableListOf<PaipuEvent>()
                for (eventIndex in 0 until tiles.length()) {
                    val event = tiles.optJSONObject(eventIndex) ?: continue
                    events += event.toPaipuEvent(events.size)
                }
                if (events.firstOrNull()?.type != PaipuEventType.NewRound) {
                    events.add(
                        0,
                        PaipuEvent(
                            index = 0,
                            type = PaipuEventType.NewRound,
                            actorSeat = null,
                            tile = null,
                            payload = newRoundPayload,
                        )
                    )
                } else {
                    events[0] = events[0].copy(payload = newRoundPayload + events[0].payload)
                }
                add(PaipuRound(roundIndex = roundIndex, events = events.mapIndexed { index, event -> event.copy(index = index) }))
            }
        }
    }

    private fun JSONObject.toPaipuEvent(index: Int): PaipuEvent {
        val rawType = optString("TileType", optString("type", ""))
        return PaipuEvent(
            index = index,
            type = rawType.toPaipuEventType(),
            actorSeat = optIntOrNull("seat") ?: optIntOrNull("actorSeat"),
            tile = optString("tile", optString("Tile", "")).ifBlank { null },
            payload = toPayload(),
        )
    }

    private fun String.toPaipuEventType(): PaipuEventType {
        return when (lowercase()) {
            "newround", "new_round" -> PaipuEventType.NewRound
            "draw", "dealtile", "deal_tile" -> PaipuEventType.DealTile
            "discard", "discardtile", "discard_tile" -> PaipuEventType.DiscardTile
            "call", "chi", "peng", "gang", "chipenggang" -> PaipuEventType.ChiPengGang
            "riichi" -> PaipuEventType.Riichi
            "hule" -> PaipuEventType.Hule
            "liuju" -> PaipuEventType.Liuju
            else -> PaipuEventType.Unknown
        }
    }

    private fun JSONObject.toPayload(): Map<String, String> {
        val result = mutableMapOf<String, String>()
        val keys = keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val value = opt(key)
            result[key] = when (value) {
                is JSONArray, is JSONObject -> value.toString()
                JSONObject.NULL, null -> ""
                else -> value.toString()
            }
        }
        return result
    }

    private fun JSONObject.toRoundPayload(): Map<String, String> {
        val wantedKeys = setOf(
            "chang",
            "ju",
            "ben",
            "honba",
            "scores",
            "finalScores",
            "doras",
            "dora",
            "tiles0",
            "tiles1",
            "tiles2",
            "tiles3",
        )
        return buildMap {
            wantedKeys.forEach { key ->
                if (has(key) && !isNull(key)) put(key, opt(key).toPayloadValue())
            }
        }
    }

    private fun JSONObject.findArray(name: String): JSONArray? {
        return optJSONArray(name) ?: optJSONArray(name.replaceFirstChar { it.lowercase() })
    }

    private fun JSONObject.findIntArray(name: String): List<Int>? {
        val array = findArray(name) ?: return null
        return buildList {
            for (index in 0 until array.length()) {
                add(array.optInt(index))
            }
        }
    }

    private fun JSONObject.resultScores(): List<Int>? {
        val players = optJSONObject("result")?.optJSONArray("players") ?: return null
        val scoresBySeat = mutableMapOf<Int, Int>()
        for (index in 0 until players.length()) {
            val player = players.optJSONObject(index) ?: continue
            val seat = player.optIntOrNull("seat") ?: index
            val score = player.optIntOrNull("partPoint1")
                ?: player.optIntOrNull("score")
                ?: continue
            scoresBySeat[seat] = score
        }
        if (scoresBySeat.isEmpty()) return null
        val maxSeat = scoresBySeat.keys.maxOrNull() ?: return null
        return (0..maxSeat).map { scoresBySeat[it] ?: 0 }
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

    private fun Any?.toPayloadValue(): String {
        return when (this) {
            is JSONArray, is JSONObject -> toString()
            JSONObject.NULL, null -> ""
            else -> toString()
        }
    }
}
