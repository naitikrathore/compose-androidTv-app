package com.iwedia.cltv.scene.custom_recording


import com.iwedia.cltv.platform.`interface`.TTSSetterInterface
import com.iwedia.cltv.platform.`interface`.ToastInterface
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.TvChannel
import world.SceneListener

interface CustomRecordingSceneListener: SceneListener, TTSSetterInterface,
     ToastInterface {
     fun refresh(position :Int)
     fun scheduleCustomRecording(tvChannel: TvChannel, startTime: Long?, endTime:Long?, repeat:Int)
     fun getChannelList(): ArrayList<TvChannel>
     fun getActiveChannel(callback: IAsyncDataCallback<TvChannel>)
     fun getCurrentTime(tvChannel: TvChannel): Long
}