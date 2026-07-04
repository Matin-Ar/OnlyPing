package com.matin.onlyping.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import java.io.BufferedReader
import java.io.InputStreamReader

class PingEngine {
    /**
     * Executes the system ping command and flows the output lines.
     */
    fun ping(host: String): Flow<String> = callbackFlow {
        val process = try {
            ProcessBuilder("ping", "-c", "1", "-W", "2", host)
                .redirectErrorStream(true)
                .start()
        } catch (e: Exception) {
            trySend("Error starting ping: ${e.message}")
            close(e)
            return@callbackFlow
        }

        val reader = BufferedReader(InputStreamReader(process.inputStream))
        
        try {
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                line?.let { trySend(it) }
            }
            process.waitFor()
        } catch (e: Exception) {
            trySend("Ping error: ${e.message}")
        } finally {
            reader.close()
            process.destroy()
            close()
        }

        awaitClose {
            process.destroy()
        }
    }.flowOn(Dispatchers.IO)
}
