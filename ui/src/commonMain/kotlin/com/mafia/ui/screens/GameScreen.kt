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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mafia.shared.model.*
import com.mafia.shared.network.messages.ServerMessage
import com.mafia.ui.theme.*

private enum class GameTab(val label: String, val icon: String) {
    VIDEO("Video", "🎥"),
    ARENA("Arena", "🎲"),
    CHAT("Chat", "💬"),
    MAFIA("Mafia", "🩸")
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
    revealedRoles: Map<String, Role>,
    allPlayers: List<PlayerPublicInfo>,
    lastEvent: ServerMessage?,
    mafiaTeammates: List<PlayerPublicInfo>,
    mafiaChatMessages: List<ChatMessage>,
    mafiaVoteTally: Map<String, Int>,
    mafiaVoteTie: Boolean,
    enableGameHistory: Boolean,
    spectators: List<PlayerPublicInfo> = emptyList(),
    rematchInitiated: Boolean = false,
    rematchReadyIds: List<String> = emptyList(),
    rematchTotalPlayers: Int = 0,
    onNightAction: (String) -> Unit, onSendChat: (String) -> Unit,
    onSendMafiaChat: (String) -> Unit,
    onVote: (String) -> Unit, onSkipVote: () -> Unit,
    onUseVeto: () -> Unit,
    onInitiateRematch: () -> Unit = {},
    onMarkRematchReady: () -> Unit = {},
    onLeave: () -> Unit
) {
    val isMafiaPlayer = myRole?.isMafia() == true
    val visibleTabs = if (isMafiaPlayer && mafiaTeammates.isNotEmpty()) GameTab.entries else GameTab.entries.filter { it != GameTab.MAFIA }
    var selectedTab by remember { mutableStateOf(GameTab.ARENA) }

    val bgGradient = when {
        phase.isNightPhase() -> Brush.verticalGradient(listOf(Color(0xFF0A0520), Color(0xFF1A0A3E)))
        else -> Brush.verticalGradient(listOf(Color(0xFF1A1530), Color(0xFF0F172A)))
    }

    Column(Modifier.fillMaxSize().background(bgGradient)) {
        // Phase Header (always visible)
        var showLeaveDialog by remember { mutableStateOf(false) }
        if (showLeaveDialog) {
            AlertDialog(
                onDismissRequest = { showLeaveDialog = false },
                title = { Text("Leave Game?", color = Color.White) },
                text = { Text("You will forfeit the game. Are you sure?", color = Color.White.copy(0.7f)) },
                confirmButton = {
                    TextButton(onClick = { showLeaveDialog = false; onLeave() }) {
                        Text("Leave", color = MafiaRed)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLeaveDialog = false }) {
                        Text("Stay", color = MafiaPurple)
                    }
                },
                containerColor = Color(0xFF1A1145)
            )
        }
        Surface(color = Color.Black.copy(0.4f), modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    if (round > 0) Text("Round $round", fontSize = 11.sp, color = Color.White.copy(0.5f))
                    Text(
                        phase.displayName, fontSize = 18.sp, fontWeight = FontWeight.Bold,
                        color = MafiaPurple, maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
                myRole?.let {
                    Spacer(Modifier.width(8.dp))
                    Surface(color = if (it.isMafia()) MafiaRed.copy(0.3f) else TownGreen.copy(0.3f), shape = RoundedCornerShape(8.dp)) {
                        Text("${it.emoji} ${it.displayName}", Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontSize = 11.sp, color = Color.White, maxLines = 1)
                    }
                }
                if (timerSeconds > 0) {
                    Spacer(Modifier.width(8.dp))
                    Surface(color = if (timerSeconds <= 10) MafiaRed.copy(0.8f) else Color.White.copy(0.15f), shape = CircleShape) {
                        Text("${timerSeconds}s", Modifier.padding(horizontal = 10.dp, vertical = 5.dp), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
                if (phase != GamePhase.GAME_OVER) {
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        color = MafiaRed.copy(0.15f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.clickable { showLeaveDialog = true }
                    ) {
                        Text("Exit", Modifier.padding(horizontal = 10.dp, vertical = 5.dp), fontSize = 12.sp, color = MafiaRed.copy(0.85f), fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // Spectator banner — shown when local player is eliminated mid-game
        val isEliminated = myPlayerId != null && phase != GamePhase.GAME_OVER &&
            phase != GamePhase.ROLE_REVEAL && alivePlayers.none { it.id == myPlayerId }

        // Tab content area
        Box(Modifier.weight(1f)) {
            when (selectedTab) {
                GameTab.VIDEO -> VideoTab()
                GameTab.ARENA -> ArenaTab(
                    phase, round, myRole, myPlayerId, alivePlayers, timerSeconds,
                    voteTally, detectiveResult, nightSummary, voteResult, voteLog,
                    eventLog, ministerVetoUsed, allPlayers, isEliminated, lastEvent,
                    mafiaTeammates, mafiaVoteTally, mafiaVoteTie,
                    enableGameHistory, rematchInitiated, rematchReadyIds, rematchTotalPlayers,
                    onNightAction, onSendChat, onVote, onSkipVote, onUseVeto,
                    onInitiateRematch, onMarkRematchReady, onLeave
                )
                GameTab.CHAT -> ChatTab(chatMessages, myPlayerId, onSendChat)
                GameTab.MAFIA -> MafiaTab(mafiaChatMessages, mafiaTeammates, myPlayerId, onSendMafiaChat)
            }
        }
        if (isEliminated) {
            SpectatorBanner(myRole, revealedRoles, allPlayers)
        }

        // Bottom Tab Bar
        Surface(color = Color(0xFF0D0B1E)) {
            Row(Modifier.fillMaxWidth().height(56.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                visibleTabs.forEach { tab ->
                    val selected = selectedTab == tab
                    val isMafiaTab = tab == GameTab.MAFIA
                    val activeColor = if (isMafiaTab) MafiaRed else MafiaPurple
                    Column(
                        Modifier.weight(1f).fillMaxHeight().clickable { selectedTab = tab },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(tab.icon, fontSize = 20.sp)
                        Text(tab.label, fontSize = 11.sp, color = if (selected) activeColor else Color.White.copy(0.4f), fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
                        if (selected) Box(Modifier.width(24.dp).height(2.dp).background(activeColor, RoundedCornerShape(1.dp)))
                    }
                }
            }
        }
    }
}

// ── Spectator Banner ──────────────────────────────────────────────────────────
@Composable
private fun SpectatorBanner(
    myRole: Role?,
    revealedRoles: Map<String, Role>,
    allPlayers: List<PlayerPublicInfo>
) {
    Surface(color = Color.Black.copy(0.7f), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("💀", fontSize = 15.sp)
                Spacer(Modifier.width(6.dp))
                Text("Eliminated — Spectating", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = DeadGray)
            }
            if (revealedRoles.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Text("Roles revealed:", fontSize = 10.sp, color = Color.White.copy(0.35f))
                Spacer(Modifier.height(4.dp))
                // Chips in rows of 3 — sorted mafia first
                val sorted = revealedRoles.entries.sortedByDescending { it.value.isMafia() }
                sorted.chunked(3).forEach { rowEntries ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(bottom = 4.dp)) {
                        rowEntries.forEach { (playerId, role) ->
                            val name = allPlayers.find { it.id == playerId }?.name ?: "?"
                            Surface(color = if (role.isMafia()) MafiaRed.copy(0.3f) else TownGreen.copy(0.2f), shape = RoundedCornerShape(6.dp)) {
                                Text("${role.emoji} $name", Modifier.padding(horizontal = 7.dp, vertical = 3.dp), fontSize = 11.sp, color = Color.White.copy(0.9f), maxLines = 1)
                            }
                        }
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

// ── Mafia Tab ─────────────────────────────────────────────────────────────────
@Composable
private fun MafiaTab(
    mafiaChatMessages: List<ChatMessage>,
    mafiaTeammates: List<PlayerPublicInfo>,
    myPlayerId: String?,
    onSendMafiaChat: (String) -> Unit
) {
    var chatText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    LaunchedEffect(mafiaChatMessages.size) { if (mafiaChatMessages.isNotEmpty()) listState.animateScrollToItem(mafiaChatMessages.size - 1) }

    Column(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF1A0510), Color(0xFF0D0818))))) {
        // Teammates header
        if (mafiaTeammates.isNotEmpty()) {
            Surface(color = MafiaRed.copy(0.15f), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    Text("🩸 Your Mafia Team", fontSize = 12.sp, color = MafiaRed.copy(0.7f), fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        mafiaTeammates.forEach { tm ->
                            Surface(color = MafiaRed.copy(0.25f), shape = RoundedCornerShape(8.dp)) {
                                Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(tm.avatarEmoji, fontSize = 18.sp)
                                    Spacer(Modifier.width(6.dp))
                                    Text(tm.name, fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }
        }
        // Messages
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(vertical = 10.dp)
        ) {
            if (mafiaChatMessages.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(top = 48.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🩸", fontSize = 32.sp)
                            Spacer(Modifier.height(8.dp))
                            Text("Mafia-only channel", fontSize = 14.sp, color = MafiaRed.copy(0.6f), fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(4.dp))
                            Text("Plan your moves here — town can't see this.", fontSize = 13.sp, color = Color.White.copy(0.3f))
                        }
                    }
                }
            }
            items(mafiaChatMessages) { msg ->
                val isMine = msg.senderId == myPlayerId
                Row(Modifier.fillMaxWidth(), horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start) {
                    Surface(
                        color = if (isMine) MafiaRed.copy(0.45f) else Color.White.copy(0.09f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(Modifier.padding(10.dp).widthIn(max = 260.dp)) {
                            if (!isMine) Text(msg.senderName, fontSize = 11.sp, color = MafiaRed, fontWeight = FontWeight.SemiBold)
                            Text(msg.text, fontSize = 14.sp, color = Color.White)
                        }
                    }
                }
            }
        }
        // Input
        Row(Modifier.fillMaxWidth().background(Color.Black.copy(0.5f)).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = chatText,
                onValueChange = { if (it.length <= 200) chatText = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Mafia only...", color = Color.White.copy(0.3f)) },
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MafiaRed,
                    unfocusedBorderColor = MafiaRed.copy(0.3f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )
            Spacer(Modifier.width(8.dp))
            FilledIconButton(
                onClick = { if (chatText.isNotBlank()) { onSendMafiaChat(chatText); chatText = "" } },
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = MafiaRed)
            ) { Text("➤", fontSize = 18.sp, color = Color.White) }
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
    allPlayers: List<PlayerPublicInfo>,
    isSpectator: Boolean,
    lastEvent: ServerMessage?,
    mafiaTeammates: List<PlayerPublicInfo>,
    mafiaVoteTally: Map<String, Int>,
    mafiaVoteTie: Boolean,
    enableGameHistory: Boolean,
    rematchInitiated: Boolean = false,
    rematchReadyIds: List<String> = emptyList(),
    rematchTotalPlayers: Int = 0,
    onNightAction: (String) -> Unit,
    onSendChat: (String) -> Unit,
    onVote: (String) -> Unit, onSkipVote: () -> Unit,
    onUseVeto: () -> Unit,
    onInitiateRematch: () -> Unit = {},
    onMarkRematchReady: () -> Unit = {},
    onLeave: () -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        // Persistent mafia teammates strip — visible across all phases for mafia players
        if (myRole?.isMafia() == true && mafiaTeammates.isNotEmpty()) {
            Surface(color = MafiaRed.copy(0.12f), modifier = Modifier.fillMaxWidth()) {
                Row(
                    Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🩸 Team:", fontSize = 11.sp, color = MafiaRed.copy(0.8f), fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.width(8.dp))
                    mafiaTeammates.forEach { tm ->
                        Surface(color = MafiaRed.copy(0.2f), shape = RoundedCornerShape(6.dp), modifier = Modifier.padding(end = 6.dp)) {
                            Text("${tm.avatarEmoji} ${tm.name}", Modifier.padding(horizontal = 8.dp, vertical = 3.dp), fontSize = 11.sp, color = Color.White)
                        }
                    }
                }
            }
        }
        Box(Modifier.weight(1f)) {
            when (phase) {
                GamePhase.ROLE_REVEAL -> RoleRevealContent(myRole)
                GamePhase.NIGHT -> NightContent(
                    myRole, myPlayerId, alivePlayers, allPlayers,
                    mafiaTeammates, mafiaVoteTally, mafiaVoteTie,
                    isSpectator, onNightAction, eventLog, enableGameHistory
                )
                GamePhase.NIGHT_RESULT -> NightResultContent(nightSummary, isSpectator, eventLog, enableGameHistory)
                GamePhase.DISCUSSION -> DiscussionArenaContent(myRole, detectiveResult, alivePlayers, allPlayers, eventLog, enableGameHistory, onSendChat)
                GamePhase.VOTING -> VotingContent(myRole, myPlayerId, alivePlayers, voteTally, voteLog, ministerVetoUsed, eventLog, enableGameHistory, onVote, onSkipVote, onUseVeto)
                GamePhase.ELIMINATION -> EliminationContent(voteResult, voteLog, isSpectator, eventLog, enableGameHistory)
                GamePhase.GAME_OVER -> GameOverContent(
                    lastEvent, allPlayers, myPlayerId, myRole,
                    rematchInitiated, rematchReadyIds, rematchTotalPlayers,
                    onInitiateRematch, onMarkRematchReady, onLeave
                )
                else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(phase.description, color = Color.White.copy(0.6f), textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 32.dp))
                        if (eventLog.isNotEmpty()) {
                            Spacer(Modifier.height(24.dp))
                            GameHistorySection(eventLog, enableGameHistory)
                        }
                    }
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
    allPlayers: List<PlayerPublicInfo>,
    mafiaTeammates: List<PlayerPublicInfo>,
    mafiaVoteTally: Map<String, Int>,
    mafiaVoteTie: Boolean,
    isSpectator: Boolean,
    onNightAction: (String) -> Unit,
    eventLog: List<GameEvent>,
    enableGameHistory: Boolean
) {
    val isMafia = myRole?.isMafia() == true
    var selected by remember { mutableStateOf<String?>(null) }
    var voted by remember { mutableStateOf(false) }
    // Reset selection on tie so mafia can re-vote
    LaunchedEffect(mafiaVoteTie) { if (mafiaVoteTie) { selected = null; voted = false } }
    // Mafia teammates IDs (so mafia can't vote for their own teammates)
    val mafiaTeammateIds = mafiaTeammates.map { it.id }.toSet()
    val targets = if (isMafia)
        alivePlayers.filter { it.id != myPlayerId && it.id !in mafiaTeammateIds }
    else
        alivePlayers.filter { it.id != myPlayerId }
    val deadPlayers = allPlayers.filter { p -> alivePlayers.none { it.id == p.id } }

    LazyColumn(Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        item {
            Text("🌙 The town sleeps...", fontSize = 18.sp, color = Color.White.copy(0.8f))
            Spacer(Modifier.height(4.dp))
            val instr = when {
                isSpectator -> "You are eliminated — watching the night unfold"
                isMafia -> if (mafiaVoteTie) "⚠️ Tie — vote again!" else "Choose a target to eliminate"
                myRole == Role.DETECTIVE -> "Choose a player to investigate"
                myRole == Role.DOCTOR -> "Choose a player to protect"
                myRole == Role.VIGILANTE -> "Choose a player to shoot (careful — you die if they're innocent)"
                myRole == Role.ESCORT -> "Choose a player to block tonight"
                else -> "Wait for dawn..."
            }
            val instrColor = if (mafiaVoteTie) MafiaRed else if (isSpectator) DeadGray else MafiaPurple.copy(0.8f)
            Text(instr, fontSize = 14.sp, color = instrColor, textAlign = TextAlign.Center, fontWeight = if (mafiaVoteTie) FontWeight.Bold else FontWeight.Normal)
        }

        // Target list
        if (!isSpectator && myRole?.hasNightAction == true) {
            item { Spacer(Modifier.height(12.dp)) }
            items(targets) { p ->
                val votes = if (isMafia) mafiaVoteTally[p.id] ?: 0 else 0
                val isSelected = selected == p.id
                val selectionColor = if (isMafia) MafiaRed.copy(0.4f) else MafiaPurple.copy(0.4f)
                Surface(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable(enabled = !voted) { selected = p.id },
                    color = if (isSelected) selectionColor else Color.White.copy(0.08f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(p.avatarEmoji, fontSize = 28.sp)
                        Spacer(Modifier.width(12.dp))
                        Text(p.name, color = Color.White, fontSize = 16.sp, modifier = Modifier.weight(1f))
                        if (votes > 0) {
                            Surface(color = MafiaRed.copy(0.6f), shape = CircleShape) {
                                Text("$votes", Modifier.padding(horizontal = 10.dp, vertical = 4.dp), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
            item {
                Spacer(Modifier.height(12.dp))
                val confirmColor = if (isMafia) MafiaRed else MafiaPurple
                Button(
                    onClick = { selected?.let { onNightAction(it); if (!isMafia) voted = true } },
                    enabled = selected != null && !voted,
                    colors = ButtonDefaults.buttonColors(containerColor = confirmColor),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text(if (isMafia) "Cast Vote" else "Confirm", fontWeight = FontWeight.SemiBold)
                }
                if (voted && !isMafia) {
                    Spacer(Modifier.height(6.dp))
                    Text("✓ Action submitted", color = TownGreen, fontSize = 13.sp)
                }
            }
        }

        if (deadPlayers.isNotEmpty()) {
            item {
                Spacer(Modifier.height(16.dp))
                Text("Eliminated", fontSize = 12.sp, color = Color.White.copy(0.35f), fontWeight = FontWeight.SemiBold, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(6.dp))
            }
            items(deadPlayers) { p ->
                Surface(Modifier.fillMaxWidth().padding(vertical = 3.dp), color = Color.White.copy(0.04f), shape = RoundedCornerShape(10.dp)) {
                    Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("💀", fontSize = 16.sp); Spacer(Modifier.width(8.dp))
                        Text(p.name, color = Color.White.copy(0.35f), fontSize = 14.sp, modifier = Modifier.weight(1f))
                        Text("eliminated", fontSize = 11.sp, color = DeadGray.copy(0.5f))
                    }
                }
            }
        }
        if (eventLog.isNotEmpty()) {
            item { Spacer(Modifier.height(24.dp)); GameHistorySection(eventLog, enableGameHistory) }
        }
    }
}

@Composable
private fun NightResultContent(nightSummary: ServerMessage.NightSummary?, isSpectator: Boolean, eventLog: List<GameEvent>, enableGameHistory: Boolean) {
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
                    if (isSpectator) {
                        nightSummary.eliminatedRole?.let { role ->
                            Spacer(Modifier.height(8.dp))
                            Surface(color = Color.White.copy(0.1f), shape = RoundedCornerShape(8.dp)) {
                                Text("They were ${role.emoji} ${role.displayName}", Modifier.padding(horizontal = 12.dp, vertical = 6.dp), fontSize = 13.sp, color = Color.White.copy(0.8f))
                            }
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
            val vigilanteKilled = nightSummary?.vigilanteKilled
            if (vigilanteKilled != null) {
                Spacer(Modifier.height(16.dp))
                Surface(color = Color(0xFF3A2800).copy(0.6f), shape = RoundedCornerShape(12.dp)) {
                    Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🤠 Vigilante fired!", fontSize = 14.sp, color = Color(0xFFFFD600), fontWeight = FontWeight.Bold)
                        Text("${vigilanteKilled.name} was shot by the Vigilante", fontSize = 13.sp, color = Color.White.copy(0.8f), textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
                        if (nightSummary?.vigilanteEliminated != null) {
                            Text("They were innocent — the Vigilante paid the price too.", fontSize = 13.sp, color = MafiaRed.copy(0.9f), textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
            }
        }
        if (eventLog.isNotEmpty()) {
            item { Spacer(Modifier.height(24.dp)); GameHistorySection(eventLog, enableGameHistory) }
        }
    }
}

@Composable
private fun DiscussionArenaContent(
    myRole: Role?,
    detectiveResult: ServerMessage.DetectiveResult?,
    alivePlayers: List<PlayerPublicInfo>,
    allPlayers: List<PlayerPublicInfo>,
    eventLog: List<GameEvent>,
    enableGameHistory: Boolean,
    onSendChat: (String) -> Unit
) {
    val aliveIds = alivePlayers.map { it.id }.toSet()
    // Use allPlayers if populated (game started), else fall back to alivePlayers
    val displayPlayers = if (allPlayers.isNotEmpty()) allPlayers else alivePlayers
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
        // All players grid (alive + dead)
        item {
            Text("Players (${alivePlayers.size} alive · ${displayPlayers.size - alivePlayers.size} eliminated)", fontSize = 13.sp, color = Color.White.copy(0.5f), fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            displayPlayers.chunked(3).forEach { row ->
                Row(
                    Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row.forEach { p ->
                        val isAlive = p.id in aliveIds
                        Surface(
                            color = if (isAlive) Color.White.copy(0.08f) else Color.White.copy(0.03f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        ) {
                            Column(
                                Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(if (isAlive) p.avatarEmoji else "💀", fontSize = 22.sp)
                                Spacer(Modifier.height(4.dp))
                                Text(p.name, fontSize = 11.sp, color = if (isAlive) Color.White else Color.White.copy(0.35f), maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                                if (!isAlive) Text("eliminated", fontSize = 9.sp, color = DeadGray.copy(0.5f))
                            }
                        }
                    }
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
        // Event log
        if (eventLog.isNotEmpty()) {
            item { Spacer(Modifier.height(8.dp)); GameHistorySection(eventLog, enableGameHistory) }
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
    enableGameHistory: Boolean,
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
                    GameHistorySection(eventLog, enableGameHistory)
                }
            }
        }
    }
}

@Composable
private fun EliminationContent(voteResult: ServerMessage.VoteResult?, voteLog: List<VoteEntry>, isSpectator: Boolean, eventLog: List<GameEvent>, enableGameHistory: Boolean) {
    LazyColumn(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, contentPadding = PaddingValues(24.dp)) {
        item {
            val eliminated = voteResult?.eliminatedPlayer
            when {
                eliminated != null -> {
                    Text("⚖️", fontSize = 64.sp)
                    Spacer(Modifier.height(12.dp))
                    Text("${eliminated.avatarEmoji} ${eliminated.name}", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MafiaRed, textAlign = TextAlign.Center)
                    Text("has been eliminated by vote", fontSize = 16.sp, color = Color.White.copy(0.7f), textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
                    if (isSpectator) {
                        voteResult.eliminatedRole?.let { role ->
                            Spacer(Modifier.height(8.dp))
                            Surface(color = if (role.isMafia()) MafiaRed.copy(0.2f) else TownGreen.copy(0.2f), shape = RoundedCornerShape(8.dp)) {
                                Text("They were ${role.emoji} ${role.displayName}", Modifier.padding(horizontal = 12.dp, vertical = 6.dp), fontSize = 13.sp, color = Color.White.copy(0.9f))
                            }
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
            item { Spacer(Modifier.height(16.dp)); GameHistorySection(eventLog, enableGameHistory) }
        }
    }
}

@Composable
private fun GameOverContent(
    lastEvent: ServerMessage?, allPlayers: List<PlayerPublicInfo>,
    myPlayerId: String?, myRole: Role?,
    rematchInitiated: Boolean, rematchReadyIds: List<String>, rematchTotalPlayers: Int,
    onInitiateRematch: () -> Unit, onMarkRematchReady: () -> Unit, onLeave: () -> Unit
) {
    val event = lastEvent as? ServerMessage.GameOver
    val isTownWin = event?.winner == Team.TOWN
    val isHost = allPlayers.find { it.id == myPlayerId }?.isHost == true
    val alreadyReady = rematchReadyIds.contains(myPlayerId)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(24.dp)
    ) {
        item {
            Spacer(Modifier.height(24.dp))
            Text(if (isTownWin) "🎉" else "🔪", fontSize = 72.sp)
            Spacer(Modifier.height(16.dp))
            Text(
                if (isTownWin) "Town Wins!" else "Mafia Wins!",
                fontSize = 32.sp, fontWeight = FontWeight.Black,
                color = if (isTownWin) TownGreen else MafiaRed
            )
            Spacer(Modifier.height(20.dp))
            // Rematch UI
            if (isHost) {
                Button(
                    onClick = onInitiateRematch,
                    colors = ButtonDefaults.buttonColors(containerColor = TownGreen),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Text("🔄 Rematch", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(8.dp))
            } else if (rematchInitiated && !alreadyReady) {
                Button(
                    onClick = onMarkRematchReady,
                    colors = ButtonDefaults.buttonColors(containerColor = TownGreen),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Text("✅ Ready for Rematch", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(8.dp))
            }
            if (rematchInitiated && rematchTotalPlayers > 0) {
                Surface(color = Color.White.copy(0.08f), shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Ready: ${rematchReadyIds.size} / $rematchTotalPlayers",
                        Modifier.padding(12.dp),
                        fontSize = 14.sp, color = TownGreen, fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(Modifier.height(8.dp))
            }
            Button(
                onClick = onLeave,
                colors = ButtonDefaults.buttonColors(containerColor = MafiaPurple),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text("Back to Menu", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        event?.allRoles?.let { allRoles ->
            item { Spacer(Modifier.height(24.dp)) }
            item {
                Surface(color = Color.White.copy(0.08f), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Final Roles", fontSize = 13.sp, color = Color.White.copy(0.5f), fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(6.dp))
                        val sorted = allRoles.entries.sortedWith(compareByDescending<Map.Entry<String, Role>> { it.value.isMafia() }.thenBy { it.value.displayName })
                        sorted.forEach { (playerId, role) ->
                            val player = allPlayers.find { it.id == playerId }
                            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(player?.avatarEmoji ?: "👤", fontSize = 20.sp)
                                Spacer(Modifier.width(10.dp))
                                Text(player?.name ?: playerId, fontSize = 14.sp, color = Color.White, modifier = Modifier.weight(1f), maxLines = 1)
                                Surface(color = if (role.isMafia()) MafiaRed.copy(0.25f) else TownGreen.copy(0.2f), shape = RoundedCornerShape(6.dp)) {
                                    Text("${role.emoji} ${role.displayName}", Modifier.padding(horizontal = 8.dp, vertical = 3.dp), fontSize = 12.sp, color = if (role.isMafia()) MafiaRed else TownGreen, maxLines = 1)
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

// ── Game History (collapsible, grouped by round) ─────────────────────────────
@Composable
fun GameHistorySection(eventLog: List<GameEvent>, enableGameHistory: Boolean) {
    val displayEvents = if (enableGameHistory) eventLog else eventLog.filter { it.isElimination }
    if (displayEvents.isEmpty()) return

    val groupedByRound = displayEvents.groupBy { it.round }
    val rounds = groupedByRound.keys.sorted()
    val maxRound = rounds.maxOrNull() ?: 0
    var expandedRounds by remember { mutableStateOf(setOf(maxRound)) }
    LaunchedEffect(maxRound) { expandedRounds = expandedRounds + maxRound }

    Surface(color = Color.White.copy(0.05f), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text("Game History", fontSize = 13.sp, color = Color.White.copy(0.5f), fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            rounds.sortedDescending().forEach { r ->
                val events = groupedByRound[r] ?: return@forEach
                val isExpanded = r in expandedRounds
                Row(
                    Modifier.fillMaxWidth()
                        .clickable { expandedRounds = if (isExpanded) expandedRounds - r else expandedRounds + r }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (r == 0) "Setup" else "Round $r",
                        fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                        color = MafiaPurple, modifier = Modifier.weight(1f)
                    )
                    Text(if (isExpanded) "▲" else "▼", fontSize = 10.sp, color = Color.White.copy(0.4f))
                }
                if (isExpanded) {
                    events.forEach { event ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 2.dp, horizontal = 4.dp), verticalAlignment = Alignment.Top) {
                            Surface(
                                color = if (event.isElimination) MafiaRed.copy(0.3f) else Color.White.copy(0.1f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    event.label,
                                    Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    fontSize = 11.sp,
                                    color = if (event.isElimination) MafiaRed else Color.White.copy(0.5f),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(event.description, fontSize = 12.sp, color = Color.White.copy(0.7f), modifier = Modifier.weight(1f))
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                }
            }
        }
    }
}
