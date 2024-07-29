package com.iwedia.cltv.sdk.handlers

import android.content.ActivityNotFoundException
import android.content.ContentResolver
import android.content.Context
import android.media.tv.TvContentRating
import android.media.tv.TvContract
import android.media.tv.TvInputInfo
import android.media.tv.TvInputManager
import android.os.Build
import android.os.Handler
import android.util.Log
import androidx.annotation.RequiresApi
import api.HandlerAPI
import com.iwedia.cltv.sdk.ReferenceSdk
import com.iwedia.cltv.sdk.entities.ReferenceTvChannel
import core_entities.Error
import listeners.AsyncDataReceiver
import listeners.AsyncReceiver
import java.lang.reflect.InvocationTargetException


/**
 * Tv input handler
 *
 * @author Dejan Nadj
 */
class ReferenceTvInputHandler : HandlerAPI {

    /**
     * TV input hash map
     */
    var inputMap: HashMap<String, TvInputInfo?>? = null

    /**
     * Application context
     */
    private var context: Context? = null

    /**
     * Tv input manager
     */
    private var tvInputManager: TvInputManager? = null

    /**
     * Tv input scan callback
     */
    var scanCallback: AsyncReceiver? = null

    /**
     * Constructor
     *
     * @param context application context
     */
    constructor(context: Context) {
        this.context = context
    }

    /**
     * Get tv input list
     *
     * @param receiver receiver
     */
    open fun getTvInputList(receiver: AsyncDataReceiver<MutableList<TvInputInfo>>) {
        val tvInputInfoList: MutableList<TvInputInfo> = mutableListOf()

        inputMap?.let {
            for (inputInfo in inputMap!!.values) {
                if (tvInputManager!!.getInputState(inputInfo!!.id) == TvInputManager.INPUT_STATE_CONNECTED) {
                    tvInputInfoList.add(inputInfo)
                }
            }
        }
        receiver.onReceive(tvInputInfoList)
    }

    /**
     * Returns tv input list without input that contains filter text as id value
     */
    open fun getTvInputFilteredList(filter: String, receiver: AsyncDataReceiver<MutableList<TvInputInfo>>) {
        val tvInputInfoList: MutableList<TvInputInfo> = mutableListOf()

        inputMap?.let {
            for (inputInfo in inputMap!!.values) {
                if (tvInputManager!!.getInputState(inputInfo!!.id) == TvInputManager.INPUT_STATE_CONNECTED) {
                    if (!inputInfo.id.contains(filter))
                        tvInputInfoList.add(inputInfo)
                }
            }
        }

        receiver.onReceive(tvInputInfoList)
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun isChannelRecordable(inputId: String): Boolean {
        for (inputInfo in inputMap!!.values) {
            if (tvInputManager!!.getInputState(inputInfo!!.id) == TvInputManager.INPUT_STATE_CONNECTED) {
                if (inputInfo.id == inputId) {
                    return inputInfo.canRecord()
                }
            }
        }
        return false
    }

    /**
     * Start setup activity for the given tv input
     *
     * @param input tv input for which to start setup activity
     */
    open fun startSetupActivity(input: TvInputInfo, receiver: AsyncReceiver) {
        val intent = input.createSetupIntent()
        if (intent == null) {
            receiver.onFailed(
                Error(
                    100,
                    "Can not create intent for input " + input.loadLabel(context!!)
                )
            )
            return
        }
        try {
            //Start setup activity for certain tv input
            scanCallback = receiver
            ReferenceSdk.sdkListener!!.startActivityForResult(
                intent,
                ReferenceSdk.TV_INPUT_SETUP_ACTIVITY_REQUEST_CODE
            )
        } catch (e: ActivityNotFoundException) {
            scanCallback = null
            receiver.onFailed(
                Error(
                    100,
                    "Can not start setup activity for input " + input.loadLabel(context!!)
                )
            )
            return
        }
    }

    /**
     * Trigger scan callback
     *
     * @param isSuccessful scan finished successfully
     */
    fun triggerScanCallback(isSuccessful: Boolean) {
        if (scanCallback != null) {
            if (isSuccessful) {
                scanCallback?.onSuccess()
            } else {
                scanCallback?.onFailed(
                    Error(
                        100,
                        "Tv input scan failed "
                    )
                )
            }
            scanCallback = null
        }
    }

    /**
     * Get channel count for the tv input
     *
     * @param input tv input
     * @param callback callback
     */
    fun getChannelCountForInput(input: TvInputInfo, callback: AsyncDataReceiver<Int>) {
        val contentResolver: ContentResolver = ReferenceSdk.context.contentResolver
        var cursor = contentResolver.query(
            TvContract.buildChannelsUriForInput(input.id),
            null,
            null,
            null
        )
        var count = cursor?.count ?: 0
        callback.onReceive(count)

    }

    /**
     * Function to check if parental is on or off
     */
    fun isParentalEnabled() : Boolean{
        return tvInputManager!!.isParentalControlsEnabled
    }

    @RequiresApi(Build.VERSION_CODES.P)
    fun getBlockedRatings(): List<TvContentRating> {
        return tvInputManager!!.blockedRatings
    }

    fun setParentalControlsEnabled(enabled: Boolean) {
        val mServiceRef = TvInputManager::class.members.single{
            it.name == "setParentalControlsEnabled"
        }
        val test = tvInputManager
        try {
            mServiceRef.call(test, enabled)
        } catch (e: Exception) {
            e.printStackTrace()
        } catch (invocationTargetException: InvocationTargetException) {
            invocationTargetException.cause?.printStackTrace()
        }

    }


    override fun setup() {
        tvInputManager = context!!.getSystemService(Context.TV_INPUT_SERVICE) as TvInputManager
        inputMap = HashMap()
        for (input in tvInputManager!!.tvInputList) {
            val inputId = input.id
            inputMap!![inputId] = input
        }
        tvInputManager!!.registerCallback(tvInputCallback, Handler())
    }

    override fun dispose() {
        tvInputManager!!.unregisterCallback(tvInputCallback)
    }

    /**
     * Tv input manager callback
     */
    private val tvInputCallback: TvInputManager.TvInputCallback =
        object : TvInputManager.TvInputCallback() {
            override fun onInputStateChanged(inputId: String, state: Int) {
                if (inputMap!!.containsKey(inputId)) {
                    inputMap!!.remove(inputId)
                    inputMap!![inputId] = tvInputManager!!.getTvInputInfo(inputId)
                }
            }

            override fun onInputAdded(inputId: String) {
                val info = tvInputManager!!.getTvInputInfo(inputId)
                if (info != null) {
                    inputMap!![inputId] = info
                }
            }

            override fun onInputRemoved(inputId: String) {
                if (inputMap!!.containsKey(inputId)) {
                    inputMap!!.remove(inputId)
                }
            }
        }
}