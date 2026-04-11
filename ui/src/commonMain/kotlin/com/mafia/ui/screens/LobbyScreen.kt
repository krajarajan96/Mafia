package com.mafia.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mafia.shared.model.GameMode
import com.mafia.ui.theme.*

@Composable
fun LobbyScreen(isMultiplayer: Boolean, onCreateRoom: (String, String, GameMode) -> Unit, onJoinRoom: (String, String, String) -> Unit, onBack: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var roomCode by remember { mutableStateOf("") }
    var selectedEmoji by remember { mutableStateOf("🕵️") }
    var showJoin by remember { mutableStateOf(false) }
    val emojis = listOf("🕵️", "🦊", "🌹", "🌙", "🎭", "🦋", "🤠", "🌿", "🦖", "🌶️", "🐺", "🎩")

    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF0F0A1E), Color(0xFF1A1145)))).imePadding(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.verticalScroll(rememberScrollState()).padding(24.dp)) {
            Text(if (isMultiplayer) "👥 Multiplayer" else "🕵️ Single Player", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text("Choose your avatar", color = Color.White.copy(0.7f), fontSize = 14.sp)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                emojis.chunked(4).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { emoji ->
                            FilledTonalButton(onClick = { selectedEmoji = emoji }, modifier = Modifier.size(56.dp), shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(containerColor = if (emoji == selectedEmoji) MafiaPurple else Color.White.copy(0.1f)),
                                contentPadding = PaddingValues(0.dp)) { Text(emoji, fontSize = 24.sp) }
                        }
                    }
                }
            }
            OutlinedTextField(value = name, onValueChange = { if (it.length <= 12) name = it }, label = { Text("Your Name") }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MafiaPurple, unfocusedBorderColor = Color.White.copy(0.3f), focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedLabelColor = MafiaPurple, unfocusedLabelColor = Color.White.copy(0.5f)))
            if (isMultiplayer && showJoin) {
                OutlinedTextField(value = roomCode, onValueChange = { if (it.length <= 6) roomCode = it.uppercase() }, label = { Text("Room Code") }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MafiaGold, unfocusedBorderColor = Color.White.copy(0.3f), focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedLabelColor = MafiaGold, unfocusedLabelColor = Color.White.copy(0.5f)))
                Button(onClick = { if (name.isNotBlank() && roomCode.length == 6) onJoinRoom(roomCode, name, selectedEmoji) }, modifier = Modifier.fillMaxWidth().height(52.dp), enabled = name.isNotBlank() && roomCode.length == 6,
                    colors = ButtonDefaults.buttonColors(containerColor = MafiaGold), shape = RoundedCornerShape(14.dp)) { Text("Join Room", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.Black) }
            }
            if (!showJoin || !isMultiplayer) {
                Button(onClick = { if (name.isNotBlank()) onCreateRoom(name, selectedEmoji, if (isMultiplayer) GameMode.MULTIPLAYER else GameMode.SINGLE_PLAYER) }, modifier = Modifier.fillMaxWidth().height(52.dp), enabled = name.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = MafiaPurple), shape = RoundedCornerShape(14.dp)) { Text(if (isMultiplayer) "Create Room" else "Start Game", fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
            }
            if (isMultiplayer && !showJoin) TextButton(onClick = { showJoin = true }) { Text("Have a room code? Join here", color = MafiaGold, fontSize = 14.sp) }
            TextButton(onClick = onBack) { Text("← Back", color = Color.White.copy(0.5f)) }
        }
    }
}
