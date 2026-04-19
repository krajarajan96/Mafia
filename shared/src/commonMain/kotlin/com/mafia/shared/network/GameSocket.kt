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

    private suspend fun connectOnce() {
        _connectionState.value = ConnectionState.CONNECTING
        session = client.webSocketSession("$baseUrl/game")
        _connectionState.value = ConnectionState.CONNECTED
        scope.launch {
            try { for (msg in outgoing) { session?.send(Frame.Text(ClientMessage.encode(msg))) } }
            catch (_: Exception) {}
        }
        try {
            session?.let { ws ->
                for (frame in ws.incoming) {
                    if (frame is Frame.Text) {
                        try { _incoming.emit(ServerMessage.decode(frame.readText())) } catch (_: Exception) {}
                    }
                }
            }
        } catch (_: Exception) {}
        // Connection closed — throw so connect() loop knows to reconnect
        throw Exception("WebSocket closed")
    }

    fun connect() {
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            var attempt = 0
            while (true) {
                try {
                    connectOnce()
                    // connectOnce returned normally — connection closed
                } catch (_: Exception) {}
                _connectionState.value = ConnectionState.DISCONNECTED
                if (attempt >= 5) break
                _connectionState.value = ConnectionState.RECONNECTING
                delay((1000L * (attempt + 1)).coerceAtMost(5000L))
                attempt++
            }
            _connectionState.value = ConnectionState.DISCONNECTED
        }
    }

    fun send(message: ClientMessage) { scope.launch { outgoing.send(message) } }

    suspend fun disconnect() {
        reconnectJob?.cancel()
        session?.close(CloseReason(CloseReason.Codes.NORMAL, "User left"))
        session = null
        _connectionState.value = ConnectionState.DISCONNECTED
    }
}
