package com.example.mahjongcoach.data

data class PaipuImportResult(
    val parsedLink: ParsedPaipuLink,
    val detail: PaipuDetail,
    val finalPaipuDownload: FinalPaipuDownload,
)

class PaipuImportPipeline(
    private val detailDownloader: PaipuDetailDownloader = PaipuDetailDownloader(),
    private val finalPaipuDownloader: FinalPaipuDownloader = FinalPaipuDownloader(),
) {
    fun import(input: String): PaipuImportResult {
        val parsed = PaipuLinkParser.parse(input)
        if (!parsed.canStartReview) {
            return PaipuImportResult(
                parsedLink = parsed,
                detail = PaipuDetail(
                    source = parsed.source,
                    uuid = parsed.uuid,
                    encodedAccountId = parsed.encodedAccountId,
                    viewAccountId = parsed.viewAccountId,
                    amaeRecordId = parsed.amaeRecordId,
                    modeId = parsed.modeId,
                    officialUrl = null,
                    fetchStatus = PaipuFetchStatus.Failed,
                    message = parsed.message,
                ),
                finalPaipuDownload = FinalPaipuDownload(
                    status = FinalPaipuDownloadStatus.Failed,
                    request = null,
                    paipu = null,
                    message = parsed.message,
                ),
            )
        }

        val detail = detailDownloader.download(parsed)
        if (detail.fetchStatus == PaipuFetchStatus.Failed) {
            return PaipuImportResult(
                parsedLink = parsed,
                detail = detail,
                finalPaipuDownload = FinalPaipuDownload(
                    status = FinalPaipuDownloadStatus.Failed,
                    request = null,
                    paipu = null,
                    message = detail.message,
                ),
            )
        }

        return PaipuImportResult(
            parsedLink = parsed,
            detail = detail,
            finalPaipuDownload = finalPaipuDownloader.fetchPublicRecord(detail),
        )
    }
}
