package com.example.mahjongcoach.data

class FinalPaipuParser {
    fun fromDecodedEvents(
        uuid: String,
        officialUrl: String,
        head: PaipuHead,
        decodedEvents: List<DecodedPaipuEvent>,
    ): FinalPaipu {
        val rounds = mutableListOf<PaipuRound>()
        var currentEvents = mutableListOf<PaipuEvent>()
        var roundIndex = -1

        decodedEvents.forEachIndexed { index, decoded ->
            if (decoded.type == PaipuEventType.NewRound) {
                if (currentEvents.isNotEmpty()) {
                    rounds += PaipuRound(roundIndex.coerceAtLeast(0), currentEvents)
                }
                roundIndex += 1
                currentEvents = mutableListOf()
            }
            currentEvents += PaipuEvent(
                index = index,
                type = decoded.type,
                actorSeat = decoded.actorSeat,
                tile = decoded.tile,
                payload = decoded.payload,
            )
        }

        if (currentEvents.isNotEmpty()) {
            rounds += PaipuRound(roundIndex.coerceAtLeast(0), currentEvents)
        }

        return FinalPaipu(
            uuid = uuid,
            officialUrl = officialUrl,
            head = head,
            rounds = rounds,
        )
    }
}

data class DecodedPaipuEvent(
    val type: PaipuEventType,
    val actorSeat: Int?,
    val tile: String?,
    val payload: Map<String, String> = emptyMap(),
)
