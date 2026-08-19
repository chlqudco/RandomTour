package com.chlqudco.randomtour

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExplorationMathTest {
    @Test
    fun distanceBetweenNearbyCoordinatesIsCalculatedInMeters() {
        val start = GeoPoint(37.5665, 126.9780)
        val destination = GeoPoint(37.5665, 126.9893)

        val distance = ExplorationMath.distanceMeters(start, destination)

        assertTrue(distance in 990.0..1010.0)
    }

    @Test
    fun bearingAndDirectionPointEast() {
        val start = GeoPoint(37.5665, 126.9780)
        val destination = GeoPoint(37.5665, 126.9893)

        assertEquals("동쪽", ExplorationMath.directionName(ExplorationMath.bearingDegrees(start, destination)))
    }

    @Test
    fun arrivalRequiresThreeAccurateConsecutiveSamples() {
        val detector = ArrivalDetector()

        assertFalse(detector.update(42.0, 20f))
        assertFalse(detector.update(38.0, 22f))
        assertTrue(detector.update(35.0, 18f))
    }

    @Test
    fun inaccurateSampleResetsArrivalProgress() {
        val detector = ArrivalDetector()

        detector.update(42.0, 20f)
        detector.update(38.0, 22f)
        assertFalse(detector.update(35.0, 60f))
        assertEquals(0, detector.consecutiveSamples)
    }
}
