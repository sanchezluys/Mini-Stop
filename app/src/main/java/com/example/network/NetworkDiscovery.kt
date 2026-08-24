package com.example.network

import android.content.Context
import android.net.wifi.WifiManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.Collections

data class DiscoveredRoom(
    val roomCode: String,
    val hostName: String,
    val hostIp: String,
    val port: Int = 8888,
    val playerCount: Int = 1,
    val maxPlayers: Int = 6,
    val lastSeenTimestamp: Long = System.currentTimeMillis()
)

class NetworkDiscovery(private val context: Context) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private var broadcastJob: Job? = null
    private var listenJob: Job? = null
    private var multicastLock: WifiManager.MulticastLock? = null

    private val _discoveredRooms = MutableSharedFlow<DiscoveredRoom>(extraBufferCapacity = 16)
    val discoveredRooms: SharedFlow<DiscoveredRoom> = _discoveredRooms

    companion object {
        const val DISCOVERY_PORT = 8889
        const val DEFAULT_GAME_PORT = 8888
    }

    fun startBroadcasting(roomCode: String, hostName: String, playerCount: Int, maxPlayers: Int) {
        stopBroadcasting()
        broadcastJob = scope.launch {
            var socket: DatagramSocket? = null
            try {
                socket = DatagramSocket()
                socket.broadcast = true

                while (isActive) {
                    val localIp = getLocalIpAddress()
                    val payload = JSONObject().apply {
                        put("type", "STOP_DISCOVERY")
                        put("roomCode", roomCode)
                        put("hostName", hostName)
                        put("hostIp", localIp)
                        put("port", DEFAULT_GAME_PORT)
                        put("playerCount", playerCount)
                        put("maxPlayers", maxPlayers)
                    }

                    val messageBytes = payload.toString().toByteArray(Charsets.UTF_8)
                    val broadcastAddress = getBroadcastAddress()

                    val packet = DatagramPacket(
                        messageBytes,
                        messageBytes.size,
                        broadcastAddress,
                        DISCOVERY_PORT
                    )

                    try {
                        socket.send(packet)
                    } catch (_: Exception) {}

                    delay(1500)
                }
            } catch (_: Exception) {
            } finally {
                socket?.close()
            }
        }
    }

    fun stopBroadcasting() {
        broadcastJob?.cancel()
        broadcastJob = null
    }

    fun startListening() {
        stopListening()
        acquireMulticastLock()

        listenJob = scope.launch {
            var socket: DatagramSocket? = null
            try {
                socket = DatagramSocket(DISCOVERY_PORT)
                socket.broadcast = true
                val buffer = ByteArray(2048)

                while (isActive) {
                    val packet = DatagramPacket(buffer, buffer.size)
                    try {
                        socket.receive(packet)
                        val text = String(packet.data, 0, packet.length, Charsets.UTF_8)
                        val json = JSONObject(text)
                        if (json.optString("type") == "STOP_DISCOVERY") {
                            val hostIp = if (json.optString("hostIp").isNotEmpty()) {
                                json.getString("hostIp")
                            } else {
                                packet.address.hostAddress ?: ""
                            }

                            val room = DiscoveredRoom(
                                roomCode = json.optString("roomCode", "STOP"),
                                hostName = json.optString("hostName", "Host"),
                                hostIp = hostIp,
                                port = json.optInt("port", DEFAULT_GAME_PORT),
                                playerCount = json.optInt("playerCount", 1),
                                maxPlayers = json.optInt("maxPlayers", 6)
                            )
                            _discoveredRooms.emit(room)
                        }
                    } catch (_: Exception) {}
                }
            } catch (_: Exception) {
            } finally {
                socket?.close()
                releaseMulticastLock()
            }
        }
    }

    fun stopListening() {
        listenJob?.cancel()
        listenJob = null
        releaseMulticastLock()
    }

    private fun acquireMulticastLock() {
        try {
            val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            multicastLock = wifi?.createMulticastLock("stop_multicast_lock")?.apply {
                setReferenceCounted(true)
                acquire()
            }
        } catch (_: Exception) {}
    }

    private fun releaseMulticastLock() {
        try {
            if (multicastLock?.isHeld == true) {
                multicastLock?.release()
            }
        } catch (_: Exception) {}
        multicastLock = null
    }

    fun getLocalIpAddress(): String {
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (intf in interfaces) {
                if (intf.isLoopback || !intf.isUp) continue
                val addrs = Collections.list(intf.inetAddresses)
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress && addr.address.size == 4) {
                        val host = addr.hostAddress ?: ""
                        if (host.startsWith("192.168.") || host.startsWith("10.") || host.startsWith("172.")) {
                            return host
                        }
                    }
                }
            }
            // fallback
            for (intf in interfaces) {
                val addrs = Collections.list(intf.inetAddresses)
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress && addr.address.size == 4) {
                        return addr.hostAddress ?: "127.0.0.1"
                    }
                }
            }
        } catch (_: Exception) {}
        return "127.0.0.1"
    }

    private fun getBroadcastAddress(): InetAddress {
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (intf in interfaces) {
                if (intf.isLoopback || !intf.isUp) continue
                for (interfaceAddress in intf.interfaceAddresses) {
                    val broadcast = interfaceAddress.broadcast
                    if (broadcast != null) {
                        return broadcast
                    }
                }
            }
        } catch (_: Exception) {}
        return InetAddress.getByName("255.255.255.255")
    }

    fun generateRoomCode(): String {
        val ip = getLocalIpAddress()
        val parts = ip.split(".")
        val suffix = if (parts.size >= 4) parts[3] else ((100..999).random().toString())
        val randomDigits = (10..99).random()
        return "STOP-$suffix$randomDigits"
    }
}
