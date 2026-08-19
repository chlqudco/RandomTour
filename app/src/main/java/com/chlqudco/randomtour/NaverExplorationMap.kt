package com.chlqudco.randomtour

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.graphics.PointF
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.chlqudco.randomtour.ui.theme.ExplorerBlue
import com.chlqudco.randomtour.ui.theme.ExplorerOrange
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.CameraAnimation
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.MapView
import com.naver.maps.map.NaverMap
import com.naver.maps.map.Symbol
import com.naver.maps.map.overlay.CircleOverlay
import com.naver.maps.map.overlay.Marker
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.hypot

@Composable
fun NaverExplorationMap(
    startLocation: GeoPoint?,
    currentLocation: LocationSnapshot?,
    radiusM: Int,
    destination: Destination?,
    revealDestination: Boolean,
    recenterRequest: Int,
    symbolCollectionRequest: Int = 0,
    onSymbolsCollected: ((Int, List<MapSymbolCandidate>) -> Unit)? = null,
    modifier: Modifier = Modifier,
    mapBottomPadding: Dp = 276.dp
) {
    val context = LocalContext.current
    val lifecycleOwner = context.findActivity() as? LifecycleOwner
    val mapView = remember {
        MapView(context).apply { onCreate(null) }
    }
    var naverMap by remember { mutableStateOf<NaverMap?>(null) }
    val radiusOverlay = remember { CircleOverlay() }
    val destinationMarker = remember { Marker() }
    val bottomPadding = with(LocalDensity.current) { mapBottomPadding.roundToPx() }
    val latestSymbolsCallback by rememberUpdatedState(onSymbolsCollected)
    var initialCameraMoved by remember { mutableStateOf(false) }

    DisposableEffect(mapView, lifecycleOwner) {
        val lifecycle = lifecycleOwner?.lifecycle
        var started = false
        var resumed = false
        var destroyed = false

        fun start() {
            if (!started && !destroyed) {
                mapView.onStart()
                started = true
            }
        }

        fun resume() {
            if (!resumed && !destroyed) {
                start()
                mapView.onResume()
                resumed = true
            }
        }

        fun pause() {
            if (resumed && !destroyed) {
                mapView.onPause()
                resumed = false
            }
        }

        fun stop() {
            if (started && !destroyed) {
                pause()
                mapView.onStop()
                started = false
            }
        }

        fun destroy() {
            if (!destroyed) {
                stop()
                mapView.onDestroy()
                destroyed = true
            }
        }

        if (lifecycle?.currentState?.isAtLeast(Lifecycle.State.STARTED) == true) start()
        if (lifecycle?.currentState?.isAtLeast(Lifecycle.State.RESUMED) == true) resume()

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> start()
                Lifecycle.Event.ON_RESUME -> resume()
                Lifecycle.Event.ON_PAUSE -> pause()
                Lifecycle.Event.ON_STOP -> stop()
                Lifecycle.Event.ON_DESTROY -> destroy()
                else -> Unit
            }
        }
        lifecycle?.addObserver(observer)
        mapView.getMapAsync { map ->
            if (destroyed) return@getMapAsync
            map.minZoom = 10.0
            map.maxZoom = 20.0
            map.uiSettings.apply {
                isCompassEnabled = false
                isScaleBarEnabled = false
                isZoomControlEnabled = false
                isLocationButtonEnabled = false
                isTiltGesturesEnabled = false
            }
            map.setContentPadding(0, 0, 0, bottomPadding)
            naverMap = map
        }

        onDispose {
            lifecycle?.removeObserver(observer)
            radiusOverlay.map = null
            destinationMarker.map = null
            naverMap = null
            destroy()
        }
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier
    )

    LaunchedEffect(naverMap, startLocation, radiusM) {
        val map = naverMap ?: return@LaunchedEffect
        val start = startLocation ?: return@LaunchedEffect
        radiusOverlay.apply {
            center = LatLng(start.latitude, start.longitude)
            radius = radiusM.toDouble()
            color = ExplorerBlue.copy(alpha = 0.10f).toArgb()
            outlineColor = ExplorerBlue.copy(alpha = 0.72f).toArgb()
            outlineWidth = 3
            this.map = map
        }
    }

    LaunchedEffect(naverMap, currentLocation) {
        val map = naverMap ?: return@LaunchedEffect
        val location = currentLocation
        map.locationOverlay.apply {
            isVisible = location != null
            if (location != null) {
                position = LatLng(location.point.latitude, location.point.longitude)
                bearing = location.bearingDegrees ?: 0f
                circleRadius = 22
            }
        }
        if (!initialCameraMoved && location != null && !revealDestination) {
            map.moveCamera(
                CameraUpdate.scrollAndZoomTo(
                    LatLng(location.point.latitude, location.point.longitude),
                    zoomForRadius(radiusM)
                ).animate(CameraAnimation.Easing, 600)
            )
            initialCameraMoved = true
        }
    }

    LaunchedEffect(naverMap, destination, revealDestination) {
        val map = naverMap ?: return@LaunchedEffect
        val target = destination
        destinationMarker.map = null
        if (target != null && revealDestination) {
            destinationMarker.apply {
                position = LatLng(target.point.latitude, target.point.longitude)
                captionText = target.name
                captionTextSize = 14f
                iconTintColor = ExplorerOrange.toArgb()
                this.map = map
            }
            radiusOverlay.map = null
            map.moveCamera(
                CameraUpdate.scrollAndZoomTo(
                    LatLng(target.point.latitude, target.point.longitude),
                    16.5
                ).animate(CameraAnimation.Easing, 700)
            )
        }
    }

    LaunchedEffect(naverMap, recenterRequest) {
        if (recenterRequest == 0) return@LaunchedEffect
        val map = naverMap ?: return@LaunchedEffect
        val location = currentLocation ?: return@LaunchedEffect
        map.moveCamera(
            CameraUpdate.scrollAndZoomTo(
                LatLng(location.point.latitude, location.point.longitude),
                zoomForRadius(radiusM)
            ).animate(CameraAnimation.Easing, 500)
        )
    }

    LaunchedEffect(naverMap, symbolCollectionRequest) {
        if (symbolCollectionRequest <= 0) return@LaunchedEffect
        val map = naverMap ?: return@LaunchedEffect
        delay(900)
        var renderAttempts = 0
        while (renderAttempts < 12 && (!map.isLoaded || !map.isRenderingStable)) {
            delay(150)
            renderAttempts += 1
        }
        val contentRect = map.contentRect
        if (contentRect.width() <= 0 || contentRect.height() <= 0) {
            latestSymbolsCallback?.invoke(symbolCollectionRequest, emptyList())
            return@LaunchedEffect
        }
        val center = PointF(contentRect.exactCenterX(), contentRect.exactCenterY())
        val radius = ceil(
            hypot(contentRect.width() / 2.0, contentRect.height() / 2.0)
        ).toInt() + 2
        val symbols = map.pickAll(center, radius)
            .filterIsInstance<Symbol>()
            .map { symbol ->
                MapSymbolCandidate(
                    name = symbol.caption,
                    point = GeoPoint(symbol.position.latitude, symbol.position.longitude)
                )
            }
            .filter { it.name.isNotBlank() }
            .distinctBy {
                "${it.name}|${"%.5f".format(Locale.US, it.point.latitude)}|${"%.5f".format(Locale.US, it.point.longitude)}"
            }
        latestSymbolsCallback?.invoke(symbolCollectionRequest, symbols)
    }
}

@Composable
fun rememberDeviceHeading(): Float? {
    val context = LocalContext.current
    val sensorManager = remember { context.getSystemService(SensorManager::class.java) }
    var heading by remember { mutableStateOf<Float?>(null) }

    DisposableEffect(sensorManager) {
        val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val listener = object : SensorEventListener {
            private val rotationMatrix = FloatArray(9)
            private val orientation = FloatArray(3)

            override fun onSensorChanged(event: SensorEvent) {
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                SensorManager.getOrientation(rotationMatrix, orientation)
                heading = ((Math.toDegrees(orientation[0].toDouble()) + 360.0) % 360.0).toFloat()
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        if (rotationSensor != null) {
            sensorManager.registerListener(listener, rotationSensor, SensorManager.SENSOR_DELAY_UI)
        }
        onDispose { sensorManager.unregisterListener(listener) }
    }
    return heading
}

private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun zoomForRadius(radiusM: Int): Double = when {
    radiusM <= 500 -> 16.0
    radiusM <= 1000 -> 15.0
    radiusM <= 1500 -> 14.5
    else -> 14.0
}
