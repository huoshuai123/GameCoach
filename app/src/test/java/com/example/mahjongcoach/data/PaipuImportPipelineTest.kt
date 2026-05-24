package com.example.mahjongcoach.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class PaipuImportPipelineTest {
    @Test
    fun importPublicLink_runsParseDetailAndPublicFetchInOneStep() {
        val pipeline = PaipuImportPipeline(
            detailDownloader = PaipuDetailDownloader(),
            finalPaipuDownloader = FinalPaipuDownloader(
                fetcher = object : PublicPaipuFetcher {
                    override fun fetch(request: PublicPaipuFetchRequest): FinalPaipu {
                        return FinalPaipu(
                            uuid = request.uuid,
                            officialUrl = request.officialUrl,
                            head = PaipuHead(
                                modeId = null,
                                startTime = null,
                                endTime = null,
                                players = listOf(PaipuPlayer(request.viewAccountId, "南风快乐岛", 2, 26000)),
                                viewSeat = 2,
                                viewPlayer = PaipuPlayer(request.viewAccountId, "南风快乐岛", 2, 26000),
                            ),
                            rounds = listOf(PaipuRound(0, listOf(PaipuEvent(0, PaipuEventType.NewRound, null, null)))),
                        )
                    }
                }
            ),
        )

        val result = pipeline.import(
            "https://game.maj-soul.com/1/?paipu=260522-2793b6c3-992e-424b-9205-11a121e9ac00_a64445483"
        )

        assertEquals(LinkParseStatus.Recognized, result.parsedLink.status)
        assertEquals(PaipuFetchStatus.Ready, result.detail.fetchStatus)
        assertEquals(FinalPaipuDownloadStatus.Fetched, result.finalPaipuDownload.status)
        assertEquals(16329130L, result.finalPaipuDownload.request?.viewAccountId)
        assertNotNull(result.finalPaipuDownload.paipu)
    }
}
