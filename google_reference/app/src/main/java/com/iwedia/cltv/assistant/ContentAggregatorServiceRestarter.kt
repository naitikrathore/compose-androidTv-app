package com.iwedia.cltv.assistant

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent


/**
 * Content aggregator service restarter
 */
class ContentAggregatorServiceRestarter : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        try {
            val serviceIntent = Intent(context, ContentAggregatorService::class.java)
            context.startService(serviceIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}