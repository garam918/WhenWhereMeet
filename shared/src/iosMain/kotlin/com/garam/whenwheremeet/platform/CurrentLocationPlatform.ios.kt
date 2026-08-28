package com.garam.whenwheremeet.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.garam.whenwheremeet.domain.model.GeoPoint
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreLocation.CLAuthorizationStatus
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLAuthorizationStatusDenied
import platform.CoreLocation.kCLAuthorizationStatusNotDetermined
import platform.CoreLocation.kCLAuthorizationStatusRestricted
import platform.Foundation.NSError
import platform.darwin.NSObject

@Composable
actual fun rememberCurrentLocationPlatform(): CurrentLocationPlatform = remember {
    IosCurrentLocationPlatform()
}

@OptIn(ExperimentalForeignApi::class)
private class IosCurrentLocationPlatform : NSObject(), CurrentLocationPlatform, CLLocationManagerDelegateProtocol {
    private val manager = CLLocationManager().apply { delegate = this@IosCurrentLocationPlatform }
    private var pendingResult: ((CurrentLocationResult) -> Unit)? = null

    override val isSupported: Boolean
        get() = CLLocationManager.locationServicesEnabled()

    override fun requestCurrentLocation(onResult: (CurrentLocationResult) -> Unit) {
        if (!isSupported) {
            onResult(CurrentLocationResult.Unavailable)
            return
        }
        pendingResult = onResult
        handleAuthorization(CLLocationManager.authorizationStatus())
    }

    override fun locationManagerDidChangeAuthorization(manager: CLLocationManager) {
        handleAuthorization(CLLocationManager.authorizationStatus())
    }

    override fun locationManager(
        manager: CLLocationManager,
        didChangeAuthorizationStatus: CLAuthorizationStatus,
    ) {
        handleAuthorization(didChangeAuthorizationStatus)
    }

    override fun locationManager(manager: CLLocationManager, didUpdateLocations: List<*>) {
        val location = didUpdateLocations.lastOrNull() as? CLLocation ?: return
        manager.stopUpdatingLocation()
        val point = location.coordinate.useContents { GeoPoint(latitude, longitude) }
        complete(CurrentLocationResult.Success(point))
    }

    override fun locationManager(manager: CLLocationManager, didFailWithError: NSError) {
        manager.stopUpdatingLocation()
        complete(CurrentLocationResult.Unavailable)
    }

    private fun handleAuthorization(status: CLAuthorizationStatus) {
        when (status) {
            kCLAuthorizationStatusAuthorizedAlways,
            kCLAuthorizationStatusAuthorizedWhenInUse -> manager.requestLocation()
            kCLAuthorizationStatusNotDetermined -> manager.requestWhenInUseAuthorization()
            kCLAuthorizationStatusDenied,
            kCLAuthorizationStatusRestricted -> complete(CurrentLocationResult.PermissionDenied)
            else -> complete(CurrentLocationResult.Unavailable)
        }
    }

    private fun complete(result: CurrentLocationResult) {
        val callback = pendingResult ?: return
        pendingResult = null
        callback(result)
    }
}
