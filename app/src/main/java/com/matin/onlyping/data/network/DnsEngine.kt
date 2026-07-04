package com.matin.onlyping.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.UnknownHostException

class DnsEngine {
    /**
     * Resolves a hostname to an IP address.
     * Returns the IP address string if successful, or null otherwise.
     */
    suspend fun resolve(host: String): String? = withContext(Dispatchers.IO) {
        try {
            InetAddress.getByName(host).hostAddress
        } catch (e: UnknownHostException) {
            null
        } catch (e: Exception) {
            null
        }
    }
}
