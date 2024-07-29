package com.iwedia.cltv.platform.mk5.mtkChannelInfo

import android.content.Context
import android.database.Cursor
import android.media.tv.TvContract
import android.media.tv.TvView
import android.net.Uri
import android.util.Log
import androidx.annotation.WorkerThread
import com.iwedia.cltv.platform.`interface`.UtilsInterface
import com.iwedia.cltv.platform.model.Constants
import com.mediatek.twoworlds.tv.MtkTvChannelList
import com.mediatek.twoworlds.tv.MtkTvConfig
import com.mediatek.twoworlds.tv.common.MtkTvChCommonBase
import com.mediatek.twoworlds.tv.common.MtkTvConfigType
import com.mediatek.twoworlds.tv.common.MtkTvConfigTypeBase
import com.mediatek.twoworlds.tv.model.MtkTvChannelInfo
import com.mediatek.twoworlds.tv.model.MtkTvChannelInfoBase
import com.mediatek.twoworlds.tv.model.MtkTvDvbChannelInfo
import com.mediatek.twoworlds.tv.model.MtkTvISDBChannelInfo
import com.mediatek.wwtv.tvcenter.util.MarketRegionInfo
import java.util.Collections
import java.util.Locale

open class MtkTIFChannelManager {
    private var mtkTvChList: MtkTvChannelList?
    val TAG = "MtkTIFChannelManager"

    private val mGetApiChannelFromSvlRecd: Boolean = true

    private var mtkTifChannelInfoList: MutableList<MtkTIFChannelInfo> = Collections.synchronizedList(arrayListOf<MtkTIFChannelInfo>())
    private var maps: HashMap<Int, MtkTvChannelInfoBase> = HashMap()
    private var getAllChannelsrunning = false
    private var mCurCategories = -1
    @Volatile
    private var activeChannel: MtkTIFChannelInfo? = null
    @Volatile
    private var activeChannelIndex: Int? = 0
    private var channelTwo : MtkTIFChannelInfo?=null

    companion object {
        private const val SELECTION = ""
        private val SELECTION_WITH_SVLID: String =
            SELECTION + "substr(cast(" +
                    TvContract.Channels.COLUMN_INTERNAL_PROVIDER_DATA + " as varchar),7,5) = ?"
        private val ORDERBY = "substr(cast(" +
                TvContract.Channels.COLUMN_INTERNAL_PROVIDER_DATA + " as varchar),19,10)"
        private val SELECTION_WITH_SVLID_INPUTID: String =
            (SELECTION_WITH_SVLID
                    + " and substr(" + TvContract.Channels.COLUMN_INPUT_ID
                    + " ,length(" + TvContract.Channels.COLUMN_INPUT_ID + ")-2,3) = ?")
        val CH_LIST_MASK = MtkTvChCommonBase.SB_VNET_ACTIVE or MtkTvChCommonBase.SB_VNET_FAKE
        val CH_LIST_VAL = MtkTvChCommonBase.SB_VNET_ACTIVE
        const val CH_LIST_3RDCAHNNEL_MASK = -1
        const val CH_LIST_3RDCAHNNEL_VAL = -1
        private const val value = 0
        private const val value1 = 0
        private const val flag = 0
        private const val flag1 = 0
        private const val COLORKEY = 0x40000
        const val CATEGORIES_CHANNELNUM_BASE = 2000

        @Volatile
        private var instance: MtkTIFChannelManager? = null
        fun getInstance(): MtkTIFChannelManager {
            instance ?: synchronized(this) {
                instance ?: MtkTIFChannelManager().also { instance = it }
            }
            return instance!!
        }
    }

    fun deleteInstance(){
        instance = null
    }

    private val CURRENT_CHANNEL_SORT = 0

    init {
        mtkTvChList = MtkTvChannelList.getInstance()
    }

    @WorkerThread
    private fun getAllChannels(): Map<Int, MtkTvChannelInfoBase> {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "getAllChannels:")
        getAllChannelsrunning = true
        val mtkInfoList: List<MtkTvChannelInfoBase> = getChannelListForMap()
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "getAllChannels mtkInfoList.size: " + (mtkInfoList.size))
        val mapstemp: MutableMap<Int, MtkTvChannelInfoBase> = HashMap()
        if (mtkInfoList != null && !mtkInfoList.isEmpty()) {
            for (info in mtkInfoList) {
                //       Log.d(Constants.LogTag.CLTV_TAG + TAG, "getAllChannels info " +info.getServiceName()+" info.getChannelId() "+info.getChannelId());
                mapstemp[info.channelId] = info
            }
        }
        maps.clear()
        maps.putAll(mapstemp)
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "getAllChannels maps.size: " + maps.size)


        return maps
    }

    //@WorkerThread
    fun getAllQueriedChannels(
        context: Context,
        utilsModule: UtilsInterface
    ): MutableList<MtkTIFChannelInfo> {
        mtkTifChannelInfoList.clear()
        activeChannel = null
        channelTwo = null
        activeChannelIndex = 0
        val svlId = getSvlId()
        val contentUri = TvContract.Channels.CONTENT_URI
        val c = context.contentResolver.query(
            contentUri, null, null, null, null
        )
        try {
            Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG, "svlId *********" + svlId)
            while (c!!.moveToNext()) {
                val mtkTIFChannelInfo = MtkTIFChannelInfo().parse(c)
                if (mtkTIFChannelInfo.mDisplayNumber.equals("2") && !mtkTIFChannelInfo.mIsBrowsable) {
                    channelTwo = mtkTIFChannelInfo
                }
                if (mtkTIFChannelInfo.mIsBrowsable && mtkTIFChannelInfo.mInternalProviderFlag1 == svlId) {
                    Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG, "mDisplayNumber *********" + mtkTIFChannelInfo.mDisplayNumber)
                    mtkTifChannelInfoList.add(mtkTIFChannelInfo)
                }
            }
            val activeChannelNum = utilsModule.getPrefsValue("factory_active_channel", "") as String

            if (activeChannelNum.isNotEmpty()) {
                val list = activeChannelNum.split("_")
                if (list.isNotEmpty()) {
                    val channelId = list[1]
                    mtkTifChannelInfoList.forEachIndexed { index, mtkTIFChannelInfo ->
                        Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG, "moveToNext *********" + mtkTIFChannelInfo.mDisplayNumber)
                        if (list[0] == mtkTIFChannelInfo.mInputServiceName && channelId.toLong() == mtkTIFChannelInfo.mId) {
                            Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +
                                TAG,
                                "display num " + mtkTIFChannelInfo.mDisplayNumber + "mId " + mtkTIFChannelInfo.mId + " index " + index
                            )
                            activeChannel = mtkTIFChannelInfo
                            activeChannelIndex = index
                        }
                    }
                }
            } else {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "No active channel in prefs")
            }

            if (activeChannel == null) {
                if (mtkTifChannelInfoList.size > 0) {
                    activeChannel = mtkTifChannelInfoList[0]
                    addLastTunedChannelToPrefs(activeChannel!!, utilsModule)
                    Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG, "first channel is active channel " + activeChannel?.mDisplayName)
                    activeChannelIndex = 0
                } else {
                    //Do direct tune to channel 2
                    if (channelTwo != null) {
                        activeChannel = channelTwo
                        addLastTunedChannelToPrefs(channelTwo!!, utilsModule)
                        Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG, "No channels available so perform direct tune")
                    } else {
                        Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG, "channel 2 not available for direct tune")
                    }
                }
            }
        } catch (e: Exception) {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "Exception occured  ********* $e")
        } finally {
            c?.close()
        }

        return mtkTifChannelInfoList
    }


    private fun getChannelListForMap(): List<MtkTvChannelInfoBase> {
        val length: Int = mtkTvChList!!.getChannelCountByFilter(
            MtkUtils().getSvlId(),
            MtkTvChCommonBase.SB_VNET_ALL
        )
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "getChannelList length $length")
        return getChannelList(0, 0, length, MtkTvChCommonBase.SB_VNET_ALL)
    }

    fun getTifChannelInfoByUri(channelUri: Uri?, context: Context, liveTvView: TvView?,utilsModule: UtilsInterface): String {
        Log.d(Constants.LogTag.CLTV_TAG +
            TAG, "start getTifChannelInfoByUri  channelUri>>>$channelUri"
        )
        if (channelUri == null) {
            return ""
        }
        val c: Cursor = context.contentResolver.query(
            channelUri, null, null,
            null, ORDERBY
        ) ?: return ""
        var temTIFChannel: MtkTIFChannelInfo? = null
        while (c.moveToNext()) {
            temTIFChannel = MtkTIFChannelInfo().parse(c)
            Log.d(Constants.LogTag.CLTV_TAG +
                TAG, " getTifChannelInfoByUri temTIFChannel >>>$temTIFChannel"
            )
            temTIFChannel.mMtkTvChannelInfo = getAPIChannelInfoByBlobData(temTIFChannel.mDataValue)
        }
        c.close()
        Log.d(Constants.LogTag.CLTV_TAG +
            TAG, "end getTifChannelInfoByUri  >>>$temTIFChannel"
        )
        activeChannel = temTIFChannel
        tuneToChannel(
            liveTvView,
            temTIFChannel!!.mInputServiceName!!,
            TvContract.buildChannelUri(temTIFChannel.mId)
        )
        addLastTunedChannelToPrefs(activeChannel!!,utilsModule)
        //TODO update channel index in bg thread
        mtkTifChannelInfoList.forEachIndexed { index, mtkTIFChannelInfo ->
            if (temTIFChannel.mDisplayNumber == mtkTIFChannelInfo.mDisplayNumber) {
                activeChannelIndex = index
            }

        }
        return temTIFChannel.mDisplayNumber!!
    }


    @Synchronized
    private fun getAPIChannelInfoByBlobData(mDataValue: LongArray?): MtkTvChannelInfoBase? {
        return if (mDataValue != null && mDataValue.size > 3) {
            if (mGetApiChannelFromSvlRecd) {
                try {
                    mtkTvChList!!
                        .getChannelInfoBySvlRecId(mDataValue[0].toInt(), mDataValue[1].toInt())
                } catch (e: Exception) {
                    Log.d(Constants.LogTag.CLTV_TAG +
                        TAG,
                        "getChannelById " + e.message
                    )
                    null
                }
            } else {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "getChannelById chID = " + mDataValue[2])
                var chInfo: MtkTvChannelInfoBase? = null
                chInfo = getChannelById(mDataValue[2].toInt())
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "getChannelById chInfo = $chInfo")
                chInfo
            }
        } else null
    }

    private fun getChannelById(chId: Int): MtkTvChannelInfoBase? {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "getChannelById chID = $chId")
        val chList: List<MtkTvChannelInfoBase> =
            getChannelList(chId, 0, 1, MtkTvChCommonBase.SB_VNET_ALL)
        var chInfo: MtkTvChannelInfoBase? = null
        if (chList != null && !chList.isEmpty()) {
            if (chId == chList[0].channelId) {
                chInfo = chList[0]
            }
        }
        if (chInfo != null) {
            Log.d(Constants.LogTag.CLTV_TAG +
                TAG, "getChannelById chId = " + chId + "chInfo.getchId" + chInfo.channelId
            )
        } else {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "getChannelList chId = " + chId + "chInfo == null")
        }
        return chInfo
    }

    open fun getChannelByNumOrName(
        chNumOrName: String?,
        isName: Boolean,
        liveTvView: TvView?,
        utilsModule: UtilsInterface
    ): Int {
        Log.d(Constants.LogTag.CLTV_TAG +
            TAG,
            "getChanelByNumOrName() " + chNumOrName + " list size " + mtkTifChannelInfoList.size
        )
        mtkTifChannelInfoList.forEachIndexed { index, chinfo ->
            if (isName) {
                if (chinfo.mDisplayName.equals(chNumOrName)) {
                    chinfo.mMtkTvChannelInfo = getAPIChannelInfoByBlobData(chinfo.mDataValue)
                    activeChannel = chinfo
                    activeChannelIndex = index
                    tuneToChannel(
                        liveTvView,
                        chinfo.mInputServiceName!!,
                        TvContract.buildChannelUri(chinfo.mId)
                    )
                    addLastTunedChannelToPrefs(activeChannel!!, utilsModule)
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "getChanelByNumOrName() isName chinfo is $chinfo")
                    return 0
                }
            } else {
                Log.d(Constants.LogTag.CLTV_TAG +
                    TAG,
                    "chinfo.mDisplayNumber() " + chinfo.mDisplayNumber + " chNumOrName " + chNumOrName
                )
                if (chinfo.mDisplayNumber.equals(chNumOrName)) {
                    chinfo.mMtkTvChannelInfo = getAPIChannelInfoByBlobData(chinfo.mDataValue)
                    activeChannelIndex = index
                    activeChannel = chinfo
                    tuneToChannel(
                        liveTvView,
                        chinfo.mInputServiceName!!,
                        TvContract.buildChannelUri(chinfo.mId)
                    )
                    addLastTunedChannelToPrefs(activeChannel!!, utilsModule)
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "getChanelByNumOrName() not isName chinfo is $chinfo")
                    return 0
                }
            }
            Log.d(Constants.LogTag.CLTV_TAG +
                TAG,
                "chinfo.mInputServiceName is " + chinfo.mInputServiceName + " chinfo.mId" + TvContract.buildChannelUri(
                    chinfo.mId
                )
            )
        }
        return 1
    }

    private fun tuneToChannel(liveTvView: TvView?, inputId: String, channelUri: Uri) {
        liveTvView!!.tune(inputId, channelUri)
    }

    fun tuneToActiveChannel(liveTvView: TvView?): String {
        Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG, "[tuneToActiveChannel] Active channel is ${activeChannel?.mDisplayNumber}")
        if (activeChannel != null) {
            tuneToChannel(
                liveTvView,
                activeChannel!!.mInputServiceName!!,
                TvContract.buildChannelUri(activeChannel!!.mId)
            )
            return activeChannel!!.mDisplayNumber!!
        }
        return ""
    }

    fun getDisplayName() : String {
        if (activeChannel != null) {
            return activeChannel?.mDisplayName!!
        }
        return ""
    }

    fun changeUpOrDown(isUp: Boolean, liveTvView: TvView?, utilsModule: UtilsInterface) :String {
        //This case occurs in channel preset when there are 0 channels in some cases
        if (mtkTifChannelInfoList.size > 0) {
            if (isUp) {
                if (activeChannelIndex!! < mtkTifChannelInfoList.size - 1) {
                    activeChannelIndex = activeChannelIndex!! + 1
                } else {
                    activeChannelIndex = 0
                }

            } else {
                if (activeChannelIndex == 0) {
                    val size = mtkTifChannelInfoList.size
                    activeChannelIndex = size - 1
                } else {
                    if (activeChannelIndex!! > 0) {
                        activeChannelIndex = activeChannelIndex!! - 1
                    } else {
                        Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG, "negative index" + activeChannelIndex)
                    }

                }
            }
            //Case - If initially there are no channels in DB
            if (activeChannel == null) {
                return ""
            }
            activeChannel = mtkTifChannelInfoList[activeChannelIndex!!]
            tuneToChannel(
                liveTvView,
                activeChannel!!.mInputServiceName!!,
                TvContract.buildChannelUri(activeChannel!!.mId)
            )
            addLastTunedChannelToPrefs(activeChannel!!, utilsModule)
            return activeChannel!!.mDisplayNumber!!
        } else {
            Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG, "No active channels found")
            return ""
        }

    }

    private fun addLastTunedChannelToPrefs(
        activeChannel: MtkTIFChannelInfo,
        utilsModule: UtilsInterface
    ) {
        var channelServiceNameAndId = activeChannel.mInputServiceName + "_" + activeChannel.mId
        utilsModule.setPrefsValue(
            "factory_active_channel",
            channelServiceNameAndId
        )
    }

    fun getChannelList(
        chId: Int,
        prevCount: Int,
        nextCount: Int,
        filter: Int
    ): List<MtkTvChannelInfoBase> {
        Log.d(Constants.LogTag.CLTV_TAG +
            TAG,
            "getChannelList chId =" + chId + "pre =" + prevCount + "nextCOunt =" + nextCount + "filter =" + filter
        )
        val chList: List<MtkTvChannelInfoBase> =
            mtkTvChList!!.getChannelListByFilter(
                MtkTvConfig.getInstance().getConfigValue(MtkTvConfigTypeBase.CFG_BS_SVL_ID),
                filter, chId, prevCount, nextCount
            )
        if (chList != null && !chList.isEmpty()) {
            Log.d(Constants.LogTag.CLTV_TAG +
                TAG, "getChannelList chId = " + chId + "chList.get(0).getchId" + chList[0].channelId
            )
        } else {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "getChannelList chId = " + chId + "chList == null,or size == 0")
        }
        return chList
    }

    fun isCNRegion(): Boolean {
        return MarketRegionInfo.getCurrentMarketRegion() == MarketRegionInfo.REGION_CN
    }

    fun isTVSourceSeparation(): Boolean {
        return MarketRegionInfo.isFunctionSupport(MarketRegionInfo.F_SEP_TV_SRC_SUPPORT)
    }

    fun getCurrentChannelId(): Int {
        val chId = MtkTvConfig.getInstance()
            .getConfigValue(MtkTvConfigType.CFG_NAV_AIR_CRNT_CH)
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "getCurrentChannelId chId = $chId")
        return chId
    }

    fun tuneFirstChannel(context: Context, liveTvView: TvView?): MtkTIFChannelInfo? {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "start getFirstChannelForScan~")
        var selection: String = SELECTION_WITH_SVLID
        var selectionargs: Array<String> = getSvlIdSelectionArgs()
        if (isTVSourceSeparation()) {
            selection =
                SELECTION_WITH_SVLID_INPUTID
            selectionargs = getSvlIdAndInputIdSelectionArgs()
        }
        var tryCount = 0
        while (tryCount <= 2) {
            val c: Cursor =
                context.contentResolver.query(
                    TvContract.Channels.CONTENT_URI,
                    null,
                    selection,
                    selectionargs,
                    ORDERBY
                )
                    ?: return null
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "getFirstChannelForScan   c>>>" + c.count + "   ORDERBY>>" + ORDERBY)
            var temTIFChannel: MtkTIFChannelInfo? = null
            while (c.moveToNext()) {
                temTIFChannel = MtkTIFChannelInfo().parse(c)
                // only US EPG use hidden channel, other region not be used
                val tempApiChannel = getAPIChannelInfoByBlobData(
                    temTIFChannel!!.mDataValue
                )
                if (checkChMask(tempApiChannel, CH_LIST_MASK, CH_LIST_VAL)
                ) {
                    temTIFChannel!!.mMtkTvChannelInfo = tempApiChannel
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "end getFirstChannelForScan  temTIFChannel>>>$temTIFChannel")
                    c.close()
                    tuneToChannel(
                        liveTvView,
                        temTIFChannel.mInputServiceName!!,
                        TvContract.buildChannelUri(temTIFChannel.mId)
                    )
                    activeChannel = temTIFChannel
                    return temTIFChannel
                }
            }
            c.close()
            tryCount++
            try {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "Thread sleep beacuse not get channel")
                Thread.sleep(2000)
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "end getFirstChannelForScan null>>> tryCount=$tryCount")
        }
        return null
    }

    open fun checkChMask(
        chinfo: MtkTvChannelInfoBase?,
        attentionMask: Int,
        expectValue: Int
    ): Boolean {
        if (isTVSourceSeparation()) {
            if (chinfo != null && !chinfo.isUserDelete && !chinfo.isAnalogService) {
                Log.d(Constants.LogTag.CLTV_TAG +
                    TAG,
                    "checkChMask chinfo.getNwMask() = " + chinfo.nwMask + "atentionMask=" + attentionMask + " expectValue=" + expectValue
                )
                if (chinfo.nwMask and attentionMask == expectValue) {
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "checkChMask true")
                    return true
                }
            }

        } else {
            if (chinfo != null && (!chinfo.isUserDelete || chinfo.isUserDelete) && isSARegion()) {
                Log.d(Constants.LogTag.CLTV_TAG +
                    TAG,
                    "checkChMask chinfo.getNwMask() = " + chinfo.nwMask + "atentionMask=" + attentionMask + " expectValue=" + expectValue
                )
                if (chinfo.nwMask and attentionMask == expectValue) {
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "checkChMask true")
                    return true
                }
            }
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "checkChMask false")
        }
        return false
    }

    fun isSARegion(): Boolean {
        return MarketRegionInfo.getCurrentMarketRegion() == MarketRegionInfo.REGION_SA
    }

    open fun isUSRegion(): Boolean {
        return MarketRegionInfo.getCurrentMarketRegion() == MarketRegionInfo.REGION_US
    }

    private fun getSvlIdAndInputIdSelectionArgs(): Array<String> {
        val svlId: Int = getSvlId()
        var selectionArgs = arrayOf(String.format(Locale.UK, "%05d", svlId))
        if (isTVSourceSeparation()) {
            selectionArgs = arrayOf(
                String.format(Locale.UK, "%05d", svlId), "HW0"
            )
        }
        return selectionArgs
    }

    private fun getSvlIdSelectionArgs(): Array<String> {
        val svlId: Int = getSvlId()
        return arrayOf(String.format(Locale.UK, "%05d", svlId))
    }

    open fun getSvlId(): Int {
        return MtkTvConfig.getInstance().getConfigValue(MtkTvConfigTypeBase.CFG_BS_SVL_ID)
    }

    /**
     * not start with "com.mediatek.tvinput/." is 3rd
     * @return
     */
    open fun is3rdTVSource(tIFChannelInfo: MtkTIFChannelInfo?): Boolean {
        return tIFChannelInfo != null && tIFChannelInfo.mInputServiceName != null && !tIFChannelInfo.mInputServiceName!!.startsWith(
            "com.mediatek.tvinput/."
        )
    }

    /**
     * selected channel up true(OK),false(error)
     */
    open fun channelUpDownByMask(isUp: Boolean, mask: Int, value: Int) {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "channelUpDownByMask")
        val tifChannel: MtkTIFChannelInfo =
            getTIFUpOrDownChannel(!isUp, mask, value, mtkTifChannelInfoList, false)!!
        tuneToChannel(
            null,
            tifChannel.mInputServiceName!!,
            TvContract.buildChannelUri(tifChannel.mId)
        )

    }

    private fun getTIFUpOrDownChannel(
        isPreChannel: Boolean, attentionMask: Int,
        expectValue: Int, mChannelList: List<MtkTIFChannelInfo>?, showSkip: Boolean
    ): MtkTIFChannelInfo? {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "start getTIFUpOrDownChannel>>$isPreChannel")
        var currentChannelId: Int = getCurrentChannelId()
        var newId = (currentChannelId.toLong() and 0xffffffffL).toInt().toLong()
        val currentChannelInfo: MtkTIFChannelInfo = getTIFChannelInfoById(currentChannelId)!!
        val current3rdChannelInfo: MtkTIFChannelInfo = getChannelInfoByUri()
        if (current3rdChannelInfo != null && current3rdChannelInfo.mMtkTvChannelInfo == null) {
            currentChannelId = current3rdChannelInfo.mId.toInt()
            newId = current3rdChannelInfo.mId
            Log.d(Constants.LogTag.CLTV_TAG +
                TAG,
                "currentChannelId:" + currentChannelId + " newId " + newId + " current3rdChannelInfo " + current3rdChannelInfo.toString() + " currentChannelInfo " + currentChannelInfo.toString()
            )
        }
        var canGetData = false
        val isSAsKIPOption = false
        Log.d(Constants.LogTag.CLTV_TAG +
            TAG,
            "getTIFUpOrDownChannel isPrePage>>>$isPreChannel  mChannelList.size() = " + (mChannelList?.size
                ?: mChannelList)
        )
        if (isPreChannel) {
            var temTIFChannel: MtkTIFChannelInfo? = null
            if (mChannelList != null) {
                for (i in mChannelList.indices.reversed()) {
                    temTIFChannel = mChannelList[i]
                    Log.d(Constants.LogTag.CLTV_TAG +
                        TAG,
                        "getTIFUpOrDownChannel temTIFChannel>>> $temTIFChannel"
                    )
                    //   parserTIFChannelData(temTIFChannel, temTIFChannel.mData);
                    if (canGetData) {
                        Log.d(Constants.LogTag.CLTV_TAG +
                            TAG,
                            "getTIFUpOrDownChannel isPreChannel canGetData>>> $canGetData"
                        )
                        if (attentionMask == CH_LIST_3RDCAHNNEL_MASK && expectValue == CH_LIST_3RDCAHNNEL_VAL) {
                            Log.d(Constants.LogTag.CLTV_TAG +
                                TAG,
                                "end 1 getTIFUpOrDownChannel> 3rd"
                            )
                            if ((temTIFChannel.mDataValue == null || is3rdTVSource(
                                    temTIFChannel
                                )) && temTIFChannel.mIsBrowsable
                            ) {
                                return temTIFChannel
                            }
                        }
                        if (!(isUSRegion() || isSARegion()) && !temTIFChannel.mIsBrowsable) { // only US EPG use
                            // hidden channel,
                            // other region not
                            // be used
                            continue
                        }
                        if (isDisableColorKey()) {
                            if ((temTIFChannel.mDataValue == null || is3rdTVSource(temTIFChannel)) && temTIFChannel.mIsBrowsable) {
                                Log.d(Constants.LogTag.CLTV_TAG +
                                    TAG,
                                    "end 1 getTIFUpOrDownChannel isDisableColorKey isPreChannel 3rd channel temTIFChannel: $temTIFChannel"
                                )
                                return temTIFChannel
                            }
                        }
                        val tempApiChannel = getAPIChannelInfoByBlobData(temTIFChannel.mDataValue)
                        if (tempApiChannel == null || !showSkip && tempApiChannel.isSkip) {
                            continue
                        }
                        if (isSARegion() && isSAsKIPOption) {
                            if (checkChMask(
                                    tempApiChannel,
                                    attentionMask,
                                    expectValue
                                )
                                && tempApiChannel is MtkTvISDBChannelInfo
                                && currentChannelInfo.mMtkTvChannelInfo is MtkTvISDBChannelInfo
                            ) {
                                if ((tempApiChannel as MtkTvISDBChannelInfo).majorNum
                                    != (currentChannelInfo.mMtkTvChannelInfo as MtkTvISDBChannelInfo)
                                        .majorNum
                                ) {
                                    Log.d(Constants.LogTag.CLTV_TAG +
                                        TAG,
                                        "end sa 1 getTIFUpOrDownChannel>>"
                                    )
                                    temTIFChannel.mMtkTvChannelInfo = tempApiChannel
                                    return temTIFChannel
                                }
                            }
                        } else {
                            if (checkChMask(
                                    tempApiChannel,
                                    attentionMask,
                                    expectValue
                                )
                            ) {
                                if (checkChCategoryMask(
                                        tempApiChannel,
                                        getmCurCategories(),
                                        getmCurCategories()
                                    )
                                ) {
                                    Log.d(Constants.LogTag.CLTV_TAG +
                                        TAG,
                                        "end 1 getTIFUpOrDownChannel>>"
                                    )
                                    return temTIFChannel
                                }
                            }
                        }
                    } else {
                        if (temTIFChannel != null && (temTIFChannel.mInternalProviderFlag3 == newId.toInt() || temTIFChannel.mId == newId)) {
                            canGetData = true
                            Log.d(Constants.LogTag.CLTV_TAG +
                                TAG,
                                "end 2 getTIFUpOrDownChannel> PreChannel canGetData = true"
                            )
                        }
                    }
                }
                for (i in mChannelList.indices.reversed()) {
                    temTIFChannel = mChannelList[i]
                    if (attentionMask == CH_LIST_3RDCAHNNEL_MASK && expectValue == CH_LIST_3RDCAHNNEL_VAL) {
                        Log.d(Constants.LogTag.CLTV_TAG +
                            TAG,
                            "end 2 getTIFUpOrDownChannel> 3rd"
                        )
                        return if ((temTIFChannel.mDataValue == null || is3rdTVSource(
                                temTIFChannel
                            )) && temTIFChannel.mId !== newId && temTIFChannel.mIsBrowsable
                        ) {
                            temTIFChannel
                        } else {
                            continue
                        }
                    }
                    if (!(isUSRegion() || isSARegion()) && !temTIFChannel.mIsBrowsable) { // only US EPG use
                        // hidden channel,
                        // other region not be
                        // used
                        continue
                    }
                    if (isDisableColorKey()) {
                        if ((temTIFChannel.mDataValue == null || is3rdTVSource(
                                temTIFChannel
                            )) && temTIFChannel.mIsBrowsable
                        ) {
                            Log.d(Constants.LogTag.CLTV_TAG +
                                TAG,
                                "end 2 getTIFUpOrDownChannel isDisableColorKey isPreChannel 3rd channel temTIFChannel: $temTIFChannel"
                            )
                            return temTIFChannel
                        }
                    }
                    //     parserTIFChannelData(temTIFChannel, temTIFChannel.mData);
                    val tempApiChannel = getAPIChannelInfoByBlobData(temTIFChannel.mDataValue)
                    if (tempApiChannel == null || !showSkip && tempApiChannel.isSkip) {
                        continue
                    }
                    if (tempApiChannel.channelId != currentChannelId) {
                        if (isSARegion() && isSAsKIPOption) {
                            if (checkChMask(
                                    tempApiChannel,
                                    attentionMask,
                                    expectValue
                                )
                                && tempApiChannel is MtkTvISDBChannelInfo
                                && currentChannelInfo.mMtkTvChannelInfo is MtkTvISDBChannelInfo
                            ) {
                                if ((tempApiChannel as MtkTvISDBChannelInfo).majorNum
                                    != (currentChannelInfo.mMtkTvChannelInfo as MtkTvISDBChannelInfo)
                                        .majorNum
                                ) {
                                    temTIFChannel.mMtkTvChannelInfo = tempApiChannel
                                    Log.d(Constants.LogTag.CLTV_TAG +
                                        TAG,
                                        "end sa 2 getTIFUpOrDownChannel>>"
                                    )
                                    return temTIFChannel
                                }
                            }
                        } else {
                            if (checkChMask(
                                    tempApiChannel,
                                    attentionMask,
                                    expectValue
                                )
                            ) {
                                if (checkChCategoryMask(
                                        tempApiChannel,
                                        getmCurCategories(),
                                        getmCurCategories()
                                    )
                                ) {
                                    temTIFChannel.mMtkTvChannelInfo = tempApiChannel
                                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "end 2 getTIFUpOrDownChannel>>")
                                    return temTIFChannel
                                }
                            }
                        }
                    } else {
                        break
                    }
                }
            }
        } else {
            if (mChannelList != null) {
                for (temTIFChannel in mChannelList) {
                    Log.d(Constants.LogTag.CLTV_TAG +
                        TAG,
                        "end 3 getTIFUpOrDownChannel> nextChannel temTIFChannel = $temTIFChannel"
                    )
                    if (canGetData) {
                        if (attentionMask == CH_LIST_3RDCAHNNEL_MASK && expectValue == CH_LIST_3RDCAHNNEL_VAL) {
                            Log.d(Constants.LogTag.CLTV_TAG + TAG, "end 3 getTIFUpOrDownChannel> 3rd")
                            if ((temTIFChannel.mDataValue == null || is3rdTVSource(temTIFChannel)) && temTIFChannel.mIsBrowsable) {
                                return temTIFChannel
                            }
                        }
                        if (!(isUSRegion() || isSARegion()) && !temTIFChannel.mIsBrowsable) { // only US EPG use
                            // hidden channel,
                            // other region not be
                            // used
                            continue
                        }
                        if (isDisableColorKey()) {
                            if ((temTIFChannel.mDataValue == null || is3rdTVSource(
                                    temTIFChannel
                                )) && temTIFChannel.mIsBrowsable
                            ) {
                                Log.d(Constants.LogTag.CLTV_TAG +
                                    TAG,
                                    "end 3 getTIFUpOrDownChannel isDisableColorKey nextChannel 3rd channel temTIFChannel: $temTIFChannel"
                                )
                                return temTIFChannel
                            }
                        }
                        val tempApiChannel = getAPIChannelInfoByBlobData(temTIFChannel.mDataValue)
                        if (tempApiChannel == null || !showSkip && tempApiChannel.isSkip) {
                            continue
                        }
                        if (isSARegion() && isSAsKIPOption) {
                            if (checkChMask(
                                    tempApiChannel,
                                    attentionMask,
                                    expectValue
                                )
                                && tempApiChannel is MtkTvISDBChannelInfo
                                && currentChannelInfo.mMtkTvChannelInfo is MtkTvISDBChannelInfo
                            ) {
                                if ((tempApiChannel as MtkTvISDBChannelInfo).majorNum
                                    != (currentChannelInfo.mMtkTvChannelInfo as MtkTvISDBChannelInfo)
                                        .majorNum
                                ) {
                                    temTIFChannel.mMtkTvChannelInfo = tempApiChannel
                                    Log.d(Constants.LogTag.CLTV_TAG +
                                        TAG,
                                        "end sa 3 getTIFUpOrDownChannel>>"
                                    )
                                    return temTIFChannel
                                }
                            }
                        } else {
                            if (checkChMask(
                                    tempApiChannel,
                                    attentionMask,
                                    expectValue
                                )
                            ) {
                                if (checkChCategoryMask(
                                        tempApiChannel,
                                        getmCurCategories(),
                                        getmCurCategories()
                                    )
                                ) {
                                    temTIFChannel.mMtkTvChannelInfo = tempApiChannel
                                    Log.d(Constants.LogTag.CLTV_TAG +
                                        TAG,
                                        "end 3 getTIFUpOrDownChannel>>"
                                    )
                                    return temTIFChannel
                                }
                            }
                        }
                    } else {
                        if (temTIFChannel != null && (temTIFChannel.mInternalProviderFlag3 == newId.toInt() || temTIFChannel.mId == newId)) {
                            canGetData = true
                            Log.d(Constants.LogTag.CLTV_TAG +
                                TAG,
                                "end 3 getTIFUpOrDownChannel> nextChannel canGetData = true"
                            )
                        }
                    }
                }
                for (temTIFChannel in mChannelList) {
                    //   temTIFChannel = TIFChannelInfo.parse(c);
                    if (attentionMask == CH_LIST_3RDCAHNNEL_MASK && expectValue == CH_LIST_3RDCAHNNEL_VAL) {
                        Log.d(Constants.LogTag.CLTV_TAG +
                            TAG,
                            "end 4 getTIFUpOrDownChannel> 3rd"
                        )
                        if ((temTIFChannel.mDataValue == null || is3rdTVSource(
                                temTIFChannel
                            )) && temTIFChannel.mId !== newId && temTIFChannel.mIsBrowsable
                        ) {
                            return temTIFChannel
                        }
                    }
                    if (!(isUSRegion() || isSARegion()) && !temTIFChannel.mIsBrowsable) { // only US EPG use
                        // hidden channel,
                        // other region not be
                        // used
                        continue
                    }
                    if (isDisableColorKey()) {
                        if ((temTIFChannel.mDataValue == null || is3rdTVSource(
                                temTIFChannel
                            )) && temTIFChannel.mIsBrowsable
                        ) {
                            Log.d(Constants.LogTag.CLTV_TAG +
                                TAG,
                                "end 4 getTIFUpOrDownChannel isDisableColorKey nextChannel 3rd channel temTIFChannel: $temTIFChannel"
                            )
                            return temTIFChannel
                        }
                    }
                    //  parserTIFChannelData(temTIFChannel, temTIFChannel.mData);
                    val tempApiChannel = getAPIChannelInfoByBlobData(temTIFChannel.mDataValue)
                    if (tempApiChannel == null || !showSkip && tempApiChannel.isSkip) {
                        continue
                    }
                    if (tempApiChannel.channelId != currentChannelId) {
                        if (isSARegion() && isSAsKIPOption) {
                            if (tempApiChannel != null && checkChMask(
                                    tempApiChannel,
                                    attentionMask,
                                    expectValue
                                )
                                && tempApiChannel is MtkTvISDBChannelInfo
                                && currentChannelInfo.mMtkTvChannelInfo is MtkTvISDBChannelInfo
                            ) {
                                if ((tempApiChannel as MtkTvISDBChannelInfo).majorNum
                                    != (currentChannelInfo.mMtkTvChannelInfo as MtkTvISDBChannelInfo)
                                        .majorNum
                                ) {
                                    temTIFChannel.mMtkTvChannelInfo = tempApiChannel
                                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "end sa 4 getTIFUpOrDownChannel>>")
                                    return temTIFChannel
                                }
                            }
                        } else {
                            if (checkChMask(
                                    tempApiChannel,
                                    attentionMask,
                                    expectValue
                                )
                            ) {
                                if (checkChCategoryMask(
                                        tempApiChannel,
                                        getmCurCategories(),
                                        getmCurCategories()
                                    )
                                ) {
                                    temTIFChannel.mMtkTvChannelInfo = tempApiChannel
                                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "end 4 getTIFUpOrDownChannel>>")
                                    return temTIFChannel
                                }
                            }
                        }
                    } else {
                        break
                    }
                }
            }
        }
        //  c.close();
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "end getTIFUpOrDownChannel>> null")
        return null
    }

    private fun isDisableColorKey(): Boolean {
        //TODO
        return false
    }

    private fun getChannelInfoByUri(): MtkTIFChannelInfo {
        TODO("Not yet implemented")
    }

    open fun checkChCategoryMask(
        info: MtkTvChannelInfoBase?,
        categoryMask: Int,
        categoryValue: Int
    ): Boolean {
//     Log.d(Constants.LogTag.CLTV_TAG + TAG, "checkChMask chinfo="+chinfo);
        if (info is MtkTvDvbChannelInfo) {
            val chinfo = info
            if (chinfo != null && (!chinfo.isUserDelete || chinfo.isUserDelete) && isSARegion()) {
                if (chinfo != null && categoryMask > 0) {
                    Log.d(Constants.LogTag.CLTV_TAG +
                        TAG,
                        "checkChCategoryMask chinfo.getCategoryMask() = " + chinfo.categoryMask + "categoryMask=" + categoryMask + " categoryValue=" + categoryValue
                    )
                    if (chinfo.categoryMask and categoryMask == categoryValue) {
                        Log.d(Constants.LogTag.CLTV_TAG +
                            TAG,
                            "checkChMask true"
                        )
                        return true
                    }
                } else if (chinfo != null && categoryMask == 0) {
                    Log.d(Constants.LogTag.CLTV_TAG +
                        TAG,
                        "checkChCategoryMask chinfo.getChannelNumber() = " + chinfo.channelNumber + " categoryMask = " + categoryMask + " categoryValue = " + categoryValue
                    )
                    if (chinfo.channelNumber >= CATEGORIES_CHANNELNUM_BASE) {
                        Log.d(Constants.LogTag.CLTV_TAG + TAG, "checkChMask true")
                        return true
                    }
                } else if (chinfo != null && categoryMask == -1) {
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "checkChCategoryMask true")
                    return true
                }
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "checkChCategoryMask false")
            }
        } else {
            return true
        }
        return false
    }

    open fun getTIFChannelInfoById(channelId: Int): MtkTIFChannelInfo? {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "start getTIFChannelInfoById>>$channelId")

        Log.d(Constants.LogTag.CLTV_TAG + TAG, "getTIFChannelInfoById mChannelList.size>>" + mtkTifChannelInfoList.size)
        for (temTIFChannel in mtkTifChannelInfoList) {
            if (temTIFChannel != null && (temTIFChannel.mInternalProviderFlag3 == channelId || temTIFChannel.mId == channelId.toLong())) {
                val tempApiChannel: MtkTvChannelInfoBase =
                    getAPIChannelInfoByChannelId(temTIFChannel.mInternalProviderFlag3)!!
                temTIFChannel.mMtkTvChannelInfo = tempApiChannel
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "end getTIFChannelInfoById>>" + temTIFChannel.mDisplayName)
                return temTIFChannel
            }
        }
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "end getTIFChannelInfoById>>null")
        return null
    }


    open fun getAPIChannelInfoByChannelId(channelId: Int): MtkTvChannelInfoBase? {
        if (maps != null && !maps.isEmpty()) {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "getAPIChannelInfoByChannelI maps.size()" + maps.size)
            return maps[channelId]!!
        } else if (!getAllChannelsrunning) {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "getAPIChannelInfoByChannelI maps.size() null")
            //TODO do in bg thread
            getAllChannels()

        } else {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "getAllchannels is running,go on updata")
        }
        return null
    }

    open fun setmCurCategories(curCategories: Int) {
        mCurCategories = curCategories
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "setmCurCategories," + mCurCategories)
    }

    open fun getmCurCategories(): Int {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "getmCurCategories," + mCurCategories)
        return mCurCategories
    }
}