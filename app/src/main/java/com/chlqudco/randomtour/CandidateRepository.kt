package com.chlqudco.randomtour

import android.content.Context
import android.location.Address
import android.location.Geocoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.URLEncoder
import java.util.Locale

private class OverpassRateLimitException : IOException()

class CandidateRepository(
    private val context: Context,
    private val store: ExplorationStore
) {
    suspend fun searchOpenData(
        origin: GeoPoint,
        radiusM: Int,
        mode: ExplorationMode,
        onStage: (DrawStage) -> Unit
    ): CandidateSearchResult = withContext(Dispatchers.IO) {
        val constraints = loadConstraints()
        onStage(DrawStage.RESOLVING_AREA)
        val areaLabel = resolveArea(origin) ?: "내 주변"
        onStage(DrawStage.SEARCHING)
        val candidates = fetchOpenStreetMap(origin, radiusM, mode)
        onStage(DrawStage.FILTERING)
        CandidateSearchResult(
            areaLabel = areaLabel,
            candidates = filterCandidates(
                candidates = candidates,
                origin = origin,
                radiusM = radiusM,
                recentKeys = constraints.recentKeys,
                excludedCategories = constraints.excludedCategories
            )
        )
    }

    suspend fun mergeMapSymbols(
        origin: GeoPoint,
        radiusM: Int,
        mode: ExplorationMode,
        areaLabel: String,
        openDataCandidates: List<Destination>,
        symbols: List<MapSymbolCandidate>
    ): CandidateSearchResult = withContext(Dispatchers.IO) {
        val constraints = loadConstraints()
        val mapCandidates = symbols
            .asSequence()
            .mapNotNull { it.toDestination(origin, mode) }
            .let { sequence ->
                val all = sequence.toList()
                val matched = all.filter { matchesMode(it.name, mode) }
                matched.ifEmpty { all }
            }
        CandidateSearchResult(
            areaLabel = areaLabel,
            candidates = filterCandidates(
                candidates = openDataCandidates + mapCandidates,
                origin = origin,
                radiusM = radiusM,
                recentKeys = constraints.recentKeys,
                excludedCategories = constraints.excludedCategories
            )
        )
    }

    suspend fun enrichAddress(destination: Destination): Destination = withContext(Dispatchers.IO) {
        if (destination.roadAddress.isNotBlank() || !Geocoder.isPresent()) {
            return@withContext destination
        }
        val address = runCatching {
            Geocoder(context, Locale.KOREA).findAddresses(destination.point, 1).firstOrNull()
        }.getOrNull()
        destination.copy(roadAddress = address?.displayAddress().orEmpty())
    }

    private fun loadConstraints(): SearchConstraints {
        val recentKeys = store.loadHistory()
            .filter { it.endedAt >= System.currentTimeMillis() - RECENT_VISIT_WINDOW_MILLIS }
            .map { it.destination.placeKey }
            .toSet()
        return SearchConstraints(
            recentKeys = recentKeys,
            excludedCategories = store.loadSettings().excludedCategories
        )
    }

    private fun fetchOpenStreetMap(
        origin: GeoPoint,
        radiusM: Int,
        mode: ExplorationMode
    ): List<Destination> {
        val query = OverpassQueryBuilder.build(origin, radiusM, mode)
        for (endpoint in OVERPASS_ENDPOINTS) {
            val candidates = try {
                parseOverpassResponse(executeOverpassRequest(endpoint, query), origin)
            } catch (_: OverpassRateLimitException) {
                return emptyList()
            } catch (_: SocketTimeoutException) {
                return emptyList()
            } catch (_: Exception) {
                null
            }
            if (candidates != null) {
                return candidates
            }
        }
        return emptyList()
    }

    private fun executeOverpassRequest(endpoint: String, query: String): String {
        val connection = URL(endpoint).openConnection() as HttpURLConnection
        return try {
            val body = "data=${URLEncoder.encode(query, Charsets.UTF_8.name())}"
                .toByteArray(Charsets.UTF_8)
            connection.requestMethod = "POST"
            connection.connectTimeout = 6_000
            connection.readTimeout = 10_000
            connection.doOutput = true
            connection.setFixedLengthStreamingMode(body.size)
            connection.setRequestProperty(
                "Content-Type",
                "application/x-www-form-urlencoded; charset=UTF-8"
            )
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty(
                "User-Agent",
                "RandomTour/1.0 Android (com.chlqudco.randomtour)"
            )
            connection.outputStream.use { it.write(body) }
            val responseCode = connection.responseCode
            if (responseCode == 429) {
                throw OverpassRateLimitException()
            }
            if (responseCode !in 200..299) {
                throw IOException("Overpass HTTP $responseCode")
            }
            connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    private fun parseOverpassResponse(response: String, origin: GeoPoint): List<Destination> {
        val elements = JSONObject(response).optJSONArray("elements") ?: return emptyList()
        return buildList {
            for (index in 0 until elements.length()) {
                val element = elements.optJSONObject(index) ?: continue
                val tags = element.optJSONObject("tags") ?: continue
                val name = sequenceOf(
                    tags.optString("name:ko"),
                    tags.optString("name"),
                    tags.optString("brand:ko"),
                    tags.optString("brand")
                ).firstOrNull { it.isNotBlank() }?.trim() ?: continue
                val point = element.point() ?: continue
                val type = element.optString("type", "element")
                val id = element.optLong("id", 0L)
                add(
                    Destination(
                        placeKey = "osm:$type:$id",
                        name = name,
                        category = tags.category(),
                        point = point,
                        roadAddress = tags.address(),
                        distanceFromStartM = ExplorationMath.distanceMeters(origin, point),
                        source = CandidateSource.OPENSTREETMAP
                    )
                )
            }
        }
    }

    private fun JSONObject.point(): GeoPoint? {
        val coordinateSource = if (has("lat") && has("lon")) this else optJSONObject("center")
            ?: return null
        val latitude = coordinateSource.optDouble("lat", Double.NaN)
        val longitude = coordinateSource.optDouble("lon", Double.NaN)
        if (!latitude.isFinite() || !longitude.isFinite()) return null
        return GeoPoint(latitude, longitude)
    }

    private fun JSONObject.category(): String = when {
        optString("amenity") == "cafe" -> "카페"
        optString("shop") in setOf("bakery", "confectionery", "coffee") -> "카페·베이커리"
        optString("amenity") in setOf("restaurant", "fast_food", "food_court") -> "음식점"
        optString("amenity") == "marketplace" -> "시장"
        optString("leisure") in setOf("park", "garden", "nature_reserve") -> "공원·산책"
        optString("tourism") == "viewpoint" -> "전망대"
        optString("place") == "square" -> "광장"
        optString("tourism") == "museum" -> "박물관"
        optString("tourism") == "gallery" -> "전시·갤러리"
        optString("historic") in setOf("monument", "memorial") -> "기념물"
        optString("amenity") == "library" || optString("shop") == "books" -> "도서·문화"
        optString("amenity") == "arts_centre" -> "전시·문화 공간"
        optString("amenity") in setOf("theatre", "cinema", "community_centre") -> "문화 공간"
        optString("tourism") == "attraction" -> "관광 명소"
        optString("shop") == "gift" -> "소품샵"
        optString("amenity").isNotBlank() -> optString("amenity")
        optString("tourism").isNotBlank() -> optString("tourism")
        optString("leisure").isNotBlank() -> optString("leisure")
        optString("shop").isNotBlank() -> optString("shop")
        else -> "장소"
    }

    private fun JSONObject.address(): String {
        optString("addr:full").takeIf { it.isNotBlank() }?.let { return it }
        return listOf(
            optString("addr:province"),
            optString("addr:city"),
            optString("addr:district"),
            optString("addr:subdistrict"),
            optString("addr:street"),
            optString("addr:housenumber")
        ).filter { it.isNotBlank() }.joinToString(" ")
    }

    private fun MapSymbolCandidate.toDestination(
        origin: GeoPoint,
        mode: ExplorationMode
    ): Destination? {
        val normalizedName = name.trim().replace(Regex("\\s+"), " ")
        if (!isLikelyDestination(normalizedName)) return null
        return Destination(
            placeKey = mapPlaceKey(normalizedName, point),
            name = normalizedName,
            category = inferredMapCategory(normalizedName, mode),
            point = point,
            roadAddress = "",
            distanceFromStartM = ExplorationMath.distanceMeters(origin, point),
            source = CandidateSource.NAVER_MAP
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
            "${normalizedName}|${"%.4f".format(Locale.US, it.point.latitude)}|${"%.4f".format(Locale.US, it.point.longitude)}"
        }
        .toList()

    private fun isLikelyDestination(name: String): Boolean {
        if (name.length !in 2..60 || name.none { it.isLetter() }) return false
        if (REJECTED_SYMBOL_WORDS.any { name.contains(it, ignoreCase = true) }) return false
        if (ROAD_NAME_PATTERN.matches(name)) return false
        return true
    }

    private fun matchesMode(name: String, mode: ExplorationMode): Boolean {
        if (mode == ExplorationMode.RANDOM) return true
        return MODE_KEYWORDS.getValue(mode).any { name.contains(it, ignoreCase = true) }
    }

    private fun inferredMapCategory(name: String, mode: ExplorationMode): String = when {
        name.contains("시장") -> "시장"
        name.contains("박물관") -> "박물관"
        listOf("전시", "갤러리", "미술관").any { name.contains(it) } -> "전시·갤러리"
        MODE_KEYWORDS.getValue(ExplorationMode.CAFE).any { name.contains(it, true) } -> "카페·베이커리"
        MODE_KEYWORDS.getValue(ExplorationMode.FOOD).any { name.contains(it, true) } -> "먹거리"
        MODE_KEYWORDS.getValue(ExplorationMode.WALK).any { name.contains(it, true) } -> "공원·산책"
        MODE_KEYWORDS.getValue(ExplorationMode.CULTURE).any { name.contains(it, true) } -> "문화 공간"
        mode == ExplorationMode.FOOD -> "먹거리"
        mode != ExplorationMode.RANDOM -> mode.label
        else -> "지도 장소"
    }

    private fun mapPlaceKey(name: String, point: GeoPoint): String =
        "naver-symbol|$name|${"%.5f".format(Locale.US, point.latitude)}|${"%.5f".format(Locale.US, point.longitude)}"
            .lowercase(Locale.KOREA)
            .hashCode()
            .toUInt()
            .toString(16)
            .let { "naver:$it" }

    private fun resolveArea(origin: GeoPoint): String? {
        if (!Geocoder.isPresent()) return null
        return runCatching {
            Geocoder(context, Locale.KOREA)
                .findAddresses(origin, 3)
                .firstOrNull()
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
        }.getOrNull()
    }

    @Suppress("DEPRECATION")
    private fun Geocoder.findAddresses(point: GeoPoint, maxResults: Int): List<Address> =
        getFromLocation(point.latitude, point.longitude, maxResults).orEmpty()

    private fun Address.displayAddress(): String =
        if (maxAddressLineIndex >= 0) getAddressLine(0).orEmpty() else listOfNotNull(
            adminArea,
            locality,
            subLocality,
            thoroughfare,
            featureName
        ).distinct().joinToString(" ")

    private data class SearchConstraints(
        val recentKeys: Set<String>,
        val excludedCategories: Set<String>
    )

    private companion object {
        const val MINIMUM_DISTANCE_M = 120.0
        const val RECENT_VISIT_WINDOW_MILLIS = 30L * 24 * 60 * 60 * 1000
        val OVERPASS_ENDPOINTS = listOf(
            "https://overpass-api.de/api/interpreter",
            "https://overpass.kumi.systems/api/interpreter"
        )
        val REJECTED_SYMBOL_WORDS = setOf(
            "아파트",
            "오피스텔",
            "주차장",
            "정류장",
            "교차로",
            "나들목",
            "분기점",
            "톨게이트",
            "초등학교",
            "중학교",
            "고등학교"
        )
        val ROAD_NAME_PATTERN = Regex(".*(대로|고속도로|로|길)$")
        val MODE_KEYWORDS = mapOf(
            ExplorationMode.RANDOM to emptyList(),
            ExplorationMode.CAFE to listOf(
                "카페", "커피", "베이커리", "제과", "디저트", "스타벅스", "투썸", "이디야",
                "메가MGC", "컴포즈", "빽다방", "폴바셋", "커피빈", "할리스", "엔제리너스"
            ),
            ExplorationMode.FOOD to listOf(
                "식당", "음식", "키친", "치킨", "피자", "버거", "국밥", "냉면", "분식", "김밥",
                "떡볶이", "돈까스", "고기", "갈비", "횟집", "스시", "초밥", "라멘", "국수", "카츠",
                "포차", "맥도날드", "롯데리아", "버거킹", "맘스터치", "교촌", "BBQ", "BHC"
            ),
            ExplorationMode.WALK to listOf("공원", "정원", "광장", "산책", "둘레길", "전망대", "수목원"),
            ExplorationMode.CULTURE to listOf(
                "도서관", "서점", "박물관", "미술관", "갤러리", "극장", "공연", "문화", "전시", "공방"
            )
        )
    }
}

internal object OverpassQueryBuilder {
    fun build(origin: GeoPoint, radiusM: Int, mode: ExplorationMode): String {
        val selectors = when (mode) {
            ExplorationMode.RANDOM -> listOf(
                "amenity" to "cafe",
                "amenity" to "restaurant",
                "amenity" to "fast_food",
                "amenity" to "food_court",
                "amenity" to "library",
                "amenity" to "arts_centre",
                "amenity" to "theatre",
                "amenity" to "cinema",
                "amenity" to "marketplace",
                "leisure" to "park",
                "leisure" to "garden",
                "leisure" to "nature_reserve",
                "tourism" to "attraction",
                "tourism" to "museum",
                "tourism" to "gallery",
                "tourism" to "viewpoint",
                "shop" to "bakery",
                "shop" to "confectionery",
                "shop" to "coffee",
                "shop" to "books",
                "shop" to "gift",
                "historic" to "monument",
                "historic" to "memorial"
            )
            ExplorationMode.CAFE -> listOf(
                "amenity" to "cafe",
                "shop" to "bakery",
                "shop" to "confectionery",
                "shop" to "coffee"
            )
            ExplorationMode.FOOD -> listOf(
                "amenity" to "restaurant",
                "amenity" to "fast_food",
                "amenity" to "food_court",
                "shop" to "bakery"
            )
            ExplorationMode.WALK -> listOf(
                "leisure" to "park",
                "leisure" to "garden",
                "leisure" to "nature_reserve",
                "tourism" to "viewpoint",
                "place" to "square"
            )
            ExplorationMode.CULTURE -> listOf(
                "amenity" to "library",
                "amenity" to "arts_centre",
                "amenity" to "theatre",
                "amenity" to "cinema",
                "amenity" to "community_centre",
                "tourism" to "museum",
                "tourism" to "gallery",
                "historic" to "monument",
                "historic" to "memorial",
                "shop" to "books"
            )
        }
        val latitude = "%.6f".format(Locale.US, origin.latitude)
        val longitude = "%.6f".format(Locale.US, origin.longitude)
        return buildString {
            append("[out:json][timeout:10];(")
            selectors.forEach { (key, value) ->
                append("nwr[\"")
                append(key)
                append("\"=\"")
                append(value)
                append("\"](around:")
                append(radiusM)
                append(',')
                append(latitude)
                append(',')
                append(longitude)
                append(");")
            }
            append(");out center tags qt 100;")
        }
    }
}
