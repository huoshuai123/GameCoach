package com.example.mahjongcoach.ui

import com.example.mahjongcoach.data.ParsedPaipuLink
import com.example.mahjongcoach.data.SampleRound
import com.example.mahjongcoach.domain.DecisionPoint
import com.example.mahjongcoach.domain.EvaluationReport

sealed interface ReviewUiState {
    object Loading : ReviewUiState
    data class LinkEntry(
        val samples: List<SampleRound>,
        val input: String = "",
        val parsedLink: ParsedPaipuLink? = null,
    ) : ReviewUiState

    data class Ready(
        val samples: List<SampleRound>,
        val selectedSample: SampleRound,
        val report: EvaluationReport,
        val selectedDecision: DecisionPoint? = report.decisionPoints.firstOrNull(),
    ) : ReviewUiState
    data class Error(val message: String) : ReviewUiState
}
