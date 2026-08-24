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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.network.DiscoveredRoom
import com.example.ui.components.SleekButton
import com.example.ui.theme.OutlineColor
import com.example.ui.theme.PlayerColors
import com.example.ui.theme.PrimaryContainer
import com.example.ui.theme.PrimaryPurple
import com.example.viewmodel.StopUiState

@Composable
fun HomeScreen(
    uiState: StopUiState,
    onNameChange: (String) -> Unit,
    onColorChange: (Int) -> Unit,
    onHostClick: () -> Unit,
    onJoinIpClick: (String) -> Unit,
    onJoinRoomClick: (DiscoveredRoom) -> Unit,
    onSoloBotsClick: () -> Unit,
    onStartDiscovery: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showJoinDialog by remember { mutableStateOf(false) }
    var manualIpText by remember { mutableStateOf("") }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header / App Title
            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(PrimaryPurple),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "¡STOP!",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "STOP! Multijugador",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                letterSpacing = (-0.5).sp
            )
            Text(
                text = "Juego local hasta 6 jugadores en la misma red Wi-Fi",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Player Profile Card
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, OutlineColor, RoundedCornerShape(24.dp))
                    .testTag("player_profile_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Text(
                        text = "Tu Perfil",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Avatar circle
                        val currentColor = PlayerColors.getOrElse(uiState.localPlayer.colorIndex) { PrimaryPurple }
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(currentColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = uiState.localPlayer.name.take(2).uppercase(),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        OutlinedTextField(
                            value = uiState.localPlayer.name,
                            onValueChange = onNameChange,
                            label = { Text("Nombre o Apodo") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryPurple,
                                unfocusedBorderColor = OutlineColor
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("player_name_input")
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Color picker row
                    Text(
                        text = "Color de ficha:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PlayerColors.forEachIndexed { index, color ->
                            val isSelected = uiState.localPlayer.colorIndex == index
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (isSelected) 3.dp else 0.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { onColorChange(index) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Action Buttons
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Host Button
                SleekButton(
                    text = "Crear Sala Local (Host)",
                    icon = Icons.Default.PlayArrow,
                    onClick = onHostClick,
                    modifier = Modifier.testTag("create_room_button")
                )

                // Join Button
                SleekButton(
                    text = "Unirse a Sala en Red",
                    icon = Icons.Default.Wifi,
                    containerColor = MaterialTheme.colorScheme.secondary,
                    onClick = {
                        onStartDiscovery()
                        showJoinDialog = true
                    },
                    modifier = Modifier.testTag("join_room_button")
                )

                // Solo with bots
                SleekButton(
                    text = "Modo con Bots / Práctica",
                    icon = Icons.Default.SmartToy,
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    onClick = onSoloBotsClick,
                    modifier = Modifier.testTag("solo_bots_button")
                )
            }
        }
    }

    // Join Room Dialog
    if (showJoinDialog) {
        AlertDialog(
            onDismissRequest = { showJoinDialog = false },
            title = {
                Text(
                    text = "Salas en Red Local",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Buscando salas Wi-Fi...",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        IconButton(
                            onClick = onStartDiscovery,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refrescar",
                                tint = PrimaryPurple
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (uiState.availableRooms.isNotEmpty()) {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                        ) {
                            items(uiState.availableRooms) { room ->
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = PrimaryContainer),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            showJoinDialog = false
                                            onJoinRoomClick(room)
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(
                                                text = room.roomCode,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                            Text(
                                                text = "Host: ${room.hostName} (${room.hostIp})",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = "Unirse",
                                            tint = PrimaryPurple
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Text(
                            text = "No se detectaron salas automáticas aún. Puedes ingresar la IP del Host manualmente abajo:",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = manualIpText,
                        onValueChange = { manualIpText = it },
                        label = { Text("IP del Anfitrión (ej. 192.168.1.5)") },
                        placeholder = { Text("192.168.1.X") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (manualIpText.isNotBlank()) {
                            showJoinDialog = false
                            onJoinIpClick(manualIpText)
                        }
                    }
                ) {
                    Text("Conectar por IP", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showJoinDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
