package com.iwedia.cltv.platform.rtk

import android.content.Context
import android.media.tv.ContentRatingSystem
import android.media.tv.TvContentRating
import android.media.tv.TvContentRatingSystemInfo
import android.text.TextUtils
import android.util.Log
import com.iwedia.cltv.platform.base.TvInputInterfaceBaseImpl
import com.iwedia.cltv.platform.`interface`.FastDataProviderInterface
import com.iwedia.cltv.platform.`interface`.ParentalControlSettingsInterface
import com.iwedia.cltv.platform.`interface`.UtilsInterface
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.platform.model.TvEvent
import com.iwedia.cltv.platform.model.parental.Region
import com.iwedia.cltv.platform.rtk.parser.ContentRatingsParser
import java.lang.reflect.InvocationTargetException
import java.util.Collections
import java.util.Locale

class TvInputInterfaceImpl(parentalControlSettingsInterface: ParentalControlSettingsInterface, utilsInterface: UtilsInterface, context: Context,private var  fastDataProviderInterface: FastDataProviderInterface) : TvInputInterfaceBaseImpl(parentalControlSettingsInterface, utilsInterface, context) {
    /** Returns a new list of all content rating systems defined.  */

    val TAG = javaClass.simpleName

    private var selectedCountry: String? = null

    init {
        updateCurrentCountry()
        createContentRatingSystem()
    }

    override fun getParentalRatingDisplayName(parentalRating: String?, tvEvent: TvEvent): String {
        val sb = StringBuilder("")
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "[getParentalRatingDisplayName] $parentalRating")
        parentalRating?.split(",")?.forEach {
            if (sb.isNotEmpty()) {
                sb.append(",")
            }
            sb.append(getParentalRating(it))

        }
        return sb.toString()
    }
    override fun getContentRatingSystemDisplayName(contentRatingSystem: ContentRatingSystem) :String{
        val mCountries = contentRatingSystem.countries
        val sb = StringBuilder()
        if(mCountries.contains("US")) {
            if (mCountries.size == 1) {
                sb.append(Locale( "", mCountries[0]).displayCountry)
            } else {
                sb.append("other_countries")
            }
        }
        else {
            if (mCountries != null) {
                if (mCountries.size == 1) {
                    sb.append(Locale("", mCountries[0]).displayCountry)
                } else if (mCountries.size > 1) {
                    val country: String = utilsInterface.getCountryCode()
                    if (mCountries.contains(country)) {
                        sb.append(Locale("", country).displayCountry)
                    } else {
                        sb.append(utilsInterface.getStringValue("other_countries"))
                    }
                }
            }
        }
        val title = contentRatingSystem.title
        if (!TextUtils.isEmpty(title)) {
            sb.append(" (")
            sb.append(title)
            sb.append(")")
        }
        return sb.toString()
    }

    override fun getContentRatingSystemsList(): MutableList<ContentRatingSystem> {
        val items: MutableList<ContentRatingSystem> = ArrayList()
        Collections.sort(mContentRatingSystems, ContentRatingSystem.DISPLAY_NAME_COMPARATOR)
        val region = utilsInterface.getRegion()

        for (s in mContentRatingSystems) {
            if(region==Region.EU) {
                val sNameToBeSearched = selectedCountry + "_DVB"
                if((s.name == "DVB") || (s.name == sNameToBeSearched)) {
                    items.add(s)
                }
            }
            else if(region==Region.US) {
                if (s.countries.contains("US")) {
                    items.add(s)
                }
            }
            if (region == Region.PA) {
                when (selectedCountry) {
                    "TH" -> {
                        if (s.name == "TH_TV") {
                            items.add(s)
                        }
                    }
                    "ID", "IN", "MY", "AE", "MM", "LT", "GH", "LV", "VN" -> {
                        if (s.name == "DVB") {
                            items.add(s)
                        }
                    }
                    "SG" -> {
                        if (s.name == "SG_TV") {
                            items.add(s)
                        }
                    }
                    "TW" -> {
                        if (s.name == "TW_TV") {
                            items.add(s)
                        }
                    }
                    "AU" -> {
                        if (s.name == "AU_TV") {
                            items.add(s)
                        }
                    }
                    "NZ" -> {
                        if (s.name == "NZ_TV") {
                            items.add(s)
                        }
                    }
                }
            }
        }
        return items
    }

    override fun getContentRatingSystems(): List<ContentRatingSystem> {
        var hasMappingCountries = false
        val itemsHiddenMultipleCountries: MutableList<ContentRatingSystem> = ArrayList()
        val items: MutableList<ContentRatingSystem> = ArrayList()
        val region = utilsInterface.getRegion()
        for (s in mContentRatingSystems) {
            if (s.countries != null && s.countries.contains("US")) {
                items.add(s)
                hasMappingCountries = true
            } else if(s.countries != null && region==Region.EU) {
                items.add(s)
            }
            else if(s.countries != null && region==Region.PA) {
                items.add(s)
            } else {
                val countries = s.countries
                if (countries.size > 2) {
                    itemsHiddenMultipleCountries.add(
                        s
                    )
                }
            }
        }
        if (!hasMappingCountries) {
            items.addAll(itemsHiddenMultipleCountries)
        }
        return items
    }

    private fun createContentRatingSystem() {
        mContentRatingSystems.clear()
        if (hasReadContentRatingSystem(context)) {
            val parser = ContentRatingsParser(context)
            val cls = getTvInputManager().javaClass
            try {
                val getTvContentRatingSystemList =
                    cls.getDeclaredMethod("getTvContentRatingSystemList")
                try {
                    val infos =
                        getTvContentRatingSystemList.invoke(getTvInputManager()) as List<TvContentRatingSystemInfo>
                    for (info in infos) {
                        val list = parser.parse(info)
                        if (list != null) {
                            mContentRatingSystems.addAll(list)
                        }
                    }
                    mContentRatingSystems.forEach {
                        it.displayName = getContentRatingSystemDisplayName(it)
                    }
                } catch (e: IllegalAccessException) {
                    e.printStackTrace()
                } catch (e: InvocationTargetException) {
                    e.printStackTrace()
                }
            } catch (e: NoSuchMethodException) {
                e.printStackTrace()
            }
        }
    }

    private fun updateCurrentCountry() {
        val region = utilsInterface.getRegion()
        val country = utilsInterface.getCountryCode()
        if (region == Region.US) {
            selectedCountry = "US"
        }
        else {
            selectedCountry = country
        }
    }

    private fun getParentalRating(ratingsString: String): Any {
        val tvContentRating = try {
            TvContentRating.unflattenFromString(ratingsString)
        } catch (e: Exception) {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, " exception while unflattenFromString")
            return ""
        }
        getContentRatingSystemsList().forEach { ratingSystem ->
            // Comparing rating system
            if (ratingSystem.domain == tvContentRating.domain && ratingSystem.name == tvContentRating.ratingSystem) {
                ratingSystem.ratings.forEach { rating ->
                    // Comparing rating
                    if (rating.name == tvContentRating.mainRating) {
                        val rating_title = utilsInterface.getStringValue("${(rating.name).lowercase().replace("-","_")}")
                        val sb = StringBuilder(rating_title)
                        if (!tvContentRating.subRatings.isNullOrEmpty()) {
                            tvContentRating.subRatings.forEach {
                                rating.subRatings.forEach { subRating ->
                                    // Comparing sub rating
                                    if (it == subRating.name) {
                                        sb.append(",")
                                        val subrating_title = utilsInterface.getStringValue("${(subRating.name).lowercase().replace("-","_")}")
                                        sb.append(subrating_title)
                                    }
                                }
                            }
                        }
                        return sb.toString()
                    }
                }
            }
        }
        val rating = tvContentRating.mainRating.split("_").last().trim()
        return if (rating == "null") ""
        else if (rating != "U" && rating != "0") rating
        else ""
    }
}