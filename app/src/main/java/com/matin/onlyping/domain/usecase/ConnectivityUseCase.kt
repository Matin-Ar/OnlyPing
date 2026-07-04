package com.matin.onlyping.domain.usecase

import com.matin.onlyping.data.repository.NetworkRepository
import com.matin.onlyping.model.*
import java.util.regex.Pattern

class ConnectivityUseCase(private val repository: NetworkRepository = NetworkRepository()) {

    private val ipPattern = Pattern.compile(
        "^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])$"
    )

    suspend fun execute(host: String, portStr: String): ConnectivityResult {
        val type = determineTargetType(host, portStr)
        val port = portStr.toIntOrNull()
        
        var isReachable = false
        var primaryLatency = 0L
        val logs = mutableListOf<LogEntry>()

        when (type) {
            TargetType.WEBSITE -> {
                // Primary: HTTP HEAD
                val httpResult = repository.checkHttp(host)
                if (httpResult != null) {
                    isReachable = true
                    logs.add(LogEntry(eventType = "HTTP", message = "Connected via HTTP (Code $httpResult)", protocol = "HTTP"))
                } else {
                    isReachable = false
                    logs.add(LogEntry(eventType = "HTTP", message = "HTTP check failed", isSuccess = false, protocol = "HTTP"))
                    // Diagnostics
                    performDiagnostics(host, port ?: 443, logs)
                }
            }
            TargetType.HOSTNAME -> {
                // Step 1: DNS Resolution
                val resolvedIp = repository.resolveDns(host)
                if (resolvedIp != null) {
                    logs.add(LogEntry(eventType = "DNS", message = "Resolved $host to $resolvedIp", protocol = "DNS"))
                    
                    if (port != null) {
                        // Primary: TCP
                        val latency = repository.checkTcp(host, port)
                        if (latency != null) {
                            isReachable = true
                            primaryLatency = latency
                            logs.add(LogEntry(eventType = "TCP", message = "TCP connected to port $port", latency = latency, protocol = "TCP"))
                        } else {
                            isReachable = false
                            logs.add(LogEntry(eventType = "TCP", message = "TCP connection failed to port $port", isSuccess = false, protocol = "TCP"))
                            // Diagnostic: ICMP
                            val icmpOk = repository.pingOnce(host)
                            logs.add(LogEntry(eventType = "ICMP", message = if (icmpOk) "ICMP Ping responded" else "ICMP Ping failed", isSuccess = icmpOk, protocol = "ICMP"))
                        }
                    } else {
                        // Primary: HTTP (Web host assumption)
                        val httpCode = repository.checkHttp(host)
                        if (httpCode != null) {
                            isReachable = true
                            logs.add(LogEntry(eventType = "HTTP", message = "HTTP reachable (Code $httpCode)", protocol = "HTTP"))
                        } else {
                            isReachable = false
                            logs.add(LogEntry(eventType = "HTTP", message = "HTTP unreachable", isSuccess = false, protocol = "HTTP"))
                            // Diagnostic: TCP 80/443
                            val tcp443 = repository.checkTcp(host, 443)
                            logs.add(LogEntry(eventType = "TCP", message = if (tcp443 != null) "TCP 443 open" else "TCP 443 closed", isSuccess = tcp443 != null, protocol = "TCP"))
                        }
                    }
                } else {
                    isReachable = false
                    logs.add(LogEntry(eventType = "DNS", message = "DNS Resolution failed", isSuccess = false, protocol = "DNS"))
                }
            }
            TargetType.IP_PORT -> {
                // Primary: TCP
                if (port != null) {
                    val latency = repository.checkTcp(host, port)
                    if (latency != null) {
                        isReachable = true
                        primaryLatency = latency
                        logs.add(LogEntry(eventType = "TCP", message = "Connected to $host:$port", latency = latency, protocol = "TCP"))
                    } else {
                        isReachable = false
                        logs.add(LogEntry(eventType = "TCP", message = "Failed to connect to $host:$port", isSuccess = false, protocol = "TCP"))
                        // Diagnostic: ICMP
                        val icmpOk = repository.pingOnce(host)
                        logs.add(LogEntry(eventType = "ICMP", message = if (icmpOk) "ICMP Ping responded" else "ICMP Ping failed", isSuccess = icmpOk, protocol = "ICMP"))
                    }
                }
            }
            TargetType.IP_ONLY -> {
                // Primary: ICMP
                val icmpOk = repository.pingOnce(host)
                if (icmpOk) {
                    isReachable = true
                    logs.add(LogEntry(eventType = "ICMP", message = "ICMP Ping successful", protocol = "ICMP"))
                } else {
                    logs.add(LogEntry(eventType = "ICMP", message = "ICMP Ping failed", isSuccess = false, protocol = "ICMP"))
                    // Fallback to TCP 443/80
                    val tcp443 = repository.checkTcp(host, 443)
                    if (tcp443 != null) {
                        isReachable = true
                        primaryLatency = tcp443
                        logs.add(LogEntry(eventType = "TCP", message = "Reachable via TCP 443 fallback", latency = tcp443, protocol = "TCP"))
                    } else {
                        val tcp80 = repository.checkTcp(host, 80)
                        if (tcp80 != null) {
                            isReachable = true
                            primaryLatency = tcp80
                            logs.add(LogEntry(eventType = "TCP", message = "Reachable via TCP 80 fallback", latency = tcp80, protocol = "TCP"))
                        } else {
                            isReachable = false
                            logs.add(LogEntry(eventType = "DIAG", message = "All reachability tests failed", isSuccess = false))
                        }
                    }
                }
            }
        }

        // Final status mapping for UI diagnostic icons
        val finalStatus = ConnectivityStatus(
            dns = getProtocolStatus(logs, "DNS"),
            icmp = getProtocolStatus(logs, "ICMP"),
            tcp = getProtocolStatus(logs, "TCP"),
            http = getProtocolStatus(logs, "HTTP")
        )

        return ConnectivityResult(
            status = finalStatus,
            logs = logs,
            isSuccess = isReachable,
            latency = primaryLatency
        )
    }

    private fun getProtocolStatus(logs: List<LogEntry>, protocol: String): Status {
        val protocolLogs = logs.filter { it.protocol == protocol }
        if (protocolLogs.isEmpty()) return Status.IDLE
        return if (protocolLogs.any { it.isSuccess }) Status.SUCCESS else Status.FAILED
    }

    private suspend fun performDiagnostics(host: String, port: Int, logs: MutableList<LogEntry>) {
        val resolvedIp = repository.resolveDns(host)
        if (resolvedIp != null) {
            logs.add(LogEntry(eventType = "DNS", message = "DNS Resolution OK ($resolvedIp)", protocol = "DNS"))
            val tcpLatency = repository.checkTcp(host, port)
            if (tcpLatency != null) {
                logs.add(LogEntry(eventType = "TCP", message = "TCP Port $port is OPEN", latency = tcpLatency, protocol = "TCP"))
            } else {
                logs.add(LogEntry(eventType = "TCP", message = "TCP Port $port is CLOSED", isSuccess = false, protocol = "TCP"))
            }
        } else {
            logs.add(LogEntry(eventType = "DNS", message = "DNS Resolution FAILED", isSuccess = false, protocol = "DNS"))
        }
    }

    private fun determineTargetType(host: String, portStr: String): TargetType {
        val hasPort = portStr.isNotBlank() && portStr.toIntOrNull() != null
        val isIp = ipPattern.matcher(host).matches()
        
        return when {
            host.startsWith("http") -> TargetType.WEBSITE
            isIp && hasPort -> TargetType.IP_PORT
            isIp -> TargetType.IP_ONLY
            else -> if (hasPort) TargetType.IP_PORT else TargetType.HOSTNAME
        }
    }
}
