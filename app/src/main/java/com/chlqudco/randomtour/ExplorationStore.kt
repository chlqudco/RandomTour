package com.chlqudco.randomtour

import android.content.Context
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject

class ExplorationStore(context: Context) {
    private val preferences = context.getSharedPreferences("random_tour", Context.MODE_PRIVATE)

    fun loadSettings(): UserSettings = UserSettings(
        radiusM = preferences.getInt("radius_m", 1000),
        mode = enumValueOrDefault(
            preferences.getString("mode", null),
            ExplorationMode.RANDOM
        ),
        difficulty = enumValueOrDefault(
            preferences.getString("difficulty", null),
            HintDifficulty.NORMAL
        ),
        excludedCategories = preferences.getStringSet("excluded_categories", emptySet()).orEmpty()
    )

    fun saveSettings(settings: UserSettings) {
        preferences.edit {
            putInt("radius_m", settings.radiusM)
            putString("mode", settings.mode.name)
            putString("difficulty", settings.difficulty.name)
            putStringSet("excluded_categories", settings.excludedCategories)
        }
    }

    fun loadHistory(): List<ExplorationRecord> = runCatching {
        val array = JSONArray(preferences.getString("history", "[]"))
        buildList {
            for (index in 0 until array.length()) {
                add(array.getJSONObject(index).toRecord())
            }
        }.sortedByDescending { it.endedAt }
    }.getOrDefault(emptyList())

    fun addRecord(record: ExplorationRecord) {
        val updated = (listOf(record) + loadHistory()).distinctBy { it.id }.take(100)
        saveHistory(updated)
    }

    fun clearHistory() {
        preferences.edit { remove("history") }
    }

    private fun saveHistory(records: List<ExplorationRecord>) {
        val array = JSONArray()
        records.forEach { array.put(it.toJson()) }
        preferences.edit { putString("history", array.toString()) }
    }

    private fun ExplorationRecord.toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("endedAt", endedAt)
        .put("radiusM", radiusM)
        .put("mode", mode.name)
        .put("durationSeconds", durationSeconds)
        .put("walkedDistanceM", walkedDistanceM)
        .put("destination", destination.toJson())

    private fun Destination.toJson(): JSONObject = JSONObject()
        .put("placeKey", placeKey)
        .put("name", name)
        .put("category", category)
        .put("latitude", point.latitude)
        .put("longitude", point.longitude)
        .put("roadAddress", roadAddress)
        .put("distanceFromStartM", distanceFromStartM)

    private fun JSONObject.toRecord(): ExplorationRecord = ExplorationRecord(
        id = getString("id"),
        endedAt = getLong("endedAt"),
        radiusM = getInt("radiusM"),
        mode = enumValueOrDefault(optString("mode"), ExplorationMode.RANDOM),
        destination = getJSONObject("destination").toDestination(),
        durationSeconds = optLong("durationSeconds"),
        walkedDistanceM = optDouble("walkedDistanceM")
    )

    private fun JSONObject.toDestination(): Destination = Destination(
        placeKey = getString("placeKey"),
        name = getString("name"),
        category = optString("category", "장소"),
        point = GeoPoint(getDouble("latitude"), getDouble("longitude")),
        roadAddress = optString("roadAddress"),
        distanceFromStartM = optDouble("distanceFromStartM")
    )

    private inline fun <reified T : Enum<T>> enumValueOrDefault(value: String?, fallback: T): T =
        runCatching { enumValueOf<T>(value.orEmpty()) }.getOrDefault(fallback)
}
