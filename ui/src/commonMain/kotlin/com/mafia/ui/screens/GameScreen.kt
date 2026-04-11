package com.mafia.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mafia.shared.model.*
import com.mafia.shared.network.messages.ServerMessage
import com.mafia.ui.theme.*

@Composable
fun GameScreen(
    phase: GamePhase, round: Int, myRole: Role?, myPlayerId: String?,
    alivePlayers: List<PlayerPublicInfo>, chatMessages: List<ChatMessage>,
    timerSeconds: Int, voteTally: Map<String, Int>,
    detectiveResult: ServerMessage.DetectiveResult?,
    nightSummary: ServerMessage.NightSummary?,
    voteResult: ServerMessage.VoteResult?,
    voteLog: List<VoteEntry>,
    lastEvent: ServerMessage?,
    onNightAction: (String) -> Unit, onSendChat: (String) -> Unit,
    onVote: (String) -> Unit, onSkipVote: () -> Unit, onLeave: () -> Unit
) {
    val bgGradient = when {
        phase.isNightPhase() -> Brush.verticalGradient(listOf(Color(0xFF0A0520), Color(0xFF1A0A3E)))
        else -> Brush.verticalGradient(listOf(Color(0xFF1A1530), Color(0xFF0F172A)))
    }
    Column(Modifier.fillMaxSize().background(bgGradient)) {
        // Phase Header
        Surface(color = Color.Black.copy(0.4f), modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    if (round > 0) Text("Round $round", fontSize = 12.sp, color = Color.White.copy(0.5f))
                    Text(phase.displayName, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MafiaPurple)
                }
                myRole?.let {
                    Surface(color = if (it.isMafia()) MafiaRed.copy(0.3f) else TownGreen.copy(0.3f), shape = RoundedCornerShape(8.dp)) {
                        Text("${it.emoji} ${it.displayName}", Modifier.padding(horizontal = 10.dp, vertical = 4.dp), fontSize = 12.sp, color = Color.White)
                    }
                }
                if (timerSeconds > 0) {
                    Spacer(Modifier.width(12.dp))
                    Surface(color = if (timerSeconds <= 10) MafiaRed.copy(0.8f) else Color.White.copy(0.15f), shape = CircleShape) {
                        Text("${timerSeconds}s", Modifier.padding(horizontal = 12.dp, vertical = 6.dp), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }

        // Main Content
        Box(Modifier.weight(1f)) {
            when (phase) {
                GamePhase.ROLE_REVEAL -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(myRole?.emoji ?: "?", fontSize = 80.sp)
                        Spacer(Modifier.height(16.dp))
                        Text("You are", fontSize = 16.sp, color = Color.White.copy(0.6f))
                        Text(myRole?.displayName ?: "Unknown", fontSize = 36.sp, fontWeight = FontWeight.Black, color = if (myRole?.isMafia() == true) MafiaRed else TownGreen)
                        Spacer(Modifier.height(8.dp))
                        Text(myRole?.description ?: "", fontSize = 14.sp, color = Color.White.copy(0.7f), textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 48.dp))
                    }
                }
                GamePhase.NIGHT -> {
                    var selected by remember { mutableStateOf<String?>(null) }
                    val targets = alivePlayers.filter { it.id != myPlayerId }
                    Column(Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🌙 The town sleeps...", fontSize = 18.sp, color = Color.White.copy(0.8f))
                        val instr = when (myRole) { Role.MAFIA -> "Choose a player to eliminate"; Role.DETECTIVE -> "Choose a player to investigate"; Role.DOCTOR -> "Choose a player to protect"; else -> "Wait for dawn..." }
                        Text(instr, fontSize = 14.sp, color = MafiaPurple.copy(0.8f))
                        if (myRole?.hasNightAction == true) {
                            Spacer(Modifier.height(16.dp))
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(targets) { p ->
                                    Surface(Modifier.fillMaxWidth().clickable { selected = p.id }, color = if (selected == p.id) MafiaPurple.copy(0.4f) else Color.White.copy(0.08f), shape = RoundedCornerShape(12.dp)) {
                                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Text(p.avatarEmoji, fontSize = 28.sp); Spacer(Modifier.width(12.dp)); Text(p.name, color = Color.White, fontSize = 16.sp) }
                                    }
                                }
                            }
                            Spacer(Modifier.height(16.dp))
                            Button(onClick = { selected?.let { onNightAction(it); selected = null } }, enabled = selected != null, colors = ButtonDefaults.buttonColors(containerColor = MafiaPurple), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().height(48.dp)) { Text("Confirm", fontWeight = FontWeight.SemiBold) }
                        }
                    }
                }
                GamePhase.NIGHT_RESULT -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                            when {
                                nightSummary?.wasSaved == true -> {
                                    Text("💚", fontSize = 72.sp)
                                    Spacer(Modifier.height(12.dp))
                                    Text("Someone was saved!", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TownGreen, textAlign = TextAlign.Center)
                                    Text("The Doctor protected their target tonight.", fontSize = 14.sp, color = Color.White.copy(0.6f), textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
                                }
                                nightSummary?.eliminatedPlayer != null -> {
                                    val dead = nightSummary.eliminatedPlayer
                                    Text("💀", fontSize = 72.sp)
                                    Spacer(Modifier.height(12.dp))
                                    Text("${dead!!.avatarEmoji} ${dead.name}", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MafiaRed, textAlign = TextAlign.Center)
                                    Text("was killed by the Mafia", fontSize = 16.sp, color = Color.White.copy(0.7f), textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
                                    nightSummary.eliminatedRole?.let { role ->
                                        Spacer(Modifier.height(8.dp))
                                        Surface(color = Color.White.copy(0.1f), shape = RoundedCornerShape(8.dp)) {
                                            Text("They were ${role.emoji} ${role.displayName}", Modifier.padding(horizontal = 12.dp, vertical = 6.dp), fontSize = 13.sp, color = Color.White.copy(0.8f))
                                        }
                                    }
                                }
                                else -> {
                                    Text("🌙", fontSize = 72.sp)
                                    Spacer(Modifier.height(12.dp))
                                    Text("A quiet night...", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center)
                                    Text("No one was harmed tonight.", fontSize = 14.sp, color = Color.White.copy(0.6f), textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
                                }
                            }
                        }
                    }
                }
                GamePhase.DISCUSSION -> {
                    var chatText by remember { mutableStateOf("") }
                    val listState = rememberLazyListState()
                    LaunchedEffect(chatMessages.size) { if (chatMessages.isNotEmpty()) listState.animateScrollToItem(chatMessages.size - 1) }
                    Column(Modifier.fillMaxSize()) {
                        // Detective: show investigation result banner + reveal button
                        if (myRole == Role.DETECTIVE && detectiveResult != null) {
                            val result = detectiveResult
                            Surface(
                                color = if (result.isMafia) MafiaRed.copy(0.15f) else TownGreen.copy(0.15f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        if (result.isMafia) "🔴 ${result.targetName} is Mafia" else "🟢 ${result.targetName} is innocent",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    OutlinedButton(
                                        onClick = {
                                            val verdict = if (result.isMafia) "MAFIA" else "innocent"
                                            onSendChat("🔍 [Detective] I investigated ${result.targetName} — they are $verdict!")
                                        },
                                        border = ButtonDefaults.outlinedButtonBorder,
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text("Reveal", fontSize = 12.sp, color = Color.White)
                                    }
                                }
                            }
                        }
                        LazyColumn(state = listState, modifier = Modifier.weight(1f).padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp), contentPadding = PaddingValues(vertical = 8.dp)) {
                            items(chatMessages) { msg ->
                                val isMine = msg.senderId == myPlayerId
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start) {
                                    Surface(color = if (isMine) MafiaPurple.copy(0.4f) else Color.White.copy(0.1f), shape = RoundedCornerShape(12.dp)) {
                                        Column(Modifier.padding(10.dp).widthIn(max = 260.dp)) {
                                            if (!isMine) Text(msg.senderName, fontSize = 11.sp, color = MafiaPurple, fontWeight = FontWeight.SemiBold)
                                            Text(msg.text, fontSize = 14.sp, color = Color.White)
                                        }
                                    }
                                }
                            }
                        }
                        Row(Modifier.fillMaxWidth().background(Color.Black.copy(0.4f)).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(value = chatText, onValueChange = { if (it.length <= 200) chatText = it }, modifier = Modifier.weight(1f), placeholder = { Text("Say something...", color = Color.White.copy(0.3f)) }, singleLine = true, shape = RoundedCornerShape(20.dp),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MafiaPurple, unfocusedBorderColor = Color.White.copy(0.2f), focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                            Spacer(Modifier.width(8.dp))
                            FilledIconButton(onClick = { if (chatText.isNotBlank()) { onSendChat(chatText); chatText = "" } }, colors = IconButtonDefaults.filledIconButtonColors(containerColor = MafiaPurple)) { Text("➤", fontSize = 18.sp, color = Color.White) }
                        }
                    }
                }
                GamePhase.VOTING -> {
                    var voted by remember { mutableStateOf(false) }
                    val targets = alivePlayers.filter { it.id != myPlayerId }
                    Column(Modifier.fillMaxSize()) {
                        // Live vote log
                        if (voteLog.isNotEmpty()) {
                            Surface(color = Color.Black.copy(0.3f), modifier = Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                    Text("Vote Log", fontSize = 12.sp, color = Color.White.copy(0.5f), fontWeight = FontWeight.SemiBold)
                                    Spacer(Modifier.height(4.dp))
                                    voteLog.takeLast(5).forEach { entry ->
                                        Text(
                                            if (entry.isSkip) "• ${entry.voterName} skipped"
                                            else "• ${entry.voterName} → ${entry.targetName}",
                                            fontSize = 12.sp,
                                            color = if (entry.isSkip) Color.White.copy(0.4f) else Color.White.copy(0.8f)
                                        )
                                    }
                                }
                            }
                        }
                        Column(Modifier.weight(1f).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🗳️ Vote to Eliminate", fontSize = 20.sp, color = MafiaRed, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(12.dp))
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                                items(targets) { p ->
                                    val votes = voteTally[p.id] ?: 0
                                    Surface(Modifier.fillMaxWidth().clickable(enabled = !voted) { onVote(p.id); voted = true }, color = Color.White.copy(0.08f), shape = RoundedCornerShape(12.dp)) {
                                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Text(p.avatarEmoji, fontSize = 28.sp); Spacer(Modifier.width(12.dp)); Text(p.name, color = Color.White, fontSize = 16.sp, modifier = Modifier.weight(1f))
                                            if (votes > 0) Surface(color = MafiaRed.copy(0.6f), shape = CircleShape) { Text("$votes", Modifier.padding(horizontal = 10.dp, vertical = 4.dp), color = Color.White, fontWeight = FontWeight.Bold) }
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            if (!voted) {
                                OutlinedButton(
                                    onClick = { onSkipVote(); voted = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonColors(
                                        containerColor = Color.Transparent,
                                        contentColor = Color.White.copy(0.6f),
                                        disabledContainerColor = Color.Transparent,
                                        disabledContentColor = Color.White.copy(0.3f)
                                    )
                                ) { Text("Skip / Abstain") }
                            } else {
                                Text("✓ Vote cast", color = TownGreen, fontSize = 14.sp)
                            }
                        }
                    }
                }
                GamePhase.ELIMINATION -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                            val eliminated = voteResult?.eliminatedPlayer
                            if (eliminated != null) {
                                Text("⚖️", fontSize = 64.sp)
                                Spacer(Modifier.height(12.dp))
                                Text("${eliminated.avatarEmoji} ${eliminated.name}", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MafiaRed, textAlign = TextAlign.Center)
                                Text("has been eliminated by vote", fontSize = 16.sp, color = Color.White.copy(0.7f), textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
                                voteResult.eliminatedRole?.let { role ->
                                    Spacer(Modifier.height(8.dp))
                                    Surface(color = if (role.isMafia()) MafiaRed.copy(0.2f) else TownGreen.copy(0.2f), shape = RoundedCornerShape(8.dp)) {
                                        Text("They were ${role.emoji} ${role.displayName}", Modifier.padding(horizontal = 12.dp, vertical = 6.dp), fontSize = 13.sp, color = Color.White.copy(0.9f))
                                    }
                                }
                            } else if (voteResult?.wasTie == true) {
                                Text("🤝", fontSize = 64.sp)
                                Spacer(Modifier.height(12.dp))
                                Text("It's a tie!", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center)
                                Text("No one was eliminated.", fontSize = 16.sp, color = Color.White.copy(0.6f), textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
                            } else {
                                Text("🕊️", fontSize = 64.sp)
                                Spacer(Modifier.height(12.dp))
                                Text("No elimination", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center)
                            }

                            // Vote summary
                            if (voteLog.isNotEmpty()) {
                                Spacer(Modifier.height(24.dp))
                                Surface(color = Color.White.copy(0.08f), shape = RoundedCornerShape(12.dp)) {
                                    Column(Modifier.padding(12.dp)) {
                                        Text("Vote Summary", fontSize = 13.sp, color = Color.White.copy(0.5f), fontWeight = FontWeight.SemiBold)
                                        Spacer(Modifier.height(6.dp))
                                        voteLog.forEach { entry ->
                                            Text(
                                                if (entry.isSkip) "• ${entry.voterName} — abstained"
                                                else "• ${entry.voterName} voted for ${entry.targetName}",
                                                fontSize = 13.sp,
                                                color = if (entry.isSkip) Color.White.copy(0.4f) else Color.White.copy(0.8f),
                                                modifier = Modifier.padding(vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                GamePhase.GAME_OVER -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        val event = lastEvent as? ServerMessage.GameOver
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                            val isTownWin = event?.winner == Team.TOWN
                            Text(if (isTownWin) "🎉" else "🔪", fontSize = 72.sp)
                            Spacer(Modifier.height(16.dp))
                            Text(if (isTownWin) "Town Wins!" else "Mafia Wins!", fontSize = 32.sp, fontWeight = FontWeight.Black, color = if (isTownWin) TownGreen else MafiaRed)
                            Spacer(Modifier.height(32.dp))
                            Button(onClick = onLeave, colors = ButtonDefaults.buttonColors(containerColor = MafiaPurple), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Back to Menu", fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
                        }
                    }
                }
                else -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(phase.description, color = Color.White.copy(0.6f)) }
                }
            }
        }

        // Player strip
        if (phase != GamePhase.GAME_OVER && phase != GamePhase.ROLE_REVEAL) {
            Surface(color = Color.Black.copy(0.4f)) {
                Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    alivePlayers.take(8).forEach { p ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(Modifier.size(36.dp).clip(CircleShape).background(if (p.id == myPlayerId) MafiaPurple.copy(0.4f) else Color.Transparent), contentAlignment = Alignment.Center) { Text(p.avatarEmoji, fontSize = 20.sp) }
                            Text(if (p.id == myPlayerId) "You" else p.name, fontSize = 10.sp, color = if (p.isAlive) Color.White.copy(0.7f) else DeadGray, textDecoration = if (!p.isAlive) TextDecoration.LineThrough else null, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}
