package com.example.mahjongcoach.mjai

import com.example.mahjongcoach.data.FinalPaipu
import com.example.mahjongcoach.domain.EvaluationReport
import org.json.JSONArray
import org.json.JSONObject

class MjaiReviewProvider(
    private val client: MjaiHttpClient = DefaultMjaiHttpClient(),
    private val trialSessionProvider: MjaiTrialSessionProvider = MjaiTrialSessionProvider(client),
    private val converter: FinalPaipuToMjaiConverter = FinalPaipuToMjaiConverter(),
    private val logger: MjaiLogger = StdoutMjaiLogger,
) {
    suspend fun assess(paipu: FinalPaipu, report: EvaluationReport): List<MjaiAssessment> {
        val contexts = converter.contexts(paipu, report.decisionPoints)
        if (contexts.isEmpty()) return emptyList()
        logger.info("Assessing ${contexts.size} decision context(s) with MJAI.")

        val token = trialSessionProvider.fetchTrialSession().getOrElse {
            logger.warn("Unable to obtain MJAI trial session: ${it.message.orEmpty().truncatedForLog()}")
            return contexts.map { it.unavailable(MjaiAssessmentStatus.TrialUnavailable) }
        }
        val model = resolveModel(token)

        val firstAttempt = contexts.map { context ->
            runCatching {
                assessContext(context, token, model)
            }.getOrElse {
                context.unavailable(MjaiAssessmentStatus.NetworkError)
            }
        }
        val retryContexts = contexts.zip(firstAttempt)
            .filter { (_, assessment) -> assessment.status == MjaiAssessmentStatus.TrialUnavailable }
            .map { (context, _) -> context }
        if (retryContexts.isEmpty()) return firstAttempt

        logger.warn("Retrying ${retryContexts.size} MJAI decision context(s) after session invalidation.")
        val retryToken = trialSessionProvider.fetchTrialSession().getOrElse {
            logger.warn("Unable to obtain MJAI trial session for retry: ${it.message.orEmpty().truncatedForLog()}")
            return firstAttempt
        }
        val retryModel = resolveModel(retryToken)
        val retriedById = retryContexts.associate { context ->
            val assessment = runCatching {
                assessContext(context, retryToken, retryModel)
            }.getOrElse {
                context.unavailable(MjaiAssessmentStatus.NetworkError)
            }
            context.decisionId to assessment
        }

        return firstAttempt.map { assessment ->
            retriedById[assessment.decisionId] ?: assessment
        }
    }

    private fun resolveModel(token: String): String {
        val response = runCatching { client.get("/mjai/list", token) }.getOrElse {
            logger.warn("MJAI /mjai/list failed before request completed: ${it.message.orEmpty().truncatedForLog()}")
            return MjaiConstants.DefaultModel
        }
        if (response.code !in 200..299) {
            logger.warn("MJAI /mjai/list failed: HTTP ${response.code}, body=${response.body.truncatedForLog()}")
            return MjaiConstants.DefaultModel
        }

        val permit = runCatching {
            val array = JSONObject(response.body).optJSONArray("permit") ?: JSONArray()
            (0 until array.length()).mapNotNull { index -> array.optString(index).takeIf { it.isNotBlank() } }
        }.getOrDefault(emptyList())
        val selected = when {
            permit.contains(MjaiConstants.DefaultModel) -> MjaiConstants.DefaultModel
            else -> permit.firstOrNull { !it.startsWith("3p", ignoreCase = true) }
        }
        if (selected == null) {
            logger.warn("MJAI /mjai/list did not include an accessible 4-player model: body=${response.body.truncatedForLog()}")
            return MjaiConstants.DefaultModel
        }
        if (selected != MjaiConstants.DefaultModel) {
            logger.warn("Default MJAI model '${MjaiConstants.DefaultModel}' unavailable; using '$selected'.")
        }
        return selected
    }

    private fun assessContext(context: MjaiDecisionContext, token: String, model: String): MjaiAssessment {
        val start = client.post(
            path = "/mjai/start",
            bearerToken = token,
            body = JSONObject()
                .put("id", context.viewSeat)
                .put("model", model)
                .put("bound", 0)
                .toString(),
        )
        if (start.code == 429) return context.unavailable(MjaiAssessmentStatus.QuotaExceeded)
        if (start.code == 401) {
            logger.warn("MJAI /mjai/start returned 401; cached session will be cleared. body=${start.body.truncatedForLog()}")
            trialSessionProvider.clearCachedSession()
            return context.unavailable(MjaiAssessmentStatus.TrialUnavailable)
        }
        if (start.code !in 200..299) {
            logger.warn("MJAI /mjai/start failed: HTTP ${start.code}, body=${start.body.truncatedForLog()}")
            return context.unavailable(MjaiAssessmentStatus.ProtocolError)
        }

        var latestResponse: JSONObject? = null
        batches(context.events).forEach { batch ->
            val response = client.post(
                path = "/mjai/batch",
                bearerToken = token,
                body = batch.toString(),
            )
            if (response.code == 429) return context.unavailable(MjaiAssessmentStatus.QuotaExceeded)
            if (response.code == 401) {
                logger.warn("MJAI /mjai/batch returned 401; cached session will be cleared. body=${response.body.truncatedForLog()}")
                trialSessionProvider.clearCachedSession()
                return context.unavailable(MjaiAssessmentStatus.TrialUnavailable)
            }
            if (response.code !in 200..299) {
                logger.warn("MJAI /mjai/batch failed: HTTP ${response.code}, body=${response.body.truncatedForLog()}")
                return context.unavailable(MjaiAssessmentStatus.ProtocolError)
            }
            latestResponse = parseActionResponse(response.body) ?: latestResponse
        }

        client.post(
            path = "/mjai/stop",
            bearerToken = token,
            body = "{}",
        )

        val action = latestResponse ?: run {
            logger.warn("MJAI did not return a dahai action for decision ${context.decisionId}.")
            return context.unavailable(MjaiAssessmentStatus.ProtocolError)
        }
        val recommended = action.optString("pai").ifBlank { null }?.fromMjaiTile()
        val weights = action.optJSONObject("meta")?.optJSONObject("q_values")
        return MjaiAssessment(
            decisionId = context.decisionId,
            recommendedDiscard = recommended,
            recommendedWeight = recommended?.let { weights?.optDouble(it.toMjaiWeightKey(), Double.NaN) }?.takeIf { !it.isNaN() },
            chosenWeight = weights?.optDouble(context.chosenDiscard.toMjaiWeightKey(), Double.NaN)?.takeIf { !it.isNaN() },
            model = model,
            status = if (recommended != null) MjaiAssessmentStatus.Success else MjaiAssessmentStatus.ProtocolError,
        )
    }

    private fun batches(events: JSONArray): List<JSONArray> {
        val result = mutableListOf<JSONArray>()
        var current = JSONArray()
        for (index in 0 until events.length()) {
            val event = JSONObject()
                .put("seq", index)
                .put("data", events.getJSONObject(index))
            val next = JSONArray(current.toString())
            next.put(event)
            if (next.toString().toByteArray(Charsets.UTF_8).size > MjaiConstants.MaxBatchBodyBytes && current.length() > 0) {
                result += current
                current = JSONArray().put(event)
            } else {
                current = next
            }
        }
        if (current.length() > 0) result += current
        return result
    }

    private fun parseActionResponse(body: String): JSONObject? {
        val json = JSONObject(body)
        json.optJSONObject("act")?.let { return it }
        val responses = json.optJSONArray("responses")
        if (responses != null) {
            for (index in responses.length() - 1 downTo 0) {
                val response = responses.optJSONObject(index) ?: continue
                if (response.optString("type") == "dahai") return response
            }
        }
        return if (json.optString("type") == "dahai") json else null
    }

    private fun MjaiDecisionContext.unavailable(status: MjaiAssessmentStatus): MjaiAssessment {
        return MjaiAssessment(
            decisionId = decisionId,
            recommendedDiscard = null,
            recommendedWeight = null,
            chosenWeight = null,
            model = MjaiConstants.DefaultModel,
            status = status,
        )
    }

    private fun String.fromMjaiTile(): String {
        return when (this) {
            "E" -> "1z"
            "S" -> "2z"
            "W" -> "3z"
            "N" -> "4z"
            "P" -> "5z"
            "F" -> "6z"
            "C" -> "7z"
            else -> replace("r", "")
        }
    }

    private fun String.toMjaiWeightKey(): String {
        return replace("0m", "5mr")
            .replace("0p", "5pr")
            .replace("0s", "5sr")
    }
}
