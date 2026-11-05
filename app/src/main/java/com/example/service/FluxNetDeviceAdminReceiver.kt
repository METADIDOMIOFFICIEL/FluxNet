package com.example.service

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

class FluxNetDeviceAdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Toast.makeText(
            context,
            "Protection FluxNet : désinstallation et arrêt forcé verrouillés !",
            Toast.LENGTH_LONG
        ).show()
    }

    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        return "Attention : désactiver l'administrateur retirera la protection contre la désinstallation et l'arrêt forcé de FluxNet."
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Toast.makeText(
            context,
            "Protection anti-désinstallation désactivée.",
            Toast.LENGTH_SHORT
        ).show()
    }
}
