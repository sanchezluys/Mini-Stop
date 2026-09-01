package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.PlayerAvatar
import com.example.ui.components.SleekButton
import com.example.ui.theme.PlayerColors
import com.example.ui.theme.SuccessGreen
import com.example.viewmodel.StopUiState

@Composable
fun RoundSummaryScreen(
    uiState: StopUiState,
    onNextRoundClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sortedPlayers = uiState.players.sortedByDescending { it.score }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Header Badge
            Card(
                shape = RoundedCornerShape(100.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Text(
                    text = "RESUMEN DE RONDA ${uiState.currentRoundNumber}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Tabla de Posiciones",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Leaderboard list
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(sortedPlayers) { index, player ->
                    val color = PlayerColors.getOrElse(player.colorIndex) { MaterialTheme.colorScheme.primary }
                    val earnedInRound = uiState.roundPointsEarned[player.id] ?: 0

                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Position number
                                Text(
                                    text = "#${index + 1}",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (index == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                PlayerAvatar(player = player, size = 36.dp)

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Text(
                                        text = player.name,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    val roundLaughs = uiState.roundLaughVotes[player.id]?.values?.sumOf { it.size } ?: 0
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "+$earnedInRound pts",
                                            fontSize = 11.sp,
                                            color = SuccessGreen,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (roundLaughs > 0) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "· 😂 +$roundLaughs",
                                                fontSize = 11.sp,
                                                color = Color(0xFFD97706),
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }

                            // Total score badge
                            Text(
                                text = "${player.score} pts",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Next Round Button
            if (uiState.isHost) {
                SleekButton(
                    text = "SIGUIENTE RONDA (${uiState.currentRoundNumber + 1}/${uiState.gameConfig.totalRounds})",
                    icon = Icons.AutoMirrored.Filled.ArrowForward,
                    onClick = onNextRoundClick,
                    modifier = Modifier.testTag("next_round_button")
                )
            } else {
                Card(
                    shape = RoundedCornerShape(100.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Esperando que el anfitrión avance a la siguiente ronda...",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}

