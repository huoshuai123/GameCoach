package com.example.mahjongcoach.data

import java.net.HttpURLConnection
import java.net.URL

data class PaipuDetail(
    val source: LinkSource,
    val uuid: String?,
    val encodedAccountId: String?,
    val amaeRecordId: String?,
    val modeId: String?,
    val officialUrl: String?,
    val fetchStatus: PaipuFetchStatus,
    val message: String,
)

enum class PaipuFetchStatus {
    Ready,
    Resolved,
    Failed,
}

class PaipuDetailDownloader(
    private val mirrorBaseUrl: String = "https://5-data.amae-koromo.com/",
) {
    fun download(parsedLink: ParsedPaipuLink): PaipuDetail {
        if (!parsedLink.canStartReview) {
            return failed(parsedLink, "链接尚未识别，无法下载详情。")
        }

        parsedLink.uuid?.let {
            return PaipuDetail(
                source = parsedLink.source,
                uuid = it,
                encodedAccountId = parsedLink.encodedAccountId,
                amaeRecordId = parsedLink.amaeRecordId,
                modeId = parsedLink.modeId,
                officialUrl = buildOfficialUrl(it, parsedLink.encodedAccountId),
                fetchStatus = PaipuFetchStatus.Ready,
                message = "已生成标准牌谱详情。完整摸切事件流仍需后续接入解析服务。",
            )
        }

        val recordId = parsedLink.amaeRecordId ?: return failed(parsedLink, "缺少牌谱屋记录 ID。")
        val modeId = parsedLink.modeId ?: return failed(parsedLink, "缺少牌谱屋模式 ID。")
        val zone = parsedLink.zone ?: "3"
        val url = buildAmaeViewUrl(zone, modeId, recordId, parsedLink.encodedAccountId)

        return try {
            val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "HEAD"
                instanceFollowRedirects = false
                connectTimeout = 8000
                readTimeout = 8000
            }
            val code = connection.responseCode
            val location = connection.getHeaderField("Location")
            connection.disconnect()

            if (code in 300..399 && !location.isNullOrBlank()) {
                val resolved = PaipuLinkParser.parse(location)
                PaipuDetail(
                    source = parsedLink.source,
                    uuid = resolved.uuid,
                    encodedAccountId = resolved.encodedAccountId ?: parsedLink.encodedAccountId,
                    amaeRecordId = recordId,
                    modeId = modeId,
                    officialUrl = location,
                    fetchStatus = PaipuFetchStatus.Resolved,
                    message = "已通过牌谱屋解析到官方牌谱链接。下一步需要接入完整牌谱事件流解析。",
                )
            } else {
                failed(parsedLink, "牌谱屋返回 $code，未获得官方牌谱跳转。")
            }
        } catch (error: Exception) {
            failed(parsedLink, error.message ?: "牌谱详情下载失败。")
        }
    }

    fun buildAmaeViewUrl(zone: String, modeId: String, recordId: String, encodedAccountId: String?): String {
        val accountSuffix = encodedAccountId?.let { "/$it" } ?: ""
        return "${mirrorBaseUrl.trimEnd('/')}/api/v2/pl4/view_game/$zone/$modeId/$recordId$accountSuffix"
    }

    private fun buildOfficialUrl(uuid: String, encodedAccountId: String?): String {
        val accountSuffix = encodedAccountId?.let { "_a$it" } ?: ""
        return "https://mahjongsoul.game.yo-star.com/?paipu=$uuid$accountSuffix"
    }

    private fun failed(parsedLink: ParsedPaipuLink, message: String): PaipuDetail {
        return PaipuDetail(
            source = parsedLink.source,
            uuid = parsedLink.uuid,
            encodedAccountId = parsedLink.encodedAccountId,
            amaeRecordId = parsedLink.amaeRecordId,
            modeId = parsedLink.modeId,
            officialUrl = null,
            fetchStatus = PaipuFetchStatus.Failed,
            message = message,
        )
    }
}
