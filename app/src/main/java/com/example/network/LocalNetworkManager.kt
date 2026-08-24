package com.example.network

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap

class LocalNetworkManager {
    private val scope = CoroutineScope(Dispatchers.IO)

    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null

    // For Host: maps playerId / connection ID to Socket + Writer
    private val clientConnections = ConcurrentHashMap<String, ClientHandler>()

    // For Client: connection to Host
    private var clientSocket: Socket? = null
    private var clientWriter: PrintWriter? = null
    private var clientJob: Job? = null

    private val _incomingPackets = MutableSharedFlow<NetworkPacket>(extraBufferCapacity = 64)
    val incomingPackets: SharedFlow<NetworkPacket> = _incomingPackets

    private val _connectionErrors = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val connectionErrors: SharedFlow<String> = _connectionErrors

    private var isHostMode: Boolean = false

    private class ClientHandler(
        val socket: Socket,
        val writer: PrintWriter,
        val job: Job
    )

    // --- HOST METHODS ---

    fun startHost(port: Int = NetworkDiscovery.DEFAULT_GAME_PORT) {
        stopAll()
        isHostMode = true

        serverJob = scope.launch {
            try {
                serverSocket = ServerSocket(port).apply {
                    reuseAddress = true
                }
                while (isActive) {
                    val socket = serverSocket?.accept() ?: break
                    handleNewClient(socket)
                }
            } catch (e: Exception) {
                if (isActive) {
                    _connectionErrors.emit("Error en servidor: ${e.localizedMessage}")
                }
            }
        }
    }

    private fun handleNewClient(socket: Socket) {
        val clientId = "conn_${System.currentTimeMillis()}_${(100..999).random()}"
        val writer = PrintWriter(socket.getOutputStream(), true)

        val job = scope.launch {
            try {
                val reader = BufferedReader(InputStreamReader(socket.getInputStream(), Charsets.UTF_8))
                while (isActive) {
                    val line = reader.readLine() ?: break
                    val packet = NetworkPacket.deserialize(line)
                    if (packet != null) {
                        _incomingPackets.emit(packet)
                    }
                }
            } catch (_: Exception) {
            } finally {
                clientConnections.remove(clientId)
                try { socket.close() } catch (_: Exception) {}
            }
        }

        clientConnections[clientId] = ClientHandler(socket, writer, job)
    }

    fun broadcastToClients(packet: NetworkPacket) {
        val serialized = packet.serialize()
        scope.launch {
            clientConnections.values.forEach { handler ->
                try {
                    handler.writer.println(serialized)
                } catch (_: Exception) {}
            }
        }
    }

    // --- CLIENT METHODS ---

    fun connectToHost(hostIp: String, port: Int = NetworkDiscovery.DEFAULT_GAME_PORT, onConnected: (Boolean) -> Unit) {
        stopAll()
        isHostMode = false

        clientJob = scope.launch {
            try {
                val socket = withContext(Dispatchers.IO) {
                    Socket(hostIp, port).apply {
                        tcpNoDelay = true
                        soTimeout = 0
                    }
                }
                clientSocket = socket
                clientWriter = PrintWriter(socket.getOutputStream(), true)

                withContext(Dispatchers.Main) {
                    onConnected(true)
                }

                val reader = BufferedReader(InputStreamReader(socket.getInputStream(), Charsets.UTF_8))
                while (isActive) {
                    val line = reader.readLine() ?: break
                    val packet = NetworkPacket.deserialize(line)
                    if (packet != null) {
                        _incomingPackets.emit(packet)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onConnected(false)
                }
                _connectionErrors.emit("No se pudo conectar al Host ($hostIp): ${e.localizedMessage}")
            } finally {
                disconnectClient()
            }
        }
    }

    fun sendToHost(packet: NetworkPacket) {
        val serialized = packet.serialize()
        scope.launch {
            try {
                clientWriter?.println(serialized)
            } catch (e: Exception) {
                _connectionErrors.emit("Error al enviar datos al host: ${e.localizedMessage}")
            }
        }
    }

    private fun disconnectClient() {
        try {
            clientWriter?.close()
            clientSocket?.close()
        } catch (_: Exception) {}
        clientWriter = null
        clientSocket = null
    }

    fun stopAll() {
        serverJob?.cancel()
        serverJob = null
        try { serverSocket?.close() } catch (_: Exception) {}
        serverSocket = null

        clientConnections.values.forEach {
            it.job.cancel()
            try { it.socket.close() } catch (_: Exception) {}
        }
        clientConnections.clear()

        clientJob?.cancel()
        clientJob = null
        disconnectClient()
    }
}
