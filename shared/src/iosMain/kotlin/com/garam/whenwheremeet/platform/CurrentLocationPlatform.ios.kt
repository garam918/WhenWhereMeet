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
private class IosCurrentLocationPlatform : CurrentLocationPlatform {
    private val manager = CLLocationManager()
    private val locationDelegate = CurrentLocationDelegate(
        onAuthorizationChanged = ::handleAuthorization,
        onLocationUpdated = ::handleLocationUpdated,
        onLocationFailed = ::handleLocationFailed,
    )
    private var pendingResult: ((CurrentLocationResult) -> Unit)? = null

    init {
        manager.delegate = locationDelegate
    }

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

    private fun handleLocationUpdated(manager: CLLocationManager, location: CLLocation) {
        manager.stopUpdatingLocation()
        val point = location.coordinate.useContents { GeoPoint(latitude, longitude) }
        complete(CurrentLocationResult.Success(point))
    }

    private fun handleLocationFailed(manager: CLLocationManager) {
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

@OptIn(ExperimentalForeignApi::class)
private class CurrentLocationDelegate(
    private val onAuthorizationChanged: (CLAuthorizationStatus) -> Unit,
    private val onLocationUpdated: (CLLocationManager, CLLocation) -> Unit,
    private val onLocationFailed: (CLLocationManager) -> Unit,
) : NSObject(), CLLocationManagerDelegateProtocol {
    override fun locationManagerDidChangeAuthorization(manager: CLLocationManager) {
        onAuthorizationChanged(CLLocationManager.authorizationStatus())
    }

    override fun locationManager(
        manager: CLLocationManager,
        didChangeAuthorizationStatus: CLAuthorizationStatus,
    ) {
        onAuthorizationChanged(didChangeAuthorizationStatus)
    }

    override fun locationManager(manager: CLLocationManager, didUpdateLocations: List<*>) {
        val location = didUpdateLocations.lastOrNull() as? CLLocation ?: return
        onLocationUpdated(manager, location)
    }

    override fun locationManager(manager: CLLocationManager, didFailWithError: NSError) {
        onLocationFailed(manager)
    }
}
