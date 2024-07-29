package com.iwedia.cltv.platform.refplus5

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.mediatek.dtv.tvinput.client.audiosignalinfo.AudioSignalInfo
import com.mediatek.dtv.tvinput.framework.tifextapi.common.audio.Constants

/**
 * Class used for getting the Audio track information
 */
object AudioInterfaceImpl {
    private const val TAG = "[CLTV] AudioInterfaceImpl"
    private var context: Context? = null
    private var audioSignalInfo: AudioSignalInfo? = null
    private var mInputSource: String = ""
    private var audioCallback: AudioSignalInfo.ISignalInfoListener? =
        null

    var mAudioTrackId = -1
    var mAudioFormatId = -1
    fun addCallback(context: Context, inputSource: String) {
        if (inputSource.isNotEmpty())
            if (inputSource != mInputSource) {
                mInputSource = inputSource
                this.context = context.applicationContext
                audioCallback = object :
                    AudioSignalInfo.ISignalInfoListener {
                    override fun onSignalInfoChanged(
                        sessionToken: String?,
                        changedSignalInfo: Bundle?
                    ) {
                        if (changedSignalInfo == null) {
                            return
                        }
                        if (changedSignalInfo.containsKey(Constants.KEY_AUDIO_REAR_CH_NUM)) {
                            mAudioTrackId = changedSignalInfo?.getInt(
                                Constants.KEY_AUDIO_FRONT_CH_NUM
                            )!!
                            Log.d(
                                TAG, "onSignalInfoChanged :: ${
                                    mAudioTrackId
                                } "
                            )

                        }
                        if (changedSignalInfo.containsKey(Constants.KEY_AUDIO_CODEC)) {
                            mAudioFormatId = changedSignalInfo?.getInt(
                                Constants.KEY_AUDIO_CODEC
                            )!!
                            Log.d(
                                TAG,
                                "onSignalInfoChanged :: mAudioFormatId :$mAudioFormatId : sessionToken : $sessionToken"
                            )
                        }
                    }
                }
                context.applicationContext?.let {
                    getAudioSignalInfo(
                        it,
                        inputSource
                    )
                        .addAudioSignalInfoListener("AudioClientToken", audioCallback)
                }
            }
    }


    private fun getAudioSignalInfo(context: Context, inputId: String): AudioSignalInfo {
        if (audioSignalInfo == null) {
            audioSignalInfo = AudioSignalInfo(context, inputId)
        }
        return audioSignalInfo!!
    }

    fun dispose() {
        context?.let {
            getAudioSignalInfo(it, mInputSource)
                .removeAudioSignalInfoListener(audioCallback)
        }
        mInputSource = ""
        mAudioFormatId = -1
        mAudioTrackId = -1
    }
}