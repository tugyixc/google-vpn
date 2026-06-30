package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.network.PhoenixVpnService
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.WarpConfigEntity
import java.text.DecimalFormat

// Cyberpunk-inspired colors
val ObsidianDark = Color(0xFF090D16)
val CardSlate = Color(0xFF121824)
val NeonGreen = Color(0xFF00FF88)
val NeonGreenDim = Color(0x2200FF88)
val CyberBlue = Color(0xFF00E5FF)
val CyberBlueDim = Color(0x2200E5FF)
val WarningAmber = Color(0xFFFFB300)
val WarningAmberDim = Color(0x22FFB300)
val DarkTerminal = Color(0xFF05080C)
val BorderSlate = Color(0xFF1E293B)
val TextGrayDim = Color(0xFF94A3B8)

@Composable
fun WarpDashboard(viewModel: WarpViewModel) {
    val context = LocalContext.current
    val isGenerating by viewModel.isGenerating.collectAsState()
    val logs by viewModel.generationLogs.collectAsState()
    val vpnState by viewModel.vpnState.collectAsState()
    val dlSpeed by viewModel.downloadSpeed.collectAsState()
    val ulSpeed by viewModel.uploadSpeed.collectAsState()
    val downloadedBytes by viewModel.bytesDownloaded.collectAsState()
    val uploadedBytes by viewModel.bytesUploaded.collectAsState()
    val ping by viewModel.pingMs.collectAsState()
    val serverLocation by viewModel.serverLocation.collectAsState()
    val latestConfig by viewModel.latestConfig.collectAsState()
    val connLogs by viewModel.connectionLogs.collectAsState()

    val vpnLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val config = latestConfig
            val endpoint = config?.endpoint ?: "162.159.192.1:500"
            val ip = config?.ipv4Address ?: "172.16.0.2"
            
            val intent = Intent(context, PhoenixVpnService::class.java).apply {
                action = PhoenixVpnService.ACTION_CONNECT
                putExtra(PhoenixVpnService.EXTRA_ENDPOINT, endpoint)
                putExtra(PhoenixVpnService.EXTRA_IP, ip)
            }
            context.startService(intent)
            viewModel.connect()
        } else {
            Toast.makeText(context, "VPN Permission denied!", Toast.LENGTH_SHORT).show()
        }
    }

    val attemptConnect = {
        val intent = VpnService.prepare(context)
        if (intent != null) {
            vpnLauncher.launch(intent)
        } else {
            val config = latestConfig
            val endpoint = config?.endpoint ?: "162.159.192.1:500"
            val ip = config?.ipv4Address ?: "172.16.0.2"
            
            val startIntent = Intent(context, PhoenixVpnService::class.java).apply {
                action = PhoenixVpnService.ACTION_CONNECT
                putExtra(PhoenixVpnService.EXTRA_ENDPOINT, endpoint)
                putExtra(PhoenixVpnService.EXTRA_IP, ip)
            }
            context.startService(startIntent)
            viewModel.connect()
        }
    }

    val attemptDisconnect = {
        val stopIntent = Intent(context, PhoenixVpnService::class.java).apply {
            action = PhoenixVpnService.ACTION_DISCONNECT
        }
        context.startService(stopIntent)
        viewModel.disconnect()
    }

    LaunchedEffect(Unit) {
        if (PhoenixVpnService.isRunning && vpnState == VpnState.DISCONNECTED) {
            viewModel.connect()
        }
    }

    // Smooth color animation based on VPN state
    val stateColor by animateColorAsState(
        targetValue = when (vpnState) {
            VpnState.CONNECTED -> NeonGreen
            VpnState.CONNECTING -> CyberBlue
            VpnState.DISCONNECTING -> WarningAmber
            VpnState.DISCONNECTED -> Color.DarkGray
        },
        animationSpec = tween(durationMillis = 500)
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = ObsidianDark
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            // 1. Header Banner Image & Title Section
            item {
                Spacer(modifier = Modifier.height(28.dp))
                
                // Futuristic Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "ဖီးနစ် VPN",
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 2.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "WireGuard Config Generator & Client",
                            color = TextGrayDim,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    
                    // State Indicator Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                when (vpnState) {
                                    VpnState.CONNECTED -> NeonGreenDim
                                    VpnState.CONNECTING -> CyberBlueDim
                                    VpnState.DISCONNECTING -> WarningAmberDim
                                    VpnState.DISCONNECTED -> Color(0x1A808080)
                                }
                            )
                            .border(
                                width = 1.dp,
                                color = stateColor,
                                shape = RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = vpnState.name,
                            color = stateColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Hero banner image
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, BorderSlate, RoundedCornerShape(16.dp))
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_vpn_hero),
                        contentDescription = "VPN Secure Hub",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    // Gradient overlay to blend with dark mode
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        ObsidianDark.copy(alpha = 0.85f)
                                    )
                                )
                            )
                    )
                    
                    // Floating stats on hero
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomStart)
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Security Status",
                                tint = if (vpnState == VpnState.CONNECTED) NeonGreen else Color.LightGray,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (vpnState == VpnState.CONNECTED) "Tunnel Encrypted" else "Tunnel Inactive",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        if (vpnState == VpnState.CONNECTED) {
                            Text(
                                text = "Ping: ${ping}ms",
                                color = NeonGreen,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // 2. MAIN HUB ACTIONS
            if (latestConfig == null) {
                // CASE 1: No config generated yet. Show big "Generate" flow.
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        colors = CardDefaults.cardColors(containerColor = CardSlate),
                        shape = RoundedCornerShape(16.dp),
                        border = borderSlateGrad()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Generate Cloudflare WARP Config",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Create a unique, cryptographically secure X25519 wireguard profile and register directly with Cloudflare's WARP service.",
                                color = TextGrayDim,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp
                            )
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            if (!isGenerating) {
                                Button(
                                    onClick = { viewModel.generateConfig() },
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                        .testTag("generate_config_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Generate",
                                        tint = ObsidianDark,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "GENERATE CONFIG",
                                        color = ObsidianDark,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp,
                                        fontSize = 14.sp
                                    )
                                }
                            } else {
                                CircularProgressIndicator(
                                    color = NeonGreen,
                                    modifier = Modifier
                                        .size(44.dp)
                                        .testTag("generating_progress_indicator")
                                )
                            }
                        }
                    }
                }

                // Show terminal logs when generating
                if (logs.isNotEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp)
                        ) {
                            Text(
                                text = "REGISTRATION CONSOLE LOG",
                                color = TextGrayDim,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 6.dp, start = 4.dp),
                                fontFamily = FontFamily.Monospace
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 120.dp, max = 220.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(DarkTerminal)
                                    .border(1.dp, BorderSlate, RoundedCornerShape(12.dp))
                                    .padding(14.dp)
                            ) {
                                // Monospace Logs
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    items(logs) { log ->
                                        Text(
                                            text = "> $log",
                                            color = if (log.contains("ERROR") || log.contains("failed")) Color.Red else NeonGreen,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp,
                                            lineHeight = 16.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

            } else {
                // CASE 2: Config is generated! Show the connection dashboard.
                // Hide generate button, show Connect/Disconnect power dials.
                item {
                    val config = latestConfig!!
                    
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        // Interactive Power Button Connection Dial
                        ConnectionPowerDial(
                            vpnState = vpnState,
                            onToggle = {
                                if (vpnState == VpnState.DISCONNECTED) {
                                    attemptConnect()
                                } else if (vpnState == VpnState.CONNECTED) {
                                    attemptDisconnect()
                                }
                            }
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Active node name display
                        Text(
                            text = if (vpnState == VpnState.CONNECTED) "CONNECTED TO" else "SECURE ENDPOINT READY",
                            color = if (vpnState == VpnState.CONNECTED) NeonGreen else TextGrayDim,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = if (vpnState == VpnState.CONNECTED) serverLocation else config.endpoint,
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 2.dp)
                        )

                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // Separate quick action buttons for connect and disconnect for accessibility/redundancy
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = { attemptConnect() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (vpnState == VpnState.DISCONNECTED) NeonGreenDim else Color(0x0F00FF88)
                                ),
                                shape = RoundedCornerShape(10.dp),
                                enabled = vpnState == VpnState.DISCONNECTED,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .border(
                                        width = 1.dp,
                                        color = if (vpnState == VpnState.DISCONNECTED) NeonGreen else Color.Transparent,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .testTag("connect_action_button"),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Connect",
                                    tint = if (vpnState == VpnState.DISCONNECTED) NeonGreen else Color.Gray,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "CONNECT",
                                    color = if (vpnState == VpnState.DISCONNECTED) Color.White else Color.Gray,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    letterSpacing = 0.5.sp
                                )
                            }

                            Button(
                                onClick = { attemptDisconnect() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (vpnState == VpnState.CONNECTED) Color(0x33FF3333) else Color(0x0FFF3333)
                                ),
                                shape = RoundedCornerShape(10.dp),
                                enabled = vpnState == VpnState.CONNECTED,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .border(
                                        width = 1.dp,
                                        color = if (vpnState == VpnState.CONNECTED) Color(0xFFFF5555) else Color.Transparent,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .testTag("disconnect_action_button"),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh, // acts as cancel/stop icon
                                    contentDescription = "Disconnect",
                                    tint = if (vpnState == VpnState.CONNECTED) Color(0xFFFF5555) else Color.Gray,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "DISCONNECT",
                                    color = if (vpnState == VpnState.CONNECTED) Color.White else Color.Gray,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(28.dp))

                        // 3. STATS DASHBOARD (Download, Upload speeds/usage)
                        TrafficStatsPanel(
                            dlSpeed = dlSpeed,
                            ulSpeed = ulSpeed,
                            downloadedBytes = downloadedBytes,
                            uploadedBytes = uploadedBytes,
                            vpnState = vpnState
                        )

                        if (connLogs.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(20.dp))
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Text(
                                    text = "LIVE HANDSHAKE CONNECTION LOG",
                                    color = TextGrayDim,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 6.dp, start = 4.dp),
                                    fontFamily = FontFamily.Monospace
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 110.dp, max = 190.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(DarkTerminal)
                                        .border(1.dp, BorderSlate, RoundedCornerShape(12.dp))
                                        .padding(14.dp)
                                ) {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        items(connLogs) { log ->
                                            Text(
                                                text = "> $log",
                                                color = if (log.contains("established") || log.contains("successfully")) NeonGreen else CyberBlue,
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 11.sp,
                                                lineHeight = 16.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(28.dp))

                        // 4. CONFIG EXPORTS
                        WireGuardProfilePanel(
                            config = config,
                            onRegenerate = {
                                viewModel.resetConfiguration()
                            }
                        )
                    }
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

/**
 * Beautiful glowing connection dial in the center
 */
@Composable
fun ConnectionPowerDial(
    vpnState: VpnState,
    onToggle: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val dialScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val activePulseAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "angleRotation"
    )

    val dialColor = when (vpnState) {
        VpnState.CONNECTED -> NeonGreen
        VpnState.CONNECTING -> CyberBlue
        VpnState.DISCONNECTING -> WarningAmber
        VpnState.DISCONNECTED -> Color.DarkGray
    }

    val glowAlpha = when (vpnState) {
        VpnState.CONNECTED -> 0.25f
        VpnState.CONNECTING, VpnState.DISCONNECTING -> 0.15f
        VpnState.DISCONNECTED -> 0.05f
    }

    Box(
        modifier = Modifier
            .size(200.dp)
            .clickable(onClick = onToggle)
            .testTag("vpn_power_dial"),
        contentAlignment = Alignment.Center
    ) {
        // Outer pulsing glow
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .offset(y = 0.dp)
        ) {
            val radius = size.minDimension / 2
            val centerOffset = Offset(size.width / 2, size.height / 2)
            
            // Draw radial pulsing glows when connected or connecting
            if (vpnState != VpnState.DISCONNECTED) {
                drawCircle(
                    color = dialColor.copy(alpha = glowAlpha * (if (vpnState == VpnState.CONNECTED) 1f else dialScale)),
                    radius = radius * (1.1f + (if (vpnState == VpnState.CONNECTED) 0f else 0.05f * dialScale)),
                    center = centerOffset
                )
            }
            
            // Draw neon border line with dash effect
            drawCircle(
                color = dialColor.copy(alpha = 0.5f),
                radius = radius * 0.95f,
                style = Stroke(
                    width = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), activePulseAngle)
                ),
                center = centerOffset
            )
        }

        // Inner Power Button Card Slate
        Box(
            modifier = Modifier
                .size(150.dp)
                .clip(CircleShape)
                .background(CardSlate)
                .border(2.dp, dialColor.copy(alpha = 0.8f), CircleShape)
                .shadow(12.dp, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Power",
                    tint = dialColor,
                    modifier = Modifier.size(46.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = when (vpnState) {
                        VpnState.DISCONNECTED -> "TAP TO CONNECT"
                        VpnState.CONNECTING -> "SHIELDING..."
                        VpnState.CONNECTED -> "SECURED"
                        VpnState.DISCONNECTING -> "DISCONNECTING"
                    },
                    color = if (vpnState == VpnState.DISCONNECTED) Color.White else dialColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * Download & Upload stats meters
 */
@Composable
fun TrafficStatsPanel(
    dlSpeed: Double,
    ulSpeed: Double,
    downloadedBytes: Long,
    uploadedBytes: Long,
    vpnState: VpnState
) {
    val dfSpeed = remember { DecimalFormat("0.0") }
    
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Download Speed Panel
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(110.dp),
                colors = CardDefaults.cardColors(containerColor = CardSlate),
                shape = RoundedCornerShape(12.dp),
                border = BorderSlateLight()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(NeonGreenDim),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Download",
                                tint = NeonGreen,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "DOWNLOAD",
                            color = TextGrayDim,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Column {
                        Text(
                            text = "${dfSpeed.format(dlSpeed)} Mbps",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Total: ${formatDataBytes(downloadedBytes)}",
                            color = TextGrayDim,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Upload Speed Panel
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(110.dp),
                colors = CardDefaults.cardColors(containerColor = CardSlate),
                shape = RoundedCornerShape(12.dp),
                border = BorderSlateLight()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(CyberBlueDim),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share, // customized upload look
                                contentDescription = "Upload",
                                tint = CyberBlue,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "UPLOAD",
                            color = TextGrayDim,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Column {
                        Text(
                            text = "${dfSpeed.format(ulSpeed)} Mbps",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Total: ${formatDataBytes(uploadedBytes)}",
                            color = TextGrayDim,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

/**
 * Beautiful WireGuard profile panel showing wgcf.conf
 */
@Composable
fun WireGuardProfilePanel(
    config: WarpConfigEntity,
    onRegenerate: () -> Unit
) {
    val context = LocalContext.current
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardSlate),
        shape = RoundedCornerShape(16.dp),
        border = BorderSlateLight()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Profile",
                        tint = NeonGreen,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "WireGuard Configuration",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Generated Cloudflare wgcf profile",
                            color = TextGrayDim,
                            fontSize = 11.sp
                        )
                    }
                }
                
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Toggle",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(14.dp))
                    
                    // Quick Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                copyToClipboard(context, config.configText)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BorderSlate),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1.5f)
                                .height(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Copy",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "COPY CONFIG", color = Color.White, fontSize = 11.sp)
                        }

                        Button(
                            onClick = {
                                onRegenerate()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0x22FF5555)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Regenerate",
                                tint = Color(0xFFFF5555),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "RESET", color = Color(0xFFFF5555), fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Code Viewer Container
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(DarkTerminal)
                            .border(1.dp, BorderSlate, RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    ) {
                        // Scrollable file content
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            item {
                                Text(
                                    text = config.configText,
                                    color = NeonGreen.copy(alpha = 0.9f),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Device registered with Cloudflare. Copied profiles can be imported directly into official WireGuard App.",
                        color = TextGrayDim,
                        fontSize = 9.sp,
                        lineHeight = 13.sp,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }
        }
    }
}

/**
 * Formatter for data bytes (B, KB, MB, GB)
 */
fun formatDataBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val exp = (Math.log(bytes.toDouble()) / Math.log(1024.0)).toInt()
    val df = DecimalFormat("0.0")
    val value = bytes / Math.pow(1024.0, exp.toDouble())
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    return "${df.format(value)} ${units[exp]}"
}

/**
 * Copies a string to clipboard
 */
fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("WireGuard Config", text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "Config copied to clipboard!", Toast.LENGTH_SHORT).show()
}

/**
 * Reusable neon borders and slates
 */
@Composable
fun borderSlateGrad() = androidx.compose.foundation.BorderStroke(
    width = 1.dp,
    color = BorderSlate
)

@Composable
fun BorderSlateLight() = androidx.compose.foundation.BorderStroke(
    width = 1.dp,
    color = BorderSlate
)
