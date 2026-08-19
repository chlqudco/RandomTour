package com.chlqudco.randomtour

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.text.Html
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.math.cos

class CandidateSearchException(message: String) : Exception(message)

class CandidateRepository(
    private val context: Context,
    private val store: ExplorationStore,
    private val backendBaseUrl: String
) {
    suspend fun search(
        origin: GeoPoint,
        radiusM: Int,
        mode: ExplorationMode,
        onStage: (DrawStage) -> Unit
    ): CandidateSearchResult = withContext(Dispatchers.IO) {
        val recentKeys = store.loadHistory()
            .filter { it.endedAt >= System.currentTimeMillis() - RECENT_VISIT_WINDOW_MILLIS }
            .map { it.destination.placeKey }
            .toSet()
        val excludedCategories = store.loadSettings().excludedCategories

        if (backendBaseUrl.isNotBlank()) {
            onStage(DrawStage.SEARCHING)
            val backendResult = runCatching {
                searchBackend(origin, radiusM, mode, recentKeys)
            }.getOrNull()
            if (backendResult != null) {
                onStage(DrawStage.FILTERING)
                val filtered = filterCandidates(
                    backendResult.candidates,
                    origin,
                    radiusM,
                    recentKeys,
                    excludedCategories
                )
                if (filtered.isNotEmpty()) {
                    return@withContext backendResult.copy(candidates = filtered, usedBackend = true)
                }
            }
        }

        if (!Geocoder.isPresent()) {
            throw CandidateSearchException("장소 검색을 사용할 수 없는 기기예요")
        }
        onStage(DrawStage.RESOLVING_AREA)
        val geocoder = Geocoder(context, Locale.KOREA)
        val areaLabel = resolveArea(geocoder, origin)
            ?: throw CandidateSearchException("현재 동네를 찾지 못했어요. 위치 정확도를 확인해 주세요")
        onStage(DrawStage.SEARCHING)
        val candidates = searchWithGeocoder(geocoder, areaLabel, origin, radiusM, mode)
        onStage(DrawStage.FILTERING)
        val filtered = filterCandidates(candidates, origin, radiusM, recentKeys, excludedCategories)
        if (filtered.isEmpty()) {
            throw CandidateSearchException("반경 안에서 탐험할 장소를 찾지 못했어요. 반경을 넓혀 다시 시도해 주세요")
        }
        CandidateSearchResult(areaLabel, filtered, false)
    }

    private fun searchBackend(
        origin: GeoPoint,
        radiusM: Int,
        mode: ExplorationMode,
        recentKeys: Set<String>
    ): CandidateSearchResult {
        val endpoint = URL("${backendBaseUrl.trimEnd('/')}/v1/explorations/candidates")
        val connection = endpoint.openConnection() as HttpURLConnection
        return try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 8_000
            connection.readTimeout = 12_000
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            val exclusions = JSONArray().apply { recentKeys.forEach(::put) }
            val request = JSONObject()
                .put("latitude", origin.latitude)
                .put("longitude", origin.longitude)
                .put("radiusM", radiusM)
                .put("mode", mode.name)
                .put("excludePlaceKeys", exclusions)
            connection.outputStream.bufferedWriter(Charsets.UTF_8).use { it.write(request.toString()) }
            if (connection.responseCode !in 200..299) {
                throw CandidateSearchException("장소 서버에 연결하지 못했어요")
            }
            val response = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val json = JSONObject(response)
            val array = json.optJSONArray("candidates") ?: JSONArray()
            val candidates = buildList {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    val point = GeoPoint(item.getDouble("latitude"), item.getDouble("longitude"))
                    val name = cleanTitle(item.optString("name", "탐험 장소"))
                    val address = item.optString("roadAddress", item.optString("address"))
                    add(
                        Destination(
                            placeKey = item.optString("placeKey").ifBlank {
                                placeKey(name, address, point)
                            },
                            name = name,
                            category = item.optString("category", "장소"),
                            point = point,
                            roadAddress = address,
                            distanceFromStartM = ExplorationMath.distanceMeters(origin, point)
                        )
                    )
                }
            }
            CandidateSearchResult(
                areaLabel = json.optString("areaLabel", "현재 동네"),
                candidates = candidates,
                usedBackend = true
            )
        } finally {
            connection.disconnect()
        }
    }

    @Suppress("DEPRECATION")
    private fun resolveArea(geocoder: Geocoder, origin: GeoPoint): String? =
        geocoder.getFromLocation(origin.latitude, origin.longitude, 3)
            ?.firstOrNull()
            ?.let { address ->
                listOf(
                    address.thoroughfare,
                    address.subLocality,
                    address.locality,
                    address.subAdminArea,
                    address.adminArea,
                    address.featureName
                ).firstOrNull { !it.isNullOrBlank() }
            }

    @Suppress("DEPRECATION")
    private fun searchWithGeocoder(
        geocoder: Geocoder,
        areaLabel: String,
        origin: GeoPoint,
        radiusM: Int,
        mode: ExplorationMode
    ): List<Destination> {
        val latitudeDelta = radiusM / 111_320.0
        val longitudeScale = cos(Math.toRadians(origin.latitude)).coerceAtLeast(0.2)
        val longitudeDelta = radiusM / (111_320.0 * longitudeScale)
        return mode.queries.flatMap { category ->
            runCatching {
                val query = "$areaLabel $category"
                val boundedResults = geocoder.getFromLocationName(
                    query,
                    5,
                    origin.latitude - latitudeDelta,
                    origin.longitude - longitudeDelta,
                    origin.latitude + latitudeDelta,
                    origin.longitude + longitudeDelta
                ).orEmpty()
                val results = boundedResults.ifEmpty {
                    geocoder.getFromLocationName(query, 5).orEmpty()
                }
                results.mapNotNull { address ->
                    address.toDestination(category, areaLabel, origin)
                }
            }.getOrDefault(emptyList())
        }
    }

    private fun Address.toDestination(
        category: String,
        areaLabel: String,
        origin: GeoPoint
    ): Destination? {
        if (!hasLatitude() || !hasLongitude()) return null
        val point = GeoPoint(latitude, longitude)
        val name = listOf(premises, featureName, thoroughfare)
            .firstOrNull { !it.isNullOrBlank() && it != areaLabel }
            ?.trim()
            ?: "$areaLabel $category"
        val address = if (maxAddressLineIndex >= 0) getAddressLine(0).orEmpty() else ""
        return Destination(
            placeKey = placeKey(name, address, point),
            name = name,
            category = category,
            point = point,
            roadAddress = address,
            distanceFromStartM = ExplorationMath.distanceMeters(origin, point)
        )
    }

    private fun filterCandidates(
        candidates: List<Destination>,
        origin: GeoPoint,
        radiusM: Int,
        recentKeys: Set<String>,
        excludedCategories: Set<String>
    ): List<Destination> = candidates
        .asSequence()
        .filter { it.point.latitude in -90.0..90.0 && it.point.longitude in -180.0..180.0 }
        .map { it.copy(distanceFromStartM = ExplorationMath.distanceMeters(origin, it.point)) }
        .filter { it.distanceFromStartM in MINIMUM_DISTANCE_M..radiusM.toDouble() }
        .filterNot { it.placeKey in recentKeys }
        .filterNot { destination ->
            excludedCategories.any { excluded ->
                destination.category.contains(excluded, ignoreCase = true)
            }
        }
        .distinctBy {
            val normalizedName = it.name.lowercase(Locale.KOREA).replace(" ", "")
            val normalizedAddress = it.roadAddress.lowercase(Locale.KOREA).replace(" ", "")
            "$normalizedName|$normalizedAddress|${"%.4f".format(Locale.US, it.point.latitude)}|${"%.4f".format(Locale.US, it.point.longitude)}"
        }
        .toList()

    private fun cleanTitle(value: String): String = Html.fromHtml(
        value,
        Html.FROM_HTML_MODE_LEGACY
    ).toString().trim().ifBlank { "탐험 장소" }

    private fun placeKey(name: String, address: String, point: GeoPoint): String =
        "$name|$address|${"%.5f".format(Locale.US, point.latitude)}|${"%.5f".format(Locale.US, point.longitude)}"
            .lowercase(Locale.KOREA)
            .hashCode()
            .toUInt()
            .toString(16)

    private companion object {
        const val MINIMUM_DISTANCE_M = 120.0
        const val RECENT_VISIT_WINDOW_MILLIS = 30L * 24 * 60 * 60 * 1000
    }
}
