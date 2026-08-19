package com.chlqudco.randomtour

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.chlqudco.randomtour.ui.theme.ExplorerBlue
import com.chlqudco.randomtour.ui.theme.ExplorerCream
import com.chlqudco.randomtour.ui.theme.ExplorerGreen
import com.chlqudco.randomtour.ui.theme.ExplorerMuted
import com.chlqudco.randomtour.ui.theme.ExplorerNavy
import com.chlqudco.randomtour.ui.theme.ExplorerOrange
import com.chlqudco.randomtour.ui.theme.ExplorerRed
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RandomTourApp(viewModel: RandomTourViewModel) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var permissionForDrawing by remember { mutableStateOf(false) }
    var showGiveUpDialog by remember { mutableStateOf(false) }

    fun hasPreciseLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true
        viewModel.setPermissionDenied(!granted)
        if (granted) {
            if (permissionForDrawing) viewModel.drawDestination() else viewModel.openSetup()
        }
        permissionForDrawing = false
    }

    fun requestLocationPermission(drawAfterGrant: Boolean) {
        if (hasPreciseLocationPermission()) {
            if (drawAfterGrant) viewModel.drawDestination() else viewModel.openSetup()
        } else {
            permissionForDrawing = drawAfterGrant
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    BackHandler(enabled = state.screen != AppScreen.HOME) {
        when (state.screen) {
            AppScreen.SETUP, AppScreen.HISTORY, AppScreen.SETTINGS, AppScreen.ARRIVAL -> viewModel.goHome()
            AppScreen.DRAW -> viewModel.backToSetup()
            AppScreen.EXPLORATION -> showGiveUpDialog = true
            AppScreen.HOME -> Unit
        }
    }

    when (state.screen) {
        AppScreen.HOME -> HomeScreen(
            state = state,
            onStart = { requestLocationPermission(false) },
            onHistory = viewModel::openHistory,
            onSettings = viewModel::openSettings,
            onOpenAppSettings = {
                context.startActivity(
                    Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", context.packageName, null)
                    )
                )
            }
        )

        AppScreen.SETUP -> SetupScreen(
            state = state,
            onBack = viewModel::goHome,
            onRadiusSelected = viewModel::selectRadius,
            onModeSelected = viewModel::selectMode,
            onDifficultySelected = viewModel::selectDifficulty,
            onDraw = { requestLocationPermission(true) }
        )

        AppScreen.DRAW -> DrawScreen(
            state = state,
            onBack = viewModel::backToSetup,
            onRetry = viewModel::retryDraw,
            onWiderRadius = viewModel::retryWithWiderRadius,
            onBegin = viewModel::beginExploration,
            onMapSymbolsCollected = viewModel::submitMapCandidates
        )

        AppScreen.EXPLORATION -> ExplorationScreen(
            state = state,
            onGiveUp = { showGiveUpDialog = true },
            onRevealHint = viewModel::revealHint
        )

        AppScreen.ARRIVAL -> ArrivalScreen(
            state = state,
            onHome = viewModel::goHome,
            onExploreAgain = viewModel::openSetup
        )

        AppScreen.HISTORY -> HistoryScreen(
            history = state.history,
            onBack = viewModel::goHome,
            onClear = viewModel::clearHistory
        )

        AppScreen.SETTINGS -> SettingsScreen(
            state = state,
            onBack = viewModel::goHome,
            onRadiusSelected = viewModel::selectRadius,
            onModeSelected = viewModel::selectMode,
            onDifficultySelected = viewModel::selectDifficulty,
            onExcludedCategoryToggle = viewModel::toggleExcludedCategory,
            onSave = viewModel::saveSettings
        )
    }

    if (showGiveUpDialog) {
        AlertDialog(
            onDismissRequest = { showGiveUpDialog = false },
            icon = { Icon(Icons.Rounded.Flag, contentDescription = null) },
            title = { Text("탐험을 그만둘까요?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("여기까지 온 것도 탐험이에요. 목적지를 확인하거나 계속 찾아갈 수 있어요.")
                    TextButton(
                        onClick = {
                            showGiveUpDialog = false
                            viewModel.giveUp(false)
                        },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("목적지를 보지 않고 종료")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showGiveUpDialog = false
                        viewModel.giveUp(true)
                    }
                ) {
                    Text("목적지 보기")
                }
            },
            dismissButton = {
                TextButton(onClick = { showGiveUpDialog = false }) {
                    Text("계속 탐험")
                }
            }
        )
    }
}

@Composable
private fun HomeScreen(
    state: AppUiState,
    onStart: () -> Unit,
    onHistory: () -> Unit,
    onSettings: () -> Unit,
    onOpenAppSettings: () -> Unit
) {
    val totalDistance = state.history.sumOf { it.walkedDistanceM }
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = ExplorerOrange,
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Explore, contentDescription = null, tint = Color.White)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("RANDOM TOUR", style = MaterialTheme.typography.labelLarge, color = ExplorerOrange)
                    Text("오늘은 어디로 갈까요?", style = MaterialTheme.typography.titleLarge)
                }
                IconButton(onClick = onSettings) {
                    Icon(Icons.Rounded.Settings, contentDescription = "설정")
                }
            }

            Surface(
                shape = RoundedCornerShape(32.dp),
                color = ExplorerNavy,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(310.dp)
            ) {
                Box(Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .size(190.dp)
                            .offset(x = 205.dp, y = (-34).dp)
                            .border(34.dp, Color.White.copy(alpha = 0.06f), CircleShape)
                    )
                    Box(
                        modifier = Modifier
                            .size(130.dp)
                            .offset(x = (-36).dp, y = 220.dp)
                            .border(25.dp, ExplorerOrange.copy(alpha = 0.18f), CircleShape)
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(26.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Surface(
                            color = Color.White.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(100.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Rounded.LocationOn,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(17.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text("내 주변 ${state.selectedRadiusM / 1000f}km", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                        Column {
                            Text(
                                "목적지는\n도착할 때까지 비밀",
                                style = MaterialTheme.typography.displaySmall,
                                color = Color.White
                            )
                            Spacer(Modifier.height(10.dp))
                            Text(
                                "거리와 방향만 믿고 걷는\n우리 동네 랜덤 탐험",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White.copy(alpha = 0.72f)
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(ExplorerOrange, CircleShape)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("지도에 목적지 핀과 경로는 표시하지 않아요", color = Color.White.copy(alpha = 0.76f), fontSize = 12.sp)
                        }
                    }
                }
            }

            if (state.permissionDenied) {
                InlineNotice(
                    title = "위치 권한이 필요해요",
                    message = "현재 위치 주변의 탐험 장소를 찾기 위해 탐험 중에만 위치를 사용해요.",
                    actionLabel = "앱 설정",
                    onAction = onOpenAppSettings,
                    error = true
                )
            }

            Button(
                onClick = onStart,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(62.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ExplorerOrange)
            ) {
                Icon(Icons.Rounded.Explore, contentDescription = null)
                Spacer(Modifier.width(10.dp))
                Text("탐험 시작", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.weight(1f))
                Icon(Icons.Rounded.ChevronRight, contentDescription = null)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard(
                    label = "누적 탐험",
                    value = "${state.history.size}회",
                    icon = { Icon(Icons.Rounded.Explore, contentDescription = null, tint = ExplorerOrange) },
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    label = "걸은 거리",
                    value = ExplorationMath.formatDistance(totalDistance),
                    icon = { Icon(Icons.AutoMirrored.Rounded.DirectionsWalk, contentDescription = null, tint = ExplorerGreen) },
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("최근 탐험", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                TextButton(onClick = onHistory) {
                    Text("전체 보기")
                    Icon(Icons.Rounded.ChevronRight, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            }

            if (state.history.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Rounded.Map, contentDescription = null, tint = ExplorerMuted, modifier = Modifier.size(34.dp))
                        Spacer(Modifier.height(10.dp))
                        Text("첫 탐험을 시작해 보세요", fontWeight = FontWeight.Bold)
                        Text("완료한 장소와 기록이 여기에 쌓여요", color = ExplorerMuted, fontSize = 13.sp)
                    }
                }
            } else {
                RecentExplorationCard(record = state.history.first(), onClick = onHistory)
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun SetupScreen(
    state: AppUiState,
    onBack: () -> Unit,
    onRadiusSelected: (Int) -> Unit,
    onModeSelected: (ExplorationMode) -> Unit,
    onDifficultySelected: (HintDifficulty) -> Unit,
    onDraw: () -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { ScreenTopBar("탐험 설정", onBack) },
        bottomBar = {
            Surface(tonalElevation = 8.dp) {
                Column(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(20.dp)
                ) {
                    Button(
                        onClick = onDraw,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ExplorerOrange)
                    ) {
                        Text("랜덤 목적지 뽑기", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.Rounded.Explore, contentDescription = null)
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            SelectionHeading("01", "얼마나 멀리 갈까요?", "직선거리 기준이며 실제 걷는 거리는 더 길 수 있어요")
            RadiusSelector(state.selectedRadiusM, onRadiusSelected)

            SelectionHeading("02", "어떤 탐험을 원하세요?", "장소 이름은 추첨 후에도 공개되지 않아요")
            ModeSelector(state.selectedMode, onModeSelected)

            SelectionHeading("03", "힌트 난이도", "언제든 온도 힌트와 남은 거리는 확인할 수 있어요")
            DifficultySelector(state.selectedDifficulty, onDifficultySelected)

            if (state.permissionDenied) {
                InlineNotice(
                    title = "위치 권한을 허용해 주세요",
                    message = "정확한 위치를 허용하면 후보 필터와 도착 판정이 더 안정적이에요.",
                    error = true
                )
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun DrawScreen(
    state: AppUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onWiderRadius: () -> Unit,
    onBegin: () -> Unit,
    onMapSymbolsCollected: (Int, List<MapSymbolCandidate>) -> Unit
) {
    Box(Modifier.fillMaxSize().background(ExplorerNavy)) {
        if (state.drawStage == DrawStage.MAP_SEARCHING && state.startLocation != null) {
            NaverExplorationMap(
                startLocation = state.startLocation.point,
                currentLocation = state.currentLocation,
                radiusM = state.selectedRadiusM,
                destination = null,
                revealDestination = false,
                recenterRequest = 0,
                symbolCollectionRequest = state.mapSearchRequest,
                onSymbolsCollected = onMapSymbolsCollected,
                mapBottomPadding = 0.dp,
                modifier = Modifier.fillMaxSize()
            )
        }
        Scaffold(
            containerColor = Color.Transparent,
            topBar = { ScreenTopBar("목적지 추첨", onBack, dark = true) }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                when {
                state.drawError != null -> {
                    Surface(
                        shape = RoundedCornerShape(30.dp),
                        color = Color.White,
                        contentColor = ExplorerNavy,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(26.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(shape = CircleShape, color = ExplorerRed.copy(alpha = 0.12f)) {
                                Icon(
                                    Icons.Rounded.WarningAmber,
                                    contentDescription = null,
                                    tint = ExplorerRed,
                                    modifier = Modifier.padding(16.dp).size(34.dp)
                                )
                            }
                            Spacer(Modifier.height(18.dp))
                            Text("목적지를 찾지 못했어요", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
                            Spacer(Modifier.height(8.dp))
                            Text(state.drawError, color = ExplorerMuted, textAlign = TextAlign.Center)
                            Spacer(Modifier.height(24.dp))
                            Button(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Rounded.Refresh, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("다시 시도")
                            }
                            OutlinedButton(
                                onClick = if (state.selectedRadiusM < 2000) onWiderRadius else onBack,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = ExplorerNavy),
                                border = BorderStroke(1.dp, ExplorerNavy.copy(alpha = 0.72f))
                            ) {
                                Text(if (state.selectedRadiusM < 2000) "반경을 넓혀 찾기" else "탐험 설정으로 돌아가기")
                            }
                        }
                    }
                }

                state.drawStage == DrawStage.READY -> {
                    Surface(
                        shape = RoundedCornerShape(30.dp),
                        color = Color.White,
                        contentColor = ExplorerNavy,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(shape = CircleShape, color = ExplorerOrange.copy(alpha = 0.13f)) {
                                Box(
                                    modifier = Modifier.padding(19.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Rounded.Lock, contentDescription = null, tint = ExplorerOrange, modifier = Modifier.size(36.dp))
                                }
                            }
                            Spacer(Modifier.height(18.dp))
                            Text("목적지를 뽑았어요", style = MaterialTheme.typography.headlineMedium)
                            Text(
                                "${state.areaLabel} · 후보 ${state.candidateCount}곳 중 하나",
                                color = ExplorerMuted,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(26.dp))
                            Surface(
                                shape = RoundedCornerShape(22.dp),
                                color = ExplorerNavy
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(22.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("목적지", color = Color.White.copy(alpha = 0.62f), fontSize = 13.sp)
                                    Text("?  ?  ?", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Black)
                                    Spacer(Modifier.height(12.dp))
                                    Text(
                                        "출발점에서 ${ExplorationMath.formatDistance(state.remainingDistanceM)}",
                                        color = ExplorerOrange,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            state.destination?.source?.let { source ->
                                Spacer(Modifier.height(14.dp))
                                CandidateSourceNotice(
                                    source = source,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            Spacer(Modifier.height(24.dp))
                            Button(
                                onClick = onBegin,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ExplorerOrange),
                                shape = RoundedCornerShape(18.dp)
                            ) {
                                Text("탐험 출발", style = MaterialTheme.typography.titleMedium)
                                Spacer(Modifier.width(8.dp))
                                Icon(Icons.Rounded.Navigation, contentDescription = null)
                            }
                        }
                    }
                }

                    else -> {
                        Surface(
                            shape = RoundedCornerShape(28.dp),
                            color = if (state.drawStage == DrawStage.MAP_SEARCHING) {
                                ExplorerNavy.copy(alpha = 0.94f)
                            } else {
                                Color.Transparent
                            }
                        ) {
                            Column(
                                modifier = Modifier.padding(
                                    if (state.drawStage == DrawStage.MAP_SEARCHING) 24.dp else 0.dp
                                ),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                DrawLoadingIndicator(stage = state.drawStage)
                                Spacer(Modifier.height(36.dp))
                                Text(state.drawStage.message, style = MaterialTheme.typography.headlineMedium, color = Color.White, textAlign = TextAlign.Center)
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    "장소명과 정확한 위치는 숨긴 채\n거리와 방향만 알려드릴게요",
                                    color = Color.White.copy(alpha = 0.65f),
                                    textAlign = TextAlign.Center
                                )
                                Spacer(Modifier.height(36.dp))
                                DrawStageList(current = state.drawStage)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExplorationScreen(
    state: AppUiState,
    onGiveUp: () -> Unit,
    onRevealHint: () -> Unit
) {
    var recenterRequest by remember { mutableIntStateOf(0) }
    val heading = rememberDeviceHeading()
    val directionVisible = state.selectedDifficulty != HintDifficulty.HARD || state.remainingDistanceM <= 400
    val categoryVisible = state.selectedDifficulty == HintDifficulty.EASY || state.hintRevealed
    val arrowRotation = ((state.targetBearingDegrees - (heading ?: 0f) + 360.0) % 360.0).toFloat()
    val temperatureColor = temperatureColor(state.temperature)

    Box(Modifier.fillMaxSize()) {
        NaverExplorationMap(
            startLocation = state.startLocation?.point,
            currentLocation = state.currentLocation,
            radiusM = state.selectedRadiusM,
            destination = state.destination,
            revealDestination = false,
            recenterRequest = recenterRequest,
            modifier = Modifier.fillMaxSize()
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surface, shadowElevation = 5.dp) {
                IconButton(onClick = onGiveUp) {
                    Icon(Icons.Rounded.Close, contentDescription = "탐험 종료")
                }
            }
            Surface(
                shape = RoundedCornerShape(100.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 5.dp,
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 15.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(9.dp).background(temperatureColor, CircleShape))
                    Spacer(Modifier.width(8.dp))
                    Text(state.areaLabel.ifBlank { "탐험 중" }, fontWeight = FontWeight.Bold, maxLines = 1)
                    Spacer(Modifier.weight(1f))
                    Text("${state.selectedRadiusM}m", color = ExplorerMuted, fontSize = 12.sp)
                }
            }
        }

        if (state.gpsWeak || state.explorationPaused || state.trackingMessage != null) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = ExplorerNavy,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 76.dp, start = 24.dp, end = 24.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.WarningAmber, contentDescription = null, tint = Color.White, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        state.trackingMessage ?: if (state.explorationPaused) "위치 추적이 잠시 멈췄어요" else "위치 정확도를 개선하고 있어요",
                        color = Color.White,
                        fontSize = 12.sp
                    )
                }
            }
        }

        Surface(
            onClick = { recenterRequest++ },
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 6.dp,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 18.dp, bottom = 294.dp)
                .size(50.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.MyLocation, contentDescription = "현재 위치로 이동", tint = ExplorerBlue)
            }
        }

        Surface(
            shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 14.dp,
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Column(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(horizontal = 22.dp, vertical = 18.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(42.dp)
                        .height(4.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(100.dp))
                        .align(Alignment.CenterHorizontally)
                )
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Surface(shape = RoundedCornerShape(100.dp), color = temperatureColor.copy(alpha = 0.13f)) {
                            Text(
                                "${state.temperature.label} · ${state.temperature.message}",
                                color = temperatureColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp)
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            ExplorationMath.formatDistance(state.remainingDistanceM),
                            style = MaterialTheme.typography.displaySmall
                        )
                        Text("목적지까지 남은 직선거리", color = ExplorerMuted, fontSize = 12.sp)
                    }
                    Surface(
                        shape = CircleShape,
                        color = ExplorerNavy,
                        modifier = Modifier.size(82.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (directionVisible) {
                                Icon(
                                    Icons.Rounded.Navigation,
                                    contentDescription = null,
                                    tint = ExplorerOrange,
                                    modifier = Modifier
                                        .size(46.dp)
                                        .rotate(arrowRotation)
                                )
                            } else {
                                Icon(Icons.Rounded.Lock, contentDescription = null, tint = Color.White.copy(alpha = 0.74f), modifier = Modifier.size(28.dp))
                            }
                        }
                    }
                }
                Spacer(Modifier.height(15.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                Spacer(Modifier.height(13.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (directionVisible) ExplorationMath.directionName(state.targetBearingDegrees) else "방향 힌트 잠김",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            when {
                                categoryVisible -> "카테고리 · ${state.destination?.category ?: state.selectedMode.label}"
                                state.selectedDifficulty == HintDifficulty.HARD -> "400m 안으로 들어오면 방향이 열려요"
                                else -> "힌트를 쓰면 카테고리를 알려드려요"
                            },
                            color = ExplorerMuted,
                            fontSize = 12.sp
                        )
                    }
                    if (!categoryVisible) {
                        OutlinedButton(onClick = onRevealHint, shape = RoundedCornerShape(14.dp)) {
                            Text("힌트 하나 더")
                        }
                    }
                }
                state.destination?.source?.let { source ->
                    Spacer(Modifier.height(12.dp))
                    CandidateSourceNotice(source = source)
                }
            }
        }
    }
}

@Composable
private fun ArrivalScreen(
    state: AppUiState,
    onHome: () -> Unit,
    onExploreAgain: () -> Unit
) {
    val destination = state.destination ?: return
    val context = LocalContext.current
    val elapsedSeconds = state.explorationStartedAt
        ?.let { ((System.currentTimeMillis() - it) / 1000).coerceAtLeast(0) }
        ?: 0
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Box(Modifier.fillMaxWidth().weight(0.42f)) {
            NaverExplorationMap(
                startLocation = state.startLocation?.point,
                currentLocation = state.currentLocation,
                radiusM = state.selectedRadiusM,
                destination = destination,
                revealDestination = true,
                recenterRequest = 0,
                mapBottomPadding = 0.dp,
                modifier = Modifier.fillMaxSize()
            )
            Surface(
                onClick = onHome,
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 5.dp,
                modifier = Modifier.statusBarsPadding().padding(14.dp).size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Close, contentDescription = "홈으로")
                }
            }
        }
        Surface(
            shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.58f)
                .offset(y = (-18).dp)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(24.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(100.dp),
                    color = if (state.arrivalCompleted) ExplorerGreen.copy(alpha = 0.13f) else ExplorerOrange.copy(alpha = 0.13f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (state.arrivalCompleted) Icons.Rounded.CheckCircle else Icons.Rounded.Info,
                            contentDescription = null,
                            tint = if (state.arrivalCompleted) ExplorerGreen else ExplorerOrange,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            if (state.arrivalCompleted) "탐험 완료" else "목적지 공개",
                            color = if (state.arrivalCompleted) ExplorerGreen else ExplorerOrange,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    if (state.arrivalCompleted) "도착했어요!" else "목적지는 여기였어요",
                    style = MaterialTheme.typography.headlineLarge
                )
                Spacer(Modifier.height(8.dp))
                Text(destination.name, style = MaterialTheme.typography.headlineMedium)
                Text(destination.category, color = ExplorerOrange, fontWeight = FontWeight.Bold)
                if (destination.roadAddress.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(Icons.Rounded.LocationOn, contentDescription = null, tint = ExplorerMuted, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(destination.roadAddress, color = ExplorerMuted, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Spacer(Modifier.height(12.dp))
                CandidateSourceNotice(source = destination.source)
                Spacer(Modifier.height(22.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CompactStat("탐험 시간", ExplorationMath.formatDuration(elapsedSeconds), Modifier.weight(1f))
                    CompactStat("걸은 거리", ExplorationMath.formatDistance(state.walkedDistanceM), Modifier.weight(1f))
                    CompactStat("첫 거리", ExplorationMath.formatDistance(destination.distanceFromStartM), Modifier.weight(1f))
                }
                Spacer(Modifier.height(22.dp))
                Button(
                    onClick = onExploreAgain,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(17.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ExplorerOrange)
                ) {
                    Text("한 번 더 탐험")
                }
                OutlinedButton(
                    onClick = {
                        val message = buildString {
                            append("랜덤 탐험으로 ${destination.name}에 도착했어요!")
                            if (destination.roadAddress.isNotBlank()) append("\n${destination.roadAddress}")
                        }
                        context.startActivity(
                            Intent.createChooser(
                                Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, message)
                                },
                                "탐험 결과 공유"
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(17.dp)
                ) {
                    Icon(Icons.Rounded.Share, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("결과 공유")
                }
                TextButton(onClick = onHome, modifier = Modifier.fillMaxWidth()) {
                    Text("홈으로")
                }
            }
        }
    }
}

@Composable
private fun CandidateSourceNotice(
    source: CandidateSource,
    modifier: Modifier = Modifier
) {
    if (source == CandidateSource.UNKNOWN) return
    val context = LocalContext.current
    val isOpenStreetMap = source == CandidateSource.OPENSTREETMAP
    Row(
        modifier = modifier.then(
            if (isOpenStreetMap) {
                Modifier.clickable {
                    runCatching {
                        context.startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://www.openstreetmap.org/copyright")
                            )
                        )
                    }
                }
            } else {
                Modifier
            }
        ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Rounded.Info,
            contentDescription = null,
            tint = ExplorerMuted,
            modifier = Modifier.size(15.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            if (isOpenStreetMap) {
                "장소 데이터 © OpenStreetMap contributors · ODbL"
            } else {
                "후보 데이터 · NAVER 지도"
            },
            color = ExplorerMuted,
            fontSize = 11.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun HistoryScreen(
    history: List<ExplorationRecord>,
    onBack: () -> Unit,
    onClear: () -> Unit
) {
    var confirmClear by remember { mutableStateOf(false) }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            ScreenTopBar(
                title = "탐험 기록",
                onBack = onBack,
                trailing = if (history.isNotEmpty()) {
                    {
                        IconButton(onClick = { confirmClear = true }) {
                            Icon(Icons.Rounded.DeleteOutline, contentDescription = "기록 삭제")
                        }
                    }
                } else null
            )
        }
    ) { innerPadding ->
        if (history.isEmpty()) {
            EmptyHistory(modifier = Modifier.padding(innerPadding))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = ExplorerNavy,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(22.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("완료한 탐험", color = Color.White.copy(alpha = 0.62f))
                                Text("${history.size}곳", style = MaterialTheme.typography.headlineLarge, color = Color.White)
                            }
                            Icon(Icons.Rounded.History, contentDescription = null, tint = ExplorerOrange, modifier = Modifier.size(42.dp))
                        }
                    }
                }
                items(history, key = { it.id }) { record ->
                    HistoryCard(record)
                }
            }
        }
    }
    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("기록을 모두 삭제할까요?") },
            text = { Text("삭제한 탐험 기록은 복구할 수 없어요.") },
            confirmButton = {
                Button(
                    onClick = {
                        onClear()
                        confirmClear = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ExplorerRed)
                ) {
                    Text("전체 삭제")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text("취소") }
            }
        )
    }
}

@Composable
private fun SettingsScreen(
    state: AppUiState,
    onBack: () -> Unit,
    onRadiusSelected: (Int) -> Unit,
    onModeSelected: (ExplorationMode) -> Unit,
    onDifficultySelected: (HintDifficulty) -> Unit,
    onExcludedCategoryToggle: (String) -> Unit,
    onSave: () -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { ScreenTopBar("설정", onBack) },
        bottomBar = {
            Surface(tonalElevation = 8.dp) {
                Button(
                    onClick = onSave,
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(20.dp)
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(17.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ExplorerOrange)
                ) {
                    Text("기본값 저장")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            Text("기본 탐험 반경", style = MaterialTheme.typography.titleLarge)
            RadiusSelector(state.selectedRadiusM, onRadiusSelected)
            Text("기본 탐험 타입", style = MaterialTheme.typography.titleLarge)
            ModeSelector(state.selectedMode, onModeSelected)
            Text("기본 힌트 난이도", style = MaterialTheme.typography.titleLarge)
            DifficultySelector(state.selectedDifficulty, onDifficultySelected)
            Text("제외할 장소", style = MaterialTheme.typography.titleLarge)
            Text("선택한 카테고리는 다음 후보 검색부터 제외돼요", color = ExplorerMuted, style = MaterialTheme.typography.bodyMedium)
            ExcludedCategorySelector(state.excludedCategories, onExcludedCategoryToggle)
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = ExplorerNavy
            ) {
                Column(Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Shield, contentDescription = null, tint = ExplorerGreen)
                        Spacer(Modifier.width(9.dp))
                        Text("위치와 데이터", color = Color.White, style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "위치는 탐험 화면이 보이는 동안에만 사용합니다. 후보 검색 시 현재 좌표와 반경을 공개 OpenStreetMap Overpass 서비스에 전송하며, 백그라운드 위치 권한은 요청하지 않습니다. 저장된 탐험 기록은 기록 화면에서 모두 삭제할 수 있어요.",
                        color = Color.White.copy(alpha = 0.72f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f)
            ) {
                Row(Modifier.padding(17.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Rounded.Info, contentDescription = null, tint = ExplorerBlue, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "별도 후보 서버 없이 OpenStreetMap 공개 장소를 먼저 조회하고, 부족하면 NAVER 지도에 표시된 장소를 활용합니다.",
                        color = ExplorerMuted,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun ScreenTopBar(
    title: String,
    onBack: () -> Unit,
    dark: Boolean = false,
    trailing: (@Composable () -> Unit)? = null
) {
    val contentColor = if (dark) Color.White else MaterialTheme.colorScheme.onBackground
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (dark) ExplorerNavy else MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .height(62.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "뒤로", tint = contentColor)
        }
        Text(title, style = MaterialTheme.typography.titleLarge, color = contentColor, modifier = Modifier.weight(1f))
        trailing?.invoke() ?: Spacer(Modifier.size(48.dp))
    }
}

@Composable
private fun SelectionHeading(number: String, title: String, description: String) {
    Row(verticalAlignment = Alignment.Top) {
        Surface(shape = CircleShape, color = ExplorerOrange) {
            Text(number, color = Color.White, fontWeight = FontWeight.Black, fontSize = 11.sp, modifier = Modifier.padding(8.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(description, color = ExplorerMuted, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun RadiusSelector(selected: Int, onSelected: (Int) -> Unit) {
    val options = listOf(500 to "가볍게", 1000 to "기본", 2000 to "도전")
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        options.forEach { (radius, label) ->
            val isSelected = selected == radius || selected == 1500 && radius == 2000
            Surface(
                onClick = { onSelected(radius) },
                shape = RoundedCornerShape(20.dp),
                color = if (isSelected) ExplorerNavy else MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, if (isSelected) ExplorerNavy else MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 17.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        if (radius >= 1000) "${radius / 1000}km" else "${radius}m",
                        style = MaterialTheme.typography.titleLarge,
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                    Text(label, color = if (isSelected) Color.White.copy(alpha = 0.64f) else ExplorerMuted, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun ModeSelector(selected: ExplorationMode, onSelected: (ExplorationMode) -> Unit) {
    ExplorationMode.entries.toList().chunked(2).forEach { rowModes ->
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            rowModes.forEach { mode ->
                val isSelected = selected == mode
                Surface(
                    onClick = { onSelected(mode) },
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSelected) ExplorerOrange.copy(alpha = 0.10f) else MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.5.dp, if (isSelected) ExplorerOrange else MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(15.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (isSelected) ExplorerOrange else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Box(Modifier.size(38.dp), contentAlignment = Alignment.Center) {
                                Text(mode.symbol, color = if (isSelected) Color.White else ExplorerMuted, fontWeight = FontWeight.Black)
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(mode.label, fontWeight = FontWeight.Bold)
                            Text(mode.description, color = ExplorerMuted, fontSize = 10.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
            if (rowModes.size == 1) Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun DifficultySelector(selected: HintDifficulty, onSelected: (HintDifficulty) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        HintDifficulty.entries.forEach { difficulty ->
            val isSelected = selected == difficulty
            Surface(
                onClick = { onSelected(difficulty) },
                shape = RoundedCornerShape(18.dp),
                color = if (isSelected) ExplorerNavy else MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, if (isSelected) ExplorerNavy else MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .border(2.dp, if (isSelected) ExplorerOrange else ExplorerMuted, CircleShape)
                            .padding(4.dp)
                    ) {
                        if (isSelected) Box(Modifier.fillMaxSize().background(ExplorerOrange, CircleShape))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(difficulty.label, fontWeight = FontWeight.Bold, color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface)
                        Text(difficulty.description, color = if (isSelected) Color.White.copy(alpha = 0.64f) else ExplorerMuted, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun ExcludedCategorySelector(
    selected: Set<String>,
    onToggle: (String) -> Unit
) {
    val categories = listOf("카페", "베이커리", "시장", "공원", "전시", "박물관")
    categories.chunked(3).forEach { rowCategories ->
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 9.dp),
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            rowCategories.forEach { category ->
                val excluded = category in selected
                Surface(
                    onClick = { onToggle(category) },
                    shape = RoundedCornerShape(16.dp),
                    color = if (excluded) ExplorerRed.copy(alpha = 0.10f) else MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, if (excluded) ExplorerRed else MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (excluded) {
                            Icon(Icons.Rounded.Close, contentDescription = null, tint = ExplorerRed, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(4.dp))
                        }
                        Text(category, color = if (excluded) ExplorerRed else MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun DrawLoadingIndicator(stage: DrawStage) {
    val transition = rememberInfiniteTransition(label = "draw")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(2200, easing = LinearEasing), RepeatMode.Restart),
        label = "rotation"
    )
    val scale by transition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "scale"
    )
    Box(
        modifier = Modifier.size(150.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    rotationZ = rotation
                    scaleX = scale
                    scaleY = scale
                }
                .border(2.dp, Color.White.copy(alpha = 0.25f), CircleShape)
        )
        Box(
            modifier = Modifier
                .size(112.dp)
                .border(1.dp, ExplorerOrange.copy(alpha = 0.65f), CircleShape)
        )
        CircularProgressIndicator(
            modifier = Modifier.size(76.dp),
            color = ExplorerOrange,
            strokeWidth = 5.dp
        )
        Icon(
            if (stage == DrawStage.LOCATING) Icons.Rounded.MyLocation else Icons.Rounded.Explore,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(30.dp)
        )
    }
}

@Composable
private fun DrawStageList(current: DrawStage) {
    val stages = listOf(
        DrawStage.LOCATING,
        DrawStage.RESOLVING_AREA,
        DrawStage.SEARCHING,
        DrawStage.MAP_SEARCHING,
        DrawStage.FILTERING
    )
    val currentIndex = stages.indexOf(current).coerceAtLeast(0)
    Surface(shape = RoundedCornerShape(22.dp), color = Color.White.copy(alpha = 0.08f)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            stages.forEachIndexed { index, stage ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = if (index <= currentIndex) ExplorerOrange else Color.White.copy(alpha = 0.12f),
                        modifier = Modifier.size(22.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (index < currentIndex) Text("✓", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(stage.message, color = if (index <= currentIndex) Color.White else Color.White.copy(alpha = 0.38f), fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun InlineNotice(
    title: String,
    message: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    error: Boolean = false
) {
    val accent = if (error) ExplorerRed else ExplorerBlue
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = accent.copy(alpha = 0.09f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.22f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Icon(if (error) Icons.Rounded.WarningAmber else Icons.Rounded.Info, contentDescription = null, tint = accent)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(message, color = ExplorerMuted, style = MaterialTheme.typography.bodyMedium)
            }
            if (actionLabel != null && onAction != null) {
                TextButton(onClick = onAction) { Text(actionLabel) }
            }
        }
    }
}

@Composable
private fun MetricCard(
    label: String,
    value: String,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier
    ) {
        Column(Modifier.padding(18.dp)) {
            icon()
            Spacer(Modifier.height(14.dp))
            Text(value, style = MaterialTheme.typography.titleLarge)
            Text(label, color = ExplorerMuted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun RecentExplorationCard(record: ExplorationRecord, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = ExplorerGreen.copy(alpha = 0.12f)) {
                Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = ExplorerGreen, modifier = Modifier.padding(12.dp))
            }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text(record.destination.name, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${record.destination.category} · ${formatDate(record.endedAt)}", color = ExplorerMuted, fontSize = 12.sp)
            }
            Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = ExplorerMuted)
        }
    }
}

@Composable
private fun CompactStat(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f), modifier = modifier) {
        Column(Modifier.padding(vertical = 13.dp, horizontal = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
            Text(label, color = ExplorerMuted, fontSize = 10.sp)
        }
    }
}

@Composable
private fun HistoryCard(record: ExplorationRecord) {
    Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = CircleShape, color = ExplorerOrange.copy(alpha = 0.12f)) {
                    Box(Modifier.size(42.dp), contentAlignment = Alignment.Center) {
                        Text(record.mode.symbol, color = ExplorerOrange, fontWeight = FontWeight.Black)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(record.destination.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(formatDate(record.endedAt), color = ExplorerMuted, fontSize = 12.sp)
                }
                Surface(shape = RoundedCornerShape(100.dp), color = ExplorerGreen.copy(alpha = 0.12f)) {
                    Text(record.destination.category, color = ExplorerGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp))
                }
            }
            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            Spacer(Modifier.height(12.dp))
            Row {
                Text("${ExplorationMath.formatDuration(record.durationSeconds)} 탐험", color = ExplorerMuted, fontSize = 12.sp)
                Text("  ·  ", color = ExplorerMuted)
                Text("${ExplorationMath.formatDistance(record.walkedDistanceM)} 걸음", color = ExplorerMuted, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun EmptyHistory(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant) {
            Icon(Icons.Rounded.History, contentDescription = null, tint = ExplorerMuted, modifier = Modifier.padding(24.dp).size(42.dp))
        }
        Spacer(Modifier.height(18.dp))
        Text("아직 탐험 기록이 없어요", style = MaterialTheme.typography.titleLarge)
        Text("첫 목적지를 찾아가면 이곳에 기록돼요", color = ExplorerMuted, textAlign = TextAlign.Center)
    }
}

private fun temperatureColor(hint: TemperatureHint): Color = when (hint) {
    TemperatureHint.COLD -> ExplorerBlue
    TemperatureHint.COOL -> Color(0xFF3BA6A0)
    TemperatureHint.WARM -> Color(0xFFE29C2B)
    TemperatureHint.HOT -> ExplorerOrange
    TemperatureHint.VERY_HOT -> ExplorerRed
}

private fun formatDate(timestamp: Long): String =
    SimpleDateFormat("M월 d일 · HH:mm", Locale.KOREA).format(Date(timestamp))
