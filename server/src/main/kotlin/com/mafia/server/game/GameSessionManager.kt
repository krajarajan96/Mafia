package com.mafia.server.game

import com.mafia.shared.model.*
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class GameSessionManager {
    private val sessions = ConcurrentHashMap<String, GameSession>()
    private val codeToRoom = ConcurrentHashMap<String, String>()
    private val playerToRoom = ConcurrentHashMap<String, String>()

    fun createRoom(mode: GameMode, settings: GameSettings = GameSettings()): GameSession {
        val roomId = UUID.randomUUID().toString()
        var code = generateRoomCode()
        while (codeToRoom.containsKey(code)) { code = generateRoomCode() }
        val session = GameSession(roomId, code, mode, AIPlayerController())
        sessions[roomId] = session; codeToRoom[code] = roomId
        return session
    }

    fun findByCode(code: String): GameSession? = codeToRoom[code.uppercase()]?.let { sessions[it] }
    fun findByRoomId(roomId: String): GameSession? = sessions[roomId]
    fun findByPlayerId(playerId: String): GameSession? = playerToRoom[playerId]?.let { sessions[it] }
    fun registerPlayer(playerId: String, roomId: String) { playerToRoom[playerId] = roomId }
    fun unregisterPlayer(playerId: String) { playerToRoom.remove(playerId) }
    fun removeRoom(roomId: String) { sessions.remove(roomId)?.let { codeToRoom.remove(it.roomCode) } }
    fun activeRoomCount(): Int = sessions.size
    fun listOpenRooms(): List<Room> = sessions.values.filter { it.room.status == RoomStatus.WAITING && it.mode == GameMode.MULTIPLAYER }.map { it.room }
}
