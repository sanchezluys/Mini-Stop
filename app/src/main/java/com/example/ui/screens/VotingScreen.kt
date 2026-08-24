package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AnswerScoreType
import com.example.ui.components.SleekButton
import com.example.ui.theme.OutlineColor
import com.example.ui.theme.PlayerColors
import com.example.ui.theme.PrimaryContainer
import com.example.ui.theme.PrimaryPurple
import com.example.ui.theme.StopRed
import com.example.ui.theme.SuccessGreen
import com.example.viewmodel.StopUiState

@Composable
fun VotingScreen(
    uiState: StopUiState,
    onScoreChange: (playerId: String, category: String, scoreType: AnswerScoreType) -> Unit,
    onFinishVotingClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val categories = uiState.gameConfig.categories
    var selectedCategoryIndex by remember { mutableIntStateOf(0) }
    val currentCategory = categories.getOrElse(selectedCategoryIndex) { categories.firstOrNull() ?: "" }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Revisión y Votación",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Card(
                            shape = RoundedCornerShape(100.dp),
                            colors = CardDefaults.cardColors(containerColor = PrimaryContainer)
                        ) {
                            Text(
                                text = "Letra ${uiState.currentLetter}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Text(
                        text = "Toca para calificar: +100 (Única), +50 (Repetida) o 0 (Nula)",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            // Category Tabs
            ScrollableTabRow(
                selectedTabIndex = selectedCategoryIndex,
                edgePadding = 16.dp,
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = PrimaryPurple,
                modifier = Modifier.fillMaxWidth()
            ) {
                categories.forEachIndexed { index, cat ->
                    Tab(
                        selected = selectedCategoryIndex == index,
                        onClick = { selectedCategoryIndex = index },
                        text = {
                            Text(
                                text = cat,
                                fontSize = 13.sp,
                                fontWeight = if (selectedCategoryIndex == index) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    )
                }
            }

            // List of Players Answers for Current Category
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(uiState.players) { player ->
                    val submission = uiState.allRoundSubmissions.find { it.playerId == player.id }
                    val answerText = submission?.answers?.get(currentCategory)?.trim() ?: ""
                    val currentScoreType = uiState.votingScores[player.id]?.get(currentCategory) ?: AnswerScoreType.INVALID
                    val avatarColor = PlayerColors.getOrElse(player.colorIndex) { PrimaryPurple }

                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, OutlineColor, RoundedCornerShape(18.dp))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp)
                        ) {
                            // Player Info & Word
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(avatarColor),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = player.name.take(2).uppercase(),
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = player.name,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                // Points preview badge
                                Card(
                                    shape = RoundedCornerShape(8.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = when (currentScoreType) {
                                            AnswerScoreType.UNIQUE -> SuccessGreen.copy(alpha = 0.15f)
                                            AnswerScoreType.REPEATED -> PrimaryPurple.copy(alpha = 0.15f)
                                            AnswerScoreType.INVALID -> StopRed.copy(alpha = 0.15f)
                                        }
                                    )
                                ) {
                                    Text(
                                        text = "+${currentScoreType.points} pts",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = when (currentScoreType) {
                                            AnswerScoreType.UNIQUE -> SuccessGreen
                                            AnswerScoreType.REPEATED -> PrimaryPurple
                                            AnswerScoreType.INVALID -> StopRed
                                        },
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Submitted Word
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    text = if (answerText.isNotEmpty()) answerText else "(Sin respuesta)",
                                    fontSize = 16.sp,
                                    fontWeight = if (answerText.isNotEmpty()) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (answerText.isNotEmpty()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Score Action Buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Unique +100
                                ScorePillButton(
                                    label = "+100 (Única)",
                                    icon = Icons.Default.Check,
                                    isSelected = currentScoreType == AnswerScoreType.UNIQUE,
                                    selectedColor = SuccessGreen,
                                    onClick = { onScoreChange(player.id, currentCategory, AnswerScoreType.UNIQUE) },
                                    modifier = Modifier.weight(1f)
                                )

                                // Repeated +50
                                ScorePillButton(
                                    label = "+50 (Repetida)",
                                    icon = Icons.Default.Repeat,
                                    isSelected = currentScoreType == AnswerScoreType.REPEATED,
                                    selectedColor = PrimaryPurple,
                                    onClick = { onScoreChange(player.id, currentCategory, AnswerScoreType.REPEATED) },
                                    modifier = Modifier.weight(1f)
                                )

                                // Invalid 0
                                ScorePillButton(
                                    label = "0 (Nula)",
                                    icon = Icons.Default.Close,
                                    isSelected = currentScoreType == AnswerScoreType.INVALID,
                                    selectedColor = StopRed,
                                    onClick = { onScoreChange(player.id, currentCategory, AnswerScoreType.INVALID) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            // Footer Button
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                if (uiState.isHost) {
                    SleekButton(
                        text = "FINALIZAR VOTACIÓN",
                        icon = Icons.Default.DoneAll,
                        onClick = onFinishVotingClick,
                        modifier = Modifier.testTag("finish_voting_button")
                    )
                } else {
                    Card(
                        shape = RoundedCornerShape(100.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Esperando que el anfitrión finalice la votación...",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ScorePillButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    selectedColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) selectedColor else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}
