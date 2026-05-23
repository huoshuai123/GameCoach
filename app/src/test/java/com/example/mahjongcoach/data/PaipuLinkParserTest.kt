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
        assertEquals("abc123", parsed.amaeRecordId)
        assertEquals("938523791", parsed.encodedAccountId)
        assertTrue(parsed.canStartReview)
    }

    @Test
    fun parseUnknownLink_reportsUnsupported() {
        val parsed = PaipuLinkParser.parse("https://example.com/not-a-paipu")

        assertEquals(LinkParseStatus.Unsupported, parsed.status)
    }
}
