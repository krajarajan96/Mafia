package com.mafia.shared.network

import com.mafia.shared.network.messages.ClientMessage
import com.mafia.shared.network.messages.ServerMessage
import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*

class GameSocket(
    private val client: HttpClient,
    private val baseUrl: String = "ws://10.0.2.2:8080"
) {
    private var session: WebSocketSession? = null
    private val _incoming = MutableSharedFlow<ServerMessage>(replay = 1)
    val incoming: SharedFlow<ServerMessage> = _incoming.asSharedFlow()
    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()
    private var reconnectJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val outgoing = Channel<ClientMessage>(Channel.BUFFERED)

    enum class ConnectionState { DISCONNECTED, CONNECTING, CONNECTED, RECONNECTING }

    suspend fun connect() {
        _connectionState.value = ConnectionState.CONNECTING
        try {
            session = client.webSocketSession("$baseUrl/game")
            _connectionState.value = ConnectionState.CONNECTED
            scope.launch { for (msg in outgoing) { session?.send(Frame.Text(ClientMessage.encode(msg))) } }
            session?.let { ws ->
                for (frame in ws.incoming) {
                    if (frame is Frame.Text) {
                        try { _incoming.emit(ServerMessage.decode(frame.readText())) } catch (_: Exception) {}
                    }
                }
            }
            _connectionState.value = ConnectionState.DISCONNECTED
            attemptReconnect()
        } catch (_: Exception) {
            _connectionState.value = ConnectionState.DISCONNECTED
            attemptReconnect()
        }
    }

    fun send(message: ClientMessage) { scope.launch { outgoing.send(message) } }

    private fun attemptReconnect() {
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            _connectionState.value = ConnectionState.RECONNECTING
            var attempt = 0
            while (attempt < 5) {
                delay((1000L * (attempt + 1)).coerceAtMost(5000L)); attempt++
                try { connect(); return@launch } catch (_: Exception) {}
            }
            _connectionState.value = ConnectionState.DISCONNECTED
        }
    }

    suspend fun disconnect() {
        reconnectJob?.cancel()
        session?.close(CloseReason(CloseReason.Codes.NORMAL, "User left"))
        session = null
        _connectionState.value = ConnectionState.DISCONNECTED
    }
}
