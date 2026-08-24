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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ADDITIONAL_CATEGORIES
import com.example.ui.components.PlayerSlotCard
import com.example.ui.components.RoomCodeBanner
import com.example.ui.components.SleekButton
import com.example.ui.components.SleekCategoryChip
import com.example.ui.theme.PrimaryPurple
import com.example.viewmodel.StopUiState

@Composable
fun LobbyScreen(
    uiState: StopUiState,
    onBackClick: () -> Unit,
    onToggleCategory: (String) -> Unit,
    onAddCustomCategory: (String) -> Unit,
    onAddBotClick: () -> Unit,
    onRemovePlayerClick: (String) -> Unit,
    onSetRoundDuration: (Int) -> Unit,
    onSetTotalRounds: (Int) -> Unit,
    onStartGameClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showCategoryDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var customCategoryText by remember { mutableStateOf("") }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header (h-16 equivalent)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("lobby_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "Sala de Espera",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = (-0.2).sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                IconButton(
                    onClick = { showSettingsDialog = true },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Configuración",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Scrollable Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Room Code Banner (Hero Section)
                RoomCodeBanner(
                    roomCode = uiState.roomCode.ifEmpty { "STOP-482" },
                    hostIp = uiState.hostIp,
                    subtitle = if (uiState.isSoloOrBotsMode) "Partida en modo local con Bots" else "Comparte el código para jugar"
                )

                // Connected Players Section
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Jugadores Conectados (${uiState.players.size}/${uiState.gameConfig.maxPlayers})",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Text(
                            text = if (uiState.players.size < 2 && !uiState.isSoloOrBotsMode) "Esperando..." else "Listo",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = PrimaryPurple
                        )
                    }

                    // Grid 2 Columns for 6 player slots
                    val totalSlots = 6
                    val players = uiState.players

                    for (row in 0 until 3) {
                        val index1 = row * 2
                        val index2 = row * 2 + 1
                        val p1 = players.getOrNull(index1)
                        val p2 = players.getOrNull(index2)

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                PlayerSlotCard(
                                    player = p1,
                                    isLocal = p1?.id == uiState.localPlayer.id,
                                    onRemoveClick = if (uiState.isHost && p1 != null && !p1.isHost) {
                                        { onRemovePlayerClick(p1.id) }
                                    } else null,
                                    onAddBotClick = if (uiState.isHost && p1 == null) {
                                        onAddBotClick
                                    } else null
                                )
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                PlayerSlotCard(
                                    player = p2,
                                    isLocal = p2?.id == uiState.localPlayer.id,
                                    onRemoveClick = if (uiState.isHost && p2 != null && !p2.isHost) {
                                        { onRemovePlayerClick(p2.id) }
                                    } else null,
                                    onAddBotClick = if (uiState.isHost && p2 == null) {
                                        onAddBotClick
                                    } else null
                                )
                            }
                        }
                    }
                }

                // Selected Categories Section
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Categorías seleccionadas (${uiState.gameConfig.categories.size})",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    // Categories chip row / wrap
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val chunks = uiState.gameConfig.categories.chunked(3)
                        chunks.forEach { rowCategories ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                rowCategories.forEach { cat ->
                                    SleekCategoryChip(
                                        name = cat,
                                        isSelected = true,
                                        onToggle = {
                                            if (uiState.isHost) onToggleCategory(cat)
                                        }
                                    )
                                }
                            }
                        }

                        if (uiState.isHost) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(PrimaryPurple)
                                        .clickable { showCategoryDialog = true },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Añadir categoría",
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Añadir o cambiar categorías",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = PrimaryPurple,
                                    modifier = Modifier.clickable { showCategoryDialog = true }
                                )
                            }
                        }
                    }
                }
            }

            // Sticky Footer
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (uiState.isHost) {
                    val canStart = uiState.players.size >= 2 || uiState.isSoloOrBotsMode
                    SleekButton(
                        text = "COMENZAR PARTIDA",
                        icon = Icons.Default.PlayArrow,
                        enabled = canStart,
                        onClick = onStartGameClick,
                        modifier = Modifier.testTag("start_game_button")
                    )
                    Text(
                        text = if (canStart) "Todos los jugadores recibirán la misma letra" else "MÍN. 2 JUGADORES PARA INICIAR",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(top = 10.dp)
                    )
                } else {
                    Card(
                        shape = RoundedCornerShape(100.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Esperando que el anfitrión inicie la partida...",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp)
                        )
                    }
                }
            }
        }
    }

    // Add / Edit Categories Dialog
    if (showCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showCategoryDialog = false },
            title = {
                Text("Gestionar Categorías", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Selecciona de la lista o escribe una propia:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Suggested categories
                    val allSuggestions = (ADDITIONAL_CATEGORIES).distinct()
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        allSuggestions.chunked(2).forEach { pair ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                pair.forEach { cat ->
                                    val isSelected = uiState.gameConfig.categories.contains(cat)
                                    Box(modifier = Modifier.weight(1f)) {
                                        SleekCategoryChip(
                                            name = cat,
                                            isSelected = isSelected,
                                            onToggle = { onToggleCategory(cat) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = customCategoryText,
                        onValueChange = { customCategoryText = it },
                        label = { Text("Nueva categoría personalizada") },
                        placeholder = { Text("ej. Marca de autos, Comida rápida") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (customCategoryText.isNotBlank()) {
                            onAddCustomCategory(customCategoryText)
                            customCategoryText = ""
                        }
                        showCategoryDialog = false
                    }
                ) {
                    Text("Guardar", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCategoryDialog = false }) {
                    Text("Cerrar")
                }
            }
        )
    }

    // Game Settings Dialog
    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = { Text("Configuración de Partida", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Duration
                    Column {
                        Text(
                            text = "Duración máxima de ronda: ${uiState.gameConfig.roundDurationSecs}s",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(45, 60, 90, 120).forEach { secs ->
                                val isSel = uiState.gameConfig.roundDurationSecs == secs
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSel) PrimaryPurple else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.clickable { onSetRoundDuration(secs) }
                                ) {
                                    Text(
                                        text = "${secs}s",
                                        color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // Total rounds
                    Column {
                        Text(
                            text = "Total de rondas: ${uiState.gameConfig.totalRounds}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(3, 5, 7, 10).forEach { rounds ->
                                val isSel = uiState.gameConfig.totalRounds == rounds
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSel) PrimaryPurple else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.clickable { onSetTotalRounds(rounds) }
                                ) {
                                    Text(
                                        text = "$rounds",
                                        color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSettingsDialog = false }) {
                    Text("Listo", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}
