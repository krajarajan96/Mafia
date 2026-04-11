package com.mafia.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mafia.ui.theme.*

@Composable
fun HomeScreen(onSinglePlayer: () -> Unit, onMultiplayer: () -> Unit, onHowToPlay: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition()
    val pulse by infiniteTransition.animateFloat(1f, 1.05f, infiniteRepeatable(tween(1500, easing = EaseInOutSine), RepeatMode.Reverse))

    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF0F0A1E), Color(0xFF1A1145), Color(0xFF0F0A1E)))), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(24.dp), modifier = Modifier.padding(32.dp)) {
            Text("🎭", fontSize = 72.sp, modifier = Modifier.scale(pulse))
            Text("MAFIA", fontSize = 48.sp, fontWeight = FontWeight.Black, color = Color.White, letterSpacing = 8.sp)
            Text("Social Deduction", fontSize = 16.sp, color = MafiaPurple.copy(alpha = 0.8f), letterSpacing = 4.sp)
            Spacer(Modifier.height(32.dp))
            Button(onClick = onSinglePlayer, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = MafiaPurple), shape = RoundedCornerShape(16.dp)) {
                Text("🕵️  Single Player", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            }
            Button(onClick = onMultiplayer, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = MafiaRed), shape = RoundedCornerShape(16.dp)) {
                Text("👥  Play with Friends", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            }
            TextButton(onClick = onHowToPlay) { Text("How to Play", color = Color.White.copy(alpha = 0.6f), fontSize = 14.sp) }
        }
    }
}
