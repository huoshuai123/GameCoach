package com.example.mahjongcoach.ui

import com.example.mahjongcoach.data.PaipuImportResult
import com.example.mahjongcoach.data.SampleRound

object ImportedReviewStateFactory {
    fun readyOrNull(samples: List<SampleRound>, result: PaipuImportResult): ReviewUiState.Ready? {
        val report = result.report ?: return null
        val uuid = result.detail.uuid ?: result.parsedLink.uuid ?: "unknown"
        val importedSample = SampleRound(
            id = "imported-$uuid",
            title = report.situation.title,
            description = "从公开牌谱链接导入并生成的复盘报告。",
            focus = "导入牌谱分析",
            assetName = "",
        )
        return ReviewUiState.Ready(
            samples = listOf(importedSample) + samples,
            selectedSample = importedSample,
            report = report,
        )
    }
}
