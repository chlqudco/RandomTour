package com.chlqudco.randomtour

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OverpassQueryBuilderTest {
    @Test
    fun cafeQueryUsesRequestedCenterRadiusAndCafeTags() {
        val query = OverpassQueryBuilder.build(
            origin = GeoPoint(37.5665, 126.9780),
            radiusM = 2000,
            mode = ExplorationMode.CAFE
        )

        assertTrue(query.contains("around:2000,37.566500,126.978000"))
        assertTrue(query.contains("\"amenity\"=\"cafe\""))
        assertTrue(query.contains("\"shop\"=\"bakery\""))
        assertTrue(query.contains("\"shop\"=\"confectionery\""))
        assertTrue(query.contains("\"shop\"=\"coffee\""))
        assertFalse(query.contains("\"tourism\"=\"museum\""))
    }

    @Test
    fun walkQueryRequestsOutdoorDestinations() {
        val query = OverpassQueryBuilder.build(
            origin = GeoPoint(35.1796, 129.0756),
            radiusM = 1000,
            mode = ExplorationMode.WALK
        )

        assertTrue(query.contains("\"leisure\"=\"park\""))
        assertTrue(query.contains("\"leisure\"=\"garden\""))
        assertTrue(query.contains("\"leisure\"=\"nature_reserve\""))
        assertTrue(query.contains("\"tourism\"=\"viewpoint\""))
        assertTrue(query.contains("\"place\"=\"square\""))
        assertFalse(query.contains("\"amenity\"=\"restaurant\""))
    }

    @Test
    fun everyModeProducesExecutableOverpassEnvelope() {
        ExplorationMode.entries.forEach { mode ->
            val query = OverpassQueryBuilder.build(
                origin = GeoPoint(37.0, 127.0),
                radiusM = 500,
                mode = mode
            )

            assertTrue(query.startsWith("[out:json][timeout:10];("))
            assertTrue(query.endsWith(");out center tags qt 100;"))
            assertTrue(query.contains("nwr["))
        }
    }
}
