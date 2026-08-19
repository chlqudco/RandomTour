package com.chlqudco.randomtour

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.max

class RandomTourViewModel(application: Application) : AndroidViewModel(application) {
    private val store = ExplorationStore(application)
    private val locationRepository = LocationRepository(application)
    private val candidateRepository = CandidateRepository(
        context = application,
        store = store
    )
    private val arrivalDetector = ArrivalDetector()
    private val initialSettings = store.loadSettings()
    private val _uiState = MutableStateFlow(
        AppUiState(
            selectedRadiusM = initialSettings.radiusM,
            selectedMode = initialSettings.mode,
            selectedDifficulty = initialSettings.difficulty,
            excludedCategories = initialSettings.excludedCategories,
            history = store.loadHistory()
        )
    )
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    private var drawJob: Job? = null
    private var mapSearchTimeoutJob: Job? = null
    private var pendingMapSearch: PendingMapSearch? = null
    private var drawRequestId = 0
    private var hostStarted = false

    fun openSetup() {
        _uiState.update {
            it.copy(
                screen = AppScreen.SETUP,
                permissionDenied = false,
                drawError = null
            )
        }
    }

    fun openHistory() {
        _uiState.update { it.copy(screen = AppScreen.HISTORY, history = store.loadHistory()) }
    }

    fun openSettings() {
        val settings = store.loadSettings()
        _uiState.update {
            it.copy(
                screen = AppScreen.SETTINGS,
                selectedRadiusM = settings.radiusM,
                selectedMode = settings.mode,
                selectedDifficulty = settings.difficulty,
                excludedCategories = settings.excludedCategories
            )
        }
    }

    fun selectRadius(radiusM: Int) {
        _uiState.update { it.copy(selectedRadiusM = radiusM) }
    }

    fun selectMode(mode: ExplorationMode) {
        _uiState.update { it.copy(selectedMode = mode) }
    }

    fun selectDifficulty(difficulty: HintDifficulty) {
        _uiState.update { it.copy(selectedDifficulty = difficulty) }
    }

    fun toggleExcludedCategory(category: String) {
        _uiState.update { state ->
            state.copy(
                excludedCategories = if (category in state.excludedCategories) {
                    state.excludedCategories - category
                } else {
                    state.excludedCategories + category
                }
            )
        }
    }

    fun setPermissionDenied(denied: Boolean) {
        _uiState.update { it.copy(permissionDenied = denied) }
    }

    fun saveSettings() {
        val state = _uiState.value
        store.saveSettings(
            UserSettings(
                radiusM = state.selectedRadiusM,
                mode = state.selectedMode,
                difficulty = state.selectedDifficulty,
                excludedCategories = state.excludedCategories
            )
        )
        goHome()
    }

    fun drawDestination() {
        drawJob?.cancel()
        mapSearchTimeoutJob?.cancel()
        pendingMapSearch = null
        drawRequestId += 1
        val requestId = drawRequestId
        locationRepository.stopUpdates()
        _uiState.update {
            it.copy(
                screen = AppScreen.DRAW,
                drawStage = DrawStage.LOCATING,
                drawError = null,
                areaLabel = "",
                candidateCount = 0,
                mapSearchRequest = 0,
                startLocation = null,
                currentLocation = null,
                destination = null,
                hintRevealed = false,
                trackingMessage = null
            )
        }
        drawJob = viewModelScope.launch {
            if (!locationRepository.hasLocationPermission()) {
                _uiState.update { it.copy(drawError = "탐험을 시작하려면 위치 권한이 필요해요") }
                return@launch
            }
            if (!locationRepository.hasPreciseLocationPermission()) {
                _uiState.update { it.copy(drawError = "안정적인 후보 검색과 도착 판정을 위해 정확한 위치를 허용해 주세요") }
                return@launch
            }
            if (!locationRepository.isLocationEnabled()) {
                _uiState.update { it.copy(drawError = "기기의 위치 서비스를 켜고 다시 시도해 주세요") }
                return@launch
            }
            val location = locationRepository.currentLocation()
            if (location == null) {
                _uiState.update { it.copy(drawError = "현재 위치를 확인하지 못했어요. 열린 공간에서 다시 시도해 주세요") }
                return@launch
            }
            _uiState.update { it.copy(startLocation = location, currentLocation = location) }
            val state = _uiState.value
            try {
                val result = candidateRepository.searchOpenData(
                    origin = location.point,
                    radiusM = state.selectedRadiusM,
                    mode = state.selectedMode
                ) { stage ->
                    if (requestId == drawRequestId) {
                        _uiState.update { current -> current.copy(drawStage = stage) }
                    }
                }
                if (requestId != drawRequestId) return@launch
                if (canDrawWithoutMap(result, state.selectedMode)) {
                    finishDraw(requestId, result, location)
                } else {
                    pendingMapSearch = PendingMapSearch(
                        requestId = requestId,
                        location = location,
                        radiusM = state.selectedRadiusM,
                        mode = state.selectedMode,
                        openDataResult = result
                    )
                    _uiState.update {
                        it.copy(
                            drawStage = DrawStage.MAP_SEARCHING,
                            areaLabel = result.areaLabel,
                            candidateCount = result.candidates.size,
                            mapSearchRequest = requestId
                        )
                    }
                    startMapSearchTimeout(requestId)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                if (requestId != drawRequestId) return@launch
                _uiState.update {
                    it.copy(
                        drawError = error.message ?: "목적지를 뽑는 중 문제가 생겼어요"
                    )
                }
            }
        }
    }

    fun submitMapCandidates(requestId: Int, symbols: List<MapSymbolCandidate>) {
        val pending = pendingMapSearch?.takeIf { it.requestId == requestId } ?: return
        pendingMapSearch = null
        mapSearchTimeoutJob?.cancel()
        drawJob = viewModelScope.launch {
            try {
                _uiState.update { it.copy(drawStage = DrawStage.FILTERING) }
                val result = candidateRepository.mergeMapSymbols(
                    origin = pending.location.point,
                    radiusM = pending.radiusM,
                    mode = pending.mode,
                    areaLabel = pending.openDataResult.areaLabel,
                    openDataCandidates = pending.openDataResult.candidates,
                    symbols = symbols
                )
                finishDraw(requestId, result, pending.location)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                if (requestId == drawRequestId) {
                    _uiState.update {
                        it.copy(drawError = error.message ?: "지도 장소를 확인하는 중 문제가 생겼어요")
                    }
                }
            }
        }
    }

    private fun canDrawWithoutMap(
        result: CandidateSearchResult,
        mode: ExplorationMode
    ): Boolean = when (mode) {
        ExplorationMode.RANDOM -> result.candidates.size >= MINIMUM_RANDOM_CANDIDATES
        else -> result.candidates.isNotEmpty()
    }

    private fun startMapSearchTimeout(requestId: Int) {
        mapSearchTimeoutJob = viewModelScope.launch {
            delay(MAP_SEARCH_TIMEOUT_MILLIS)
            val pending = pendingMapSearch?.takeIf { it.requestId == requestId } ?: return@launch
            pendingMapSearch = null
            if (pending.openDataResult.candidates.isNotEmpty()) {
                finishDraw(requestId, pending.openDataResult, pending.location)
            } else if (requestId == drawRequestId) {
                _uiState.update {
                    it.copy(
                        drawStage = DrawStage.FILTERING,
                        mapSearchRequest = 0,
                        drawError = "공개 장소 데이터와 NAVER 지도에서 반경 안의 후보를 찾지 못했어요"
                    )
                }
            }
        }
    }

    private suspend fun finishDraw(
        requestId: Int,
        result: CandidateSearchResult,
        location: LocationSnapshot
    ) {
        if (requestId != drawRequestId) return
        val selected = result.candidates.randomOrNull()
        if (selected == null) {
            _uiState.update {
                it.copy(
                    drawStage = DrawStage.FILTERING,
                    mapSearchRequest = 0,
                    drawError = "공개 장소 데이터와 NAVER 지도에서 반경 안의 후보를 찾지 못했어요"
                )
            }
            return
        }
        val destination = candidateRepository.enrichAddress(selected)
        if (requestId != drawRequestId) return
        _uiState.update {
            it.copy(
                drawStage = DrawStage.READY,
                areaLabel = result.areaLabel,
                candidateCount = result.candidates.size,
                mapSearchRequest = 0,
                destination = destination,
                remainingDistanceM = destination.distanceFromStartM,
                targetBearingDegrees = ExplorationMath.bearingDegrees(
                    location.point,
                    destination.point
                ),
                temperature = ExplorationMath.temperature(destination.distanceFromStartM)
            )
        }
    }

    fun retryDraw() {
        drawDestination()
    }

    fun retryWithWiderRadius() {
        val widerRadius = when (_uiState.value.selectedRadiusM) {
            in 0..500 -> 1000
            in 501..1000 -> 2000
            else -> 2000
        }
        selectRadius(widerRadius)
        drawDestination()
    }

    fun beginExploration() {
        val state = _uiState.value
        val destination = state.destination ?: return
        val current = state.currentLocation ?: state.startLocation ?: return
        val distance = ExplorationMath.distanceMeters(current.point, destination.point)
        arrivalDetector.reset()
        _uiState.update {
            it.copy(
                screen = AppScreen.EXPLORATION,
                currentLocation = current,
                remainingDistanceM = distance,
                targetBearingDegrees = ExplorationMath.bearingDegrees(current.point, destination.point),
                temperature = ExplorationMath.temperature(distance),
                walkedDistanceM = 0.0,
                explorationStartedAt = System.currentTimeMillis(),
                explorationPaused = !hostStarted,
                trackingMessage = null,
                arrivalCompleted = false
            )
        }
        if (hostStarted) startTracking()
    }

    fun revealHint() {
        _uiState.update { it.copy(hintRevealed = true) }
    }

    fun giveUp(revealDestination: Boolean) {
        locationRepository.stopUpdates()
        if (revealDestination && _uiState.value.destination != null) {
            _uiState.update {
                it.copy(
                    screen = AppScreen.ARRIVAL,
                    arrivalCompleted = false,
                    explorationPaused = false,
                    trackingMessage = null
                )
            }
        } else {
            goHome()
        }
    }

    fun clearHistory() {
        store.clearHistory()
        _uiState.update { it.copy(history = emptyList()) }
    }

    fun goHome() {
        drawRequestId += 1
        drawJob?.cancel()
        mapSearchTimeoutJob?.cancel()
        pendingMapSearch = null
        locationRepository.stopUpdates()
        arrivalDetector.reset()
        _uiState.update {
            it.copy(
                screen = AppScreen.HOME,
                drawError = null,
                destination = null,
                startLocation = null,
                currentLocation = null,
                explorationStartedAt = null,
                explorationPaused = false,
                trackingMessage = null,
                hintRevealed = false,
                history = store.loadHistory()
            )
        }
    }

    fun backToSetup() {
        drawRequestId += 1
        drawJob?.cancel()
        mapSearchTimeoutJob?.cancel()
        pendingMapSearch = null
        _uiState.update { it.copy(screen = AppScreen.SETUP, drawError = null) }
    }

    fun onHostStarted() {
        hostStarted = true
        if (_uiState.value.screen == AppScreen.EXPLORATION) {
            _uiState.update { it.copy(explorationPaused = false) }
            startTracking()
        }
    }

    fun onHostStopped() {
        hostStarted = false
        locationRepository.stopUpdates()
        if (_uiState.value.screen == AppScreen.EXPLORATION) {
            _uiState.update { it.copy(explorationPaused = true) }
        }
    }

    private fun startTracking() {
        locationRepository.startUpdates(
            onLocation = ::handleLocation,
            onError = { message ->
                _uiState.update {
                    it.copy(
                        trackingMessage = message,
                        explorationPaused = true
                    )
                }
            }
        )
    }

    private fun handleLocation(location: LocationSnapshot) {
        val state = _uiState.value
        if (state.screen != AppScreen.EXPLORATION) return
        val destination = state.destination ?: return
        val previous = state.currentLocation
        val segmentDistance = if (previous != null) {
            ExplorationMath.distanceMeters(previous.point, location.point)
        } else {
            0.0
        }
        val walkedDistance = if (
            location.accuracyM <= 80f &&
            previous?.accuracyM?.let { it <= 80f } != false &&
            segmentDistance in 1.0..150.0
        ) {
            state.walkedDistanceM + segmentDistance
        } else {
            state.walkedDistanceM
        }
        val remainingDistance = ExplorationMath.distanceMeters(location.point, destination.point)
        val arrived = arrivalDetector.update(remainingDistance, location.accuracyM)
        _uiState.update {
            it.copy(
                currentLocation = location,
                remainingDistanceM = remainingDistance,
                targetBearingDegrees = ExplorationMath.bearingDegrees(location.point, destination.point),
                temperature = ExplorationMath.temperature(remainingDistance),
                walkedDistanceM = max(0.0, walkedDistance),
                gpsWeak = location.accuracyM > 80f,
                explorationPaused = false,
                trackingMessage = null
            )
        }
        if (arrived) completeExploration()
    }

    private fun completeExploration() {
        val state = _uiState.value
        val destination = state.destination ?: return
        val startedAt = state.explorationStartedAt ?: System.currentTimeMillis()
        val endedAt = System.currentTimeMillis()
        val record = ExplorationRecord(
            id = endedAt.toString(),
            endedAt = endedAt,
            radiusM = state.selectedRadiusM,
            mode = state.selectedMode,
            destination = destination,
            durationSeconds = ((endedAt - startedAt) / 1000).coerceAtLeast(0),
            walkedDistanceM = state.walkedDistanceM
        )
        locationRepository.stopUpdates()
        store.addRecord(record)
        _uiState.update {
            it.copy(
                screen = AppScreen.ARRIVAL,
                arrivalCompleted = true,
                explorationPaused = false,
                trackingMessage = null,
                history = store.loadHistory()
            )
        }
    }

    override fun onCleared() {
        mapSearchTimeoutJob?.cancel()
        locationRepository.stopUpdates()
    }

    private data class PendingMapSearch(
        val requestId: Int,
        val location: LocationSnapshot,
        val radiusM: Int,
        val mode: ExplorationMode,
        val openDataResult: CandidateSearchResult
    )

    private companion object {
        const val MINIMUM_RANDOM_CANDIDATES = 5
        const val MAP_SEARCH_TIMEOUT_MILLIS = 12_000L
    }
}
