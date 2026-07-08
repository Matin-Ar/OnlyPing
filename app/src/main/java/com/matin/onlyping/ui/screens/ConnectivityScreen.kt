package com.matin.onlyping.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import com.matin.onlyping.BuildConfig
import com.matin.onlyping.model.*
import com.matin.onlyping.ui.viewmodel.ConnectivityViewModel
import java.util.Locale

private const val GITHUB_URL = "https://github.com/Matin-Ar/OnlyPing" // Change to your repo
private const val BTC_ADDRESS = "bc1qskmtga2nfxcdzvhdedcm3l3q9h30czrplw603y"
private const val ETH_ADDRESS = "0x9530BF0A5023Aa659CD4cf62E051e65F423058aE"
private const val USDT_ADDRESS = "TBm3hiDK3wYz4XcV4MEnCJjxZG9eYnHhLp"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectivityScreen(
    viewModel: ConnectivityViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val logs by viewModel.logs.collectAsState()
    val listState = rememberLazyListState()
    val context = LocalContext.current

    var showMenu by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    var showDonate by remember { mutableStateOf(false) }
    var showPrivacy by remember { mutableStateOf(false) }

    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }

    if (showAbout) AboutDialog { showAbout = false }
    if (showDonate) DonateDialog { showDonate = false }
    if (showPrivacy) PrivacyDialog { showPrivacy = false }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Only Ping", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("About") },
                            onClick = {
                                showMenu = false
                                showAbout = true
                            },
                            leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("GitHub") },
                            onClick = {
                                showMenu = false
                                val intent = Intent(Intent.ACTION_VIEW, GITHUB_URL.toUri())
                                context.startActivity(intent)
                            },
                            leadingIcon = { Icon(Icons.Default.Build, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Donate") },
                            onClick = {
                                showMenu = false
                                showDonate = true
                            },
                            leadingIcon = { Icon(Icons.Default.Favorite, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Privacy") },
                            onClick = {
                                showMenu = false
                                showPrivacy = true
                            },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) }
                        )
                    }
                }
            )
        },
        bottomBar = {
            StatsSection(uiState.stats)
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            InputSection(
                host = uiState.host,
                port = uiState.port,
                isMonitoring = uiState.isMonitoring,
                onHostChange = viewModel::updateHost,
                onPortChange = viewModel::updatePort,
                onStartStop = {
                    if (uiState.isMonitoring) viewModel.stopMonitoring() else viewModel.startMonitoring()
                },
                onClearLogs = viewModel::clearLogs
            )

            StatusSection(uiState.currentStatus)

            ConsoleSection(logs, listState, Modifier.weight(1f))
        }
    }
}

@Composable
fun InputSection(
    host: String,
    port: String,
    isMonitoring: Boolean,
    onHostChange: (String) -> Unit,
    onPortChange: (String) -> Unit,
    onStartStop: () -> Unit,
    onClearLogs: () -> Unit
) {
    var showPortInfo by remember { mutableStateOf(false) }
    var showHostInfo by remember { mutableStateOf(false) }

    if (showPortInfo) {
        AlertDialog(
            onDismissRequest = { showPortInfo = false },
            title = { Text("Port Settings") },
            text = { Text("The Port field is optional. Use it for specific TCP checks. If left empty, the app automatically detects the protocol or falls back to ICMP/common ports.") },
            confirmButton = {
                TextButton(onClick = { showPortInfo = false }) {
                    Text("OK")
                }
            }
        )
    }

    if (showHostInfo) {
        AlertDialog(
            onDismissRequest = { showHostInfo = false },
            title = { Text("Destination Examples") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("You can enter various types of destinations:")
                    Text("• Hostnames: google.com", fontWeight = FontWeight.Bold)
                    Text("• Websites: https://google.com", fontWeight = FontWeight.Bold)
                    Text("• IP Addresses: 8.8.8.8", fontWeight = FontWeight.Bold)
                    Text("• Host with Port: google.com:443", fontWeight = FontWeight.Bold)
                    Text("• IP with Port: 1.1.1.1:853", fontWeight = FontWeight.Bold)
                }
            },
            confirmButton = {
                TextButton(onClick = { showHostInfo = false }) {
                    Text("OK")
                }
            }
        )
    }

    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Destination", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { showHostInfo = true }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    OutlinedTextField(
                        value = host,
                        onValueChange = onHostChange,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = !isMonitoring,
                        leadingIcon = { Icon(Icons.Default.Place, contentDescription = null) },
                        trailingIcon = {
                            if (host.isNotEmpty() && !isMonitoring) {
                                IconButton(onClick = { onHostChange("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear Destination")
                                }
                            }
                        },
                        shape = RoundedCornerShape(8.dp)
                    )
                }
                Column(modifier = Modifier.width(120.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Port", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        Text(" (Optional)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        IconButton(onClick = { showPortInfo = true }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    OutlinedTextField(
                        value = port,
                        onValueChange = onPortChange,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = !isMonitoring,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        trailingIcon = {
                            if (port.isNotEmpty() && !isMonitoring) {
                                IconButton(onClick = { onPortChange("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear Port")
                                }
                            }
                        },
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onStartStop,
                    modifier = Modifier.weight(1f),
                    enabled = host.isNotBlank() || isMonitoring,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isMonitoring) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(if (isMonitoring) Icons.Default.Close else Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (isMonitoring) "Stop Monitoring" else "Start Monitoring")
                }
                IconButton(
                    onClick = onClearLogs,
                    enabled = !isMonitoring
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Clear Logs")
                }
            }
        }
    }
}

@Composable
fun StatusSection(status: ConnectivityStatus) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        StatusCard("DNS", status.dns, Icons.Default.Search, Modifier.weight(1f))
        Spacer(Modifier.width(8.dp))
        StatusCard("ICMP", status.icmp, Icons.Default.Notifications, Modifier.weight(1f))
        Spacer(Modifier.width(8.dp))
        StatusCard("TCP", status.tcp, Icons.Default.Share, Modifier.weight(1f))
        Spacer(Modifier.width(8.dp))
        StatusCard("HTTP", status.http, Icons.Default.Info, Modifier.weight(1f))
    }
}

@Composable
fun StatusCard(label: String, status: Status, icon: ImageVector, modifier: Modifier = Modifier) {
    val color = when (status) {
        Status.SUCCESS -> Color(0xFF4CAF50)
        Status.FAILED -> Color(0xFFF44336)
        Status.PENDING -> Color(0xFFFFC107)
        Status.IDLE -> MaterialTheme.colorScheme.outline
    }
    
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Text(text = label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
fun ConsoleSection(logs: List<LogEntry>, listState: androidx.compose.foundation.lazy.LazyListState, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text("Console Log", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(4.dp))
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF1E1E1E),
            shape = RoundedCornerShape(8.dp)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.padding(8.dp),
                contentPadding = PaddingValues(bottom = 8.dp)
            ) {
                items(logs) { log ->
                    Row(modifier = Modifier.padding(vertical = 1.dp)) {
                        Text(
                            text = "[${log.formattedTime}] ",
                            color = Color.Gray,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp
                        )
                        Text(
                            text = "${log.eventType}: ",
                            color = Color.Cyan,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = log.message,
                            color = log.color,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatsSection(stats: NetworkStats) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 4.dp,
        modifier = Modifier.navigationBarsPadding()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatItem("Availability", String.format(Locale.US, "%.1f%%", stats.availabilityPercentage), MaterialTheme.colorScheme.primary)
                StatItem("Failures", stats.failureCount.toString(), MaterialTheme.colorScheme.error)
                StatItem("Latency", "${stats.currentLatency}ms", MaterialTheme.colorScheme.secondary)
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatItem("Min", "${if (stats.minLatency == Long.MAX_VALUE) 0 else stats.minLatency}ms")
                StatItem("Max", "${stats.maxLatency}ms")
                StatItem("Avg", String.format(Locale.US, "%.1fms", stats.avgLatency))
                StatItem("Checks", stats.totalChecks.toString())
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
        Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = color)
    }
}

@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = { Text("Only Ping") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Version ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodySmall)
                Text(
                    "Only Ping is a lightweight network reachability monitor for Android. It helps verify whether websites, hosts, IP addresses, and services are reachable from your current internet connection using multiple network protocols.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun DonateDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Support Only Ping ❤️") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("If Only Ping has been useful to you, consider supporting future development.")
                
                AddressCard("Bitcoin (BTC)", BTC_ADDRESS)
                AddressCard("Ethereum (ETH)", ETH_ADDRESS)
                AddressCard("USDT (TRC20)", USDT_ADDRESS)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun AddressCard(label: String, address: String) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                clipboardManager.setText(AnnotatedString(address))
                Toast.makeText(context, "Address copied", Toast.LENGTH_SHORT).show()
            },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = address,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace
                )
                IconButton(onClick = {
                    clipboardManager.setText(AnnotatedString(address))
                    Toast.makeText(context, "Address copied", Toast.LENGTH_SHORT).show()
                }) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
fun PrivacyDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Privacy Policy") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PrivacyItem("No personal data collection.")
                PrivacyItem("No analytics or tracking.")
                PrivacyItem("No advertisements.")
                PrivacyItem("All network tests are executed locally on the device.")
                PrivacyItem("No information is sent to external servers.")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Understood") }
        }
    )
}

@Composable
fun PrivacyItem(text: String) {
    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}
