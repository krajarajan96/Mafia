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
    Column(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0F0A1E), Color(0xFF1A1145))))
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("🎭 How to Play", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(Modifier.height(4.dp))
        Text(
            "A game of deception, deduction, and survival",
            fontSize = 14.sp, color = Color.White.copy(0.6f), textAlign = TextAlign.Center
        )

        // ── OBJECTIVE ──────────────────────────────────────────────
        Spacer(Modifier.height(24.dp))
        SectionHeader("🎯 Objective")
        InfoCard(
            "⚔️", "Two Teams, One Winner",
            "The Town must root out and eliminate all Mafia members before they are outnumbered. " +
            "The Mafia must eliminate enough Town players to gain majority control.",
            MafiaGold
        )

        // ── ROLES ──────────────────────────────────────────────────
        Spacer(Modifier.height(20.dp))
        SectionHeader("👥 Roles")

        InfoCard(
            "👤", "Townsfolk  (Town)",
            "You have no special night ability. Your power is in discussion — observe behaviour, " +
            "call out suspicious players, and vote wisely. Every elimination matters.",
            TownGreen
        )
        InfoCard(
            "🔍", "Detective  (Town)",
            "Each night you secretly investigate one player and learn whether they are Mafia or innocent. " +
            "During Discussion you can reveal your findings with the Reveal button, or stay silent to protect your identity. " +
            "Use your knowledge carefully — the Mafia will try to eliminate you once exposed.",
            MafiaPurple
        )
        InfoCard(
            "💉", "Doctor  (Town)",
            "Each night you choose one player to protect. If the Mafia targets that player, " +
            "the kill is blocked and they survive. You can protect yourself, but you cannot save the same person two nights in a row.",
            TownGreen
        )
        InfoCard(
            "🔪", "Mafia",
            "Each night the Mafia collectively eliminates one Town player. During the day you must blend in, " +
            "cast suspicion on innocents, and avoid being voted out. " +
            "Win by outnumbering the remaining Town players.",
            MafiaRed
        )

        // ── ROLE DISTRIBUTION ──────────────────────────────────────
        Spacer(Modifier.height(20.dp))
        SectionHeader("🎲 Role Distribution")
        Surface(
            color = Color.White.copy(0.06f),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DistributionRow("5–6 players", "1 Mafia · 1 Detective · 1 Doctor · 2–3 Townsfolk")
                DistributionRow("7–8 players", "2 Mafia · 1 Detective · 1 Doctor · 3–4 Townsfolk")
                DistributionRow("9–10 players", "3 Mafia · 1 Detective · 1 Doctor · 5–6 Townsfolk")
            }
        }

        // ── GAME FLOW ──────────────────────────────────────────────
        Spacer(Modifier.height(20.dp))
        SectionHeader("⏳ Game Flow")

        FlowStep("1", "🎭", "Role Reveal  (8 s)",
            "The game begins. Your secret role is shown. Remember it — you cannot check it again mid-game.",
            MafiaPurple)
        FlowStep("2", "🌙", "Night  (30 s)",
            "The town sleeps. Each role with a night action selects a target:\n" +
            "• Mafia picks someone to eliminate\n" +
            "• Detective picks someone to investigate\n" +
            "• Doctor picks someone to protect\n" +
            "Townsfolk wait for dawn.",
            Color(0xFF6366F1))
        FlowStep("3", "🌅", "Night Result  (6 s)",
            "The outcome of the night is announced — who was eliminated (if anyone), " +
            "and whether a save occurred. Roles are revealed on death by default.",
            DawnOrange)
        FlowStep("4", "💬", "Discussion  (90 s)",
            "All living players debate openly. Share suspicions, defend yourself, " +
            "or (if you are the Detective) tap Reveal to broadcast your investigation result to the group. " +
            "Mafia players must lie convincingly to survive.",
            MafiaGold)
        FlowStep("5", "🗳️", "Voting  (30 s)",
            "Each player casts one vote for who they believe is Mafia. " +
            "The player with the most votes is eliminated. Ties result in no elimination.",
            MafiaRed)
        FlowStep("6", "⚰️", "Elimination  (6 s)",
            "The voted-out player's role is revealed. The game then checks win conditions " +
            "before cycling back to Night.",
            DeadGray)

        // ── WIN CONDITIONS ─────────────────────────────────────────
        Spacer(Modifier.height(20.dp))
        SectionHeader("🏆 Win Conditions")
        InfoCard("🏙️", "Town Wins",
            "All Mafia members have been eliminated. The town is safe!", TownGreen)
        InfoCard("🔪", "Mafia Wins",
            "The number of Mafia players equals or exceeds the remaining Town players. " +
            "Resistance is futile.", MafiaRed)

        // ── TIPS ───────────────────────────────────────────────────
        Spacer(Modifier.height(20.dp))
        SectionHeader("💡 Tips")
        Surface(
            color = Color.White.copy(0.06f),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                TipRow("🔍", "Detective: Reveal early if you find Mafia — waiting lets them kill more.")
                TipRow("💉", "Doctor: Protect the Detective once identified; they are the Mafia's top target.")
                TipRow("🔪", "Mafia: Vote with the Town early to avoid suspicion, then steer votes wrong.")
                TipRow("👤", "Townsfolk: Track who votes for whom across rounds — patterns expose Mafia.")
                TipRow("🗳️", "Never skip a vote — a tie is a wasted round and helps the Mafia.")
            }
        }

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onBack,
            Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MafiaPurple),
            shape = RoundedCornerShape(14.dp)
        ) { Text("Got it! Let's Play 🎮", fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MafiaGold)
    Spacer(Modifier.height(10.dp))
}

@Composable
private fun InfoCard(emoji: String, title: String, body: String, accent: Color = MafiaPurple) {
    Surface(
        color = Color.White.copy(0.06f),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
    ) {
        Row(Modifier.padding(16.dp)) {
            Text(emoji, fontSize = 28.sp, modifier = Modifier.padding(end = 12.dp))
            Column {
                Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = accent)
                Spacer(Modifier.height(4.dp))
                Text(body, fontSize = 13.sp, color = Color.White.copy(0.75f), lineHeight = 19.sp)
            }
        }
    }
}

@Composable
private fun FlowStep(step: String, emoji: String, title: String, body: String, accent: Color) {
    Surface(
        color = Color.White.copy(0.06f),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
            // Step number badge
            Surface(color = accent.copy(0.25f), shape = RoundedCornerShape(8.dp)) {
                Text(
                    step, Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                    fontSize = 13.sp, fontWeight = FontWeight.Black, color = accent
                )
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(emoji, fontSize = 18.sp)
                    Spacer(Modifier.width(6.dp))
                    Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = accent)
                }
                Spacer(Modifier.height(4.dp))
                Text(body, fontSize = 13.sp, color = Color.White.copy(0.75f), lineHeight = 19.sp)
            }
        }
    }
}

@Composable
private fun DistributionRow(players: String, roles: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Text(players, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MafiaGold, modifier = Modifier.width(100.dp))
        Text(roles, fontSize = 13.sp, color = Color.White.copy(0.75f), modifier = Modifier.weight(1f))
    }
}

@Composable
private fun TipRow(emoji: String, text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Text(emoji, fontSize = 16.sp, modifier = Modifier.padding(end = 8.dp, top = 1.dp))
        Text(text, fontSize = 13.sp, color = Color.White.copy(0.75f), lineHeight = 19.sp, modifier = Modifier.weight(1f))
    }
}
