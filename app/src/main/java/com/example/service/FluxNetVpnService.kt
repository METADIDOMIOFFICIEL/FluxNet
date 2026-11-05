package com.example.service

import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import com.example.data.local.PlanPreferencesRepository
import com.example.data.network.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.FileInputStream
import java.nio.ByteBuffer

class FluxNetVpnService : VpnService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var vpnInterface: ParcelFileDescriptor? = null
    private var drainJob: Job? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopFirewall()
                return START_NOT_STICKY
            }
            ACTION_START_OR_UPDATE, null -> {
                val blockedPackages = intent?.getStringArrayListExtra(EXTRA_BLOCKED_PACKAGES) ?: arrayListOf()
                startOrUpdateFirewall(blockedPackages)
            }
        }
        return START_STICKY
    }

    private fun startOrUpdateFirewall(blockedPackages: List<String>) {
        val notification = NotificationHelper.buildFirewallNotification(this, blockedPackages.size)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NotificationHelper.NOTIF_ID_FIREWALL,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NotificationHelper.NOTIF_ID_FIREWALL, notification)
        }

        drainJob?.cancel()
        runCatching { vpnInterface?.close() }
        vpnInterface = null

        val builder = Builder()
            .setSession("FluxNet Bouclier")
            .setMtu(1500)
            .addAddress("10.10.10.1", 32)
            .addRoute("0.0.0.0", 0)

        // Route ONLY blocked packages into the local blackhole TUN interface.
        // If no packages are blocked, route a dummy non-existent package or keep TUN idle
        // so all other apps bypass the VPN at full native speed.
        var addedAny = false
        for (pkg in blockedPackages) {
            if (pkg != packageName) {
                runCatching {
                    builder.addAllowedApplication(pkg)
                    addedAny = true
                }
            }
        }

        if (!addedAny) {
            // Route only our own unused dummy or disallow all except nothing:
            // On Android VpnService, if addAllowedApplication is not called, ALL apps are routed!
            // So when blockedPackages is empty, we add our own package (which makes no background requests)
            // so user apps are completely unaffected.
            runCatching { builder.addAllowedApplication(packageName) }
        }

        vpnInterface = runCatching { builder.establish() }.getOrNull()
        val pfd = vpnInterface ?: return

        drainJob = serviceScope.launch {
            val input = FileInputStream(pfd.fileDescriptor)
            val buffer = ByteBuffer.allocate(32767)
            while (isActive) {
                val length = runCatching { input.read(buffer.array()) }.getOrDefault(-1)
                if (length < 0) break
                buffer.clear()
            }
        }
    }

    private fun stopFirewall() {
        drainJob?.cancel()
        runCatching { vpnInterface?.close() }
        vpnInterface = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onRevoke() {
        serviceScope.launch {
            PlanPreferencesRepository(applicationContext).setFirewallEnabled(false)
        }
        stopFirewall()
        super.onRevoke()
    }

    override fun onDestroy() {
        drainJob?.cancel()
        runCatching { vpnInterface?.close() }
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        const val ACTION_START_OR_UPDATE = "com.example.service.ACTION_START_OR_UPDATE"
        const val ACTION_STOP = "com.example.service.ACTION_STOP"
        const val EXTRA_BLOCKED_PACKAGES = "extra_blocked_packages"

        fun syncFirewall(context: Context, enabled: Boolean, blockedPackages: List<String>) {
            if (!enabled) {
                val stopIntent = Intent(context, FluxNetVpnService::class.java).apply {
                    action = ACTION_STOP
                }
                runCatching { context.startService(stopIntent) }
                return
            }

            if (prepare(context) == null) {
                val intent = Intent(context, FluxNetVpnService::class.java).apply {
                    action = ACTION_START_OR_UPDATE
                    putStringArrayListExtra(EXTRA_BLOCKED_PACKAGES, ArrayList(blockedPackages))
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            }
        }
    }
}
