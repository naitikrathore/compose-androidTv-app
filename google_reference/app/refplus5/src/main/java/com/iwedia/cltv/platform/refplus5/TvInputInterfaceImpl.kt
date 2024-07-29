package com.iwedia.cltv.platform.refplus5

import android.content.Context
import android.media.tv.ContentRatingSystem
import android.media.tv.TvContentRating
import android.media.tv.TvContentRatingSystemInfo
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import com.iwedia.cltv.platform.base.TvInputInterfaceBaseImpl
import com.iwedia.cltv.platform.`interface`.ParentalControlSettingsInterface
import com.iwedia.cltv.platform.`interface`.TvInputInterface
import com.iwedia.cltv.platform.`interface`.UtilsInterface
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.platform.model.CoroutineHelper
import com.iwedia.cltv.platform.model.TvEvent
import com.iwedia.cltv.platform.model.parental.Region
import com.iwedia.cltv.platform.refplus5.parental.TvConstantsBase
import com.iwedia.cltv.platform.refplus5.parental.TvRrt5Rating
import com.iwedia.cltv.platform.refplus5.parser.ContentRatingsParser
import com.mediatek.dtv.tvinput.framework.tifextapi.atsc.view.rating.MtkTvRRTRatingRegionInfo
import java.lang.reflect.InvocationTargetException
import java.util.Collections
import java.util.Locale
import java.util.stream.Collectors


class TvInputInterfaceImpl(parentalControlSettingsInterface: ParentalControlSettingsInterface, utilsInterface: UtilsInterface, context: Context) : TvInputInterfaceBaseImpl(parentalControlSettingsInterface, utilsInterface, context) {

    private val TAG = "TvInputInterfaceImpl"

    private var mSelectCountry: String? = null

    private val useSystemDomainForEurope = false

    init {
        CoroutineHelper.runCoroutine({
            updateCurrentCountry()
            createContentRatingSystem()
            setDefaultRatingSystemsIfNeeded()
        })
    }

    private fun updateCurrentCountry() {
        val region = utilsInterface.getRegion()
        val country = Settings.Global.getString(context.contentResolver, "M_CURRENT_COUNTRY_REGION")
        if (region == Region.US) {
            mSelectCountry = "US"
        } else if (region == Region.EU || region == Region.PA
        //|| region == Region.CO
        ) {
            mSelectCountry = convertCountry(country)
        } else if (region == Region.SA) {
            mSelectCountry =
                if (TvConstantsBase.S3166_CFG_COUNT_ARG.equals(country)
                    || TvConstantsBase.S3166_CFG_COUNT_PRY.equals(country)
                    || TvConstantsBase.S3166_CFG_COUNT_URY.equals(country)
                ) {
                    "AR"
                } else {
                    "BR"
                }
        }
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
                        val region = utilsInterface.getRegion()
                        val country = Settings.Global.getString(context.contentResolver, "M_CURRENT_COUNTRY_REGION")

                        if(useSystemDomainForEurope) {
                            //Belgium, Italy and Romania must use com.mediatek.dtv domain
                            //com.mediatek.dtv.tvinput.dvbtuner authority is converted to com.mediatek.dtv domain in parser
                            if (country == TvConstantsBase.S3166_CFG_COUNT_BEL ||
                                country == TvConstantsBase.S3166_CFG_COUNT_ITA ||
                                country == TvConstantsBase.S3166_CFG_COUNT_ROU
                            ) {
                                if (info.xmlUri.authority != "com.mediatek.dtv.tvinput.dvbtuner") {
                                    continue
                                }
                            } else {
                                if (((region == Region.US) || (region == Region.EU) || (region == Region.PA)) && (info.xmlUri.authority != context.packageName)) {
                                    continue
                                }
                            }
                        } else {
                            if(((region == Region.EU) || (region == Region.PA))&& (info.xmlUri.authority != "com.mediatek.dtv.tvinput.dvbtuner")) {
                                continue
                            }
                            if((region == Region.US) && (info.xmlUri.authority != context.packageName)) {
                                continue
                            }
                        }

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

    /** Returns a new list of all content rating systems defined.  */
    override fun getContentRatingSystems(): List<ContentRatingSystem> {
        Collections.sort(mContentRatingSystems, ContentRatingSystem.DISPLAY_NAME_COMPARATOR)
        val items: MutableList<ContentRatingSystem> = ArrayList()
        val itemsHidden: MutableList<ContentRatingSystem> = ArrayList()
        val itemsHiddenMultipleCountries: MutableList<ContentRatingSystem> = ArrayList()
        var hasMappingCountries = false
        val region = utilsInterface.getRegion()
        var othersFound = false
        // Add default, custom and preselected content rating systems to the "short" list.
        for (s in mContentRatingSystems) {
            if (s.countries != null && s.countries.contains(mSelectCountry)) {
                if(hasMappingCountries) {
                    continue
                }
                items.add(s)
                hasMappingCountries = true
            } else if (s.isCustom) {
                items.add(s)
            } else if(region==Region.EU) {
                if((s.name == "FR_DVB") || (s.name == "ES_DVB") || (s.name == "DVB") || (s.name == "IT_DVB") ||
                    (s.name == "BE_DVB") || (s.name == "RO_DVB")) {
                    items.add(s)
                }
            } else
            {
                val countries = s.countries
                if (countries.size > 2) {
                    if(othersFound) {
                        continue
                    }
                    itemsHiddenMultipleCountries.add(s)
                    othersFound = true
                } else {
                    itemsHidden.add(s)
                }
            }

        }
        if (!hasMappingCountries) {
            items.addAll(itemsHiddenMultipleCountries)
        }
        return items
    }

    override fun getContentRatingSystemsList(): MutableList<ContentRatingSystem> {
        val items: MutableList<ContentRatingSystem> = ArrayList()

        Collections.sort(mContentRatingSystems, ContentRatingSystem.DISPLAY_NAME_COMPARATOR)
        val region = utilsInterface.getRegion()

        for (s in mContentRatingSystems) {
            if(region==Region.EU) {
                if ((s.name == "FR_DVB") || (s.name == "ES_DVB") || (s.name == "DVB") || (s.name == "IT_DVB") ||
                    (s.name == "BE_DVB") || (s.name == "RO_DVB")
                ) {
                    items.add(s)
                }
            }
            if(region==Region.US) {
                if ((s.name == "US_TV") || (s.name == "US_MV") || (s.name == "CA_TV_EN") || (s.name == "CA_TV_FR")) {
                    items.add(s)
                }
            }
            if (region == Region.PA) {
                when (mSelectCountry) {
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

    private fun setDefaultRatingSystemsIfNeeded(){
        val settings: ParentalControlSettingsInterface = parentalControlSettingsInterface

        if (settings.isContentRatingSystemSet(context)) {
            return
        }

        // Sets the default if the content rating system has never been set.
        val manager: TvInputInterface = this
        var otherCountries: ContentRatingSystem? = null
        var hasMappingCountries = false
        for (s in getContentRatingSystemsList()) {
            if (!s.isCustom && s.countries != null && s.countries.contains(mSelectCountry)) {
                settings.setContentRatingSystemEnabled(manager, s, true)
                hasMappingCountries = true
            }
            if (!s.isCustom && s.countries != null && s.countries.size > 2) {
                otherCountries = s
            }
        }
        if (!hasMappingCountries && otherCountries != null) {
            settings.setContentRatingSystemEnabled(manager, otherCountries, true)
        }
    }

    override fun getContentRatingSystemDisplayName(contentRatingSystem: ContentRatingSystem) :String{
        val mCountries = contentRatingSystem.countries
        val sb = StringBuilder()

        if (mCountries != null) {
            if (mCountries.size == 1) {
                val currentCountry: String = convertCountry(Settings.Global.getString(context.contentResolver, "M_CURRENT_COUNTRY_REGION"))
                sb.append(Locale("", mCountries[0]).displayCountry)
            } else if (mCountries.size > 1) {
                val locale = Locale.getDefault()
                val country: String = Settings.Global.getString(context.contentResolver, "M_CURRENT_COUNTRY_REGION")
                val currentCountry: String = convertCountry(country)
                if (mCountries.contains(currentCountry)) {
                    // Shows the country name instead of "Other countries" if the current
                    // country is one of the countries this rating system applies to.
                    if (utilsInterface.getRegion() == Region.US && "US" == locale.country && mCountries.contains("CA")) {
                        val index: Int = mCountries.indexOf("CA")
                        sb.append(Locale("", mCountries[index]).displayCountry)
                    } else {
                        sb.append(Locale("", currentCountry).displayCountry)
                    }
                } else {
                    sb.append(utilsInterface.getStringValue("other_countries"))
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

    override fun getParentalRatingDisplayName(parentalRating: String?, tvEvent: TvEvent): String {
        var rating = parentalRating
        if (rating.isNullOrEmpty()) {
            val mtkChannelRating = if(!tvEvent.tvChannel.isFastChannel()) (utilsInterface as UtilsInterfaceImpl).getMtkChannelRating(tvEvent.tvChannel) else ""
            if(mtkChannelRating.isNotEmpty()) rating = mtkChannelRating else return ""
        }

        val displayNames = mutableListOf<String>()
        rating.split(",").forEach {
            val displayName = getParentalRating(it, tvEvent)
            if (!displayNames.contains(displayName)) {
                displayNames.add(displayName)
            }
        }
        return displayNames.joinToString("/")
    }

    private fun getParentalRating(parentalRating: String, tvEvent: TvEvent): String {
        val tvContentRating = try {
            TvContentRating.unflattenFromString(parentalRating)
        } catch (_: IllegalArgumentException) {
            // For invalid rating
            return ""
        }

        getContentRatingSystemsList().forEach { ratingSystem ->
            // Comparing rating system
            if (ratingSystem.name == tvContentRating.ratingSystem) {
                ratingSystem.ratings.forEach { rating ->
                    // Comparing rating
                    if (rating.name == tvContentRating.mainRating) {
                        val ratingTitle =
                            if (ratingSystem.name == "US_MV") "MV-${rating.title}"
                            else utilsInterface.getStringValue((rating.name).lowercase().replace("-", "_"))
                        val sb = StringBuilder(ratingTitle)
                        if (!tvContentRating.subRatings.isNullOrEmpty()) {
                            tvContentRating.subRatings.forEach {
                                rating.subRatings.forEach { subRating ->
                                    // Comparing sub rating
                                    if (it == subRating.name) {
                                        sb.append("-")
                                        val subRatingTitle = utilsInterface.getStringValue((subRating.name).lowercase().replace("-","_"))
                                        sb.append(subRatingTitle)
                                    }
                                }
                            }
                        }
                        return sb.toString()
                    }
                }
            }
        }

        // If rating is not defined
        var rating = tvContentRating.mainRating.split("_").last().trim()
        if (rating.contains("RRT") && !tvContentRating.subRatings.isNullOrEmpty()) {

            if (tvEvent.rrt5Rating.isNotEmpty()) {
                var rrt5RatingString = ""
                try {
                    rrt5RatingString = tvEvent.rrt5Rating
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                if (rrt5RatingString.isEmpty()) {
                    if (tvContentRating.subRatings != null && tvContentRating.subRatings.size == 1) {
                        try {
                            rrt5RatingString = tvEvent.rrt5Rating
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        if (rrt5RatingString.isEmpty()) {
                            rrt5RatingString = tvContentRating.subRatings[0]
                        }

                        return rrt5RatingString.filter { !it.isDigit() }
                    }
                }
                return rrt5RatingString.replace("/", "-")
            } else {
                val subRatingSb = StringBuilder()
                tvContentRating.subRatings.forEach {
                    if (subRatingSb.isNotEmpty()) subRatingSb.append("-")
                    subRatingSb.append(it.split("\u0005").last().trim())
                }
                rating = subRatingSb.toString()
            }
        }
        return if (rating == "null") ""
        else if (rating != "U" && rating != "0") {
            if (tvContentRating.mainRating.contains("US_MV")) {
                if (rating == "NA") "MV-N/A"
                else "MV-$rating"
            }
            else if (rating == "NONE") "None"
            else if (rating == "RRT") "Not Rated"
            else rating
        }
        else ""
    }

    private fun convertCountry(country: String): String {
        val conList = mapOf(
            TvConstantsBase.S3166_CFG_COUNT_AUS to "AU",
            TvConstantsBase.S3166_CFG_COUNT_BEL to "BE",
            TvConstantsBase.S3166_CFG_COUNT_CHE to "CH",
            TvConstantsBase.S3166_CFG_COUNT_CZE to "CS",
            TvConstantsBase.S3166_CFG_COUNT_DEU to "DE",
            TvConstantsBase.S3166_CFG_COUNT_DNK to "DN",
            TvConstantsBase.S3166_CFG_COUNT_ESP to "ES",
            TvConstantsBase.S3166_CFG_COUNT_FIN to "FI",
            TvConstantsBase.S3166_CFG_COUNT_FRA to "FR",
            TvConstantsBase.S3166_CFG_COUNT_GBR to "GB",
            TvConstantsBase.S3166_CFG_COUNT_ITA to "IT",
            TvConstantsBase.S3166_CFG_COUNT_LUX to "LU",
            TvConstantsBase.S3166_CFG_COUNT_NLD to "NL",
            TvConstantsBase.S3166_CFG_COUNT_NOR to "NO",
            TvConstantsBase.S3166_CFG_COUNT_SWE to "SE",
            TvConstantsBase.S3166_CFG_COUNT_HRV to "HR",
            TvConstantsBase.S3166_CFG_COUNT_GRC to "GR",
            TvConstantsBase.S3166_CFG_COUNT_HUN to "HU",
            TvConstantsBase.S3166_CFG_COUNT_IRL to "IE",
            TvConstantsBase.S3166_CFG_COUNT_POL to "PL",
            TvConstantsBase.S3166_CFG_COUNT_PRT to "PT",
            TvConstantsBase.S3166_CFG_COUNT_ROU to "RO",
            TvConstantsBase.S3166_CFG_COUNT_RUS to "RU",
            TvConstantsBase.S3166_CFG_COUNT_SRB to "SR",
            TvConstantsBase.S3166_CFG_COUNT_SVK to "SK",
            TvConstantsBase.S3166_CFG_COUNT_SVN to "SI",
            TvConstantsBase.S3166_CFG_COUNT_TUR to "TR",
            TvConstantsBase.S3166_CFG_COUNT_EST to "EE",
            TvConstantsBase.S3166_CFG_COUNT_UKR to "UA",
            TvConstantsBase.S3166_CFG_COUNT_THA to "TH",
            TvConstantsBase.S3166_CFG_COUNT_ZAF to "ZA",
            TvConstantsBase.S3166_CFG_COUNT_SQP to "SG",
            TvConstantsBase.S3166_CFG_COUNT_ARG to "AR",
            TvConstantsBase.S3166_CFG_COUNT_BRA to "BR",
            TvConstantsBase.S3166_CFG_COUNT_CAN to "CA",
            TvConstantsBase.S3166_CFG_COUNT_JPN to "JP",
            TvConstantsBase.S3166_CFG_COUNT_NZL to "NZ",
            TvConstantsBase.S3166_CFG_COUNT_MYS to "MY",
            TvConstantsBase.S3166_CFG_COUNT_IDN to "ID",
            TvConstantsBase.S3166_CFG_COUNT_VNM to "VN",
            TvConstantsBase.S3166_CFG_COUNT_MMR to "MM",
            TvConstantsBase.S3166_CFG_COUNT_IND to "IN"
        )

        val simCon: String? = conList[country]
        return if (TextUtils.isEmpty(simCon)) country else simCon!!
    }
}