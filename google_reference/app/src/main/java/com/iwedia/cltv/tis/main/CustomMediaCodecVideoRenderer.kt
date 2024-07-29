package com.iwedia.cltv.tis.main

import android.content.Context
import android.os.Handler
import android.util.Log
import com.google.android.exoplayer2.mediacodec.MediaCodecAdapter
import com.google.android.exoplayer2.mediacodec.MediaCodecSelector
import com.google.android.exoplayer2.video.MediaCodecVideoRenderer
import com.google.android.exoplayer2.video.VideoRendererEventListener
import com.iwedia.cltv.platform.model.Constants

class CustomMediaCodecVideoRenderer(
    context: Context?,
    codecAdapterFactory: MediaCodecAdapter.Factory?,
    mediaCodecSelector: MediaCodecSelector?,
    allowedJoiningTimeMs: Long,
    enableDecoderFallback: Boolean,
    eventHandler: Handler?,
    eventListener: VideoRendererEventListener?,
    maxDroppedFramesToNotify: Int
) : MediaCodecVideoRenderer(
    context!!,
    codecAdapterFactory!!,
    mediaCodecSelector!!,
    allowedJoiningTimeMs,
    enableDecoderFallback,
    eventHandler,
    eventListener,
    maxDroppedFramesToNotify
) {
    override fun onCodecReleased(name: String) {
        if (name.contains("secure")) {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "Sleep after secure codec release")
            try {
                Thread.sleep(500)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }
        super.onCodecReleased(name)
    }

    companion object {
        private const val TAG = "CustomMediaCodecVideoRenderer"
    }
}