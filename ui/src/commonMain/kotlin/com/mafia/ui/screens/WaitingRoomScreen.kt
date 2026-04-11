package com.mafia.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mafia.shared.model.Room
import com.mafia.ui.theme.*

@Composable
fun WaitingRoomScreen(room: Room, myPlayerId: String, onStartGame: () -> Unit, onLeave: () -> Unit) {
    val clipboardManager = LocalClipboardManager.current
    val isHost = room.hostId == myPlayerId
    val dots = rememberInfiniteTransition()
    val dotScale by dots.animateFloat(1f, 1.2f, infiniteRepeatable(tween(800), RepeatMode.Reverse))

    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF0F0A1E), Color(0xFF1A1145)))).padding(24.dp)) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Waiting Room", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.height(16.dp))
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
            Text("${room.playerCount}/${room.maxPlayers} Players", fontSize = 14.sp, color = Color.White.copy(0.6f))
            Spacer(Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
            Spacer(Modifier.weight(1f))
            if (isHost) {
                Button(onClick = onStartGame, enabled = room.canStart, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = TownGreen), shape = RoundedCornerShape(14.dp)) {
                    Text(if (room.canStart) "Start Game 🎮" else "Need ${room.minPlayers - room.playerCount} more", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            } else Text("Waiting for host to start...", color = Color.White.copy(0.5f), fontSize = 14.sp)
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = onLeave) { Text("← Leave Room", color = MafiaRed.copy(0.7f)) }
        }
    }
}
