package com.chlqudco.randomtour

import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

object ExplorationMath {
    private const val EARTH_RADIUS_M = 6_371_000.0

    fun distanceMeters(from: GeoPoint, to: GeoPoint): Double {
        val fromLatitude = Math.toRadians(from.latitude)
        val toLatitude = Math.toRadians(to.latitude)
        val latitudeDelta = Math.toRadians(to.latitude - from.latitude)
        val longitudeDelta = Math.toRadians(to.longitude - from.longitude)
        val haversine = sin(latitudeDelta / 2) * sin(latitudeDelta / 2) +
            cos(fromLatitude) * cos(toLatitude) *
            sin(longitudeDelta / 2) * sin(longitudeDelta / 2)
        val angularDistance = 2 * atan2(sqrt(haversine), sqrt(1 - haversine))
        return EARTH_RADIUS_M * angularDistance
    }

    fun bearingDegrees(from: GeoPoint, to: GeoPoint): Double {
        val fromLatitude = Math.toRadians(from.latitude)
        val toLatitude = Math.toRadians(to.latitude)
        val longitudeDelta = Math.toRadians(to.longitude - from.longitude)
        val y = sin(longitudeDelta) * cos(toLatitude)
        val x = cos(fromLatitude) * sin(toLatitude) -
            sin(fromLatitude) * cos(toLatitude) * cos(longitudeDelta)
        return (Math.toDegrees(atan2(y, x)) + 360.0) % 360.0
    }

    fun directionName(bearingDegrees: Double): String {
        val directions = listOf("북쪽", "북동쪽", "동쪽", "남동쪽", "남쪽", "남서쪽", "서쪽", "북서쪽")
        val index = ((bearingDegrees + 22.5) / 45.0).toInt() % directions.size
        return directions[index]
    }

    fun temperature(distanceM: Double): TemperatureHint = when {
        distanceM > 700 -> TemperatureHint.COLD
        distanceM > 400 -> TemperatureHint.COOL
        distanceM > 150 -> TemperatureHint.WARM
        distanceM > 50 -> TemperatureHint.HOT
        else -> TemperatureHint.VERY_HOT
    }

    fun formatDistance(distanceM: Double): String = if (distanceM < 1000) {
        "${distanceM.coerceAtLeast(0.0).roundToInt()}m"
    } else {
        String.format(Locale.KOREA, "%.1fkm", distanceM / 1000.0)
    }

    fun formatDuration(seconds: Long): String {
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return if (minutes == 0L) "${remainingSeconds}초" else "${minutes}분 ${remainingSeconds}초"
    }
}

class ArrivalDetector(
    private val requiredSamples: Int = 3,
    private val maximumDistanceM: Double = 50.0,
    private val maximumAccuracyM: Float = 35f
) {
    var consecutiveSamples: Int = 0
        private set

    fun update(distanceM: Double, accuracyM: Float): Boolean {
        consecutiveSamples = if (
            distanceM <= maximumDistanceM && accuracyM in 0.1f..maximumAccuracyM
        ) {
            consecutiveSamples + 1
        } else {
            0
        }
        return consecutiveSamples >= requiredSamples
    }

    fun reset() {
        consecutiveSamples = 0
    }
}
