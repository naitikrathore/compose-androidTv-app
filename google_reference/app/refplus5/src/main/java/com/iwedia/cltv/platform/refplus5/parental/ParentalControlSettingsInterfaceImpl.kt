package com.iwedia.cltv.platform.refplus5.parental

import android.annotation.SuppressLint
import android.content.Context
import android.media.tv.ContentRatingSystem
import android.media.tv.TvContentRating
import android.os.Build
import android.preference.PreferenceManager
import androidx.annotation.RequiresApi
import com.iwedia.cltv.parental_controls.ContentRatingLevelPolicy
import com.iwedia.cltv.platform.base.parental.ParentalControlSettingsInterfaceBaseImpl
import com.iwedia.cltv.platform.`interface`.FastDataProviderInterface
import com.iwedia.cltv.platform.`interface`.TvInputInterface
import com.iwedia.cltv.platform.refplus5.SaveValue
import com.iwedia.cltv.platform.refplus5.UtilsInterfaceImpl
import com.mediatek.dtv.tvinput.client.rating.TvRating
import com.mediatek.dtv.tvinput.client.scan.Constants.TYPE_ATSC
import com.mediatek.dtv.tvinput.framework.tifextapi.atsc.view.rating.MtkTvRRTRatingDimInfo
import com.mediatek.dtv.tvinput.framework.tifextapi.atsc.view.rating.MtkTvRRTRatingLevelInfo
import com.mediatek.dtv.tvinput.framework.tifextapi.atsc.view.rating.MtkTvRRTRatingRegionInfo
import com.mediatek.dtv.tvinput.framework.tifextapi.isdb.settings.isdbtuner.lib.Constants.Companion.Common.Columns.BLOCK_UNRATED_PROG
import com.mediatek.dtv.tvinput.framework.tifextapi.isdb.settings.isdbtuner.lib.Constants.Companion.Common.Values.BLOCK_UNRATED_PROG_OFF
import com.mediatek.dtv.tvinput.framework.tifextapi.isdb.settings.isdbtuner.lib.Constants.Companion.Common.Values.BLOCK_UNRATED_PROG_ON

open class ParentalControlSettingsInterfaceImpl(
    private var context: Context,
    private var fastDataProviderInterface: FastDataProviderInterface,
    private var utilsInterfaceImpl : UtilsInterfaceImpl,
) : ParentalControlSettingsInterfaceBaseImpl(context, fastDataProviderInterface) {
    val authority: String =
        com.mediatek.dtv.tvinput.framework.tifextapi.atsc.settings.lib.Constants.AUTHORITY
    private val general: String =
        com.mediatek.dtv.tvinput.framework.tifextapi.atsc.settings.lib.Constants.SETTING_CATEGORY_GENERAL
    private var regionInfoList = arrayListOf<MtkTvRRTRatingRegionInfo>()
    private var dimInfoList = mutableListOf<MtkTvRRTRatingDimInfo>()
    private var levelInfoList = mutableListOf<MtkTvRRTRatingLevelInfo>()
    private val blockedList = hashMapOf<Int, Int>()

    companion object {
        val CONTENT_RATING_FRANCE_CATEGORY_I = 0
        val CONTENT_RATING_FRANCE_CATEGORY_II = 1
        val CONTENT_RATING_FRANCE_CATEGORY_III = 2
        val CONTENT_RATING_FRANCE_CATEGORY_IV = 3
        val CONTENT_RATING_FRANCE_CATEGORY_V = 4
        val CONTENT_RATING_FRANCE_CUSTOM = 5
    }

    @get:SuppressLint("WrongConstant")
    override val contentRatingLevel: Int
        get() {
            var currentLevel = getContentRatingLevel(context)
            if(utilsInterfaceImpl.getCountryCode()=="FR") {
                if (currentLevel > CONTENT_RATING_FRANCE_CUSTOM) {
                    currentLevel = 0
                }
            } else {
                if (currentLevel > CONTENT_RATING_LEVEL_CUSTOM) {
                    currentLevel = 0
                }
            }
            return currentLevel
        }

    override fun getBlockUnrated(): Int {
        return if (isBlockUnratedEnabled()) 1 else 0
    }

    override fun setBlockUnrated(isBlockUnrated: Boolean) {
        saveBlockUnratedSetting(isBlockUnrated)
    }

    override fun getGlobalRestrictionsArray(): MutableList<String> {
        if(utilsInterfaceImpl.getCountryCode()=="FR") {
            val restrictions: MutableList<String> = ArrayList()
            restrictions.add("france_category_i")
            restrictions.add("france_category_ii")
            restrictions.add("france_category_iii")
            restrictions.add("france_category_iv")
            restrictions.add("france_category_v")
            restrictions.add("option_rating_custom")
            return restrictions
        } else {
            return super.getGlobalRestrictionsArray()
        }
    }

    private fun isBlockUnratedEnabled(): Boolean {
        return SaveValue.readTISSettingsIntValues(
            context, BLOCK_UNRATED_PROG, authority, general, BLOCK_UNRATED_PROG_OFF
        ) == BLOCK_UNRATED_PROG_ON
    }

    private fun saveBlockUnratedSetting(isBlockUnrated: Boolean) {
        SaveValue.saveTISSettingsIntValue(
            context,
            BLOCK_UNRATED_PROG,
            authority,
            general,
            if (isBlockUnrated) BLOCK_UNRATED_PROG_ON else BLOCK_UNRATED_PROG_OFF
        )
    }

    override fun getRRT5Regions(): MutableList<String> {
        val regionList = mutableListOf<String>()
        regionList.clear()
        try {
            val rating = TvRating(context, TYPE_ATSC)
            val resultGet = rating.getRRTRatingInfo()
            resultGet?.classLoader = MtkTvRRTRatingRegionInfo::class.java.classLoader
            regionInfoList =
                resultGet?.getParcelableArrayList(
                    com.mediatek.dtv.tvinput.framework.tifextapi.atsc.view.rating.Constants.KEY_RRT_LIST
                )!!
            RatingPara.rrt5Ratings = regionInfoList
            regionInfoList
        } catch (e: Exception) {
            regionInfoList
        }

        regionInfoList.forEach {
            regionList.add(it.ratingRegionText)
        }
        return regionList
    }

    override fun getRRT5Dim(index: Int): MutableList<String> {
        val dimList = mutableListOf<String>()
        dimList.clear()
        RatingPara.regionIndex = index
        if (regionInfoList.size > 0) {
            dimInfoList = regionInfoList[index].ratingDimInfoList
        }
        dimInfoList.forEach {
            dimList.add(it.dimText)
        }
        return dimList
    }

    override fun getRRT5Level(countryIndex: Int, dimIndex: Int): MutableList<String> {
        val levelList = mutableListOf<String>()
        levelList.clear()
        RatingPara.dimIndex = dimIndex
        if (dimInfoList.size > 0) levelInfoList = dimInfoList[dimIndex].ratingLevelList

        levelInfoList.forEach {
            if (!it.lvlAbbrText.isNullOrEmpty()) levelList.add(it.lvlAbbrText)
        }
        return levelList
    }

    override fun setSelectedItemsForRRT5Level(regionIndex: Int, dimIndex: Int, levelIndex: Int) {
        RatingPara.dimIndex = dimIndex
        RatingPara.levelIndex = levelIndex
        RatingPara.regionIndex = regionIndex
        val mtkTvRRTRatingDimInfo =
            RatingPara.rrt5Ratings?.get(RatingPara.regionIndex!!)?.ratingDimInfoList?.get(RatingPara.dimIndex!!)
        RatingPara.levelIndex = levelIndex

        val levelBlock = mtkTvRRTRatingDimInfo?.ratingLevelList!![levelIndex].levelBlock
        val control = !levelBlock

        val order = mtkTvRRTRatingDimInfo.dimGrad
        if (order) {
            for (i in 0 until mtkTvRRTRatingDimInfo.ratingLevelList.size) {
                if (control && i > levelIndex) {
                    // block
                    mtkTvRRTRatingDimInfo.ratingLevelList[i].levelBlock = control
                }
                if (!control && i < levelIndex) {
                    // unblock
                    mtkTvRRTRatingDimInfo.ratingLevelList[i].levelBlock = control
                }
            }
        }
        mtkTvRRTRatingDimInfo.ratingLevelList[RatingPara.levelIndex!!].levelBlock = control
        RatingPara.rrt5Ratings!!.let {
            TvRrt5Rating.setRRTRatingInfo(
                context, TYPE_ATSC, it
            )
        }
    }

    override fun rrt5BlockedList(regionPosition: Int, position: Int): HashMap<Int, Int> {
        blockedList.clear()
        val mtkTvRRTRatingDimInfo =
            RatingPara.rrt5Ratings?.get(regionPosition)?.ratingDimInfoList?.get(position)

        mtkTvRRTRatingDimInfo!!.ratingLevelList.forEachIndexed { index, mtkTvRRTRatingLevelInfo ->
            val ratingLevelInfo: MtkTvRRTRatingLevelInfo = mtkTvRRTRatingLevelInfo
            val block = ratingLevelInfo.levelBlock
            blockedList[index] = if (block) 1 else 0
        }
        return blockedList
    }


    override fun resetRRT5() {
        val tvRating = TvRating(context, TYPE_ATSC)
        tvRating.setResetRrt5();
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

    @SuppressLint("NewApi")
    private fun storeRatings() {
        val removed: MutableSet<TvContentRating> = HashSet(mTvInputManager.blockedRatings)
        removed.removeAll(mRatings)
        for (tvContentRating in removed) {
            val str = tvContentRating.flattenToString()
            removeBlockedRating(tvContentRating)
        }
        val added: MutableSet<TvContentRating> = HashSet(mRatings)
        added.removeAll(mTvInputManager.blockedRatings)
        for (tvContentRating in added) {
            addBlockedRating(tvContentRating)
        }
    }

    private fun toTvContentRating(
        contentRatingSystem: ContentRatingSystem, rating: ContentRatingSystem.Rating
    ): TvContentRating {
        return TvContentRating.createRating(
            contentRatingSystem.domain, contentRatingSystem.name, rating.name
        )
    }

    private fun toTvContentRating(
        contentRatingSystem: ContentRatingSystem,
        rating: ContentRatingSystem.Rating,
        subRating: ContentRatingSystem.SubRating
    ): TvContentRating {
        return TvContentRating.createRating(
            contentRatingSystem.domain,
            contentRatingSystem.name,
            rating.name,
            subRating.name
        )
    }

    private fun setFranceContentRatingLevel(tvInputInterface: TvInputInterface?, level: Int) {
        val currentLevel = contentRatingLevel
        if (level == currentLevel) {
            return
        }
        if (currentLevel == CONTENT_RATING_FRANCE_CUSTOM) {
            mCustomRatings = mRatings
        }
        super.setContentRatingLevel(context, level)
        if (level == CONTENT_RATING_FRANCE_CUSTOM) {
            if (mCustomRatings != null) {
                mRatings = HashSet(mCustomRatings)
            }
        } else {
            mRatings = ContentRatingLevelPolicyFrance.getRatingsForLevel(this, tvInputInterface, level)
            if (level != CONTENT_RATING_FRANCE_CATEGORY_I) {
                mRatings.add(TvContentRating.UNRATED)
            } else {
                mRatings.clear()
            }
        }
        storeRatings()

    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun setContentRatingLevel(
        tvInputInterface: TvInputInterface?, level: Int
    ) {
        if(utilsInterfaceImpl.getCountryCode()=="FR") {
            return setFranceContentRatingLevel(tvInputInterface, level)
        } else {
            val currentLevel = contentRatingLevel
            if (level == currentLevel) {
                return
            }
            if (currentLevel == CONTENT_RATING_LEVEL_CUSTOM) {
                mCustomRatings = mRatings
            }
            setContentRatingLevel(context, level)
            if (level == CONTENT_RATING_LEVEL_CUSTOM) {
                if (mCustomRatings != null) {
                    mRatings = HashSet(mCustomRatings)
                }
            } else {
                mRatings = ContentRatingLevelPolicy.getRatingsForLevel(this, tvInputInterface, level)
                if (level == CONTENT_RATING_LEVEL_NONE) {
                    mRatings.clear()
                }
            }
            storeRatings()
        }
    }

    private fun getContentRatingSystemSet(context: Context?): MutableSet<String> {
        return HashSet(
            PreferenceManager.getDefaultSharedPreferences(context)
                .getStringSet(PREF_CONTENT_RATING_SYSTEMS, emptySet())!!
        )
    }

    private fun removeContentRatingSystem(context: Context?, id: String) {
        val contentRatingSystemSet = getContentRatingSystemSet(context)
        if (contentRatingSystemSet.remove(id)) {
            PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putStringSet(PREF_CONTENT_RATING_SYSTEMS, contentRatingSystemSet)
                .apply()
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun updateRatingsForCurrentLevel(tvInputInterface: TvInputInterface) {
        val currentLevel = contentRatingLevel
        if (currentLevel != CONTENT_RATING_FRANCE_CUSTOM) {
            mRatings =
                ContentRatingLevelPolicy.getRatingsForLevel(this, tvInputInterface, currentLevel)
            if (currentLevel != CONTENT_RATING_FRANCE_CATEGORY_I) {
                // UNRATED contents should be blocked unless the rating level is none or custom
                mRatings.add(TvContentRating.UNRATED)
            }
            storeRatings()
        }
    }

    private fun removeBlockedRating(tvContentRating: TvContentRating) {
        try {
            cls?.getDeclaredMethod("removeBlockedRating", TvContentRating::class.java)
                ?.invoke(mTvInputManager, tvContentRating)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun addBlockedRating(tvContentRating: TvContentRating) {
        try {
            cls?.getDeclaredMethod("addBlockedRating", TvContentRating::class.java)
                ?.invoke(mTvInputManager, tvContentRating)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @SuppressLint("NewApi")
    private fun setFranceContentRatingSystemEnabled(tvInputInterface: TvInputInterface,
                                                    contentRatingSystem: ContentRatingSystem,
                                                    enabled: Boolean) {
        if (enabled) {
            addContentRatingSystem(
                context,
                contentRatingSystem.id
            )
            // Ensure newly added system has ratings for current level set
            updateRatingsForCurrentLevel(tvInputInterface)
        } else {
            // Ensure no ratings are blocked for the selected rating system
            for (tvContentRating in mTvInputManager.blockedRatings) {
                if (contentRatingSystem.ownsRating(tvContentRating)) {
                    removeBlockedRating(tvContentRating)
                    mRatings.remove(tvContentRating)
                }
            }
            removeContentRatingSystem(
                context,
                contentRatingSystem.id
            )
        }
    }

    override fun setContentRatingSystemEnabled(
        tvInputInterface: TvInputInterface,
        contentRatingSystem: ContentRatingSystem,
        enabled: Boolean
    ) {
        if(utilsInterfaceImpl.getCountryCode()=="FR") {
            setFranceContentRatingSystemEnabled(tvInputInterface, contentRatingSystem, enabled)
        } else {
            super.setContentRatingSystemEnabled(tvInputInterface, contentRatingSystem, enabled)
        }
    }

    private fun setFranceRatingBlockedInternal(
        contentRatingSystem: ContentRatingSystem,
        rating: ContentRatingSystem.Rating,
        subRating: ContentRatingSystem.SubRating?,
        blocked: Boolean
    ): Boolean {
        val tvContentRating = subRating?.let { toTvContentRating(contentRatingSystem, rating, it) }
            ?: toTvContentRating(contentRatingSystem, rating)
        val changed: Boolean
        if (blocked) {
            changed = mRatings.add(tvContentRating)
            addBlockedRating(tvContentRating)
        } else {
            changed = mRatings.remove(tvContentRating)
            removeBlockedRating(tvContentRating)
        }
        if (changed) {
            changeToCustomLevel()
        }
        return changed
    }

    private fun changeToCustomLevel() {
        if (contentRatingLevel != CONTENT_RATING_FRANCE_CUSTOM) {
            setContentRatingLevel(
                context,
                CONTENT_RATING_FRANCE_CUSTOM
            )
        }
    }

    override fun setSubRatingBlocked(
        contentRatingSystem: ContentRatingSystem,
        rating: ContentRatingSystem.Rating,
        subRating: ContentRatingSystem.SubRating?,
        blocked: Boolean
    ): Boolean {
        if(utilsInterfaceImpl.getCountryCode()=="FR") {
            return setFranceRatingBlockedInternal(contentRatingSystem, rating, subRating, blocked)
        } else {
            return super.setSubRatingBlocked(contentRatingSystem, rating, subRating, blocked)
        }
    }

    override fun setRatingBlocked(
        contentRatingSystem: ContentRatingSystem,
        rating: ContentRatingSystem.Rating,
        blocked: Boolean
    ): Boolean {
        if(utilsInterfaceImpl.getCountryCode()=="FR") {
            return setFranceRatingBlockedInternal(contentRatingSystem, rating, null, blocked)
        } else {
            return super.setRatingBlocked(contentRatingSystem, rating, blocked)
        }
    }

    override fun getContentRatingLevel(context: Context?): Int {
        if(utilsInterfaceImpl.getCountryCode()=="FR") {
            return PreferenceManager.getDefaultSharedPreferences(context)
                .getInt(PREF_CONTENT_RATING_LEVEL, CONTENT_RATING_FRANCE_CATEGORY_I)
        } else {
            return super.getContentRatingLevel(context)
        }
    }
}