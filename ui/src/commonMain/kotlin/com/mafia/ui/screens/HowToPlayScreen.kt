package com.mafia.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mafia.ui.theme.*

@Composable
fun HowToPlayScreen(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF0F0A1E), Color(0xFF1A1145)))).verticalScroll(rememberScrollState()).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("🎭 How to Play", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(Modifier.height(8.dp))
        Text("A game of deception, deduction, and survival", fontSize = 14.sp, color = Color.White.copy(0.6f), textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        InfoCard("📖", "The Setup", "Players are secretly assigned roles. Most are Townsfolk, but hidden among them are Mafia members. Find and eliminate them!")
        InfoCard("👤", "Townsfolk", "No special ability. Use wits and persuasion to find the Mafia.", TownGreen)
        InfoCard("🔍", "Detective", "Investigate one player each night to learn if they are Mafia.", MafiaPurple)
        InfoCard("💉", "Doctor", "Protect one player each night from elimination.", TownGreen)
        InfoCard("🔪", "Mafia", "Eliminate a player each night. Blend in during the day.", MafiaRed)
        Spacer(Modifier.height(8.dp))
        Text("⏳ Game Flow", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MafiaGold)
        Spacer(Modifier.height(12.dp))
        InfoCard("🌙", "Night (30s)", "Mafia eliminates, Detective investigates, Doctor protects.")
        InfoCard("💬", "Discussion (90s)", "Debate, accuse, defend — find the Mafia!")
        InfoCard("🗳️", "Voting (30s)", "Vote to eliminate. Most votes loses. Ties = no elimination.")
        Spacer(Modifier.height(16.dp))
        InfoCard("🏆", "Win Conditions", "Town wins: all Mafia eliminated. Mafia wins: outnumber Town.", MafiaGold)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onBack, Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = MafiaPurple), shape = RoundedCornerShape(14.dp)) { Text("Got it! Let's Play 🎮", fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun InfoCard(emoji: String, title: String, body: String, accent: Color = MafiaPurple) {
    Surface(color = Color.White.copy(0.06f), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(Modifier.padding(16.dp)) {
            Text(emoji, fontSize = 28.sp, modifier = Modifier.padding(end = 12.dp))
            Column { Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = accent); Spacer(Modifier.height(4.dp)); Text(body, fontSize = 13.sp, color = Color.White.copy(0.75f), lineHeight = 19.sp) }
        }
    }
}
