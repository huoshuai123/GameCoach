package com.example.mahjongcoach.data

data class FinalPaipu(
    val uuid: String,
    val officialUrl: String,
    val head: PaipuHead,
    val rounds: List<PaipuRound>,
)

data class PaipuHead(
    val modeId: String?,
    val startTime: Long?,
    val endTime: Long?,
    val players: List<PaipuPlayer>,
)

data class PaipuPlayer(
    val accountId: Long?,
    val nickname: String,
    val seat: Int,
    val score: Int?,
)

data class PaipuRound(
    val roundIndex: Int,
    val events: List<PaipuEvent>,
)

data class PaipuEvent(
    val index: Int,
    val type: PaipuEventType,
    val actorSeat: Int?,
    val tile: String?,
    val payload: Map<String, String> = emptyMap(),
)

enum class PaipuEventType(val label: String) {
    NewRound("新局"),
    DealTile("摸牌"),
    DiscardTile("切牌"),
    ChiPengGang("副露/杠"),
    Riichi("立直"),
    Hule("和牌"),
    Liuju("流局"),
    Unknown("未知事件"),
}

data class FinalPaipuDownload(
    val status: FinalPaipuDownloadStatus,
    val request: OfficialPaipuRequest?,
    val paipu: FinalPaipu?,
    val message: String,
)

data class OfficialPaipuRequest(
    val uuid: String,
    val officialUrl: String,
    val encodedAccountId: String?,
    val clientVersionString: String? = null,
    val requiresWebSocket: Boolean = true,
    val requiresOAuth: Boolean = true,
    val requiresProtobuf: Boolean = true,
)

enum class FinalPaipuDownloadStatus {
    ReadyToFetch,
    RequiresOfficialProtocol,
    Failed,
}
