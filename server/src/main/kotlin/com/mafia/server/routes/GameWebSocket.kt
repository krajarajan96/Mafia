package com.mafia.server.routes

import com.mafia.server.game.GameSessionManager
import com.mafia.shared.model.*
import com.mafia.shared.network.messages.ClientMessage
import com.mafia.shared.network.messages.ServerMessage
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import java.util.UUID

fun Routing.configureGameWebSocket(manager: GameSessionManager) {
    webSocket("/game") {
        var playerId: String? = null
        var currentRoomId: String? = null
        try {
            for (frame in incoming) {
                if (frame !is Frame.Text) continue
                try {
                    when (val msg = ClientMessage.decode(frame.readText())) {
                        is ClientMessage.CreateRoom -> {
                            val session = manager.createRoom(msg.mode)
                            val id = UUID.randomUUID().toString(); playerId = id; currentRoomId = session.roomId
                            val player = Player(id = id, name = msg.playerName, avatarEmoji = msg.emoji, isHost = true)
                            session.addPlayer(player, this); manager.registerPlayer(id, session.roomId)
                            send(Frame.Text(ServerMessage.encode(ServerMessage.RoomCreated(session.room, id))))
                        }
                        is ClientMessage.JoinRoom -> {
                            val session = manager.findByCode(msg.roomCode)
                            if (session == null) { send(Frame.Text(ServerMessage.encode(ServerMessage.Error("Room not found")))); continue }
                            val id = UUID.randomUUID().toString(); playerId = id; currentRoomId = session.roomId
                            val player = Player(id = id, name = msg.playerName, avatarEmoji = msg.emoji)
                            if (!session.addPlayer(player, this)) { send(Frame.Text(ServerMessage.encode(ServerMessage.Error("Room full or game started")))); continue }
                            manager.registerPlayer(id, session.roomId)
                            send(Frame.Text(ServerMessage.encode(ServerMessage.RoomJoined(session.room, id))))
                            session.state.players.filter { it.id != id }.forEach { p ->
                                session.getConnection(p.id)?.let { ws ->
                                    try { ws.send(Frame.Text(ServerMessage.encode(ServerMessage.PlayerJoined(PlayerPublicInfo.from(player))))) } catch (_: Exception) {}
                                }
                            }
                        }
                        is ClientMessage.StartGame -> {
                            val session = currentRoomId?.let { manager.findByRoomId(it) }
                            if (session != null && playerId == session.room.hostId) session.startGame()
                        }
                        is ClientMessage.NightAction -> currentRoomId?.let { manager.findByRoomId(it) }?.let { s -> playerId?.let { s.submitNightAction(it, msg.targetId) } }
                        is ClientMessage.SendChat -> currentRoomId?.let { manager.findByRoomId(it) }?.let { s -> playerId?.let { s.handleChat(it, msg.text) } }
                        is ClientMessage.CastVote -> currentRoomId?.let { manager.findByRoomId(it) }?.let { s -> playerId?.let { s.submitVote(it, msg.targetId) } }
                        is ClientMessage.SkipVote -> currentRoomId?.let { manager.findByRoomId(it) }?.let { s -> playerId?.let { s.submitSkip(it) } }
                        is ClientMessage.UseVeto -> currentRoomId?.let { manager.findByRoomId(it) }?.let { s -> playerId?.let { s.submitMinisterVeto(it) } }
                        is ClientMessage.UpdateSettings -> {
                            val session = currentRoomId?.let { manager.findByRoomId(it) }
                            if (session != null && playerId == session.room.hostId) session.updateSettings(msg.settings)
                        }
                        is ClientMessage.Accuse -> currentRoomId?.let { manager.findByRoomId(it) }?.let { s -> playerId?.let { s.handleChat(it, "I accuse ${msg.targetId}: ${msg.reason}") } }
                        is ClientMessage.LeaveRoom -> { handleDisconnect(manager, playerId, currentRoomId); playerId = null; currentRoomId = null; continue }
                        is ClientMessage.Ready -> {}
                    }
                } catch (e: Exception) { send(Frame.Text(ServerMessage.encode(ServerMessage.Error("Invalid message: ${e.message}")))) }
            }
        } finally { handleDisconnect(manager, playerId, currentRoomId) }
    }
}

private suspend fun handleDisconnect(manager: GameSessionManager, playerId: String?, roomId: String?) {
    playerId?.let { pid -> manager.unregisterPlayer(pid); roomId?.let { manager.findByRoomId(it)?.removePlayer(pid) } }
}
