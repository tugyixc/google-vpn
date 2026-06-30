package com.example.network

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.FileInputStream
import java.net.InetSocketAddress
import java.nio.channels.DatagramChannel

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

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "VpnService onCreate")
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
        
        try {
            val builder = Builder()
                .setSession("Phoenix VPN")
                .setMtu(1280)
                .addAddress(ip, 32)
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
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopVpn()
        serviceScope.cancel()
        Log.d(TAG, "VpnService onDestroy")
    }
}
