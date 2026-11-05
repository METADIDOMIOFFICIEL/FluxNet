package com.example.service

import android.app.ActivityManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.PlanPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class FluxNetWatchdogService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var activityManager: ActivityManager
    private lateinit var notificationManager: NotificationManager

    companion object {
        const val CHANNEL_ID = "fluxnet_watchdog_channel"
        const val NOTIFICATION_ID = 2001
        const val ACTION_START = "com.example.service.action.START_WATCHDOG"
        const val ACTION_STOP = "com.example.service.action.STOP_WATCHDOG"

        fun startWatchdog(context: Context) {
            runCatching {
                val intent = Intent(context, FluxNetWatchdogService::class.java).apply {
                    action = ACTION_START
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            }
        }

        fun stopWatchdog(context: Context) {
            runCatching {
                val intent = Intent(context, FluxNetWatchdogService::class.java).apply {
                    action = ACTION_STOP
                }
                context.startService(intent)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            runCatching { stopForeground(STOP_FOREGROUND_REMOVE) }
            stopSelf()
            return START_NOT_STICKY
        }

        runCatching {
            val notification = createNotification("Bouclier FluxNet actif : protection anti-arrêt et surveillance RAM")
            startForeground(NOTIFICATION_ID, notification)
        }

        startRamMonitoringLoop()

        return START_STICKY
    }

    private fun startRamMonitoringLoop() {
        serviceScope.launch {
            val prefs = PlanPreferencesRepository(applicationContext)
            while (isActive) {
                try {
                    val config = prefs.planConfigFlow.first()
                    // Memory inspection
                    val memInfo = ActivityManager.MemoryInfo()
                    activityManager.getMemoryInfo(memInfo)

                    val thresholdMb = config.backgroundThresholdMbPerMin * 50 // e.g. 5 * 50 = 250 MB
                    val runningProcs = activityManager.runningAppProcesses ?: emptyList()

                    for (proc in runningProcs) {
                        if (proc.pkgList != null) {
                            for (pkg in proc.pkgList) {
                                if (pkg == packageName) continue

                                // Query process memory footprint
                                val pids = intArrayOf(proc.pid)
                                val pssInfo = activityManager.getProcessMemoryInfo(pids)
                                val pssKb = pssInfo.firstOrNull()?.totalPss ?: 0
                                val pssMb = pssKb / 1024

                                if (pssMb > 350) { // Exceeding RAM threshold
                                    activityManager.killBackgroundProcesses(pkg)
                                }
                            }
                        }
                    }
                } catch (_: Exception) {
                }
                delay(12_000L) // Scan every 12 seconds
            }
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // When task is removed from recents, automatically restart to prevent killing
        val restartIntent = Intent(applicationContext, FluxNetWatchdogService::class.java).apply {
            action = ACTION_START
        }
        val pendingIntent = PendingIntent.getService(
            applicationContext,
            1,
            restartIntent,
            PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as? android.app.AlarmManager
        alarmManager?.set(
            android.app.AlarmManager.RTC,
            System.currentTimeMillis() + 1000L,
            pendingIntent
        )
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Protection Anti-Arrêt & Anti-Lenteur",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Maintient FluxNet actif en continu et surveille la mémoire vive"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(contentText: String) =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Bouclier Anti-Lenteur & Anti-Arrêt")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(
                PendingIntent.getActivity(
                    this,
                    0,
                    Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE
                )
            )
            .build()
}
