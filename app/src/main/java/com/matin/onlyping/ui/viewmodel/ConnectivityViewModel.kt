package com.matin.onlyping.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.matin.onlyping.data.repository.NetworkRepository
import com.matin.onlyping.domain.usecase.ConnectivityUseCase
import com.matin.onlyping.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class ConnectivityViewModel(
    private val connectivityUseCase: ConnectivityUseCase = ConnectivityUseCase(),
    private val repository: NetworkRepository = NetworkRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConnectivityUiState())
    val uiState: StateFlow<ConnectivityUiState> = _uiState.asStateFlow()

    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    private var monitoringJob: Job? = null
    private val maxLogSize = 100

    fun updateHost(host: String) {
        if (host.contains(":") && !host.startsWith("http")) {
            val parts = host.split(":")
            if (parts.size == 2) {
                val extractedHost = parts[0]
                val extractedPort = parts[1]
                if (extractedPort.toIntOrNull() != null) {
                    _uiState.update { it.copy(host = extractedHost, port = extractedPort) }
                    return
                }
            }
        }
        _uiState.update { it.copy(host = host) }
    }

    fun updatePort(port: String) {
        if (port.isEmpty() || port.all { it.isDigit() }) {
            _uiState.update { it.copy(port = port) }
        }
    }

    fun startMonitoring() {
        if (_uiState.value.isMonitoring) return
        
        val host = _uiState.value.host
        val port = _uiState.value.port
        
        _uiState.update { 
            it.copy(
                isMonitoring = true,
                stats = NetworkStats(startTime = System.currentTimeMillis())
            ) 
        }
        _logs.value = emptyList()

        monitoringJob = viewModelScope.launch(Dispatchers.IO) {
            // Start ICMP Flow
            val icmpJob = launch {
                repository.startPing(host).collect { line ->
                    // Only update status icon from continuous ping, don't duplicate logs
                    // if they are already coming from the cycle.
                    // Actually, let's keep it for more real-time feel if needed,
                    // but filter to only show actual results.
                    if (line.contains("time=") || line.contains("timeout") || line.contains("Unreachable")) {
                        parseIcmpLine(line, shouldLog = false)
                    }
                }
            }

            // Start Cycle Loop
            while (isActive) {
                val result = connectivityUseCase.execute(host, port)
                updateStateWithResult(result)
                delay(2000)
            }
            
            icmpJob.cancel()
        }
    }

    fun stopMonitoring() {
        monitoringJob?.cancel()
        monitoringJob = null
        _uiState.update { 
            it.copy(
                isMonitoring = false,
                currentStatus = ConnectivityStatus()
            ) 
        }
    }

    fun clearLogs() {
        _logs.value = emptyList()
    }

    private fun updateStateWithResult(result: ConnectivityResult) {
        _uiState.update { state ->
            val newStats = state.stats.copy(
                totalChecks = state.stats.totalChecks + 1,
                successCount = state.stats.successCount + (if (result.isSuccess) 1 else 0),
                failureCount = state.stats.failureCount + (if (result.isSuccess) 0 else 1),
                minLatency = if (result.isSuccess && result.latency > 0) minOf(state.stats.minLatency, result.latency) else state.stats.minLatency,
                maxLatency = if (result.isSuccess) maxOf(state.stats.maxLatency, result.latency) else state.stats.maxLatency,
                totalLatency = state.stats.totalLatency + result.latency,
                currentLatency = result.latency,
                lastCheckTime = System.currentTimeMillis()
            )
            state.copy(
                currentStatus = result.status,
                stats = newStats
            )
        }
        addLogs(result.logs)
    }

    private fun parseIcmpLine(line: String, shouldLog: Boolean = true) {
        val isSuccess = line.contains("time=")
        val latency = if (isSuccess) {
            line.substringAfter("time=").substringBefore(" ms").toDoubleOrNull()?.toLong()
        } else null
        
        if (isSuccess || line.contains("timeout") || line.contains("Unreachable")) {
            _uiState.update { state ->
                state.copy(
                    currentStatus = state.currentStatus.copy(
                        icmp = if (isSuccess) Status.SUCCESS else Status.FAILED
                    )
                )
            }
            
            if (shouldLog) {
                val log = LogEntry(
                    eventType = "ICMP",
                    message = line,
                    latency = latency,
                    protocol = "ICMP",
                    isSuccess = isSuccess
                )
                addLogs(listOf(log))
            }
        }
    }

    private fun addLogs(newLogs: List<LogEntry>) {
        _logs.update { currentLogs ->
            (currentLogs + newLogs).takeLast(maxLogSize)
        }
    }
}

data class ConnectivityUiState(
    val host: String = "",
    val port: String = "",
    val isMonitoring: Boolean = false,
    val currentStatus: ConnectivityStatus = ConnectivityStatus(),
    val stats: NetworkStats = NetworkStats()
)
