package com.example.mahjongcoach.data

import java.net.URLDecoder

data class ParsedPaipuLink(
    val rawInput: String,
    val source: LinkSource,
    val uuid: String?,
    val encodedAccountId: String?,
    val viewAccountId: Long?,
    val amaeRecordId: String?,
    val modeId: String?,
    val zone: String?,
    val status: LinkParseStatus,
    val message: String,
) {
    val canStartReview: Boolean = status == LinkParseStatus.Recognized
}

enum class LinkSource(val label: String) {
    MahjongSoul("雀魂牌谱链接"),
    AmaeKoromo("雀魂牌谱屋链接"),
    Unknown("未知来源"),
}

enum class LinkParseStatus {
    Recognized,
    Unsupported,
}

object PaipuLinkParser {
    private val uuidRegex = Regex("""\d{6}-[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}""")
    private val accountRegex = Regex("""_a(\d+)""")
    private val amaeViewRegex = Regex("""/view_game/([^/]+)/([^/]+)/([^/?#]+)(?:/(\d+))?""")

    fun parse(input: String): ParsedPaipuLink {
        val trimmed = input.trim()
        if (trimmed.isBlank()) {
            return unsupported(trimmed, "请先粘贴雀魂牌谱链接或雀魂牌谱屋链接。")
        }

        val decoded = URLDecoder.decode(trimmed, "UTF-8")
        val source = when {
            decoded.contains("amae-koromo", ignoreCase = true) ||
                decoded.contains("sapk.ch", ignoreCase = true) ||
                decoded.contains("data.amae-koromo", ignoreCase = true) -> LinkSource.AmaeKoromo
            decoded.contains("mahjongsoul", ignoreCase = true) ||
                decoded.contains("maj-soul", ignoreCase = true) ||
                decoded.contains("paipu=", ignoreCase = true) -> LinkSource.MahjongSoul
            else -> LinkSource.Unknown
        }

        val uuid = uuidRegex.find(decoded)?.value
        val accountId = accountRegex.find(decoded)?.groupValues?.getOrNull(1)
        val amaeMatch = amaeViewRegex.find(decoded)
        val zone = amaeMatch?.groupValues?.getOrNull(1)?.ifBlank { null }
        val modeId = amaeMatch?.groupValues?.getOrNull(2)?.ifBlank { null }
        val amaeRecordId = amaeMatch?.groupValues?.getOrNull(3)?.ifBlank { null }
        val amaeAccountId = amaeMatch?.groupValues?.getOrNull(4)?.ifBlank { null }
        val encodedAccountId = accountId ?: amaeAccountId

        if (uuid != null) {
            return ParsedPaipuLink(
                rawInput = trimmed,
                source = source,
                uuid = uuid,
                encodedAccountId = encodedAccountId,
                viewAccountId = decodeAccountId(encodedAccountId),
                amaeRecordId = amaeRecordId,
                modeId = modeId,
                zone = zone,
                status = LinkParseStatus.Recognized,
                message = "已识别公开牌谱 UUID。可不登录提取用户提交的公开牌谱。",
            )
        }

        if (source == LinkSource.AmaeKoromo && amaeRecordId != null) {
            return ParsedPaipuLink(
                rawInput = trimmed,
                source = source,
                uuid = null,
                encodedAccountId = encodedAccountId,
                viewAccountId = decodeAccountId(encodedAccountId),
                amaeRecordId = amaeRecordId,
                modeId = modeId,
                zone = zone,
                status = LinkParseStatus.Recognized,
                message = "已识别牌谱屋记录。牌谱屋可辅助定位记录，但当前版本不直接请求第三方完整牌谱数据。",
            )
        }

        return ParsedPaipuLink(
            rawInput = trimmed,
            source = source,
            uuid = null,
            encodedAccountId = null,
            viewAccountId = null,
            amaeRecordId = null,
            modeId = null,
            zone = null,
            status = LinkParseStatus.Unsupported,
            message = "未找到可识别的牌谱 UUID 或牌谱屋记录 ID。",
        )
    }

    private fun unsupported(input: String, message: String): ParsedPaipuLink {
        return ParsedPaipuLink(
            rawInput = input,
            source = LinkSource.Unknown,
            uuid = null,
            encodedAccountId = null,
            viewAccountId = null,
            amaeRecordId = null,
            modeId = null,
            zone = null,
            status = LinkParseStatus.Unsupported,
            message = message,
        )
    }

    fun decodeAccountId(encodedAccountId: String?): Long? {
        val encoded = encodedAccountId?.toLongOrNull() ?: return null
        return (((encoded - 1358437L) xor 86216345L) - 1117113L) / 7L
    }
}
