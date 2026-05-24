package com.example.mahjongcoach.data

class FinalPaipuDownloader(
    private val fetcher: PublicPaipuFetcher = MajGgPaipuFetcher(),
) {
    fun prepareDownload(detail: PaipuDetail): FinalPaipuDownload {
        val request = buildPublicRequest(detail)
            ?: return FinalPaipuDownload(
                status = FinalPaipuDownloadStatus.Failed,
                request = null,
                paipu = null,
                message = "缺少官方牌谱链接或 UUID，无法提取公开牌谱。",
            )

        return FinalPaipuDownload(
            status = FinalPaipuDownloadStatus.ReadyToFetchPublicRecord,
            request = request,
            paipu = null,
            message = "已准备公开牌谱提取请求。不需要账号登录，仅使用用户提交的公开牌谱链接。",
        )
    }

    fun fetchPublicRecord(detail: PaipuDetail): FinalPaipuDownload {
        val request = buildPublicRequest(detail)
            ?: return FinalPaipuDownload(
                status = FinalPaipuDownloadStatus.Failed,
                request = null,
                paipu = null,
                message = "缺少官方牌谱链接或 UUID，无法提取公开牌谱。",
            )

        return try {
            val paipu = fetcher.fetch(request)
            FinalPaipuDownload(
                status = FinalPaipuDownloadStatus.Fetched,
                request = request,
                paipu = paipu,
                message = "已通过公开解析源提取牌谱。不涉及账号登录或历史牌谱同步。",
            )
        } catch (error: Exception) {
            FinalPaipuDownload(
                status = FinalPaipuDownloadStatus.Failed,
                request = request,
                paipu = null,
                message = error.message ?: "公开解析源暂不可用。",
            )
        }
    }

    private fun buildPublicRequest(detail: PaipuDetail): PublicPaipuFetchRequest? {
        val uuid = detail.uuid
        val officialUrl = detail.officialUrl
        if (uuid.isNullOrBlank() || officialUrl.isNullOrBlank()) {
            return null
        }

        return PublicPaipuFetchRequest(
            uuid = uuid,
            officialUrl = officialUrl,
            majGgUrl = "https://maj.gg/game/$uuid",
            encodedAccountId = detail.encodedAccountId,
            viewAccountId = detail.viewAccountId,
        )
    }
}
