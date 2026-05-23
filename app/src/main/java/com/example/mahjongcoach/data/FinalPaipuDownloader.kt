package com.example.mahjongcoach.data

class FinalPaipuDownloader {
    fun prepareDownload(detail: PaipuDetail): FinalPaipuDownload {
        val uuid = detail.uuid
        val officialUrl = detail.officialUrl
        if (uuid.isNullOrBlank() || officialUrl.isNullOrBlank()) {
            return FinalPaipuDownload(
                status = FinalPaipuDownloadStatus.Failed,
                request = null,
                paipu = null,
                message = "缺少官方牌谱链接或 UUID，无法准备最终牌谱下载。",
            )
        }

        val request = OfficialPaipuRequest(
            uuid = uuid,
            officialUrl = officialUrl,
            encodedAccountId = detail.encodedAccountId,
        )

        return FinalPaipuDownload(
            status = FinalPaipuDownloadStatus.RequiresOfficialProtocol,
            request = request,
            paipu = null,
            message = "已准备官方牌谱下载请求。完整牌谱事件流需要通过雀魂官方 WebSocket、OAuth 登录和 protobuf 解码获取，不能用普通 HTTP 直接下载。",
        )
    }
}
