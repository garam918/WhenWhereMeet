package com.garam.whenwheremeet.platform

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.garam.whenwheremeet.domain.model.GeoPoint

@Composable
actual fun rememberCurrentLocationPlatform(): CurrentLocationPlatform {
    val context = LocalContext.current
    var pendingRequest by remember { mutableStateOf<(() -> Unit)?>(null) }
    var pendingPermissionDenied by remember { mutableStateOf<(() -> Unit)?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        val request = pendingRequest
        val denied = pendingPermissionDenied
        pendingRequest = null
        pendingPermissionDenied = null
        if (granted) request?.invoke() else denied?.invoke()
    }

    return remember(context, permissionLauncher) {
        object : CurrentLocationPlatform {
            override val isSupported: Boolean = true

            override fun requestCurrentLocation(onResult: (CurrentLocationResult) -> Unit) {
                val hasPermission = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                ) == PackageManager.PERMISSION_GRANTED
                if (hasPermission) {
                    context.requestCurrentGeoPoint(onResult)
                } else {
                    pendingRequest = { context.requestCurrentGeoPoint(onResult) }
                    pendingPermissionDenied = { onResult(CurrentLocationResult.PermissionDenied) }
                    permissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                        ),
                    )
                }
            }
        }
    }
}

@SuppressLint("MissingPermission")
private fun Context.requestCurrentGeoPoint(onResult: (CurrentLocationResult) -> Unit) {
    val manager = getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        ?: return onResult(CurrentLocationResult.Unavailable)
    val enabledProviders = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        .filter { provider -> runCatching { manager.isProviderEnabled(provider) }.getOrDefault(false) }
    if (enabledProviders.isEmpty()) {
        onResult(CurrentLocationResult.Unavailable)
        return
    }

    val lastLocation = enabledProviders.mapNotNull { provider ->
        runCatching { manager.getLastKnownLocation(provider) }.getOrNull()
    }.maxByOrNull(Location::getTime)
    val isRecent = lastLocation?.let {
        SystemClock.elapsedRealtimeNanos() - it.elapsedRealtimeNanos <= RECENT_LOCATION_NANOS
    } == true
    if (isRecent) {
        onResult(lastLocation.toSuccess())
        return
    }

    val provider = enabledProviders.first()
    val handler = Handler(Looper.getMainLooper())
    var completed = false
    fun complete(result: CurrentLocationResult) {
        if (completed) return
        completed = true
        onResult(result)
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val cancellationSignal = CancellationSignal()
        handler.postDelayed({
            cancellationSignal.cancel()
            complete(lastLocation?.toSuccess() ?: CurrentLocationResult.Unavailable)
        }, LOCATION_TIMEOUT_MILLIS)
        runCatching {
            manager.getCurrentLocation(provider, cancellationSignal, mainExecutor) { location ->
                complete(location?.toSuccess() ?: lastLocation?.toSuccess() ?: CurrentLocationResult.Unavailable)
            }
        }.onFailure {
            complete(lastLocation?.toSuccess() ?: CurrentLocationResult.Unavailable)
        }
    } else {
        lateinit var listener: LocationListener
        listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                manager.removeUpdates(this)
                complete(location.toSuccess())
            }

            @Deprecated("Deprecated in Android")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit

            override fun onProviderEnabled(provider: String) = Unit

            override fun onProviderDisabled(provider: String) = Unit
        }
        handler.postDelayed({
            manager.removeUpdates(listener)
            complete(lastLocation?.toSuccess() ?: CurrentLocationResult.Unavailable)
        }, LOCATION_TIMEOUT_MILLIS)
        runCatching { manager.requestSingleUpdate(provider, listener, Looper.getMainLooper()) }
            .onFailure { complete(lastLocation?.toSuccess() ?: CurrentLocationResult.Unavailable) }
    }
}

private fun Location.toSuccess(): CurrentLocationResult.Success =
    CurrentLocationResult.Success(GeoPoint(latitude, longitude))

private const val LOCATION_TIMEOUT_MILLIS = 12_000L
private const val RECENT_LOCATION_NANOS = 5L * 60L * 1_000_000_000L
