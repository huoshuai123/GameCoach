package com.example.mahjongcoach.ui

import com.example.mahjongcoach.data.FinalPaipuDownload
import com.example.mahjongcoach.data.FinalPaipuDownloadStatus
import com.example.mahjongcoach.data.LinkParseStatus
import com.example.mahjongcoach.data.LinkSource
import com.example.mahjongcoach.data.PaipuDetail
import com.example.mahjongcoach.data.PaipuFetchStatus
import com.example.mahjongcoach.data.PaipuImportResult
import com.example.mahjongcoach.data.ParsedPaipuLink
import com.example.mahjongcoach.domain.EvaluationReport
import com.example.mahjongcoach.domain.Situation
import com.example.mahjongcoach.domain.TrainingFocus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ImportedReviewStateFactoryTest {
    @Test
    fun readyOrNull_returnsReadyStateForImportedReport() {
        val report = EvaluationReport(
            situation = Situation(game = "Mahjong Soul", title = "导入复盘"),
            summary = "导入牌谱分析完成",
            metrics = emptyList(),
            decisionPoints = emptyList(),
            trainingFocus = TrainingFocus("保持决策质量", "继续复盘", "导入牌谱"),
        )
        val result = PaipuImportResult(
            parsedLink = ParsedPaipuLink(
                rawInput = "https://game.maj-soul.com/1/?paipu=game_a1",
                source = LinkSource.MahjongSoul,
                uuid = "game",
                encodedAccountId = "1",
                viewAccountId = 2,
                amaeRecordId = null,
                modeId = null,
                zone = null,
                status = LinkParseStatus.Recognized,
                message = "ready",
            ),
            detail = PaipuDetail(
                source = LinkSource.MahjongSoul,
                uuid = "game",
                encodedAccountId = "1",
                viewAccountId = 2,
                amaeRecordId = null,
                modeId = null,
                officialUrl = "https://game.maj-soul.com/1/?paipu=game_a1",
                fetchStatus = PaipuFetchStatus.Ready,
                message = "ready",
            ),
            finalPaipuDownload = FinalPaipuDownload(
                status = FinalPaipuDownloadStatus.Fetched,
                request = null,
                paipu = null,
                message = "fetched",
            ),
            report = report,
        )

        val state = ImportedReviewStateFactory.readyOrNull(result)

        assertTrue(state is ReviewUiState.Ready)
        val ready = state as ReviewUiState.Ready
        assertEquals("imported-game", ready.selectedSample.id)
        assertEquals("导入复盘", ready.selectedSample.title)
        assertEquals(listOf(ready.selectedSample), ready.samples)
        assertEquals(report, ready.report)
    }
}
