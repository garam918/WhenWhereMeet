package com.garam.whenwheremeet.domain.usecase

import com.garam.whenwheremeet.domain.model.DestinationStationOption
import com.garam.whenwheremeet.domain.model.DestinationStationProposal
import com.garam.whenwheremeet.domain.model.DestinationStationVote
import com.garam.whenwheremeet.domain.model.PlaceCandidate
import com.garam.whenwheremeet.domain.model.PlaceCategory
import com.garam.whenwheremeet.domain.model.PlaceSource
import com.garam.whenwheremeet.domain.model.TransitStation
import com.garam.whenwheremeet.domain.model.UserStartLocation

class BuildDestinationStationOptionsUseCase {
    operator fun invoke(
        proposals: List<DestinationStationProposal>,
        votes: List<DestinationStationVote>,
    ): List<DestinationStationOption> {
        val validStationIds = proposals.mapTo(mutableSetOf()) { it.station.id }
        val votesByStation = votes
            .filter { it.stationId in validStationIds }
            .groupBy { it.stationId }

        return proposals
            .groupBy { it.station.id }
            .map { (_, stationProposals) ->
                val station = stationProposals.minBy { it.updatedAt }.station
                DestinationStationOption(
                    station = station,
                    proposerParticipantIds = stationProposals.map { it.participantId }.distinct(),
                    voterParticipantIds = votesByStation[station.id]
                        .orEmpty()
                        .map { it.participantId }
                        .distinct(),
                )
            }
            .sortedWith(
                compareByDescending<DestinationStationOption> { it.voteCount }
                    .thenBy { it.station.name },
            )
    }
}

class KakaoMapRouteUrlBuilder {
    fun routeUrl(origin: UserStartLocation, destination: TransitStation): String =
        "https://map.kakao.com/link/by/traffic/" +
            "${origin.label.urlPathEncode()},${origin.latitude},${origin.longitude}/" +
            "${destination.name.urlPathEncode()},${destination.latitude},${destination.longitude}"

    fun destinationUrl(destination: TransitStation): String =
        "https://map.kakao.com/link/map/" +
            "${destination.name.urlPathEncode()},${destination.latitude},${destination.longitude}"

    fun routeUrl(origin: UserStartLocation, destination: PlaceCandidate): String =
        "https://map.kakao.com/link/by/traffic/" +
            "${origin.label.urlPathEncode()},${origin.latitude},${origin.longitude}/" +
            "${destination.name.urlPathEncode()},${destination.latitude},${destination.longitude}"
}

fun TransitStation.toConfirmedStationPlace(
    routeUrlBuilder: KakaoMapRouteUrlBuilder = KakaoMapRouteUrlBuilder(),
): PlaceCandidate = PlaceCandidate(
    id = "station-$id",
    name = name,
    category = PlaceCategory.ETC,
    address = region,
    latitude = latitude,
    longitude = longitude,
    openingHoursSummary = lines.joinToString(" · "),
    mapUrl = routeUrlBuilder.destinationUrl(this),
    source = PlaceSource.CUSTOM,
)

private fun String.urlPathEncode(): String = encodeToByteArray().joinToString("") { byte ->
    val value = byte.toInt() and 0xff
    when {
        value in 'A'.code..'Z'.code -> value.toChar().toString()
        value in 'a'.code..'z'.code -> value.toChar().toString()
        value in '0'.code..'9'.code -> value.toChar().toString()
        value == '-'.code || value == '_'.code || value == '.'.code || value == '~'.code -> value.toChar().toString()
        else -> "%${value.toString(16).uppercase().padStart(2, '0')}"
    }
}
