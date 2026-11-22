package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.ScreenState
import com.example.ui.components.NotificationBanner
import com.example.ui.screens.GameOverScreen
import com.example.ui.screens.GameRoundScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LetterSelectionScreen
import com.example.ui.screens.LobbyScreen
import com.example.ui.screens.RoundSummaryScreen
import com.example.ui.screens.VotingScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.update.InAppUpdateController
import com.example.viewmodel.StopGameViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: StopGameViewModel by viewModels()
    private lateinit var inAppUpdateController: InAppUpdateController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        inAppUpdateController = InAppUpdateController(this)
        inAppUpdateController.checkForUpdates()

        setContent {
            MyApplicationTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                val updateState by inAppUpdateController.uiState.collectAsStateWithLifecycle()
                val snackbarHostState = remember { SnackbarHostState() }

                val updateReadyMessage = stringResource(R.string.update_ready_message)
                val updateReadyAction = stringResource(R.string.update_ready_action)

                LaunchedEffect(updateState.isUpdateReadyToInstall) {
                    if (updateState.isUpdateReadyToInstall) {
                        val result = snackbarHostState.showSnackbar(
                            message = updateReadyMessage,
                            actionLabel = updateReadyAction,
                            duration = SnackbarDuration.Indefinite
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            inAppUpdateController.completeFlexibleUpdate()
                        }
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = MaterialTheme.colorScheme.background,
                    contentWindowInsets = WindowInsets.safeDrawing,
                    snackbarHost = {
                        SnackbarHost(hostState = snackbarHostState)
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (uiState.currentScreen) {
                            ScreenState.HOME -> {
                                HomeScreen(
                                    uiState = uiState,
                                    onNameChange = { viewModel.setPlayerName(it) },
                                    onColorChange = { viewModel.setPlayerColor(it) },
                                    onAvatarChange = { viewModel.setPlayerAvatarFromUri(it) },
                                    onRemoveAvatar = { viewModel.removePlayerAvatar() },
                                    onHostClick = { viewModel.hostGame() },
                                    onJoinIpClick = { viewModel.joinByIp(it) },
                                    onJoinRoomClick = { viewModel.joinByRoom(it) },
                                    onSoloBotsClick = { viewModel.startSoloWithBots() },
                                    onStartDiscovery = { viewModel.startDiscovery() }
                                )
                            }
                            ScreenState.LOBBY -> {
                                LobbyScreen(
                                    uiState = uiState,
                                    onBackClick = { viewModel.returnToHome() },
                                    onToggleCategory = { viewModel.toggleCategory(it) },
                                    onAddCustomCategory = { viewModel.addCustomCategory(it) },
                                    onAddBotClick = { viewModel.addBotPlayer() },
                                    onRemovePlayerClick = { viewModel.removePlayer(it) },
                                    onSetRoundDuration = { viewModel.setRoundDuration(it) },
                                    onSetTotalRounds = { viewModel.setTotalRounds(it) },
                                    onStartGameClick = { viewModel.startGame() }
                                )
                            }
                            ScreenState.LETTER_ROULETTE -> {
                                LetterSelectionScreen(
                                    uiState = uiState
                                )
                            }
                            ScreenState.ROUND_PLAYING -> {
                                GameRoundScreen(
                                    uiState = uiState,
                                    onAnswerChanged = { cat, text -> viewModel.onAnswerChanged(cat, text) },
                                    onStopClick = { viewModel.callStop() }
                                )
                            }
                            ScreenState.REVIEW_VOTING -> {
                                VotingScreen(
                                    uiState = uiState,
                                    onScoreChange = { pId, cat, type -> viewModel.setAnswerVote(pId, cat, type) },
                                    onLaughClick = { pId, cat -> viewModel.toggleLaughVote(pId, cat) },
                                    onFinishVotingClick = { viewModel.finishVotingAndShowSummary() },
                                    onEditAnswer = { pId, cat, text -> viewModel.editAnswerText(pId, cat, text) }
                                )
                            }
                            ScreenState.ROUND_SUMMARY -> {
                                RoundSummaryScreen(
                                    uiState = uiState,
                                    onNextRoundClick = { viewModel.nextRound() }
                                )
                            }
                            ScreenState.GAME_OVER -> {
                                GameOverScreen(
                                    uiState = uiState,
                                    onRestartClick = { viewModel.restartGame() },
                                    onHomeClick = { viewModel.returnToHome() }
                                )
                            }
                        }

                        // Subtle non-blocking download progress pill when flexible update downloads
                        AnimatedVisibility(
                            visible = updateState.isDownloading,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shadowElevation = 4.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.SystemUpdate,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = stringResource(
                                                R.string.update_downloading,
                                                updateState.downloadProgressPercent
                                            ),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                    LinearProgressIndicator(
                                        progress = { updateState.downloadProgressPercent / 100f },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 6.dp)
                                    )
                                }
                            }
                        }

                        // Top Notification Banner for room connections, stops, and alerts
                        NotificationBanner(
                            message = uiState.bannerMessage,
                            onDismiss = { viewModel.dismissBanner() },
                            modifier = Modifier.align(Alignment.TopCenter)
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        inAppUpdateController.onResumeCheck()
    }

    override fun onDestroy() {
        inAppUpdateController.onDestroy()
        super.onDestroy()
    }
}
