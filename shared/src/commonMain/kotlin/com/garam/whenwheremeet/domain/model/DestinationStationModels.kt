package com.garam.whenwheremeet.domain.model

import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class DestinationStationProposal(
    val roomId: String,
    val participantId: String,
    val station: TransitStation,
    val updatedAt: Instant,
)

@Serializable
data class DestinationStationVote(
    val roomId: String,
    val participantId: String,
    val stationId: String,
    val updatedAt: Instant,
)

data class DestinationStationOption(
    val station: TransitStation,
    val proposerParticipantIds: List<String>,
    val voterParticipantIds: List<String>,
) {
    val voteCount: Int get() = voterParticipantIds.size
}
