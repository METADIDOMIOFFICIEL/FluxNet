package com.example.data.network

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R

object NotificationHelper {

    const val CHANNEL_ALERTS = "fluxnet_alerts"
    const val CHANNEL_FIREWALL = "fluxnet_firewall"
    const val NOTIF_ID_FIREWALL = 1001
    const val NOTIF_ID_DATA_ALERT = 1002
    const val NOTIF_ID_BG_BLOCKED = 1003

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val alertsChannel = NotificationChannel(
                CHANNEL_ALERTS,
                context.getString(R.string.notif_channel_alerts),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notif_channel_alerts_desc)
            }

            val firewallChannel = NotificationChannel(
                CHANNEL_FIREWALL,
                context.getString(R.string.notif_channel_firewall),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.notif_channel_firewall_desc)
                setShowBadge(false)
            }

            manager.createNotificationChannel(alertsChannel)
            manager.createNotificationChannel(firewallChannel)
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun buildFirewallNotification(context: Context, blockedCount: Int): Notification {
        createChannels(context)
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val contentText = if (blockedCount == 0) {
            "Surveillance active • Aucune application bloquée"
        } else {
            "$blockedCount application(s) restreinte(s) actuellement"
        }

        return NotificationCompat.Builder(context, CHANNEL_FIREWALL)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle("Bouclier Réseau FluxNet actif")
            .setContentText(contentText)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    fun notifyDataThresholdReached(
        context: Context,
        usagePercent: Int,
        usedFormatted: String,
        quotaFormatted: String
    ) {
        if (!hasNotificationPermission(context)) return
        createChannels(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            1,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ALERTS)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle("Alerte Forfait : $usagePercent% consommés")
            .setContentText("Vous avez utilisé $usedFormatted sur $quotaFormatted.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIF_ID_DATA_ALERT, notification)
    }

    fun notifyBackgroundAppBlocked(context: Context, appName: String) {
        if (!hasNotificationPermission(context)) return
        createChannels(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            2,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ALERTS)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle("Application gourmande bloquée")
            .setContentText("$appName a été restreinte en arrière-plan.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIF_ID_BG_BLOCKED, notification)
    }
}
