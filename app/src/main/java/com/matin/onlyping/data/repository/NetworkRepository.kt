package com.matin.onlyping.data.repository

import com.matin.onlyping.data.network.DnsEngine
import com.matin.onlyping.data.network.HttpEngine
import com.matin.onlyping.data.network.PingEngine
import com.matin.onlyping.data.network.TcpEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class NetworkRepository(
    private val dnsEngine: DnsEngine = DnsEngine(),
    private val tcpEngine: TcpEngine = TcpEngine(),
    private val httpEngine: HttpEngine = HttpEngine(),
    private val pingEngine: PingEngine = PingEngine()
) {
    suspend fun resolveDns(host: String): String? = dnsEngine.resolve(host)

    suspend fun checkTcp(host: String, port: Int): Long? = tcpEngine.check(host, port)

    suspend fun checkHttp(host: String, port: Int? = null, useHttps: Boolean = true): Int? =
        httpEngine.check(host, port, useHttps)

    suspend fun pingOnce(host: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val process = ProcessBuilder("ping", "-c", "1", "-W", "1", host).start()
            process.waitFor() == 0
        } catch (e: Exception) {
            false
        }
    }

    fun startPing(host: String): Flow<String> = pingEngine.ping(host)
}
