package com.iwedia.cltv.platform.t56.parental

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.preference.PreferenceManager
import com.iwedia.cltv.platform.base.parental.ParentalControlSettingsInterfaceBaseImpl
import com.iwedia.cltv.platform.`interface`.FastDataProviderInterface
import com.iwedia.cltv.platform.model.parental.InputSourceData
import com.mediatek.twoworlds.tv.MtkTvATSCRating
import com.mediatek.twoworlds.tv.MtkTvInputSource
import com.mediatek.twoworlds.tv.model.MtkTvOpenVCHIPInfoBase
import com.mediatek.twoworlds.tv.model.MtkTvOpenVCHIPPara
import com.mediatek.twoworlds.tv.model.MtkTvOpenVCHIPSettingInfoBase

open class ParentalControlSettingsInterfaceImpl(
    private var context: Context,
    private var fastDataProviderInterface: FastDataProviderInterface
) : ParentalControlSettingsInterfaceBaseImpl(context, fastDataProviderInterface) {

    private var mRegionNum = 0
    private var dimNum = 0
    private var levelNum = 0
    private var para: MtkTvOpenVCHIPPara? = null
    private val mTvRatingSettingInfo: MtkTvATSCRating? = MtkTvATSCRating.getInstance()

    @SuppressLint("StaticFieldLeak")
    private val regionList = mutableListOf<String>()
    private val dimList = mutableListOf<String>()
    private val levelList = mutableListOf<String>()
    private val blockedList = hashMapOf<Int, Int>()
    private val rrt5BlockedList = hashMapOf<Int, Int>()
    private var regionPosition = 0
    private var levelPosition = 0
    var dimIndex = 0
    var iniValue = 0
    lateinit var block: ByteArray

    init {
//        migratePreferenceFromLiveTvApp()
    }

    /**
     * Migrate Preference from Live Tv App as default preference
     */
    private fun migratePreferenceFromLiveTvApp() {

        // If current app rating system is not yet set up
        if (!isContentRatingSystemSet(context)) {
            try {
                val liveTvContext = context.createPackageContext(
                    "com.mediatek.wwtv.tvcenter",
                    Context.CONTEXT_IGNORE_SECURITY
                )
                val liveTvPreferences = PreferenceManager.getDefaultSharedPreferences(liveTvContext)

                // Migrating enabled rating systems
                val enabledRatingSystems: Set<String> =
                    liveTvPreferences.getStringSet(PREF_CONTENT_RATING_SYSTEMS, emptySet())!!

                enabledRatingSystems.forEach {
                    addContentRatingSystem(context, it)
                }

                // Migrating rating level
                val ratingLevel =
                    liveTvPreferences.getInt(PREF_CONTENT_RATING_LEVEL, CONTENT_RATING_LEVEL_NONE)

                setContentRatingLevel(context, ratingLevel)

            } catch (_: PackageManager.NameNotFoundException) {
                // Ignore If Live Tv App is not installed
            }
        }
    }

    override fun blockTvInputCount(blockedInputs: MutableList<InputSourceData>): Int {
        var blockedInputCount = 0
        blockedInputs.forEach {
            if (MtkTvInputSource.getInstance().isBlock(it.hardwareId shr 16)) {

                blockedInputCount++
            }
        }
        return blockedInputCount

    }

    override fun blockInput(selected: Boolean, item: InputSourceData) {
        MtkTvInputSource.getInstance().block(item.hardwareId shr 16, selected)
    }

    override fun isBlockSource(hardwareId: Int): Boolean {
        return MtkTvInputSource.getInstance().isBlock(hardwareId shr 16)
    }

    override fun getBlockUnrated(): Int {
        return if (MtkTvATSCRating.getInstance().blockUnrated) 1 else 0
    }

    override fun setBlockUnrated(isBlockUnrated: Boolean) {
        MtkTvATSCRating.getInstance().blockUnrated = isBlockUnrated
    }

    private fun getOpenVCHIPPara(): MtkTvOpenVCHIPPara? {
        if (para == null) {
            para = MtkTvOpenVCHIPPara()
        }
        return para
    }

    private fun getOpenVchip(): MtkTvOpenVCHIPInfoBase? {
        para = getOpenVCHIPPara()
        return mTvRatingSettingInfo?.getOpenVCHIPInfo(para)
    }

    override fun getRRT5Regions(): MutableList<String> {
        regionList.clear()
        getOpenVCHIPPara()?.openVCHIPParaType =
            MtkTvOpenVCHIPPara.OPEN_VCHIP_KEY_GET_RGN_NUM
        if (getOpenVchip() != null) {
            mRegionNum = getOpenVchip()!!.regionNum
        }
        for (i in 0 until mRegionNum) {
            getOpenVCHIPPara()?.openVCHIPParaType =
                MtkTvOpenVCHIPPara.OPEN_VCHIP_KEY_GET_RGN_TEXT
            getOpenVCHIPPara()?.regionIndex = i
            getOpenVchip()?.regionText?.let { regionList.add(it) }
        }
        return regionList
    }

    @SuppressLint("SuspiciousIndentation")
    override fun getRRT5Dim(index: Int): MutableList<String> {
        dimList.clear()
        getOpenVCHIPPara()?.regionIndex = index
        getOpenVCHIPPara()?.openVCHIPParaType =
            MtkTvOpenVCHIPPara.OPEN_VCHIP_KEY_GET_DIM_NUM
        if (getOpenVchip() != null) {
            dimNum = getOpenVchip()!!.dimNum
        }
        for (j in 0 until dimNum) {
            getOpenVCHIPPara()?.openVCHIPParaType =
                MtkTvOpenVCHIPPara.OPEN_VCHIP_KEY_GET_DIM_TEXT
            getOpenVCHIPPara()?.dimIndex = j
            getOpenVchip()?.dimText?.let { dimList.add(it) }
        }
        return dimList
    }

    override fun getRRT5Level(regionPosition: Int, position: Int): MutableList<String> {
        levelList.clear()
        getOpenVCHIPPara()?.regionIndex = regionPosition
        getOpenVCHIPPara()?.dimIndex = position
        dimIndex = position
        getOpenVCHIPPara()?.openVCHIPParaType =
            MtkTvOpenVCHIPPara.OPEN_VCHIP_KEY_GET_LVL_NUM
        if (getOpenVchip() != null) {
            levelNum = getOpenVchip()!!.levelNum
        }
        getOpenVCHIPPara()?.openVCHIPParaType =
            MtkTvOpenVCHIPPara.OPEN_VCHIP_KEY_GET_LVL_ABBR
        for (k in 0 until levelNum) {
            getOpenVCHIPPara()?.levelIndex = k + 1
            val textString: String = getOpenVchip()!!.lvlAbbrText
            levelList.add(textString)
        }
        return levelList
    }

    override fun getSelectedItemsForRRT5Level(): HashMap<Int, Int> {
        blockedList.clear()
        getOpenVCHIPPara()?.regionIndex = regionPosition
        getOpenVCHIPPara()?.dimIndex = dimIndex
        getOpenVCHIPPara()?.openVCHIPParaType =
            MtkTvOpenVCHIPPara.OPEN_VCHIP_KEY_GET_LVL_NUM
        if (getOpenVchip() != null) {
            levelNum = getOpenVchip()!!.levelNum
        }
        getOpenVCHIPPara()?.openVCHIPParaType =
            MtkTvOpenVCHIPPara.OPEN_VCHIP_KEY_GET_LVL_ABBR
        // Callback to get blocked events
        val info: MtkTvOpenVCHIPSettingInfoBase =
            mTvRatingSettingInfo!!.getOpenVCHIPSettingInfo(
                regionPosition, dimIndex
            )
        val block = info.lvlBlockData
        for (k in 0 until levelNum) {
            getOpenVCHIPPara()?.levelIndex = k + 1
            iniValue = block[k].toInt()
            blockedList[k] = iniValue
        }
        return blockedList
    }

    override fun rrt5BlockedList(regionPosition: Int, position: Int): HashMap<Int, Int> {
        rrt5BlockedList.clear()
        getOpenVCHIPPara()?.regionIndex = regionPosition
        getOpenVCHIPPara()?.dimIndex = position
        getOpenVCHIPPara()?.openVCHIPParaType =
            MtkTvOpenVCHIPPara.OPEN_VCHIP_KEY_GET_LVL_NUM
        if (getOpenVchip() != null) {
            levelNum = getOpenVchip()!!.levelNum
        }
        getOpenVCHIPPara()?.openVCHIPParaType =
            MtkTvOpenVCHIPPara.OPEN_VCHIP_KEY_GET_LVL_ABBR
        // Callback to get blocked events
        val info: MtkTvOpenVCHIPSettingInfoBase =
            mTvRatingSettingInfo!!.getOpenVCHIPSettingInfo(
                regionPosition, position
            )
        val block = info.lvlBlockData
        for (k in 0 until levelNum) {
            getOpenVCHIPPara()?.levelIndex = k + 1
            iniValue = block[k].toInt()
            rrt5BlockedList[k] = iniValue
        }
        return rrt5BlockedList
    }

    override fun setSelectedItemsForRRT5Level(
        regionIndex: Int,
        dimIndex: Int,
        levelIndex: Int
    ) {
        val info: MtkTvOpenVCHIPSettingInfoBase = mTvRatingSettingInfo!!.getOpenVCHIPSettingInfo(
            regionIndex, dimIndex
        )
        block = info.lvlBlockData
        iniValue = block[levelIndex].toInt()
        getOpenVCHIPPara()!!.openVCHIPParaType = MtkTvOpenVCHIPPara.OPEN_VCHIP_KEY_GET_DIM_GRAD
        getOpenVCHIPPara()!!.regionIndex = regionIndex
        getOpenVCHIPPara()!!.dimIndex = dimIndex
        getOpenVCHIPPara()!!.levelIndex = levelIndex
        if (getOpenVchip()!!.isDimGrad) {
            for (i in block.indices) {
                if (iniValue == 0) {
                    if (i >= levelIndex) {
                        block[i] = 1
                    }
                } else if (iniValue == 1) {
                    if (i <= levelIndex) {
                        block[i] = 0
                    }
                }
            }
        } else {
            if (iniValue == 0) {
                block[levelIndex] = 1
            } else if (iniValue == 1) {
                block[levelIndex] = 0
            }
        }
        info.regionIndex = regionIndex
        info.dimIndex = dimIndex
        info.lvlBlockData = block
        mTvRatingSettingInfo.openVCHIPSettingInfo = info
        mTvRatingSettingInfo.setAtscStorage(true)
    }

    override fun resetRRT5() {
        getOpenVCHIPPara()!!.openVCHIPParaType = MtkTvOpenVCHIPPara.OPEN_VCHIP_KEY_GET_RGN_NUM
        mRegionNum = getOpenVchip()!!.regionNum
        for (i in 0 until mRegionNum) {
            getOpenVCHIPPara()!!.openVCHIPParaType =
                MtkTvOpenVCHIPPara.OPEN_VCHIP_KEY_GET_RGN_TEXT
            getOpenVCHIPPara()!!.regionIndex = i
            getOpenVCHIPPara()!!.openVCHIPParaType =
                MtkTvOpenVCHIPPara.OPEN_VCHIP_KEY_GET_DIM_NUM
            dimNum = getOpenVchip()!!.dimNum
            for (j in 0 until dimNum) {
                getOpenVCHIPPara()!!.openVCHIPParaType =
                    MtkTvOpenVCHIPPara.OPEN_VCHIP_KEY_GET_DIM_TEXT
                getOpenVCHIPPara()!!.dimIndex = j
                getOpenVCHIPPara()!!.openVCHIPParaType =
                    MtkTvOpenVCHIPPara.OPEN_VCHIP_KEY_GET_LVL_NUM
                levelNum = getOpenVchip()!!.levelNum
                for (k in 0 until levelNum) {
                    getOpenVCHIPPara()!!.levelIndex = k + 1
                    val info: MtkTvOpenVCHIPSettingInfoBase =
                        mTvRatingSettingInfo!!.getOpenVCHIPSettingInfo(i, j)
                    block = info.lvlBlockData
                    iniValue = block[k].toInt()
                    for (l in block.indices) {
                        block[l] = 0
                        if (iniValue == 0) {
                            if (l >= k) {
                                block[l] = 0
                            }
                        } else if (iniValue == 1) {
                            if (i <= k) {
                                block[l] = 0
                            }
                        }
                    }
                    info.regionIndex = i
                    info.dimIndex = j
                    info.lvlBlockData = block
                    mTvRatingSettingInfo.openVCHIPSettingInfo = info
                    mTvRatingSettingInfo.setAtscStorage(false)
                }
            }
        }
    }

    override fun setParentalControlsEnabled(enabled: Boolean) {
        try {
            val setParentalControlsEnabled = cls?.getDeclaredMethod(
                "setParentalControlsEnabled",
                Boolean::class.java
            )
            setParentalControlsEnabled?.invoke(mTvInputManager, enabled)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun isParentalControlsEnabled(): Boolean {
        return mTvInputManager.isParentalControlsEnabled
    }
}