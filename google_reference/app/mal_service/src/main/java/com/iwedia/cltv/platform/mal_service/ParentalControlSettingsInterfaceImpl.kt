package com.iwedia.cltv.platform.mal_service

import android.content.Context
import android.media.tv.ContentRatingSystem
import android.media.tv.TvContentRating
import android.media.tv.TvInputManager
import android.os.Handler
import android.os.Looper
import com.cltv.mal.IServiceAPI
import com.cltv.mal.interfaces.ITvInputInterface
import com.cltv.mal.model.content_rating.ContentRatingSystemRating
import com.cltv.mal.model.content_rating.ContentRatingSystemSubRating
import com.iwedia.cltv.platform.`interface`.ParentalControlSettingsInterface
import com.iwedia.cltv.platform.`interface`.TvInputInterface
import com.iwedia.cltv.platform.`interface`.UtilsInterface
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.platform.model.TvEvent
import com.iwedia.cltv.platform.model.parental.InputSourceData

class ParentalControlSettingsInterfaceImpl(private val context: Context, private val serviceImpl: IServiceAPI) :
    ParentalControlSettingsInterface {
    private var mTvInputManager: TvInputManager = context
        .getSystemService(Context.TV_INPUT_SERVICE) as TvInputManager
    private var cls: Class<*>? = null
    init {
        cls = mTvInputManager.javaClass
        if(context.getSharedPreferences(UtilsInterface.PREFS_TAG, Context.MODE_PRIVATE).getInt(
                Constants.SharedPrefsConstants.ANOKI_TEMPORARY_RATING_ENABLED_TAG, 0) != 0) {
            context.getSharedPreferences(UtilsInterface.PREFS_TAG, Context.MODE_PRIVATE).edit()
                .putInt(Constants.SharedPrefsConstants.ANOKI_TEMPORARY_RATING_ENABLED_TAG, 0)
                .apply()

            context.getSharedPreferences(UtilsInterface.PREFS_TAG, Context.MODE_PRIVATE).edit()
                .putInt(Constants.SharedPrefsConstants.ANOKI_TEMPORARY_RATING_LEVEL_TAG, 0)
                .apply()
        }
    }
    override fun setTvInputInterface(tvInputInterface: TvInputInterface) {
        Handler(Looper.getMainLooper()).post {
            //serviceImpl.setTvInputInterface(tvInputInterface as ITvInputInterface)
        }

    }

    override fun isParentalControlsEnabled(): Boolean {
        return serviceImpl.isParentalControlsEnabled
    }

    override fun setParentalControlsEnabled(enabled: Boolean) {
        serviceImpl.setParantalControlsEnabled(enabled)
    }

    override fun isAnokiParentalControlsEnabled(): Boolean {
        return serviceImpl.isAnokiParentalControlsEnabled
    }

    override fun setAnokiParentalControlsEnabled(enabled: Boolean) {
        serviceImpl.isAnokiParentalControlsEnabled = enabled

        // Set parental control enabled shared prefs value Anoki parental case
        context.getSharedPreferences(UtilsInterface.PREFS_TAG, Context.MODE_PRIVATE).edit()
            .putBoolean(Constants.SharedPrefsConstants.PARENTAL_CONTROLS_ENABLED_TAG, enabled)
            .apply()
        if (enabled) {
            var anokiRatingList = serviceImpl.anokiRatingList
            anokiRatingLevel =
                context.getSharedPreferences(UtilsInterface.PREFS_TAG, Context.MODE_PRIVATE)
                    .getInt(Constants.SharedPrefsConstants.ANOKI_PARENTAL_CONTROLS_LEVEL_TAG, anokiRatingList.size - 1)
        }
    }

    override fun setContentRatingSystemEnabled(
        tvInputInterface: TvInputInterface,
        contentRatingSystem: ContentRatingSystem,
        enabled: Boolean
    ) {
        serviceImpl.setTvContentRatingSystemEnabled(
            toTvContentRatingString(contentRatingSystem,null,null),
            enabled
        )
    }

    override fun isContentRatingSystemEnabled(contentRatingSystem: ContentRatingSystem): Boolean {
        return serviceImpl.isTvContentRatingSystemEnabled(
            toTvContentRatingString(contentRatingSystem,null,null)
        )
    }

    override fun hasContentRatingSystemSet(): Boolean {
        return serviceImpl.hasContentRatingSystemSet()
    }

    override fun loadRatings() {
        serviceImpl.loadRatings()
    }

    override fun getRatings(): MutableSet<TvContentRating> {
        var result = mutableSetOf<TvContentRating>()
        serviceImpl.ratings.forEach {
            result.add(fromServiceContentRating(it))
        }
        return result
    }

    override fun clearRatingBeforeSetRating() {
        serviceImpl.clearRatingBeforeSetRating()
    }

    override fun setContentRatingLevel(tvInputInterface: TvInputInterface?, level: Int) {
        serviceImpl.setContentRatingLevelWithTvInputInterface(
            level
        )
    }

    override fun setContentRatingLevel(context: Context?, level: Int) {
        serviceImpl.contentRatingLevel = level
    }

    override fun setRatingBlocked(
        contentRatingSystem: ContentRatingSystem,
        rating: ContentRatingSystem.Rating,
        blocked: Boolean
    ): Boolean {
        return serviceImpl.setTvContentRatingBlocked(toTvContentRatingString(contentRatingSystem,rating,null),blocked)
    }

    override fun isRatingBlocked(ratings: Array<TvContentRating?>?): Boolean {
        var ratingList = arrayListOf<com.cltv.mal.model.content_rating.TvContentRating>()
        ratings?.forEach {
            ratingList.add(toServiceContentRating(it!!))
        }
        return serviceImpl.isRatingBlocked(ratingList.toTypedArray())
    }

    override fun isRatingBlocked(
        contentRatingSystem: ContentRatingSystem,
        rating: ContentRatingSystem.Rating
    ): Boolean {
        return serviceImpl.isTvContentRatingBlocked(toTvContentRatingString(contentRatingSystem,rating))
    }

    override fun setSubRatingBlocked(
        contentRatingSystem: ContentRatingSystem,
        rating: ContentRatingSystem.Rating,
        subRating: ContentRatingSystem.SubRating?,
        blocked: Boolean
    ): Boolean {
        return serviceImpl.setTvContentRatingBlocked(toTvContentRatingString(contentRatingSystem,rating,subRating),blocked)
    }

    override fun isSubRatingEnabled(
        contentRatingSystem: ContentRatingSystem,
        rating: ContentRatingSystem.Rating,
        subRating: ContentRatingSystem.SubRating
    ): Boolean {
        return serviceImpl.isTvContentRatingBlocked(toTvContentRatingString(contentRatingSystem,rating,subRating))
    }

    override fun getBlockedStatus(
        contentRatingSystem: ContentRatingSystem,
        rating: ContentRatingSystem.Rating
    ): Int {
        var subRatings = mutableListOf<ContentRatingSystemSubRating>()
        rating.subRatings.forEach {
            subRatings.add(ContentRatingSystemSubRating(it.name, it.title, it.description, it.icon))
        }
        var contentRatingSystemRating = ContentRatingSystemRating(
            rating.name,
            rating.title,
            rating.description,
            rating.icon,
            rating.ageHint,
            subRatings
        )
        return serviceImpl.getBlockedStatus(
            toServiceContentRatingSystem(contentRatingSystem),
            contentRatingSystemRating
        )
    }

    override fun setRelativeRatingsEnabled(
        contentRatingSystem: ContentRatingSystem,
        selectRating: ContentRatingSystem.Rating?,
        enabled: Boolean
    ) {
        serviceImpl.setTvContentRatingRelativeRatingsEnabled(
            toTvContentRatingString(contentRatingSystem, selectRating!!),
            enabled
        )
    }

    override fun setRelativeRating2SubRatingEnabled(
        contentRatingSystem: ContentRatingSystem,
        enabled: Boolean,
        relativeRating: ContentRatingSystem.Rating?,
        subRating: ContentRatingSystem.SubRating
    ) {
        serviceImpl.setTvContentRatingRelativeRating2SubRatingEnabled(
            toTvContentRatingString(contentRatingSystem,relativeRating,subRating),
            enabled,
        )
    }

    override fun removeContentRatingSystem(context: Context?) {
        serviceImpl.removeContentRatingSystem()
    }

    override fun isContentRatingSystemSet(context: Context?): Boolean {
        return serviceImpl.isContentRatingSystemSet
    }

    override fun getContentRatingLevel(context: Context?): Int {
        return serviceImpl.contentRatingLevel
    }

    override fun getContentRatingLevelIndex(): Int {
        return serviceImpl.contentRatingLevelIndex
    }

    override fun getGlobalRestrictionValue(value: Int): String {
        return serviceImpl.getGlobalRestrictionValue(value)
    }

    override fun getGlobalRestrictionsArray(): MutableList<String> {
        var list = mutableListOf<String>()
        serviceImpl.globalRestrictionsArray.forEach {
            list.add(it)
        }
        return list
    }

    override fun getRatingsPerRatingLevelMap(): HashMap<String?, String?> {
        var map = HashMap<String?, String?>()
        map.putAll(serviceImpl.ratingsPerRatingLevelMap)
        return map
    }

    override fun getRatingsSubratingsPerRatingLevelMap(): HashMap<String?, String?> {
        var map = HashMap<String?, String?>()
        map.putAll(serviceImpl.ratingsSubratingsPerRatingLevelMap)
        return map
    }

    override fun blockTvInputCount(blockedInputs: MutableList<InputSourceData>): Int {
        return serviceImpl.blockTvInputCount()
    }

    override fun blockInput(selected: Boolean, item: InputSourceData) {
        serviceImpl.blockInput(
            selected,
            toServiceInputSourceData(item)
        )
    }

    override fun isBlockSource(hardwareId: Int): Boolean {
        return serviceImpl.isBlockSource(hardwareId)
    }

    override fun getBlockUnrated(): Int {
        return serviceImpl.blockUnrated
    }

    override fun setBlockUnrated(isBlockUnrated: Boolean) {
        serviceImpl.setBlockUnrated(isBlockUnrated)
    }

    override fun getRRT5Regions(): MutableList<String> {
        var list = mutableListOf<String>()
        list.addAll(serviceImpl.rrT5Regions)
        return list
    }

    override fun getRRT5Dim(index: Int): MutableList<String> {
        var list = mutableListOf<String>()
        list.addAll(serviceImpl.getRRT5Dim(index))
        return list
    }

    override fun getRRT5CrsInfo(regionName: String): MutableList<ContentRatingSystem> {
        return mutableListOf()
    }

    override fun getRRT5Level(countryIndex: Int, dimIndex: Int): MutableList<String> {
        var list = mutableListOf<String>()
        list.addAll(
            serviceImpl.getRRT5Level(
                countryIndex,
                dimIndex
            )
        )
        return list
    }

    override fun getSelectedItemsForRRT5Level(): HashMap<Int, Int> {
        var result = hashMapOf<Int, Int>()
        /*serviceImpl.selectedItemsForRRT5Level.forEachIndexed { index, ints ->
            result.put(index, ints[0])
        }*/
        return result
    }

    override fun rrt5BlockedList(regionPosition: Int, position: Int): HashMap<Int, Int> {
        return HashMap()
        //TODO return hash map from service
       /* var result = hashMapOf<Int, Int>()
        serviceImpl.rrt5BlockedList(regionPosition, position)
            .forEachIndexed { index, ints ->
                result.put(index, ints[0])
            }
        return result*/
    }

    override fun setSelectedItemsForRRT5Level(regionIndex: Int, dimIndex: Int, levelIndex: Int) {
        serviceImpl.setSelectedItemsForRRT5Level(
            regionIndex,
            dimIndex,
            levelIndex
        )
    }

    override fun resetRRT5() {
        serviceImpl.resetRRT5()
    }

    override fun getRRT5LevelInfo(): MutableList<String> {
        var list = mutableListOf<String>()
        list.addAll(serviceImpl.rrT5LevelInfo)
        return list
    }

    private var anokiRatingLevel = 0
    private var temporary = false
    override fun setAnokiRatingLevel(level: Int, temporary: Boolean) {
        serviceImpl.setAnokiRatingLevel(level, temporary)
        anokiRatingLevel = level
        this.temporary = temporary
        if (!temporary) {
            context.getSharedPreferences(UtilsInterface.PREFS_TAG, Context.MODE_PRIVATE).edit()
                .putInt(Constants.SharedPrefsConstants.ANOKI_PARENTAL_CONTROLS_LEVEL_TAG, level).apply()

            if(context.getSharedPreferences(UtilsInterface.PREFS_TAG, Context.MODE_PRIVATE)
                    .getInt(Constants.SharedPrefsConstants.ANOKI_TEMPORARY_RATING_ENABLED_TAG, 0) == 1) {
                context.getSharedPreferences(UtilsInterface.PREFS_TAG, Context.MODE_PRIVATE).edit()
                    .putInt(Constants.SharedPrefsConstants.ANOKI_TEMPORARY_RATING_ENABLED_TAG, 2)
                    .apply()
                context.getSharedPreferences(UtilsInterface.PREFS_TAG, Context.MODE_PRIVATE).edit()
                    .putInt(Constants.SharedPrefsConstants.ANOKI_TEMPORARY_RATING_LEVEL_TAG, 0)
                    .apply()
            }
        } else {
            context.getSharedPreferences(UtilsInterface.PREFS_TAG, Context.MODE_PRIVATE).edit()
                .putInt(Constants.SharedPrefsConstants.ANOKI_TEMPORARY_RATING_ENABLED_TAG, 1).apply()
            context.getSharedPreferences(UtilsInterface.PREFS_TAG, Context.MODE_PRIVATE).edit()
                .putInt(Constants.SharedPrefsConstants.ANOKI_TEMPORARY_RATING_LEVEL_TAG, level).apply()
        }
    }

    override fun getAnokiRatingLevel(): Int {
        if (!temporary) {

            val sharedPreferences = context.getSharedPreferences(UtilsInterface.PREFS_TAG, Context.MODE_PRIVATE)
            var anokiRatingList = serviceImpl.anokiRatingList
            // Writing default Anoki parental control level if it is not exists
            if(!sharedPreferences.contains(Constants.SharedPrefsConstants.ANOKI_PARENTAL_CONTROLS_LEVEL_TAG) && anokiRatingList.isNotEmpty()){
                val editor = sharedPreferences.edit()
                editor.putInt(Constants.SharedPrefsConstants.ANOKI_PARENTAL_CONTROLS_LEVEL_TAG, anokiRatingList.size - 1)
                editor.apply()
            }

            anokiRatingLevel =
                context.getSharedPreferences(UtilsInterface.PREFS_TAG, Context.MODE_PRIVATE)
                    .getInt(Constants.SharedPrefsConstants.ANOKI_PARENTAL_CONTROLS_LEVEL_TAG, anokiRatingList.size - 1)
        }
        return anokiRatingLevel
    }

    override fun getAnokiRatingList(): MutableList<String> {
        var list = mutableListOf<String>()
        list.addAll(serviceImpl.anokiRatingList)
        return list
    }

    override fun isEventLocked(tvEvent: TvEvent?): Boolean {
        return if (tvEvent !=  null) serviceImpl.isEventLocked(toServiceTvEvent(tvEvent!!)) else false
    }

    private fun toTvContentRatingString(
        contentRatingSystem: ContentRatingSystem, rating: ContentRatingSystem.Rating?, subRating: ContentRatingSystem.SubRating? = null
    ): String {
        val tvContentRating = subRating?.let {
            TvContentRating.createRating(
                contentRatingSystem.domain,
                contentRatingSystem.name,
                rating?.name,
                subRating.name
            )
        } ?: TvContentRating.createRating(
            contentRatingSystem.domain, contentRatingSystem.name, rating?.name ?: "null"
        )
        return tvContentRating.flattenToString()
    }
}