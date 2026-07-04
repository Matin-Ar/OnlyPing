package com.matin.onlyping.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

class TcpEngine {
    /**
     * Checks if a TCP connection can be established to the host:port.
     * Returns the latency in ms if successful, or null otherwise.
     */
    suspend fun check(host: String, port: Int, timeoutMs: Int = 3000): Long? = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, port), timeoutMs)
                System.currentTimeMillis() - startTime
            }
        } catch (e: Exception) {
            null
        }
    }
}
