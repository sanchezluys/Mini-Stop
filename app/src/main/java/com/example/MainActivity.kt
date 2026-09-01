package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.example.viewmodel.StopGameViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: StopGameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
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
                                    onFinishVotingClick = { viewModel.finishVotingAndShowSummary() }
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
}
