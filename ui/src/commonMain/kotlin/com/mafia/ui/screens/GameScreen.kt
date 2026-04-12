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

private enum class GameTab(val label: String, val icon: String) {
    VIDEO("Video", "🎥"),
    ARENA("Arena", "🎲"),
    CHAT("Chat", "💬")
}

@Composable
fun GameScreen(
    phase: GamePhase, round: Int, myRole: Role?, myPlayerId: String?,
    alivePlayers: List<PlayerPublicInfo>, chatMessages: List<ChatMessage>,
    timerSeconds: Int, voteTally: Map<String, Int>,
    detectiveResult: ServerMessage.DetectiveResult?,
    nightSummary: ServerMessage.NightSummary?,
    voteResult: ServerMessage.VoteResult?,
    voteLog: List<VoteEntry>,
    eventLog: List<GameEvent>,
    ministerVetoUsed: Boolean,
    lastEvent: ServerMessage?,
    onNightAction: (String) -> Unit, onSendChat: (String) -> Unit,
    onVote: (String) -> Unit, onSkipVote: () -> Unit,
    onUseVeto: () -> Unit, onLeave: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(GameTab.ARENA) }

    val bgGradient = when {
        phase.isNightPhase() -> Brush.verticalGradient(listOf(Color(0xFF0A0520), Color(0xFF1A0A3E)))
        else -> Brush.verticalGradient(listOf(Color(0xFF1A1530), Color(0xFF0F172A)))
    }

    Column(Modifier.fillMaxSize().background(bgGradient)) {
        // Phase Header (always visible)
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

        // Tab content area
        Box(Modifier.weight(1f)) {
            when (selectedTab) {
                GameTab.VIDEO -> VideoTab()
                GameTab.ARENA -> ArenaTab(
                    phase, round, myRole, myPlayerId, alivePlayers, timerSeconds,
                    voteTally, detectiveResult, nightSummary, voteResult, voteLog,
                    eventLog, ministerVetoUsed, lastEvent,
                    onNightAction, onSendChat, onVote, onSkipVote, onUseVeto, onLeave
                )
                GameTab.CHAT -> ChatTab(chatMessages, myPlayerId, onSendChat)
            }
        }

        // Player strip (always visible except GAME_OVER and ROLE_REVEAL)
        if (phase != GamePhase.GAME_OVER && phase != GamePhase.ROLE_REVEAL) {
            Surface(color = Color.Black.copy(0.4f)) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    alivePlayers.take(8).forEach { p ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(Modifier.size(36.dp).clip(CircleShape).background(if (p.id == myPlayerId) MafiaPurple.copy(0.4f) else Color.Transparent), contentAlignment = Alignment.Center) {
                                Text(p.avatarEmoji, fontSize = 20.sp)
                            }
                            Text(if (p.id == myPlayerId) "You" else p.name, fontSize = 10.sp, color = if (p.isAlive) Color.White.copy(0.7f) else DeadGray, textDecoration = if (!p.isAlive) TextDecoration.LineThrough else null, maxLines = 1)
                        }
                    }
                }
            }
        }

        // Bottom Tab Bar
        Surface(color = Color(0xFF0D0B1E)) {
            Row(Modifier.fillMaxWidth().height(56.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                GameTab.entries.forEach { tab ->
                    val selected = selectedTab == tab
                    Column(
                        Modifier.weight(1f).fillMaxHeight().clickable { selectedTab = tab },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(tab.icon, fontSize = 20.sp)
                        Text(tab.label, fontSize = 11.sp, color = if (selected) MafiaPurple else Color.White.copy(0.4f), fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
                        if (selected) Box(Modifier.width(24.dp).height(2.dp).background(MafiaPurple, RoundedCornerShape(1.dp)))
                    }
                }
            }
        }
    }
}

// ── Video Tab ─────────────────────────────────────────────────────────────────
@Composable
private fun VideoTab() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Text("🎥", fontSize = 72.sp)
            Spacer(Modifier.height(16.dp))
            Text("Video & Audio", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.height(8.dp))
            Surface(color = MafiaPurple.copy(0.2f), shape = RoundedCornerShape(8.dp)) {
                Text("Coming Soon", Modifier.padding(horizontal = 16.dp, vertical = 6.dp), fontSize = 13.sp, color = MafiaPurple, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(16.dp))
            Text("Live voice and video calls will let you read body language, bluff, and convince — making the game even more intense.", fontSize = 14.sp, color = Color.White.copy(0.5f), textAlign = TextAlign.Center)
        }
    }
}

// ── Chat Tab ──────────────────────────────────────────────────────────────────
@Composable
private fun ChatTab(
    chatMessages: List<ChatMessage>,
    myPlayerId: String?,
    onSendChat: (String) -> Unit
) {
    var chatText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    LaunchedEffect(chatMessages.size) { if (chatMessages.isNotEmpty()) listState.animateScrollToItem(chatMessages.size - 1) }
    Column(Modifier.fillMaxSize()) {
        LazyColumn(state = listState, modifier = Modifier.weight(1f).padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp), contentPadding = PaddingValues(vertical = 8.dp)) {
            if (chatMessages.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(top = 48.dp), contentAlignment = Alignment.Center) {
                        Text("No messages yet. Say something!", fontSize = 14.sp, color = Color.White.copy(0.3f))
                    }
                }
            }
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
            FilledIconButton(onClick = { if (chatText.isNotBlank()) { onSendChat(chatText); chatText = "" } }, colors = IconButtonDefaults.filledIconButtonColors(containerColor = MafiaPurple)) {
                Text("➤", fontSize = 18.sp, color = Color.White)
            }
        }
    }
}

// ── Arena Tab ─────────────────────────────────────────────────────────────────
@Composable
private fun ArenaTab(
    phase: GamePhase, round: Int, myRole: Role?, myPlayerId: String?,
    alivePlayers: List<PlayerPublicInfo>, timerSeconds: Int,
    voteTally: Map<String, Int>,
    detectiveResult: ServerMessage.DetectiveResult?,
    nightSummary: ServerMessage.NightSummary?,
    voteResult: ServerMessage.VoteResult?,
    voteLog: List<VoteEntry>,
    eventLog: List<GameEvent>,
    ministerVetoUsed: Boolean,
    lastEvent: ServerMessage?,
    onNightAction: (String) -> Unit,
    onSendChat: (String) -> Unit,
    onVote: (String) -> Unit, onSkipVote: () -> Unit,
    onUseVeto: () -> Unit, onLeave: () -> Unit
) {
    when (phase) {
        GamePhase.ROLE_REVEAL -> RoleRevealContent(myRole)
        GamePhase.NIGHT -> NightContent(myRole, myPlayerId, alivePlayers, onNightAction, eventLog)
        GamePhase.NIGHT_RESULT -> NightResultContent(nightSummary, eventLog)
        GamePhase.DISCUSSION -> DiscussionArenaContent(myRole, detectiveResult, alivePlayers, eventLog, onSendChat)
        GamePhase.VOTING -> VotingContent(myRole, myPlayerId, alivePlayers, voteTally, voteLog, ministerVetoUsed, eventLog, onVote, onSkipVote, onUseVeto)
        GamePhase.ELIMINATION -> EliminationContent(voteResult, voteLog, eventLog)
        GamePhase.GAME_OVER -> GameOverContent(lastEvent, onLeave)
        else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(phase.description, color = Color.White.copy(0.6f), textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 32.dp))
                if (eventLog.isNotEmpty()) {
                    Spacer(Modifier.height(24.dp))
                    EventLogSection(eventLog)
                }
            }
        }
    }
}

// ── Phase-specific content composables ───────────────────────────────────────

@Composable
private fun RoleRevealContent(myRole: Role?) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
            Text(myRole?.emoji ?: "?", fontSize = 80.sp)
            Spacer(Modifier.height(16.dp))
            Text("You are", fontSize = 16.sp, color = Color.White.copy(0.6f))
            Text(myRole?.displayName ?: "Unknown", fontSize = 36.sp, fontWeight = FontWeight.Black, color = if (myRole?.isMafia() == true) MafiaRed else TownGreen)
            Spacer(Modifier.height(8.dp))
            Text(myRole?.description ?: "", fontSize = 14.sp, color = Color.White.copy(0.7f), textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 32.dp))
        }
    }
}

@Composable
private fun NightContent(
    myRole: Role?, myPlayerId: String?,
    alivePlayers: List<PlayerPublicInfo>,
    onNightAction: (String) -> Unit,
    eventLog: List<GameEvent>
) {
    var selected by remember { mutableStateOf<String?>(null) }
    val targets = alivePlayers.filter { it.id != myPlayerId }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        item {
            Text("🌙 The town sleeps...", fontSize = 18.sp, color = Color.White.copy(0.8f))
            val instr = when (myRole) {
                Role.MAFIA -> "Choose a player to eliminate"
                Role.DETECTIVE -> "Choose a player to investigate"
                Role.DOCTOR -> "Choose a player to protect"
                Role.VIGILANTE -> "Choose a player to shoot (careful — you die if they're innocent)"
                Role.ESCORT -> "Choose a player to block tonight"
                else -> "Wait for dawn..."
            }
            Text(instr, fontSize = 14.sp, color = MafiaPurple.copy(0.8f), textAlign = TextAlign.Center)
        }
        if (myRole?.hasNightAction == true) {
            item { Spacer(Modifier.height(12.dp)) }
            items(targets) { p ->
                Surface(Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { selected = p.id }, color = if (selected == p.id) MafiaPurple.copy(0.4f) else Color.White.copy(0.08f), shape = RoundedCornerShape(12.dp)) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(p.avatarEmoji, fontSize = 28.sp); Spacer(Modifier.width(12.dp)); Text(p.name, color = Color.White, fontSize = 16.sp)
                    }
                }
            }
            item {
                Spacer(Modifier.height(12.dp))
                Button(onClick = { selected?.let { onNightAction(it); selected = null } }, enabled = selected != null, colors = ButtonDefaults.buttonColors(containerColor = MafiaPurple), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().height(48.dp)) {
                    Text("Confirm", fontWeight = FontWeight.SemiBold)
                }
            }
        }
        if (eventLog.isNotEmpty()) {
            item { Spacer(Modifier.height(24.dp)); EventLogSection(eventLog) }
        }
    }
}

@Composable
private fun NightResultContent(nightSummary: ServerMessage.NightSummary?, eventLog: List<GameEvent>) {
    LazyColumn(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, contentPadding = PaddingValues(32.dp)) {
        item {
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
            // Vigilante results
            if (nightSummary?.vigilanteKilled != null) {
                Spacer(Modifier.height(16.dp))
                Surface(color = Color(0xFF3A2800).copy(0.6f), shape = RoundedCornerShape(12.dp)) {
                    Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🤠 Vigilante fired!", fontSize = 14.sp, color = Color(0xFFFFD600), fontWeight = FontWeight.Bold)
                        Text("${nightSummary.vigilanteKilled.name} was shot by the Vigilante", fontSize = 13.sp, color = Color.White.copy(0.8f), textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
                        if (nightSummary.vigilanteEliminated != null) {
                            Text("They were innocent — the Vigilante paid the price too.", fontSize = 13.sp, color = MafiaRed.copy(0.9f), textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
            }
        }
        if (eventLog.isNotEmpty()) {
            item { Spacer(Modifier.height(24.dp)); EventLogSection(eventLog) }
        }
    }
}

@Composable
private fun DiscussionArenaContent(
    myRole: Role?,
    detectiveResult: ServerMessage.DetectiveResult?,
    alivePlayers: List<PlayerPublicInfo>,
    eventLog: List<GameEvent>,
    onSendChat: (String) -> Unit
) {
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), contentPadding = PaddingValues(vertical = 12.dp)) {
        // Detective result banner
        if (myRole == Role.DETECTIVE && detectiveResult != null) {
            item {
                Surface(color = if (detectiveResult.isMafia) MafiaRed.copy(0.15f) else TownGreen.copy(0.15f), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(if (detectiveResult.isMafia) "🔴 ${detectiveResult.targetName} is Mafia" else "🟢 ${detectiveResult.targetName} is innocent", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        Spacer(Modifier.width(8.dp))
                        OutlinedButton(onClick = {
                            val verdict = if (detectiveResult.isMafia) "MAFIA" else "innocent"
                            onSendChat("🔍 [Detective] I investigated ${detectiveResult.targetName} — they are $verdict!")
                        }, shape = RoundedCornerShape(8.dp), contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)) {
                            Text("Reveal", fontSize = 12.sp, color = Color.White)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
        }
        // Alive players grid
        item {
            Text("Players (${alivePlayers.size} alive)", fontSize = 13.sp, color = Color.White.copy(0.5f), fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            alivePlayers.chunked(3).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { p ->
                        Surface(color = Color.White.copy(0.08f), shape = RoundedCornerShape(10.dp), modifier = Modifier.weight(1f)) {
                            Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(p.avatarEmoji, fontSize = 24.sp)
                                Text(p.name, fontSize = 12.sp, color = Color.White, maxLines = 1)
                            }
                        }
                    }
                    // fill empty cells
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
        // Event log
        if (eventLog.isNotEmpty()) {
            item { Spacer(Modifier.height(8.dp)); EventLogSection(eventLog) }
        }
    }
}

@Composable
private fun VotingContent(
    myRole: Role?, myPlayerId: String?,
    alivePlayers: List<PlayerPublicInfo>,
    voteTally: Map<String, Int>,
    voteLog: List<VoteEntry>,
    ministerVetoUsed: Boolean,
    eventLog: List<GameEvent>,
    onVote: (String) -> Unit, onSkipVote: () -> Unit, onUseVeto: () -> Unit
) {
    var voted by remember { mutableStateOf(false) }
    val targets = alivePlayers.filter { it.id != myPlayerId }
    val canVeto = myRole == Role.MINISTER && !ministerVetoUsed && !voted

    LazyColumn(Modifier.fillMaxSize()) {
        // Live vote log
        if (voteLog.isNotEmpty()) {
            item {
                Surface(color = Color.Black.copy(0.3f), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Text("Vote Log", fontSize = 12.sp, color = Color.White.copy(0.5f), fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(4.dp))
                        voteLog.takeLast(5).forEach { entry ->
                            Text(if (entry.isSkip) "• ${entry.voterName} skipped" else "• ${entry.voterName} → ${entry.targetName}", fontSize = 12.sp, color = if (entry.isSkip) Color.White.copy(0.4f) else Color.White.copy(0.8f))
                        }
                    }
                }
            }
        }
        item {
            Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🗳️ Vote to Eliminate", fontSize = 20.sp, color = MafiaRed, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
            }
        }
        items(targets) { p ->
            val votes = voteTally[p.id] ?: 0
            Surface(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).clickable(enabled = !voted) { onVote(p.id); voted = true }, color = Color.White.copy(0.08f), shape = RoundedCornerShape(12.dp)) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(p.avatarEmoji, fontSize = 28.sp); Spacer(Modifier.width(12.dp)); Text(p.name, color = Color.White, fontSize = 16.sp, modifier = Modifier.weight(1f))
                    if (votes > 0) Surface(color = MafiaRed.copy(0.6f), shape = CircleShape) { Text("$votes", Modifier.padding(horizontal = 10.dp, vertical = 4.dp), color = Color.White, fontWeight = FontWeight.Bold) }
                }
            }
        }
        item {
            Column(Modifier.padding(horizontal = 16.dp)) {
                Spacer(Modifier.height(12.dp))
                if (!voted) {
                    OutlinedButton(onClick = { onSkipVote(); voted = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                        colors = ButtonColors(containerColor = Color.Transparent, contentColor = Color.White.copy(0.6f), disabledContainerColor = Color.Transparent, disabledContentColor = Color.White.copy(0.3f))) {
                        Text("Skip / Abstain")
                    }
                    if (canVeto) {
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { onUseVeto(); voted = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A3500))) {
                            Text("🏛️ Use Secret Veto", color = Color(0xFFFFD600))
                        }
                    }
                } else {
                    Text("✓ Vote cast", color = TownGreen, fontSize = 14.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
                    if (myRole == Role.MINISTER && ministerVetoUsed) {
                        Text("🏛️ Veto used this game", fontSize = 12.sp, color = Color(0xFFFFD600).copy(0.7f), modifier = Modifier.align(Alignment.CenterHorizontally))
                    }
                }
                if (eventLog.isNotEmpty()) {
                    Spacer(Modifier.height(16.dp))
                    EventLogSection(eventLog)
                }
            }
        }
    }
}

@Composable
private fun EliminationContent(voteResult: ServerMessage.VoteResult?, voteLog: List<VoteEntry>, eventLog: List<GameEvent>) {
    LazyColumn(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, contentPadding = PaddingValues(24.dp)) {
        item {
            val eliminated = voteResult?.eliminatedPlayer
            when {
                eliminated != null -> {
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
                }
                voteResult?.wasTie == true -> {
                    Text("🤝", fontSize = 64.sp)
                    Spacer(Modifier.height(12.dp))
                    Text("It's a tie!", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center)
                    Text("No one was eliminated.", fontSize = 16.sp, color = Color.White.copy(0.6f), textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
                }
                else -> {
                    Text("🕊️", fontSize = 64.sp)
                    Spacer(Modifier.height(12.dp))
                    Text("No elimination", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center)
                }
            }

            if (voteLog.isNotEmpty()) {
                Spacer(Modifier.height(24.dp))
                Surface(color = Color.White.copy(0.08f), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Vote Summary", fontSize = 13.sp, color = Color.White.copy(0.5f), fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(6.dp))
                        voteLog.forEach { entry ->
                            Text(if (entry.isSkip) "• ${entry.voterName} — abstained" else "• ${entry.voterName} voted for ${entry.targetName}", fontSize = 13.sp, color = if (entry.isSkip) Color.White.copy(0.4f) else Color.White.copy(0.8f), modifier = Modifier.padding(vertical = 2.dp))
                        }
                    }
                }
            }
        }
        if (eventLog.isNotEmpty()) {
            item { Spacer(Modifier.height(16.dp)); EventLogSection(eventLog) }
        }
    }
}

@Composable
private fun GameOverContent(lastEvent: ServerMessage?, onLeave: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val event = lastEvent as? ServerMessage.GameOver
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
            val isTownWin = event?.winner == Team.TOWN
            Text(if (isTownWin) "🎉" else "🔪", fontSize = 72.sp)
            Spacer(Modifier.height(16.dp))
            Text(if (isTownWin) "Town Wins!" else "Mafia Wins!", fontSize = 32.sp, fontWeight = FontWeight.Black, color = if (isTownWin) TownGreen else MafiaRed)
            // Show all roles
            event?.allRoles?.let { allRoles ->
                Spacer(Modifier.height(24.dp))
                Surface(color = Color.White.copy(0.08f), shape = RoundedCornerShape(12.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Final Roles", fontSize = 13.sp, color = Color.White.copy(0.5f), fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(6.dp))
                        allRoles.entries.forEach { (_, role) ->
                            Text("${role.emoji} ${role.displayName}", fontSize = 13.sp, color = if (role.isMafia()) MafiaRed.copy(0.9f) else TownGreen.copy(0.9f))
                        }
                    }
                }
            }
            Spacer(Modifier.height(32.dp))
            Button(onClick = onLeave, colors = ButtonDefaults.buttonColors(containerColor = MafiaPurple), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Text("Back to Menu", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ── Event Log (shared across Arena tabs) ─────────────────────────────────────
@Composable
fun EventLogSection(eventLog: List<GameEvent>) {
    Surface(color = Color.White.copy(0.05f), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text("Game History", fontSize = 13.sp, color = Color.White.copy(0.5f), fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            eventLog.reversed().take(10).forEach { event ->
                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.Top) {
                    Surface(color = if (event.isElimination) MafiaRed.copy(0.3f) else Color.White.copy(0.1f), shape = RoundedCornerShape(4.dp)) {
                        Text(event.label, Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 11.sp, color = if (event.isElimination) MafiaRed else Color.White.copy(0.5f), fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(event.description, fontSize = 12.sp, color = Color.White.copy(0.7f), modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
