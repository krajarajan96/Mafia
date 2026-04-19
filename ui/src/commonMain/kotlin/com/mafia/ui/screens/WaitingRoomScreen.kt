package com.mafia.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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

    Box(
        Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0F0A1E), Color(0xFF1A1145))))
            .padding(24.dp)
    ) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Waiting Room", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.height(16.dp))

            // Room code card
            Surface(color = Color.White.copy(0.08f), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Room Code", fontSize = 12.sp, color = Color.White.copy(0.5f))
                    Spacer(Modifier.height(4.dp))
                    Text(room.code, fontSize = 36.sp, fontWeight = FontWeight.Black, color = MafiaGold, letterSpacing = 6.sp, fontFamily = FontFamily.Monospace)
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { clipboardManager.setText(AnnotatedString(room.code)) }) {
                        Text("📋 Copy Code", color = MafiaGold.copy(0.8f), fontSize = 13.sp)
                    }
                }
            }
            Spacer(Modifier.height(24.dp))

            // Players list with bot fill info
            val humanCount = room.playerCount
            val botCount = maxOf(0, settings.maxPlayers - humanCount)
            Text(
                "$humanCount / ${settings.maxPlayers} players joined · $botCount AI bot${if (botCount != 1) "s" else ""} will fill",
                fontSize = 13.sp, color = Color.White.copy(0.6f)
            )
            Spacer(Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                room.players.forEach { player ->
                    Surface(color = Color.White.copy(0.08f), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(40.dp).clip(CircleShape).background(MafiaPurple.copy(0.3f)), contentAlignment = Alignment.Center) {
                                Text(player.avatarEmoji, fontSize = 22.sp)
                            }
                            Spacer(Modifier.width(12.dp))
                            Text(player.name, color = Color.White, fontSize = 16.sp, modifier = Modifier.weight(1f))
                            if (player.isHost) Surface(color = MafiaGold.copy(0.3f), shape = RoundedCornerShape(6.dp)) {
                                Text("👑 Host", Modifier.padding(horizontal = 8.dp, vertical = 2.dp), fontSize = 11.sp, color = MafiaGold)
                            }
                        }
                    }
                }
            }

            // Game Config (host only)
            if (isHost) {
                Spacer(Modifier.height(20.dp))

                // Slot math based on maxPlayers setting
                val totalPlayers = settings.maxPlayers
                val mafiaCount = when (totalPlayers) { in 5..6 -> 1; in 7..8 -> 2; else -> 3 }
                val maxSpecials = totalPlayers - 2 * mafiaCount - 1
                val optionalSlots = maxOf(0, maxSpecials - 1)
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

                            // Max players stepper
                            Text("Players", fontSize = 12.sp, color = Color.White.copy(0.5f), fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(8.dp))
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text("Total players", fontSize = 14.sp, color = Color.White)
                                    Text("${settings.maxPlayers} total · $botCount AI bot${if (botCount != 1) "s" else ""}", fontSize = 12.sp, color = Color.White.copy(0.4f))
                                }
                                Surface(color = Color.White.copy(0.08f), shape = RoundedCornerShape(10.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        TextButton(
                                            onClick = { if (settings.maxPlayers > 5) { settings = settings.copy(maxPlayers = settings.maxPlayers - 1); onUpdateSettings(settings) } },
                                            enabled = settings.maxPlayers > 5,
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                        ) { Text("−", fontSize = 18.sp, color = if (settings.maxPlayers > 5) Color.White else Color.White.copy(0.3f)) }
                                        Text(
                                            "${settings.maxPlayers}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MafiaPurple,
                                            modifier = Modifier.width(28.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                        TextButton(
                                            onClick = { if (settings.maxPlayers < 10) { settings = settings.copy(maxPlayers = settings.maxPlayers + 1); onUpdateSettings(settings) } },
                                            enabled = settings.maxPlayers < 10,
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                        ) { Text("+", fontSize = 18.sp, color = if (settings.maxPlayers < 10) Color.White else Color.White.copy(0.3f)) }
                                    }
                                }
                            }
                            Spacer(Modifier.height(12.dp))

                            // Classic / Custom preset
                            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                PresetChip("Classic", "Mafia · Doctor · Townsfolk", isClassic, onClick = {
                                    settings = settings.copy(enableDetective = false, enableVigilante = false, enableEscort = false, enableMinister = false)
                                    onUpdateSettings(settings)
                                }, modifier = Modifier.weight(1f).fillMaxHeight())
                                PresetChip("Custom", "Choose specials below", !isClassic, onClick = {}, modifier = Modifier.weight(1f).fillMaxHeight())
                            }

                            Spacer(Modifier.height(12.dp))
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text("Special Roles", fontSize = 12.sp, color = Color.White.copy(0.5f), fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                Text("$enabledOptionals / $optionalSlots slots used", fontSize = 11.sp, color = if (enabledOptionals >= optionalSlots && optionalSlots > 0) MafiaRed.copy(0.8f) else Color.White.copy(0.35f))
                            }
                            Spacer(Modifier.height(4.dp))

                            RoleToggleRow("💉 Doctor", "Protects one player per night", enabled = true, checked = true, required = true) {}
                            RoleToggleRow("🔍 Detective", "Investigates one player per night",
                                enabled = settings.enableDetective || enabledOptionals < optionalSlots, checked = settings.enableDetective,
                                lockedReason = if (!settings.enableDetective && enabledOptionals >= optionalSlots) "Need more players" else null
                            ) { settings = settings.copy(enableDetective = it); onUpdateSettings(settings) }
                            RoleToggleRow("🤠 Vigilante", "Shoots a player at night (backfires if innocent)",
                                enabled = settings.enableVigilante || enabledOptionals < optionalSlots, checked = settings.enableVigilante,
                                lockedReason = if (!settings.enableVigilante && enabledOptionals >= optionalSlots) "Need more players" else null
                            ) { settings = settings.copy(enableVigilante = it); onUpdateSettings(settings) }
                            RoleToggleRow("💃 Escort", "Blocks a player's night action",
                                enabled = settings.enableEscort || enabledOptionals < optionalSlots, checked = settings.enableEscort,
                                lockedReason = if (!settings.enableEscort && enabledOptionals >= optionalSlots) "Need more players" else null
                            ) { settings = settings.copy(enableEscort = it); onUpdateSettings(settings) }
                            RoleToggleRow("🏛️ Minister", "Secret one-time veto on an elimination vote",
                                enabled = settings.enableMinister || enabledOptionals < optionalSlots, checked = settings.enableMinister,
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
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text("📜 Game History", fontSize = 14.sp, color = Color.White)
                                    Text("Show full event log (off = eliminations only)", fontSize = 12.sp, color = Color.White.copy(0.4f))
                                }
                                Switch(checked = settings.enableGameHistory, onCheckedChange = { settings = settings.copy(enableGameHistory = it); onUpdateSettings(settings) }, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = MafiaPurple))
                            }
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text("💡 Phase Tips", fontSize = 14.sp, color = Color.White)
                                    Text("Show hints at the start of each phase", fontSize = 12.sp, color = Color.White.copy(0.4f))
                                }
                                Switch(checked = settings.enableTips, onCheckedChange = { settings = settings.copy(enableTips = it); onUpdateSettings(settings) }, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = MafiaPurple))
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            if (isHost) {
                Button(
                    onClick = onStartGame, enabled = room.canStart,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TownGreen),
                    shape = RoundedCornerShape(14.dp)
                ) { Text("Start Game 🎮", fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
            } else {
                Text("Waiting for host to start...", color = Color.White.copy(0.5f), fontSize = 14.sp)
            }
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = onLeave) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MafiaRed.copy(0.7f), modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Leave Room", color = MafiaRed.copy(0.7f))
            }
        }
    }
}

@Composable
private fun RoleToggleRow(
    name: String, description: String,
    enabled: Boolean, checked: Boolean,
    required: Boolean = false, lockedReason: String? = null,
    onToggle: (Boolean) -> Unit
) {
    val isInteractive = enabled && !required
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(name, fontSize = 14.sp, color = if (isInteractive || checked) Color.White else Color.White.copy(0.4f), maxLines = 1)
                Spacer(Modifier.width(6.dp))
                when {
                    required -> Surface(color = Color.White.copy(0.1f), shape = RoundedCornerShape(4.dp)) {
                        Text("Required", Modifier.padding(horizontal = 6.dp, vertical = 1.dp), fontSize = 10.sp, color = Color.White.copy(0.4f))
                    }
                    lockedReason != null -> Surface(color = MafiaRed.copy(0.15f), shape = RoundedCornerShape(4.dp)) {
                        Text("Need more players", Modifier.padding(horizontal = 6.dp, vertical = 1.dp), fontSize = 10.sp, color = MafiaRed.copy(0.8f), maxLines = 1)
                    }
                }
            }
            Text(description, fontSize = 12.sp, color = Color.White.copy(0.4f), maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
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
private fun PresetChip(label: String, subtitle: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        color = if (selected) MafiaPurple.copy(0.2f) else Color.Transparent,
        shape = RoundedCornerShape(10.dp),
        modifier = modifier.border(1.dp, if (selected) MafiaPurple else Color.White.copy(0.15f), RoundedCornerShape(10.dp)).clickable(onClick = onClick)
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = if (selected) MafiaPurple else Color.White.copy(0.6f))
            Text(subtitle, fontSize = 10.sp, color = Color.White.copy(0.35f))
        }
    }
}
