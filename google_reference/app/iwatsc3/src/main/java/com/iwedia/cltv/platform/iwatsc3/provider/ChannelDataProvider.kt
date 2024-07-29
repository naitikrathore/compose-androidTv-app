package com.iwedia.cltv.platform.rtk.provider

import android.annotation.SuppressLint
import android.content.*
import android.database.ContentObserver
import android.database.Cursor
import android.media.tv.TvContract
import android.media.tv.TvInputManager
import android.net.Uri
import android.os.Build
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresApi
import com.iwedia.cltv.platform.base.content_provider.TifChannelDataProvider
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.content_provider.ContentProvider
import com.iwedia.cltv.platform.model.content_provider.Contract
import com.iwedia.cltv.platform.model.information_bus.events.Events
import com.iwedia.cltv.platform.model.information_bus.events.InformationBus
import kotlin.collections.ArrayList


class ChannelDataProvider(context: Context) : TifChannelDataProvider(context) {

}