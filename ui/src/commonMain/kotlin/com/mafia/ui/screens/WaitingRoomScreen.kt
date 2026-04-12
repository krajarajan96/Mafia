package com.mafia.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mafia.shared.model.GameSettings
import com.mafia.shared.model.Room
import com.mafia.ui.theme.*

@Composable
fun WaitingRoomScreen(
    room: Room, myPlayerId: String,
    onStartGame: () -> Unit,
    onLeave: () -> Unit,
    onUpdateSettings: (GameSettings) -> Unit = {}
) {
    val clipboardManager = LocalClipboardManager.current
    val isHost = room.hostId == myPlayerId

    var showConfig by remember { mutableStateOf(false) }
    var settings by remember(room.settings) { mutableStateOf(room.settings) }

    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF0F0A1E), Color(0xFF1A1145)))).padding(24.dp)) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Waiting Room", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.height(16.dp))

            // Room code card
            Surface(color = Color.White.copy(0.08f), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Room Code", fontSize = 12.sp, color = Color.White.copy(0.5f))
                    Spacer(Modifier.height(4.dp))
                    Text(room.code, fontSize = 36.sp, fontWeight = FontWeight.Black, color = MafiaGold, letterSpacing = 6.sp, fontFamily = FontFamily.Monospace)
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { clipboardManager.setText(AnnotatedString(room.code)) }) { Text("📋 Copy Code", color = MafiaGold.copy(0.8f), fontSize = 13.sp) }
                }
            }
            Spacer(Modifier.height(24.dp))

            // Players
            Text("${room.playerCount}/${room.maxPlayers} Players", fontSize = 14.sp, color = Color.White.copy(0.6f))
            Spacer(Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                room.players.forEach { player ->
                    Surface(color = Color.White.copy(0.08f), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(40.dp).clip(CircleShape).background(MafiaPurple.copy(0.3f)), contentAlignment = Alignment.Center) { Text(player.avatarEmoji, fontSize = 22.sp) }
                            Spacer(Modifier.width(12.dp))
                            Text(player.name, color = Color.White, fontSize = 16.sp, modifier = Modifier.weight(1f))
                            if (player.isHost) Surface(color = MafiaGold.copy(0.3f), shape = RoundedCornerShape(6.dp)) { Text("👑 Host", Modifier.padding(horizontal = 8.dp, vertical = 2.dp), fontSize = 11.sp, color = MafiaGold) }
                            if (player.isAI) Surface(color = MafiaPurple.copy(0.3f), shape = RoundedCornerShape(6.dp)) { Text("🤖 AI", Modifier.padding(horizontal = 8.dp, vertical = 2.dp), fontSize = 11.sp, color = MafiaPurple) }
                        }
                    }
                }
            }

            // Game Config (host only)
            if (isHost) {
                Spacer(Modifier.height(20.dp))

                // Slot math:
                //   Classic core: Mafia + Doctor + Townsfolk (townsfolk must outnumber mafia)
                //   maxSpecials = playerCount - 2×mafiaCount - 1  (1 Doctor base + optional slots)
                //   optionalSlots = maxSpecials - 1
                val playerCount = room.playerCount
                val mafiaCount = when (playerCount) { in 5..6 -> 1; in 7..8 -> 2; else -> 3 }
                val maxSpecials = playerCount - 2 * mafiaCount - 1
                val optionalSlots = maxOf(0, maxSpecials - 1)   // subtract 1 for Doctor (base)
                val enabledOptionals = listOf(
                    settings.enableDetective, settings.enableVigilante,
                    settings.enableEscort, settings.enableMinister
                ).count { it }
                val isClassic = enabledOptionals == 0

                Surface(color = Color.White.copy(0.06f), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("⚙️ Game Settings", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White, modifier = Modifier.weight(1f))
                            TextButton(onClick = { showConfig = !showConfig }) {
                                Text(if (showConfig) "▲ Hide" else "▼ Show", fontSize = 12.sp, color = MafiaPurple)
                            }
                        }
                        if (showConfig) {
                            Spacer(Modifier.height(12.dp))

                            // Classic / Custom preset row
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                PresetChip(
                                    label = "Classic",
                                    subtitle = "Mafia · Doctor · Townsfolk",
                                    selected = isClassic,
                                    onClick = {
                                        settings = settings.copy(
                                            enableDetective = false, enableVigilante = false,
                                            enableEscort = false, enableMinister = false
                                        )
                                        onUpdateSettings(settings)
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                PresetChip(
                                    label = "Custom",
                                    subtitle = "Choose specials below",
                                    selected = !isClassic,
                                    onClick = {},   // just a visual indicator; toggling any special switches to custom
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Spacer(Modifier.height(12.dp))
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text("Special Roles", fontSize = 12.sp, color = Color.White.copy(0.5f), fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                Text("$enabledOptionals / $optionalSlots slots used", fontSize = 11.sp, color = if (enabledOptionals >= optionalSlots && optionalSlots > 0) MafiaRed.copy(0.8f) else Color.White.copy(0.35f))
                            }
                            Spacer(Modifier.height(4.dp))

                            // Doctor — always on (base classic role)
                            RoleToggleRow("💉 Doctor", "Protects one player per night", enabled = true, checked = true, required = true) {}

                            // Detective — optional special
                            RoleToggleRow("🔍 Detective", "Investigates one player per night",
                                enabled = settings.enableDetective || enabledOptionals < optionalSlots,
                                checked = settings.enableDetective,
                                lockedReason = if (!settings.enableDetective && enabledOptionals >= optionalSlots) "Need more players" else null
                            ) { settings = settings.copy(enableDetective = it); onUpdateSettings(settings) }

                            // Vigilante
                            RoleToggleRow("🤠 Vigilante", "Shoots a player at night (backfires if innocent)",
                                enabled = settings.enableVigilante || enabledOptionals < optionalSlots,
                                checked = settings.enableVigilante,
                                lockedReason = if (!settings.enableVigilante && enabledOptionals >= optionalSlots) "Need more players" else null
                            ) { settings = settings.copy(enableVigilante = it); onUpdateSettings(settings) }

                            // Escort
                            RoleToggleRow("💃 Escort", "Blocks a player's night action",
                                enabled = settings.enableEscort || enabledOptionals < optionalSlots,
                                checked = settings.enableEscort,
                                lockedReason = if (!settings.enableEscort && enabledOptionals >= optionalSlots) "Need more players" else null
                            ) { settings = settings.copy(enableEscort = it); onUpdateSettings(settings) }

                            // Minister
                            RoleToggleRow("🏛️ Minister", "Secret one-time veto on an elimination vote",
                                enabled = settings.enableMinister || enabledOptionals < optionalSlots,
                                checked = settings.enableMinister,
                                lockedReason = if (!settings.enableMinister && enabledOptionals >= optionalSlots) "Need more players" else null
                            ) { settings = settings.copy(enableMinister = it); onUpdateSettings(settings) }

                            Spacer(Modifier.height(12.dp))
                            Text("Options", fontSize = 12.sp, color = Color.White.copy(0.5f), fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(8.dp))
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text("Reveal role on death", fontSize = 14.sp, color = Color.White)
                                    Text("Show eliminated player's role", fontSize = 12.sp, color = Color.White.copy(0.4f))
                                }
                                Switch(checked = settings.revealRoleOnDeath, onCheckedChange = { settings = settings.copy(revealRoleOnDeath = it); onUpdateSettings(settings) }, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = MafiaPurple))
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            if (isHost) {
                Button(onClick = onStartGame, enabled = room.canStart, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = TownGreen), shape = RoundedCornerShape(14.dp)) {
                    Text(if (room.canStart) "Start Game 🎮" else "Need ${room.minPlayers - room.playerCount} more", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            } else {
                Text("Waiting for host to start...", color = Color.White.copy(0.5f), fontSize = 14.sp)
            }
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = onLeave) { Text("← Leave Room", color = MafiaRed.copy(0.7f)) }
        }
    }
}

@Composable
private fun RoleToggleRow(
    name: String, description: String,
    enabled: Boolean, checked: Boolean,
    required: Boolean = false,
    lockedReason: String? = null,
    onToggle: (Boolean) -> Unit
) {
    val isInteractive = enabled && !required
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(name, fontSize = 14.sp, color = if (isInteractive || checked) Color.White else Color.White.copy(0.4f))
                Spacer(Modifier.width(6.dp))
                when {
                    required -> Surface(color = Color.White.copy(0.1f), shape = RoundedCornerShape(4.dp)) {
                        Text("Required", Modifier.padding(horizontal = 6.dp, vertical = 1.dp), fontSize = 10.sp, color = Color.White.copy(0.4f))
                    }
                    lockedReason != null -> Surface(color = MafiaRed.copy(0.15f), shape = RoundedCornerShape(4.dp)) {
                        Text(lockedReason, Modifier.padding(horizontal = 6.dp, vertical = 1.dp), fontSize = 10.sp, color = MafiaRed.copy(0.8f))
                    }
                }
            }
            Text(description, fontSize = 12.sp, color = Color.White.copy(0.4f))
        }
        Switch(
            checked = checked,
            onCheckedChange = if (isInteractive) onToggle else { _ -> },
            enabled = isInteractive,
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = MafiaPurple, disabledCheckedTrackColor = MafiaPurple.copy(0.4f))
        )
    }
}

@Composable
private fun PresetChip(
    label: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (selected) MafiaPurple else Color.White.copy(0.15f)
    val bgColor = if (selected) MafiaPurple.copy(0.2f) else Color.Transparent
    Surface(
        color = bgColor,
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = if (selected) MafiaPurple else Color.White.copy(0.6f))
            Text(subtitle, fontSize = 10.sp, color = Color.White.copy(0.35f))
        }
    }
}
