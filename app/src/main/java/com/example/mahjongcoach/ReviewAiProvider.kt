package com.example.mahjongcoach

import com.example.mahjongcoach.data.FinalPaipu
import com.example.mahjongcoach.domain.EvaluationReport
import com.example.mahjongcoach.mjai.MjaiAssessment

interface ReviewAiProvider {
    suspend fun assess(paipu: FinalPaipu, report: EvaluationReport): List<MjaiAssessment>
}

object ReviewDependencyOverrides {
    @Volatile
    var aiReviewProvider: ReviewAiProvider? = null

    fun reset() {
        aiReviewProvider = null
    }
}
