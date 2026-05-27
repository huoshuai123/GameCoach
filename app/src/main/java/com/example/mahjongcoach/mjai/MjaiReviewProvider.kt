package com.example.mahjongcoach.mjai

import com.example.mahjongcoach.data.FinalPaipu
import com.example.mahjongcoach.domain.EvaluationReport
import org.json.JSONArray
import org.json.JSONObject

class MjaiReviewProvider(
    private val client: MjaiHttpClient = DefaultMjaiHttpClient(),
    private val trialSessionProvider: MjaiTrialSessionProvider = MjaiTrialSessionProvider(client),
    private val converter: FinalPaipuToMjaiConverter = FinalPaipuToMjaiConverter(),
) {
    suspend fun assess(paipu: FinalPaipu, report: EvaluationReport): List<MjaiAssessment> {
        val contexts = converter.contexts(paipu, report.decisionPoints)
        if (contexts.isEmpty()) return emptyList()

        val token = trialSessionProvider.fetchTrialSession().getOrElse {
            return contexts.map { it.unavailable(MjaiAssessmentStatus.TrialUnavailable) }
        }

        return contexts.map { context ->
            runCatching {
                assessContext(context, token)
            }.getOrElse {
                context.unavailable(MjaiAssessmentStatus.NetworkError)
            }
        }
    }

    private fun assessContext(context: MjaiDecisionContext, token: String): MjaiAssessment {
        val start = client.post(
            path = "/mjai/start",
            bearerToken = token,
            body = JSONObject()
                .put("id", context.viewSeat)
                .put("model", MjaiConstants.DefaultModel)
                .put("bound", 0)
                .toString(),
        )
        if (start.code == 429) return context.unavailable(MjaiAssessmentStatus.QuotaExceeded)
        if (start.code !in 200..299) return context.unavailable(MjaiAssessmentStatus.ProtocolError)

        var latestResponse: JSONObject? = null
        batches(context.events).forEach { batch ->
            val response = client.post(
                path = "/mjai/batch",
                bearerToken = token,
                body = batch.toString(),
            )
            if (response.code == 429) return context.unavailable(MjaiAssessmentStatus.QuotaExceeded)
            if (response.code !in 200..299) return context.unavailable(MjaiAssessmentStatus.ProtocolError)
            latestResponse = parseActionResponse(response.body) ?: latestResponse
        }

        client.post(
            path = "/mjai/stop",
            bearerToken = token,
            body = "{}",
        )

        val action = latestResponse ?: return context.unavailable(MjaiAssessmentStatus.ProtocolError)
        val recommended = action.optString("pai").ifBlank { null }?.fromMjaiTile()
        val weights = action.optJSONObject("meta")?.optJSONObject("q_values")
        return MjaiAssessment(
            decisionId = context.decisionId,
            recommendedDiscard = recommended,
            recommendedWeight = recommended?.let { weights?.optDouble(it.toMjaiWeightKey(), Double.NaN) }?.takeIf { !it.isNaN() },
            chosenWeight = weights?.optDouble(context.chosenDiscard.toMjaiWeightKey(), Double.NaN)?.takeIf { !it.isNaN() },
            model = MjaiConstants.DefaultModel,
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
