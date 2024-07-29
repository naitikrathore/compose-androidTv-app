package com.iwedia.cltv.platform.base

import android.content.ActivityNotFoundException
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.media.tv.*
import android.os.Handler
import android.text.TextUtils
import com.iwedia.cltv.platform.`interface`.TvInputInterface
import com.iwedia.cltv.platform.`interface`.ParentalControlSettingsInterface
import com.iwedia.cltv.platform.`interface`.UtilsInterface
import com.iwedia.cltv.platform.model.IAsyncCallback
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.TvEvent
import com.iwedia.cltv.platform.model.information_bus.events.Events
import com.iwedia.cltv.platform.model.information_bus.events.InformationBus
import java.lang.reflect.InvocationTargetException
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

open class TvInputInterfaceBaseImpl(val parentalControlSettingsInterface: ParentalControlSettingsInterface, val utilsInterface: UtilsInterface, val context: Context): TvInputInterface {
    /**
     * TV input hash map
     */
    var inputMap: HashMap<String, TvInputInfo?>? = null

    /**
     * Tv input manager
     */
    private var tvInputManager: TvInputManager? = null

    /**
     * Tv input scan callback
     */
    var scanCallback: IAsyncCallback? = null

    /**
     * content rating system list
     */
    protected val mContentRatingSystems = mutableListOf<ContentRatingSystem>()

    init {
        tvInputManager = context!!.getSystemService(Context.TV_INPUT_SERVICE) as TvInputManager
        inputMap = HashMap()
        for (input in tvInputManager!!.tvInputList) {
            val inputId = input.id
            inputMap!![inputId] = input
        }

        var tvInputCallback: TvInputManager.TvInputCallback =
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
        tvInputManager!!.registerCallback(tvInputCallback, Handler())
        mContentRatingSystems.clear()
        if (hasReadContentRatingSystem(context)) {

            val parser = ContentRatingsParser(context)
            val cls = tvInputManager!!.javaClass
            try {
                val getTvContentRatingSystemList =
                    cls.getDeclaredMethod("getTvContentRatingSystemList")
                try {
                    val infos =
                        getTvContentRatingSystemList.invoke(tvInputManager) as List<TvContentRatingSystemInfo>
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

    override fun getTvInputManager(): TvInputManager {
        return tvInputManager!!
    }

    override fun getTvInputList(callback: IAsyncDataCallback<ArrayList<TvInputInfo>>) {
        val tvInputInfoList: ArrayList<TvInputInfo> = arrayListOf()

        inputMap?.let {
            for (inputInfo in inputMap!!.values) {
                if (tvInputManager!!.getInputState(inputInfo!!.id) == TvInputManager.INPUT_STATE_CONNECTED) {
                    tvInputInfoList.add(inputInfo)
                }
            }
        }
        callback.onReceive(tvInputInfoList)
    }

    override fun getTvInputFilteredList(filter: String, callback: IAsyncDataCallback<ArrayList<TvInputInfo>>) {
        val tvInputInfoList: ArrayList<TvInputInfo> = arrayListOf()

        inputMap?.let {
            for (inputInfo in inputMap!!.values) {
                if (tvInputManager!!.getInputState(inputInfo!!.id) == TvInputManager.INPUT_STATE_CONNECTED) {
                    if (!inputInfo.id.contains(filter))
                        tvInputInfoList.add(inputInfo)
                }
            }
        }

        callback.onReceive(tvInputInfoList)
    }

    override fun startSetupActivity(input: TvInputInfo, callback: IAsyncCallback) {
        val intent = input.createSetupIntent()
        if (intent == null) {
            callback.onFailed(
                Error("Can not create intent for input " + input.loadLabel(context!!))
            )
            return
        }
        try {
            //Start setup activity for certain tv input
            scanCallback = callback
            InformationBus.informationBusEventListener.submitEvent(Events.START_TV_INPUT_SETUP_ACTIVITY, arrayListOf(intent))
        } catch (e: ActivityNotFoundException) {
            scanCallback = null
            callback.onFailed(
                Error("Can not start setup activity for input " + input.loadLabel(context!!)
                )
            )
            return
        }
    }

    override fun triggerScanCallback(isSuccessful: Boolean) {
        if (scanCallback != null) {
            if (isSuccessful) {
                scanCallback?.onSuccess()
            } else {
                scanCallback?.onFailed(
                    Error("Tv input scan failed ")
                )
            }
            scanCallback = null
        }
    }

    override fun getChannelCountForInput(input: TvInputInfo, callback: IAsyncDataCallback<Int>) {
        val contentResolver: ContentResolver = context.contentResolver
        var cursor = contentResolver.query(
            TvContract.buildChannelsUriForInput(input.id),
            null,
            null,
            null
        )
        var count = cursor?.count ?: 0
        callback.onReceive(count)
    }

    override fun isParentalEnabled(): Boolean {
        return tvInputManager!!.isParentalControlsEnabled
    }

    protected fun hasReadContentRatingSystem(mContext: Context): Boolean {
        return (mContext.checkSelfPermission(
            "android.permission.READ_CONTENT_RATING_SYSTEMS"
        ) == PackageManager.PERMISSION_GRANTED)
    }

    /** Returns a new list of all content rating systems defined.  */
    override fun getContentRatingSystems(): List<ContentRatingSystem> {
        var hasMappingCountries = false
        val itemsHiddenMultipleCountries: MutableList<ContentRatingSystem> = ArrayList()
        val items: MutableList<ContentRatingSystem> = ArrayList()
        for (s in mContentRatingSystems) {
            if (s.countries != null && s.countries.contains("US")) {
                items.add(s)
                hasMappingCountries = true
            } else if (s.isCustom) {
                // items.add(s);
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

    override fun getContentRatingSystemsList(): MutableList<ContentRatingSystem> {
        setDefaultRatingSystemsIfNeeded()
        var hasMappingCountries = false
        val itemsHiddenMultipleCountries: MutableList<ContentRatingSystem> = ArrayList()
        val items: MutableList<ContentRatingSystem> = ArrayList()
        for (s in mContentRatingSystems) {
            if (s.countries != null && s.countries.contains("US")) {
                items.add(s)
                if (parentalControlSettingsInterface.isContentRatingSystemEnabled(s)) {
                    parentalControlSettingsInterface.setContentRatingSystemEnabled(this, s, true)
                }
                hasMappingCountries = true
            } else if (s.isCustom) {
                //items.add(s);
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

    private fun setDefaultRatingSystemsIfNeeded(){
        val settings: ParentalControlSettingsInterface = parentalControlSettingsInterface
        if (settings.isContentRatingSystemSet(context)) {
            return
        }
        // Sets the default if the content rating system has never been set.
        val manager: TvInputInterface = this
        var otherCountries: ContentRatingSystem? = null
        var hasMappingCountries = false
        for (s in getContentRatingSystems()) {
            if (s.countries != null && s.countries.contains("US")) {
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
        val countries = contentRatingSystem.countries

        val sb = StringBuilder()
        if (countries.size == 1 && countries.contains("US")) {
            sb.append(Locale( "", countries.get(0)).getDisplayCountry())
        } else {
            sb.append("other_countries")
        }

        val title = contentRatingSystem.title
        if (!TextUtils.isEmpty(title)) {
            sb.append(" (")
            sb.append(title)
            sb.append(")")
        }
        return sb.toString()
    }

    /**
     * Generate rating display name with sub ratings for an event.
     *
     * @param parentalRating Flattened rating string
     * @return Rating display name. Example: PV-14
     */
    override fun getParentalRatingDisplayName(parentalRating: String?, tvEvent: TvEvent): String {
        if (parentalRating.isNullOrEmpty()) return ""

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
                        val rating_title = utilsInterface.getStringValue("${(rating.name).lowercase().replace("-","_")}")
                        val sb = StringBuilder(rating_title)
                        if (!tvContentRating.subRatings.isNullOrEmpty()) {
                            tvContentRating.subRatings.forEach {
                                rating.subRatings.forEach { subRating ->
                                    // Comparing sub rating
                                    if (it == subRating.name) {
                                        sb.append("-")
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

        // If rating is not defined
        val rating = tvContentRating.mainRating.split("_").last().trim()
        return if (rating == "null") ""
        else if (rating != "U" && rating != "0") rating
        else ""
    }
}