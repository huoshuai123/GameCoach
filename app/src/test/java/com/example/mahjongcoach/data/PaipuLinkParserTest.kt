package com.example.mahjongcoach.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PaipuLinkParserTest {
    @Test
    fun parseMahjongSoulLink_extractsUuidAndPerspective() {
        val parsed = PaipuLinkParser.parse(
            "https://mahjongsoul.game.yo-star.com/?paipu=250130-56d469f5-c213-4b1c-8fe1-4f7b006ab82e_a938523791"
        )

        assertEquals(LinkSource.MahjongSoul, parsed.source)
        assertEquals("250130-56d469f5-c213-4b1c-8fe1-4f7b006ab82e", parsed.uuid)
        assertEquals("938523791", parsed.encodedAccountId)
        assertTrue(parsed.canStartReview)
    }

    @Test
    fun parseAmaeKoromoMaskedLink_extractsRecordMetadata() {
        val parsed = PaipuLinkParser.parse(
            "https://5-data.amae-koromo.com/api/v2/pl4/view_game/zh/16/abc123/938523791"
        )

        assertEquals(LinkSource.AmaeKoromo, parsed.source)
        assertEquals("16", parsed.modeId)
        assertEquals("zh", parsed.zone)
        assertEquals("abc123", parsed.amaeRecordId)
        assertEquals("938523791", parsed.encodedAccountId)
        assertTrue(parsed.canStartReview)
    }

    @Test
    fun downloader_buildsOfficialDetailForDirectMahjongSoulLink() {
        val parsed = PaipuLinkParser.parse(
            "https://mahjongsoul.game.yo-star.com/?paipu=250130-56d469f5-c213-4b1c-8fe1-4f7b006ab82e_a938523791"
        )
        val detail = PaipuDetailDownloader().download(parsed)

        assertEquals(PaipuFetchStatus.Ready, detail.fetchStatus)
        assertEquals("250130-56d469f5-c213-4b1c-8fe1-4f7b006ab82e", detail.uuid)
        assertEquals(
            "https://mahjongsoul.game.yo-star.com/?paipu=250130-56d469f5-c213-4b1c-8fe1-4f7b006ab82e_a938523791",
            detail.officialUrl,
        )
    }

    @Test
    fun downloader_buildsAmaeViewUrl() {
        val url = PaipuDetailDownloader("https://example.test/")
            .buildAmaeViewUrl("3", "16", "abc123", "938523791")

        assertEquals("https://example.test/api/v2/pl4/view_game/3/16/abc123/938523791", url)
    }

    @Test
    fun finalDownloader_preparesOfficialProtocolRequest() {
        val detail = PaipuDetail(
            source = LinkSource.MahjongSoul,
            uuid = "250130-56d469f5-c213-4b1c-8fe1-4f7b006ab82e",
            encodedAccountId = "938523791",
            amaeRecordId = null,
            modeId = null,
            officialUrl = "https://mahjongsoul.game.yo-star.com/?paipu=250130-56d469f5-c213-4b1c-8fe1-4f7b006ab82e_a938523791",
            fetchStatus = PaipuFetchStatus.Ready,
            message = "ready",
        )

        val download = FinalPaipuDownloader().prepareDownload(detail)

        assertEquals(FinalPaipuDownloadStatus.RequiresOfficialProtocol, download.status)
        assertEquals(detail.uuid, download.request?.uuid)
        assertTrue(download.request?.requiresWebSocket == true)
        assertTrue(download.request?.requiresOAuth == true)
        assertTrue(download.request?.requiresProtobuf == true)
    }

    @Test
    fun finalParser_groupsDecodedEventsIntoRounds() {
        val head = PaipuHead(
            modeId = "16",
            startTime = 1,
            endTime = 2,
            players = listOf(PaipuPlayer(1, "玩家A", 0, 25000)),
        )

        val paipu = FinalPaipuParser().fromDecodedEvents(
            uuid = "250130-56d469f5-c213-4b1c-8fe1-4f7b006ab82e",
            officialUrl = "https://mahjongsoul.game.yo-star.com/",
            head = head,
            decodedEvents = listOf(
                DecodedPaipuEvent(PaipuEventType.NewRound, null, null),
                DecodedPaipuEvent(PaipuEventType.DealTile, 0, "5m"),
                DecodedPaipuEvent(PaipuEventType.DiscardTile, 0, "5m"),
                DecodedPaipuEvent(PaipuEventType.NewRound, null, null),
                DecodedPaipuEvent(PaipuEventType.DealTile, 1, "7p"),
            ),
        )

        assertEquals(2, paipu.rounds.size)
        assertEquals(3, paipu.rounds[0].events.size)
        assertEquals(2, paipu.rounds[1].events.size)
    }

    @Test
    fun parseUnknownLink_reportsUnsupported() {
        val parsed = PaipuLinkParser.parse("https://example.com/not-a-paipu")

        assertEquals(LinkParseStatus.Unsupported, parsed.status)
    }
}
