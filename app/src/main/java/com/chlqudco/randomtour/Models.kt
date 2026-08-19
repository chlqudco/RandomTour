package com.chlqudco.randomtour

enum class AppScreen {
    HOME,
    SETUP,
    DRAW,
    EXPLORATION,
    ARRIVAL,
    HISTORY,
    SETTINGS
}

enum class ExplorationMode(
    val label: String,
    val description: String,
    val symbol: String,
    val queries: List<String>
) {
    RANDOM(
        "완전 랜덤",
        "무엇이 나올지 모르는 탐험",
        "?",
        listOf("카페", "맛집", "베이커리", "공원", "서점", "전시", "소품샵", "시장")
    ),
    CAFE(
        "카페",
        "커피와 디저트를 찾아서",
        "C",
        listOf("카페", "커피", "디저트", "베이커리")
    ),
    FOOD(
        "먹거리",
        "근처의 한 끼를 우연히",
        "F",
        listOf("맛집", "분식", "한식", "일식", "중식", "베이커리")
    ),
    WALK(
        "산책",
        "공원과 걷기 좋은 장소",
        "W",
        listOf("공원", "산책로", "광장", "정원", "전망대")
    ),
    CULTURE(
        "문화",
        "책과 전시, 작은 발견",
        "A",
        listOf("서점", "전시", "갤러리", "박물관", "공방")
    )
}

enum class HintDifficulty(val label: String, val description: String) {
    EASY("쉬움", "거리·방향·카테고리"),
    NORMAL("보통", "거리·방향, 카테고리는 힌트"),
    HARD("하드코어", "거리 중심, 가까워지면 방향 공개")
}

enum class TemperatureHint(val label: String, val message: String) {
    COLD("COLD", "아직 멀어요"),
    COOL("COOL", "조금씩 가까워지고 있어요"),
    WARM("WARM", "좋아요, 방향이 맞아요"),
    HOT("HOT", "거의 다 왔어요"),
    VERY_HOT("VERY HOT", "주변을 천천히 살펴보세요")
}

enum class DrawStage(val message: String) {
    LOCATING("현재 위치를 확인하고 있어요"),
    RESOLVING_AREA("동네 이름을 찾고 있어요"),
    SEARCHING("주변 장소를 모으고 있어요"),
    FILTERING("반경 안 후보를 고르고 있어요"),
    READY("목적지를 뽑았어요")
}

data class GeoPoint(
    val latitude: Double,
    val longitude: Double
)

data class LocationSnapshot(
    val point: GeoPoint,
    val accuracyM: Float,
    val bearingDegrees: Float?,
    val timestamp: Long
)

data class Destination(
    val placeKey: String,
    val name: String,
    val category: String,
    val point: GeoPoint,
    val roadAddress: String,
    val distanceFromStartM: Double
)

data class CandidateSearchResult(
    val areaLabel: String,
    val candidates: List<Destination>,
    val usedBackend: Boolean
)

data class ExplorationRecord(
    val id: String,
    val endedAt: Long,
    val radiusM: Int,
    val mode: ExplorationMode,
    val destination: Destination,
    val durationSeconds: Long,
    val walkedDistanceM: Double
)

data class UserSettings(
    val radiusM: Int = 1000,
    val mode: ExplorationMode = ExplorationMode.RANDOM,
    val difficulty: HintDifficulty = HintDifficulty.NORMAL,
    val excludedCategories: Set<String> = emptySet()
)

data class AppUiState(
    val screen: AppScreen = AppScreen.HOME,
    val selectedRadiusM: Int = 1000,
    val selectedMode: ExplorationMode = ExplorationMode.RANDOM,
    val selectedDifficulty: HintDifficulty = HintDifficulty.NORMAL,
    val excludedCategories: Set<String> = emptySet(),
    val drawStage: DrawStage = DrawStage.LOCATING,
    val drawError: String? = null,
    val areaLabel: String = "",
    val candidateCount: Int = 0,
    val usingDeviceSearch: Boolean = false,
    val startLocation: LocationSnapshot? = null,
    val currentLocation: LocationSnapshot? = null,
    val destination: Destination? = null,
    val remainingDistanceM: Double = 0.0,
    val targetBearingDegrees: Double = 0.0,
    val temperature: TemperatureHint = TemperatureHint.COLD,
    val walkedDistanceM: Double = 0.0,
    val explorationStartedAt: Long? = null,
    val hintRevealed: Boolean = false,
    val gpsWeak: Boolean = false,
    val explorationPaused: Boolean = false,
    val trackingMessage: String? = null,
    val permissionDenied: Boolean = false,
    val arrivalCompleted: Boolean = false,
    val history: List<ExplorationRecord> = emptyList()
)
