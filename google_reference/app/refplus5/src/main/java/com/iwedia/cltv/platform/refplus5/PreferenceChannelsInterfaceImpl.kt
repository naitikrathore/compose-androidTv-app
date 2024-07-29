package com.iwedia.cltv.platform.refplus5

import android.content.ContentValues
import android.content.Context
import android.media.tv.TvContract
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal.COUNTRY
import android.util.Log
import androidx.annotation.RequiresApi
import com.iwedia.cltv.platform.`interface`.PreferenceChannelsInterface
import com.iwedia.cltv.platform.`interface`.TvInterface
import com.iwedia.cltv.platform.model.CoroutineHelper
import com.iwedia.cltv.platform.model.IAsyncCallback
import com.iwedia.cltv.platform.model.TvChannel
import com.mediatek.dtv.tvinput.client.cam.caminfo.CamMonitoringService
import com.mediatek.dtv.tvinput.client.scan.Constants
import com.mediatek.dtv.tvinput.client.scan.TvScan
import com.mediatek.dtv.tvinput.dvbtuner.scan.ScanConstants
import com.mediatek.dtv.tvinput.dvbtuner.scan.ScanConstants.OPERATOR_GENERAL_SATELLITE
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.concurrent.thread

class PreferenceChannelsInterfaceImpl(
    private var context: Context,
    private val tvModule: TvInterface,
    private val utilsInterfaceImpl: UtilsInterfaceImpl
) : PreferenceChannelsInterface {

    private val TAG = javaClass.simpleName
    private var mChannelList: ArrayList<TvChannel> = tvModule.getChannelList()

    val TUNER_MODE_PREFER_SAT = "TUNER_MODE_PREFER_SAT"
    val BS_SAT_GENERAL = 0
    val BS_SAT_PREFER = 1
    val BS_SRC_AIR = 0
    val BS_SRC_CABLE = 1
    val BS_CH_LST_TYPE_BROADCAST = 0
    val CFG_MISC_CH_LST_TYPE = "CFG_MISC_CH_LST_TYPE"

    val CFG_GRP_BS_PREFIX = "g_bs__"
    val CFG_BS_BS_SAT_BRDCSTER = CFG_GRP_BS_PREFIX + "bs_sat_brdcster"
    private var updateChannels: MutableList<TvChannel> = arrayListOf()

    @RequiresApi(Build.VERSION_CODES.S)
    override fun swapChannel(
        firstChannel: TvChannel,
        secondChannel: TvChannel,
        previousPosition: Int,
        newPosition: Int
    ): Boolean {
        Log.d(TAG, "swapChannel")
        try {
            var channelIndex = utilsInterfaceImpl.getPrefsValue("CurrentActiveChannelId", 0) as Int
            if(channelIndex.toInt() == -1){
                channelIndex = 0
            }
            var channelMap = (tvModule as TvInterfaceImpl).getAllChannelListForDb()
            if(channelMap == null || channelMap.size == 0){
                return false
            }
            var savedChannel = channelMap?.get(channelIndex.toLong())
            if (channelMap != null && channelMap.size != 0){
                run exitForEach@{
                    channelMap.forEach {
                        if ((it.value.onId == savedChannel?.onId) && (it.value.tsId == savedChannel?.tsId) && (it.value.serviceId == savedChannel?.serviceId)) {
                            savedChannel = it.value
                            return@exitForEach
                        }
                    }
                }
            }

            val firstSwap = channelMap!!.get(firstChannel.channelId)
            val secondSwap = channelMap!!.get(secondChannel.channelId)

            var tempDN = firstSwap!!.displayNumber
            firstSwap.displayNumber = secondSwap!!.displayNumber
            secondSwap.displayNumber = tempDN

            val swappedChannels = mutableListOf(secondSwap, firstSwap)

            var activeIsSwapped = false
            run exitForEach@{
                swappedChannels.forEach {
                    if ((it.onId == savedChannel?.onId) && (it.tsId == savedChannel?.tsId) && (it.serviceId == savedChannel?.serviceId)) {
                        activeIsSwapped = true
                        return@exitForEach
                    }
                }
            }
            (tvModule as TvInterfaceImpl).updateChannels(swappedChannels)
            if(savedChannel != null) {
                reZapAfterSwap(true, savedChannel!!)
            }
        }catch (E: Exception){
            E.printStackTrace()
        }
        return false
    }


    @RequiresApi(Build.VERSION_CODES.S)
    override fun moveChannel(
        moveChannelList: ArrayList<TvChannel>,
        previousIndex: Int,
        newIndex: Int,
        channelMap: HashMap<Int, String>
    ): Boolean {
        Log.d(TAG, "moveChannel")
        try {
            var channelIndex = utilsInterfaceImpl.getPrefsValue("CurrentActiveChannelId", 0) as Int
            if(channelIndex.toInt() == -1){
                channelIndex = 0
            }
            var channelMap = (tvModule as TvInterfaceImpl).getAllChannelListForDb()
            var savedChannel = channelMap?.get(channelIndex.toLong())
            if (channelMap != null && channelMap.size != 0){
                run exitForEach@{
                    channelMap.forEach {
                        if ((it.value.onId == savedChannel?.onId) && (it.value.tsId == savedChannel?.tsId) && (it.value.serviceId == savedChannel?.serviceId)) {
                            savedChannel = it.value
                            return@exitForEach
                        }
                    }
                }
            }
            var activeIsSwapped = false
            run exitForEach@{
                moveChannelList.forEach {
                    if ((it.onId == savedChannel?.onId) && (it.tsId == savedChannel?.tsId) && (it.serviceId == savedChannel?.serviceId)) {
                        activeIsSwapped = true
                        return@exitForEach
                    }
                }
            }
            setChannelMove(moveChannelList, getChannelList()[newIndex])
            if(savedChannel != null) {
                reZapAfterSwap(true, savedChannel!!)
            }
        }catch (E: Exception){
            E.printStackTrace()
        }
        return true
    }

    private fun reZapAfterSwap(activeIsSwapped: Boolean, savedChannel: TvChannel){
        var newSavedChannel = savedChannel
        var savedIndex = 0
        if(activeIsSwapped){
            try {
                var channelMap = tvModule.getChannelList()
                run exitForEach@{
                    channelMap?.forEach {
                        if ((it.onId == newSavedChannel.onId) && (it.tsId == newSavedChannel.tsId) && (it.serviceId == newSavedChannel.serviceId)) {
                            newSavedChannel = it
                            return@exitForEach
                        }
                        savedIndex++
                    }
                }
                tvModule.changeChannel(tvModule.getChannelList().indexOf(newSavedChannel),object:
                    IAsyncCallback {
                    override fun onFailed(error: Error) {
                        Log.d(TAG, "error.message ${error.message}")
                    }
                    override fun onSuccess() {

                    }
                })
            }catch (E: Exception){
                E.printStackTrace()
            }
        }
    }

    private fun getChannelList(): ArrayList<TvChannel>{
        var channelMap = (tvModule as TvInterfaceImpl).getAllChannelListForDb()
        var channelList: ArrayList<TvChannel> = arrayListOf()
        channelMap?.forEach {
            channelList.add(it.value)
        }
        return channelList
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun setChannelMove(channelMoveInfos: MutableList<TvChannel>, channelMovetag: TvChannel){
        channelMoveInfos.sortBy { it.displayNumber.toInt() }
        //all of this bellow is mtk code name changed of function per request
        val channelMoveNums: MutableList<Long> = arrayListOf()
        for (info in channelMoveInfos) {
            channelMoveNums.add(info.id.toLong())
        }
        updateChannels = arrayListOf()
        updateChannels.addAll(mChannelList)
        if (channelMoveInfos[channelMoveInfos.size - 1].displayNumber < channelMovetag.displayNumber) { // case 1:
            setBellowTheList(channelMoveNums, channelMovetag)
        } else if (channelMoveInfos[0].displayNumber > channelMovetag.displayNumber) { // case 3:
            setAboveTheList(channelMoveNums, channelMovetag)
        } else if (channelMoveNums.contains(channelMovetag.id.toLong())) { // case 4:
            setInTheMiddleOnOneValue(channelMoveNums, channelMovetag)
        } else if (!channelMoveNums.contains(channelMovetag.id.toLong())) { // case 2: case 5:
            setInTheMiddleNotOneValue(channelMoveNums, channelMovetag)
        }
        (tvModule as TvInterfaceImpl).updateChannels(updateChannels)
        updateChannels.sortBy { it.displayNumber.toInt() }
        Log.d(TAG, "end sort mIds.")
    }
    private fun setBellowTheList(
        channelMoveNums: List<Long>,
        channelMovetag: TvChannel
    ){
        var j = -1
        for (i in updateChannels.indices) {
            if (updateChannels[i].id != channelMovetag.id) {
                if (j < channelMoveNums.size &&
                    channelMoveNums.contains(updateChannels[i].id.toLong()) &&
                    updateChannels[i].id != channelMovetag.id
                ) {
                    j++
                    Log.d(TAG, "case 1 j:$j")
                } else if (j > -1) {
                    Log.d(TAG, "case 1 first not i:$i")
                    var k = i - j - 1
                    for (l in 0..j) { // j starts at -1
                        Log.d(TAG, "case 1 k:$k")
                        var info = TvChannel()
                        info = updateChannels[k].copy()
                        updateChannels[k] = updateChannels[i].copy()
                        updateChannels[i] = info.copy()
                        k++
                    }
                }
            }
        }
    }

    private fun setAboveTheList(
        channelMoveNums: List<Long>,
        channelMovetag: TvChannel
    ){
        var j = -1
        var i = updateChannels.size - 1
        while (i >= 0 && Integer.parseInt(updateChannels[i].displayNumber) >= Integer.parseInt(channelMovetag.displayNumber)) {
            if (j < channelMoveNums.size &&
                channelMoveNums.contains(updateChannels[i].id.toLong()) &&
                updateChannels[i].id != channelMovetag.id) {
                j++
                Log.d(TAG, "case 3 j:$j")
            } else if (j > -1) {
                Log.d(TAG, "case 3 first not i:$i")
                var k = i + 1
                for (l in 0..j) {
                    Log.d(TAG, "case 3 k:$k")
                    var info = TvChannel()
                    info = updateChannels[k].copy()
                    updateChannels[k] = updateChannels[i + l].copy()
                    updateChannels[i + l] = info.copy()
                    k++
                    // MtkLog.d(TAG, "second down mIds:"+updateChannels.subList(0,i+4).toString())
                }
            }
            i--
        }
    }

    private fun setInTheMiddleOnOneValue(
        channelMoveNums: List<Long>,
        channelMovetag: TvChannel
    ){
        var j = -1
        var i = 0
        while (i < updateChannels.size && updateChannels[i].id != channelMovetag.id) {
            if (j < channelMoveNums.size &&
                channelMoveNums.contains(updateChannels[i].id.toLong()) &&
                updateChannels[i].id != channelMovetag.id) {
                j++
                Log.d(TAG, "case 4 j:$j")
            } else if (j > -1) {
                Log.d(TAG, "case 4 first not i:$i")
                var k = i - j - 1
                for (l in 0..j) {
                    Log.d(TAG, "case 4 k:$k")
                    var info = TvChannel()
                    info = updateChannels[k].copy()
                    updateChannels[k] = updateChannels[i].copy()
                    updateChannels[i] = info.copy()
                    k++
                }
            }
            i++
        }

        j = -1
        i = updateChannels.size - 1
        while (i >= 0 && Integer.parseInt(updateChannels[i].displayNumber) > Integer.parseInt(channelMovetag.displayNumber)) {
            if (j < channelMoveNums.size &&
                channelMoveNums.contains(updateChannels[i].id.toLong()) &&
                updateChannels[i].id != channelMovetag.id) {
                j++
                Log.d(TAG, "case 4 j:$j")
            } else if (j > -1) {
                Log.d(TAG, "case 4 first not i:$i")
                var k = i + 1
                for (l in 0..j) {
                    Log.d(TAG, "case 4 k:$k")
                    var info = TvChannel()
                    info = updateChannels[k].copy()
                    updateChannels[k] = updateChannels[i + l].copy()
                    updateChannels[i + l] = info.copy()
                    k++
                }
            }
            i--
        }
    }

    private fun setInTheMiddleNotOneValue(
        channelMoveNums: List<Long>,
        channelMovetag: TvChannel
    ){
        var j = -1
        var i = 0
        while (i < updateChannels.size && updateChannels[i].id != channelMovetag.id) {
            if (j < channelMoveNums.size &&
                channelMoveNums.contains(updateChannels[i].id.toLong()) &&
                updateChannels[i].id != channelMovetag.id) {
                j++
                Log.d(TAG, "case 25 j:$j")
            } else if (j > -1) {
                Log.d(TAG, "case 25 first not i:$i")
                var k = i - j - 1
                for (l in 0..j) {
                    Log.d(TAG, "case 25 k:$k")
                    var info = TvChannel()
                    info = updateChannels[k].copy()
                    updateChannels[k] = updateChannels[i].copy()
                    updateChannels[i] = info.copy()
                    k++
                }
            }
            i++
        }

        j = -1
        i = updateChannels.size - 1
        while (i >= 0 && Integer.parseInt(updateChannels[i].displayNumber) >= Integer.parseInt(channelMovetag.displayNumber)) {
            if (j < channelMoveNums.size &&
                channelMoveNums.contains(updateChannels[i].id.toLong()) &&
                updateChannels[i].id != channelMovetag.id) {
                j++
                Log.d(TAG, "case 25 j:$j")
            } else if (j > -1) {
                Log.d(TAG, "case 25 first not i:$i")
                var k = i + 1
                for (l in 0..j) {
                    Log.d(TAG, "case 25 k:$k")
                    var info = TvChannel()
                    info = updateChannels[k].copy()
                    updateChannels[k] = updateChannels[i + l].copy()
                    updateChannels[i + l] = info.copy()
                    k++
                }
            }
            i--
        }
    }

    override fun deleteAllChannels() {
        //Delete only broadcast channels, cuz option is selected from broadcast tab
        tvModule.getBrowsableChannelList().forEach { tvChannel ->
            tvModule.deleteChannel(tvChannel)
        }

        val broadcastType = getChannelBroadcastType(context)
        thread {
            clearChannelList(context,broadcastType)
        }
    }


    private fun setChannelBroadcastType(value: Int, context: Context) {
        if (value > 1 || value < 0) {
            Log.i("TAG", "setChannelBroadcastType ERROR!!! $value")
            return
        }
        utilsInterfaceImpl.saveMtkInternalGlobalValue(context, CFG_MISC_CH_LST_TYPE, value.toString(),false)
    }


    private fun getChannelBroadcastType(context: Context): Int {
        return readMtkInternalGlobalIntValue(context, CFG_MISC_CH_LST_TYPE,0);
    }

    private fun getCamScan(context: Context, tunerType: Int): TvScan {
        return when (tunerType) {
            BS_SRC_AIR -> TvScan(context, Constants.TYPE_DVB_T)
            BS_SRC_CABLE -> TvScan(context, Constants.TYPE_DVB_C)
            else -> TvScan(context, Constants.TYPE_DVB_S)
        }
    }

    private fun clearChannelList(context: Context, broadcastType: Int) {
        try {
            Log.i(TAG, "clearChannelList start.")
            val tunerMode: Int = readMtkInternalGlobalIntValue(
                context,
                com.iwedia.cltv.platform.refplus5.screenMode.Constants.CFG_BS_BS_SRC,
                0
            )
            val mTvScanDVB = getCamScan(context, tunerMode)
            val bundle = Bundle()
            bundle.putInt(ScanConstants.KEY_BROADCAST_TYPE, getBroadcastType(tunerMode))
            bundle.putInt(ScanConstants.KEY_OPERATOR_ID, getOperator(context, tunerMode))
            if (broadcastType != BS_CH_LST_TYPE_BROADCAST) {
                setChannelBroadcastType(broadcastType, context)
                bundle.putInt(
                    com.mediatek.dtv.tvinput.framework.tisapi.dvb.scan.Constants.KEY_SLOT_NUMBER,
                    CamMonitoringService(context, CiPlusInterfaceImpl.inputid).getSlotIds()[0]
                )
            }
            bundle.putString(ScanConstants.KEY_COUNTRY_CODE, getCountry(context))
            Log.i(TAG, "clearChannelList setScanListener.")
            mTvScanDVB.clearChannelList(bundle)
            mTvScanDVB.storeChannelList()
            CoroutineScope(Dispatchers.IO).launch {
                mTvScanDVB.releaseScanSession()
            }

        } catch (e: Exception) {
            Log.i(TAG, "clearChannelList error: /n${e.printStackTrace()}")
        }
    }

    private fun getCountry(context: Context): String {
        val country = SaveValue.readWorldStringValue(context, COUNTRY)
        Log.i(TAG, "getCountry $country")
        return country ?: "zho"
    }


    private fun getBroadcastType(tunerType: Int) =
        when (tunerType) {
            BS_SRC_AIR -> ScanConstants.BROADCAST_TYPE_DVB_T
            BS_SRC_CABLE -> ScanConstants.BROADCAST_TYPE_DVB_C
            else -> ScanConstants.BROADCAST_TYPE_DVB_S
        }


    private fun getOperator(context: Context, tunerType: Int) =
        when (tunerType) {
            BS_SRC_AIR -> ScanConstants.OPERATOR_DVBT_OTHERS
            BS_SRC_CABLE -> readMtkInternalGlobalIntValue(context, "CABLE_BRDCSTER", ScanConstants.OPERATOR_DVBC_OTHERS)

            else -> getSatelliteOperator()
        }


    private fun getSatelliteOperator(): Int {
        if (readMtkInternalGlobalIntValue(
                context!!,
                TUNER_MODE_PREFER_SAT,
                BS_SAT_GENERAL
            ) == BS_SAT_PREFER
        ) {
            return readMtkInternalGlobalIntValue(
                context!!,
                CFG_BS_BS_SAT_BRDCSTER,
                OPERATOR_GENERAL_SATELLITE
            )
        } else {
            return OPERATOR_GENERAL_SATELLITE
        }
    }

    private fun readMtkInternalGlobalIntValue(context: Context, name: String, default: Int): Int {
        try {
            var value = utilsInterfaceImpl.readMtkInternalGlobalValue(context, name)
            return Integer.parseInt(value)
        } catch (e: Exception) {
            Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG, "readMtkInternalGlobalIntValue: Int conversion failed ${e.message}")
        }
        return default
    }
}