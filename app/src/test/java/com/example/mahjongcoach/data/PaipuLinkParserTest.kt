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
    fun parseMahjongSoulLink_decodesViewAccountId() {
        val parsed = PaipuLinkParser.parse(
            "https://game.maj-soul.com/1/?paipu=260522-2793b6c3-992e-424b-9205-11a121e9ac00_a64445483"
        )

        assertEquals("64445483", parsed.encodedAccountId)
        assertEquals(16329130L, parsed.viewAccountId)
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
    fun finalDownloader_preparesPublicRecordRequest() {
        val detail = PaipuDetail(
            source = LinkSource.MahjongSoul,
            uuid = "250130-56d469f5-c213-4b1c-8fe1-4f7b006ab82e",
            encodedAccountId = "938523791",
            viewAccountId = PaipuLinkParser.decodeAccountId("938523791"),
            amaeRecordId = null,
            modeId = null,
            officialUrl = "https://mahjongsoul.game.yo-star.com/?paipu=250130-56d469f5-c213-4b1c-8fe1-4f7b006ab82e_a938523791",
            fetchStatus = PaipuFetchStatus.Ready,
            message = "ready",
        )

        val download = FinalPaipuDownloader().prepareDownload(detail)

        assertEquals(FinalPaipuDownloadStatus.ReadyToFetchPublicRecord, download.status)
        assertEquals(detail.uuid, download.request?.uuid)
        assertEquals(
            "https://maj.gg/game/250130-56d469f5-c213-4b1c-8fe1-4f7b006ab82e",
            download.request?.majGgUrl,
        )
        assertEquals(detail.encodedAccountId, download.request?.encodedAccountId)
    }

    @Test
    fun majGgFetcher_extractsFreshStateAndMapsFinalPaipu() {
        val html = """
            <html>
              <body>
                <script id="__FRSH_STATE" type="application/json">
                  {
                    "data": {
                      "game": {
                        "config": {"meta": {"modeId": 12}},
                        "accounts": [
                          {"accountId": 100, "nickname": "上家", "seat": 0},
                          {"accountId": 200, "nickname": "下家", "seat": 1},
                          {"accountId": 16329130, "nickname": "南风快乐岛", "seat": 2},
                          {"accountId": 300, "nickname": "对家", "seat": 3}
                        ],
                        "finalScores": [56500, 14000, 12900, 16600],
                        "Rounds": [
                          {
                            "chang": 0,
                            "ju": 2,
                            "ben": 1,
                            "scores": [25000, 25000, 25000, 25000],
                            "Tile": [
                              {"TileType": "Draw", "seat": 2, "tile": "9s"},
                              {"TileType": "Discard", "seat": 2, "tile": "9s", "moqie": true},
                              {"TileType": "Call", "seat": 0, "tiles": ["2m", "3m", "4m"]}
                            ]
                          }
                        ]
                      }
                    }
                  }
                </script>
              </body>
            </html>
        """.trimIndent()

        val request = PublicPaipuFetchRequest(
            uuid = "260522-2793b6c3-992e-424b-9205-11a121e9ac00",
            officialUrl = "https://game.maj-soul.com/1/?paipu=260522-2793b6c3-992e-424b-9205-11a121e9ac00_a64445483",
            majGgUrl = "https://maj.gg/game/260522-2793b6c3-992e-424b-9205-11a121e9ac00",
            encodedAccountId = "64445483",
            viewAccountId = 16329130L,
        )

        val paipu = MajGgPaipuFetcher().parseHtml(request, html)

        assertEquals("260522-2793b6c3-992e-424b-9205-11a121e9ac00", paipu.uuid)
        assertEquals("12", paipu.head.modeId)
        assertEquals(2, paipu.head.viewSeat)
        assertEquals("南风快乐岛", paipu.head.viewPlayer?.nickname)
        assertEquals(12900, paipu.head.viewPlayer?.score)
        assertEquals(1, paipu.rounds.size)
        assertEquals(PaipuEventType.NewRound, paipu.rounds[0].events[0].type)
        assertEquals("0", paipu.rounds[0].events[0].payload["chang"])
        assertEquals("2", paipu.rounds[0].events[0].payload["ju"])
        assertEquals("1", paipu.rounds[0].events[0].payload["ben"])
        assertEquals(PaipuEventType.DealTile, paipu.rounds[0].events[1].type)
        assertEquals(PaipuEventType.DiscardTile, paipu.rounds[0].events[2].type)
        assertEquals("true", paipu.rounds[0].events[2].payload["moqie"])
    }

    @Test(expected = IllegalStateException::class)
    fun majGgFetcher_failsWhenViewAccountIsMissing() {
        val html = """
            <script id="__FRSH_STATE" type="application/json">
              {"data":{"game":{"accounts":[{"accountId":100,"nickname":"上家","seat":0}],"Rounds":[{"Tile":[]}]}}}
            </script>
        """.trimIndent()

        MajGgPaipuFetcher().parseHtml(
            PublicPaipuFetchRequest(
                uuid = "260522-2793b6c3-992e-424b-9205-11a121e9ac00",
                officialUrl = "https://game.maj-soul.com/1/?paipu=260522-2793b6c3-992e-424b-9205-11a121e9ac00_a64445483",
                majGgUrl = "https://maj.gg/game/260522-2793b6c3-992e-424b-9205-11a121e9ac00",
                encodedAccountId = "64445483",
                viewAccountId = 16329130L,
            ),
            html,
        )
    }

    @Test
    fun majGgFetcher_extractsAssignedFreshStateWithoutRegexBraceParsing() {
        val html = """
            <script>
              window.__FRSH_STATE = {"data":{"game":{"accounts":[{"accountId":16329130,"nickname":"南风快乐岛","seat":2}],"Rounds":[{"Tile":[{"TileType":"Draw","seat":2,"tile":"1m"}]}]}}};
            </script>
        """.trimIndent()

        val paipu = MajGgPaipuFetcher().parseHtml(
            PublicPaipuFetchRequest(
                uuid = "260522-2793b6c3-992e-424b-9205-11a121e9ac00",
                officialUrl = "https://game.maj-soul.com/1/?paipu=260522-2793b6c3-992e-424b-9205-11a121e9ac00_a64445483",
                majGgUrl = "https://maj.gg/game/260522-2793b6c3-992e-424b-9205-11a121e9ac00",
                encodedAccountId = "64445483",
                viewAccountId = 16329130L,
            ),
            html,
        )

        assertEquals(2, paipu.head.viewSeat)
        assertEquals(PaipuEventType.DealTile, paipu.rounds[0].events[1].type)
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
