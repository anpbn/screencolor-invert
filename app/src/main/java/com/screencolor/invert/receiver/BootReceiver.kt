package com.screencolor.invert.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.screencolor.invert.utils.PreferenceManager

/**
 * Broadcast receiver for boot completed event
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(TAG, "Boot completed received")
            
            // Check if auto-start is enabled (can be added to preferences)
            val preferenceManager = PreferenceManager(context)
            
            // Currently no auto-start, but can be implemented
            // if (preferenceManager.isAutoStartEnabled()) {
            //     // Start service
            // }
        }
    }
}
