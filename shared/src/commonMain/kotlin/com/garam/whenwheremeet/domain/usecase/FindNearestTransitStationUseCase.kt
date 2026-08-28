package com.garam.whenwheremeet.domain.usecase

import com.garam.whenwheremeet.domain.model.GeoPoint
import com.garam.whenwheremeet.domain.model.TransitStation
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

class FindNearestTransitStationUseCase {
    operator fun invoke(
        currentLocation: GeoPoint,
        stations: List<TransitStation>,
    ): TransitStation? = stations.minByOrNull { station ->
        haversineKilometers(currentLocation, station.point)
    }
}

private fun haversineKilometers(origin: GeoPoint, destination: GeoPoint): Double {
    val earthRadius = 6371.0
    val originLatitude = origin.latitude * PI / 180
    val destinationLatitude = destination.latitude * PI / 180
    val latitudeDelta = (destination.latitude - origin.latitude) * PI / 180
    val longitudeDelta = (destination.longitude - origin.longitude) * PI / 180
    val value = sin(latitudeDelta / 2).pow(2) +
        cos(originLatitude) * cos(destinationLatitude) * sin(longitudeDelta / 2).pow(2)
    return earthRadius * 2 * atan2(sqrt(value), sqrt(1 - value))
}
