package com.matin.onlyping.model

import androidx.compose.ui.graphics.Color
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

enum class Status {
    IDLE, PENDING, SUCCESS, FAILED
}

enum class TargetType {
    WEBSITE, HOSTNAME, IP_PORT, IP_ONLY
}

data class ConnectivityStatus(
    val dns: Status = Status.IDLE,
    val icmp: Status = Status.IDLE,
    val tcp: Status = Status.IDLE,
    val http: Status = Status.IDLE
)

data class LogEntry(
    val timestamp: Long = System.currentTimeMillis(),
    val eventType: String,
    val message: String,
    val latency: Long? = null,
    val protocol: String? = null,
    val isSuccess: Boolean = true
) {
    private val formatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")
        .withZone(ZoneId.systemDefault())

    val formattedTime: String get() = formatter.format(Instant.ofEpochMilli(timestamp))
    
    val color: Color get() = if (isSuccess) Color(0xFF81C784) else Color(0xFFE57373)
}

data class NetworkStats(
    val totalChecks: Int = 0,
    val successCount: Int = 0,
    val failureCount: Int = 0,
    val minLatency: Long = Long.MAX_VALUE,
    val maxLatency: Long = 0,
    val totalLatency: Long = 0,
    val currentLatency: Long = 0,
    val startTime: Long = 0,
    val lastCheckTime: Long = 0
) {
    val avgLatency: Double get() = if (successCount > 0) totalLatency.toDouble() / successCount else 0.0
    val availabilityPercentage: Double get() = if (totalChecks > 0) (successCount.toDouble() / totalChecks) * 100 else 0.0
    val runningTimeSeconds: Long get() = if (startTime > 0) (System.currentTimeMillis() - startTime) / 1000 else 0
}

data class ConnectivityResult(
    val status: ConnectivityStatus,
    val logs: List<LogEntry>,
    val isSuccess: Boolean,
    val latency: Long
)
