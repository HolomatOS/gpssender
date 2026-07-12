package com.holomatos.gpssender

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val prefs = context.getSharedPreferences(
                LocationForegroundService.PREFS_NAME, Context.MODE_PRIVATE
            )
            val code = prefs.getString(LocationForegroundService.KEY_PAIRING_CODE, null)
            if (!code.isNullOrEmpty()) {
                val serviceIntent = Intent(context, LocationForegroundService::class.java)
                ContextCompat.startForegroundService(context, serviceIntent)
            }
        }
    }
}
