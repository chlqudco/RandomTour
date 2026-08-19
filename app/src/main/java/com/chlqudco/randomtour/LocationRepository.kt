package com.chlqudco.randomtour

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.location.LocationRequest
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

class LocationRepository(private val context: Context) {
    private val locationManager = context.getSystemService(LocationManager::class.java)
    private var activeListener: LocationListener? = null

    fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    fun hasPreciseLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    fun isLocationEnabled(): Boolean = locationManager.isLocationEnabled

    @SuppressLint("MissingPermission")
    suspend fun currentLocation(): LocationSnapshot? {
        if (!hasLocationPermission() || !isLocationEnabled()) return null
        val provider = bestProvider() ?: return null
        return withTimeoutOrNull(12_000) {
            suspendCancellableCoroutine { continuation ->
                val cancellationSignal = CancellationSignal()
                continuation.invokeOnCancellation { cancellationSignal.cancel() }
                runCatching {
                    locationManager.getCurrentLocation(
                        provider,
                        cancellationSignal,
                        ContextCompat.getMainExecutor(context)
                    ) { location ->
                        if (continuation.isActive) continuation.resume(location?.toSnapshot())
                    }
                }.onFailure {
                    if (continuation.isActive) continuation.resume(lastKnownLocation())
                }
            }
        } ?: lastKnownLocation()
    }

    @SuppressLint("MissingPermission")
    fun startUpdates(
        onLocation: (LocationSnapshot) -> Unit,
        onError: (String) -> Unit
    ) {
        stopUpdates()
        if (!hasLocationPermission()) {
            onError("위치 권한이 필요해요")
            return
        }
        if (!isLocationEnabled()) {
            onError("기기의 위치 서비스를 켜 주세요")
            return
        }
        val provider = bestProvider()
        if (provider == null) {
            onError("사용할 수 있는 위치 제공자가 없어요")
            return
        }
        val listener = LocationListener { location -> onLocation(location.toSnapshot()) }
        val request = LocationRequest.Builder(2_500L)
            .setMinUpdateIntervalMillis(1_500L)
            .setMinUpdateDistanceMeters(2f)
            .setQuality(LocationRequest.QUALITY_HIGH_ACCURACY)
            .build()
        runCatching {
            locationManager.requestLocationUpdates(
                provider,
                request,
                ContextCompat.getMainExecutor(context),
                listener
            )
            activeListener = listener
        }.onFailure {
            onError("현재 위치를 계속 확인할 수 없어요")
        }
    }

    fun stopUpdates() {
        activeListener?.let(locationManager::removeUpdates)
        activeListener = null
    }

    @SuppressLint("MissingPermission")
    private fun lastKnownLocation(): LocationSnapshot? = if (!hasLocationPermission()) {
        null
    } else {
        listOf(
            LocationManager.FUSED_PROVIDER,
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER
        ).mapNotNull { provider ->
            runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull()
        }.maxByOrNull(Location::getTime)?.toSnapshot()
    }

    private fun bestProvider(): String? = listOf(
        LocationManager.FUSED_PROVIDER,
        LocationManager.GPS_PROVIDER,
        LocationManager.NETWORK_PROVIDER
    ).firstOrNull { provider ->
        locationManager.allProviders.contains(provider) &&
            runCatching { locationManager.isProviderEnabled(provider) }.getOrDefault(false)
    }

    private fun Location.toSnapshot(): LocationSnapshot = LocationSnapshot(
        point = GeoPoint(latitude, longitude),
        accuracyM = if (hasAccuracy()) accuracy else 999f,
        bearingDegrees = if (hasBearing()) bearing else null,
        timestamp = time.takeIf { it > 0 } ?: System.currentTimeMillis()
    )
}
