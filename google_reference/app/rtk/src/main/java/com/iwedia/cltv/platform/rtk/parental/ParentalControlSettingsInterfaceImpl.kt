package com.iwedia.cltv.platform.rtk.parental

import android.annotation.SuppressLint
import android.content.Context
import android.media.tv.ContentRatingSystem
import android.os.Build
import android.text.TextUtils
import android.media.tv.Rrt5ContentRatingSystem
import android.util.Log
import androidx.annotation.RequiresApi
import com.iwedia.cltv.platform.*
import com.iwedia.cltv.platform.base.parental.ParentalControlSettingsInterfaceBaseImpl
import com.iwedia.cltv.platform.`interface`.FastDataProviderInterface
import com.iwedia.cltv.platform.`interface`.UtilsInterface
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.platform.model.parental.InputSourceData
import com.iwedia.cltv.platform.rtk.UtilsInterfaceImpl
import com.realtek.tv.RatingDimension
import com.realtek.tv.Tv


@RequiresApi(Build.VERSION_CODES.P)
open class ParentalControlSettingsInterfaceImpl(
    private var context: Context,
    private var fastDataProviderInterface: FastDataProviderInterface,
    private var utilsInterface: UtilsInterface
) : ParentalControlSettingsInterfaceBaseImpl(context, fastDataProviderInterface) {

    private val TAG = "ParentalControlSettingsInterfaceImpl"

    @SuppressLint("StaticFieldLeak")
    private val regionList = mutableListOf<String>()
    private val mRtkContentRatingSystems = mutableListOf<Rrt5ContentRatingSystem>()
    private val regionCrsMap = hashMapOf<String, MutableList<ContentRatingSystem>>()
    private val COUNTRY_USA = "US"
    private val TV_DOMAIN = "com.android.tv"
    private val SI_RATING_REGION_USA_D = 5


    override fun blockTvInputCount(blockedInputs: MutableList<InputSourceData>): Int {
        return 0

    }

    override fun blockInput(selected: Boolean, item: InputSourceData) {

    }

    override fun isBlockSource(hardwareId: Int): Boolean {
        return false
    }

    override fun getBlockUnrated(): Int {
        return 0
    }

    override fun setBlockUnrated(isBlockUnrated: Boolean) {

    }


    override fun getRRT5Regions(): MutableList<String> {
        loadRRt5ratings()
        return regionList
    }

    @SuppressLint("SuspiciousIndentation")
    override fun getRRT5CrsInfo(regionName: String): MutableList<ContentRatingSystem> {
        if (regionCrsMap.size > 0) {
            if (regionCrsMap.containsKey(regionName)) {
                return regionCrsMap[regionName]!!
            } else {
                Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG, "regionCrsMap doesn't contain region name")
            }

        }
        return mutableListOf()
    }

    override fun getRRT5Level(regionPosition: Int, position: Int): MutableList<String> {
        return mutableListOf()
    }

    override fun getSelectedItemsForRRT5Level(): HashMap<Int, Int> {
        return hashMapOf()
    }

    override fun rrt5BlockedList(regionPosition: Int, position: Int): HashMap<Int, Int> {
        return hashMapOf()
    }

    override fun setSelectedItemsForRRT5Level(
        regionIndex: Int,
        dimIndex: Int,
        levelIndex: Int
    ) {

    }

    override fun resetRRT5() {
        for (crs in mRtkContentRatingSystems) {
            if (isContentRatingSystemEnabled(crs)) {
                for (rating in crs.ratings) {
                    if (isRatingBlocked(crs, rating)) {
                        setRatingBlocked(crs, rating, false)
                    }
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

    @RequiresApi(Build.VERSION_CODES.P)
    private fun loadRRt5ratings() {
        mRtkContentRatingSystems.clear()
        regionList.clear()
        regionCrsMap.clear()
        val tvSetting = (utilsInterface as UtilsInterfaceImpl).getTvSetting()
        addRrt5RatingList(tvSetting, SI_RATING_REGION_USA_D)
    }




    /**
     * addDrrtRatingList
     *
     * @param tv
     * @param parser
     * @param ratingId
     */
    @RequiresApi(Build.VERSION_CODES.P)
    private fun addRrt5RatingList(tv: Tv, ratingId: Int) {
        val rrtRatingTable = tv.getRatingTable(ratingId)
        if (rrtRatingTable != null) {
            val rrtRatingsList: List<Rrt5ContentRatingSystem> =
                parseCustomRatingSystem(rrtRatingTable)!!
            mRtkContentRatingSystems.addAll(rrtRatingsList)
            for (rrt5Rating in rrtRatingsList) {
                setDefaultContentRatingSystem(rrt5Rating)
                val name: String? = rrt5Rating.mRatingRegionName
                if (name != null && isContentRatingSystemEnabled(rrt5Rating)) {
                    val crsList =
                        regionCrsMap.getOrDefault(rrt5Rating.mRatingRegionName, mutableListOf())
                    crsList.add(rrt5Rating)
                    crsList.sortBy { it.name }
                    regionCrsMap[name] = crsList

                    if (!regionList.contains(name)) {
                        if (!TextUtils.isEmpty(name.trim())) {
                            regionList.add(name)
                        }
                    }
                }

            }
            regionList.sortedBy { it }
        }
        Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG, "rrtRatingTable list is null")
    }

    override fun setRatingBlocked(
        contentRatingSystem: ContentRatingSystem,
        rating: ContentRatingSystem.Rating,
        blocked: Boolean
    ): Boolean {
        return super.setRatingBlocked(contentRatingSystem, rating, blocked)
    }



    private fun setDefaultContentRatingSystem(contentRatingSystem: Rrt5ContentRatingSystem) {
        if (!isContentRatingSystemEnabled(contentRatingSystem)) {
            setContentRatingSystemEnabled(mTvInputInterface, contentRatingSystem, true)
        }
    }


    open fun parseCustomRatingSystem(list: List<RatingDimension>?): List<Rrt5ContentRatingSystem>? {
        if (list.isNullOrEmpty()) {
            return null
        }
        val ratingSystems: MutableList<Rrt5ContentRatingSystem> = ArrayList()
        for (index in list.indices) {
            val ratingDimension = list[index]
            if (ratingDimension.valueCount == 0) {
                continue
            }
            val builder = Rrt5ContentRatingSystem.RtkBuilder(context)
            builder.setDomain(TV_DOMAIN)
            builder.setRegionName(ratingDimension.regionName)
            builder.addCountry(COUNTRY_USA)

            builder.setName(ratingDimension.dimensionName)
            builder.setTitle(ratingDimension.dimensionName) //mTitle
            builder.setDescription("description dim $index")
            builder.setIsCustom(true)
            val flag = ratingDimension.graduatedScale
            // Rating
            val ob = ContentRatingSystem.Order.Builder()
            for (i in 0 until ratingDimension.valueCount) {
                //if(i==0)
                //    continue;
                var name = ratingDimension.abbrevValueName[i]
                if (TextUtils.isEmpty(name)) {
                    name = ratingDimension.fullValueName[i]
                }
                if (TextUtils.isEmpty(name)) {
                    name = " "
                }
                val rb: ContentRatingSystem.Rating.Builder = ContentRatingSystem.Rating.Builder()
                rb.setName(name)
                rb.setTitle(name)
                //rb.setDescription("description "+i);
                rb.setContentAgeHint(i)
                builder.addRatingBuilder(rb)
                if (flag != 0) {
                    //RatingOrder
                    ob.addRatingName(name)
                }
            }
            if (flag != 0) {
                builder.addOrderBuilder(ob)
            }
            ratingSystems.add(builder.build())
        }
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "rating-system construct success!")
        return ratingSystems
    }

}