package com.example.mahjongcoach.mjai

import org.json.JSONArray

object MjaiConstants {
    const val BaseUrl = "https://mjai.7xcnnw11phu.eu.org"
    const val DefaultModel = "mini"
    const val MaxDecisionPoints = 5
    const val TimeoutMillis = 10000
    const val MaxBatchBodyBytes = 4000
}

data class MjaiHttpResponse(
    val code: Int,
    val body: String,
)

data class MjaiDecisionContext(
    val decisionId: String,
    val roundIndex: Int,
    val viewSeat: Int,
    val chosenDiscard: String,
    val events: JSONArray,
)

data class MjaiAssessment(
    val decisionId: String,
    val recommendedDiscard: String?,
    val recommendedWeight: Double?,
    val chosenWeight: Double?,
    val model: String,
    val status: MjaiAssessmentStatus,
)

enum class MjaiAssessmentStatus(val schemaValue: String) {
    Success("success"),
    TrialUnavailable("trial_unavailable"),
    QuotaExceeded("quota_exceeded"),
    ProtocolError("protocol_error"),
    NetworkError("network_error"),
}
