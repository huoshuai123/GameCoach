package com.example.mahjongcoach.ui

import com.example.mahjongcoach.domain.DecisionPoint
import com.example.mahjongcoach.domain.EvaluationReport
import com.example.mahjongcoach.data.SampleRound

sealed interface ReviewUiState {
    object Loading : ReviewUiState
    data class SampleList(
        val samples: List<SampleRound>,
    ) : ReviewUiState

    data class Ready(
        val samples: List<SampleRound>,
        val selectedSample: SampleRound,
        val report: EvaluationReport,
        val selectedDecision: DecisionPoint? = report.decisionPoints.firstOrNull(),
    ) : ReviewUiState
    data class Error(val message: String) : ReviewUiState
}
