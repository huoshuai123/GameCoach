package com.example.mahjongcoach.ui

import com.example.mahjongcoach.domain.DecisionPoint
import com.example.mahjongcoach.domain.EvaluationReport

sealed interface ReviewUiState {
    object Loading : ReviewUiState
    data class Ready(
        val report: EvaluationReport,
        val selectedDecision: DecisionPoint? = report.decisionPoints.firstOrNull(),
    ) : ReviewUiState
    data class Error(val message: String) : ReviewUiState
}
