package com.matin.onlyping.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

class HttpEngine {
    /**
     * Performs a HEAD request to the host.
     * Returns the HTTP response code if successful, or null otherwise.
     */
    suspend fun check(host: String, port: Int? = null, useHttps: Boolean = true): Int? = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val protocol = if (useHttps) "https" else "http"
            val portSuffix = if (port != null && port != 80 && port != 443) ":$port" else ""
            val urlString = if (host.startsWith("http")) host else "$protocol://$host$portSuffix"
            val url = URL(urlString)
            
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "HEAD"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.instanceFollowRedirects = true
            
            val responseCode = connection.responseCode
            responseCode
        } catch (e: Exception) {
            null
        } finally {
            connection?.disconnect()
        }
    }
}
