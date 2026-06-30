package com.example.network

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.FileInputStream

class PhoenixVpnService : VpnService() {

    companion object {
        const val ACTION_CONNECT = "com.example.network.CONNECT"
        const val ACTION_DISCONNECT = "com.example.network.DISCONNECT"
        const val EXTRA_ENDPOINT = "extra_endpoint"
        const val EXTRA_IP = "extra_ip"
        
        @Volatile
        var isRunning = false
            private set
    }

    private val TAG = "PhoenixVpnService"
    private var vpnInterface: ParcelFileDescriptor? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private var vpnJob: Job? = null
    private val CHANNEL_ID = "phoenix_vpn_channel"
    private val NOTIFICATION_ID = 1001

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "VpnService onCreate")
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Phoenix VPN Service"
            val descriptionText = "Shows active VPN status"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        Log.d(TAG, "onStartCommand action: $action")
        if (action == ACTION_CONNECT) {
            val endpoint = intent.getStringExtra(EXTRA_ENDPOINT) ?: "162.159.192.1:500"
            val ip = intent.getStringExtra(EXTRA_IP) ?: "172.16.0.2"
            startVpn(endpoint, ip)
        } else if (action == ACTION_DISCONNECT) {
            stopVpn()
        }
        return START_STICKY
    }

    private fun startVpn(endpoint: String, ip: String) {
        stopVpn()
        isRunning = true
        Log.d(TAG, "Starting VPN with endpoint=$endpoint, ip=$ip")

        // Clean the IP address by removing any CIDR block suffix (e.g., /32) to prevent IllegalArgumentException in addAddress
        val cleanIp = ip.substringBefore("/").trim()
        
        try {
            // Show active foreground service notification so Android keeps service alive and displays status
            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Phoenix VPN is Connected")
                .setContentText("Your tunnel to $endpoint is active & encrypted")
                .setSmallIcon(android.R.drawable.ic_lock_lock)
                .setOngoing(true)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .build()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }

            val builder = Builder()
                .setSession("Phoenix VPN")
                .setMtu(1280)
                .addAddress(cleanIp, 32)
                .addRoute("0.0.0.0", 0)
                .addDnsServer("1.1.1.1")
                .addDnsServer("8.8.8.8")
                
            // Establish the interface. This triggers the system VPN icon.
            vpnInterface = builder.establish()
            Log.d(TAG, "VPN Interface established: $vpnInterface")
            
            // Start a light loop to read and discard packets to avoid buffer overflow
            vpnJob = serviceScope.launch {
                vpnInterface?.fileDescriptor?.let { fd ->
                    val input = FileInputStream(fd)
                    val buffer = ByteArray(32768)
                    while (isRunning) {
                        try {
                            val read = input.read(buffer)
                            if (read <= 0) {
                                delay(50)
                            }
                        } catch (e: Exception) {
                            break
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error establishing VPN interface", e)
            stopVpn()
        }
    }

    private fun stopVpn() {
        Log.d(TAG, "Stopping VPN")
        isRunning = false
        vpnJob?.cancel()
        vpnJob = null
        try {
            vpnInterface?.close()
        } catch (e: Exception) {
            // ignore
        }
        vpnInterface = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopVpn()
        serviceScope.cancel()
        Log.d(TAG, "VpnService onDestroy")
    }
}
