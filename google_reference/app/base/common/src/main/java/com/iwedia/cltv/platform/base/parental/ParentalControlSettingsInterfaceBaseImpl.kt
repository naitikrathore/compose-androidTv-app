package com.iwedia.cltv.platform.base.parental

import android.annotation.SuppressLint
import android.content.Context
import android.media.tv.ContentRatingSystem
import android.media.tv.TvContentRating
import android.media.tv.TvInputManager
import android.os.Build
import android.preference.PreferenceManager
import android.util.Log
import androidx.annotation.RequiresApi
import com.iwedia.cltv.parental_controls.ContentRatingLevelPolicy
import com.iwedia.cltv.platform.`interface`.FastDataProviderInterface
import com.iwedia.cltv.platform.`interface`.ParentalControlSettingsInterface
import com.iwedia.cltv.platform.`interface`.TvInputInterface
import com.iwedia.cltv.platform.`interface`.UtilsInterface
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.platform.model.TvEvent
import com.iwedia.cltv.platform.model.information_bus.events.Events
import com.iwedia.cltv.platform.model.information_bus.events.InformationBus
import com.iwedia.cltv.platform.model.parental.InputSourceData

open class ParentalControlSettingsInterfaceBaseImpl(
    private var context: Context,
    private val fastDataProvider: FastDataProviderInterface
) : ParentalControlSettingsInterface {

    protected lateinit var mTvInputManager: TvInputManager
    protected lateinit var mTvInputInterface: TvInputInterface

    // mRatings is expected to be synchronized with mTvInputManager.getBlockedRatings().
    protected var mRatings = mutableSetOf<TvContentRating>()
    protected var mCustomRatings = mutableSetOf<TvContentRating>()
    protected var cls: Class<*>? = null

    init {
        mTvInputManager = context
            .getSystemService(Context.TV_INPUT_SERVICE) as TvInputManager
        cls = mTvInputManager.javaClass
        loadRatings()
        if(context.getSharedPreferences(UtilsInterface.PREFS_TAG, Context.MODE_PRIVATE).getInt(Constants.SharedPrefsConstants.ANOKI_TEMPORARY_RATING_ENABLED_TAG, 0) != 0) {
            context.getSharedPreferences(UtilsInterface.PREFS_TAG, Context.MODE_PRIVATE).edit()
                .putInt(Constants.SharedPrefsConstants.ANOKI_TEMPORARY_RATING_ENABLED_TAG, 0)
                .apply()

            context.getSharedPreferences(UtilsInterface.PREFS_TAG, Context.MODE_PRIVATE).edit()
                .putInt(Constants.SharedPrefsConstants.ANOKI_TEMPORARY_RATING_LEVEL_TAG, 0)
                .apply()
        }
    }

    companion object {
        private const val TAG = "ParentalControlSettings"

        // Parental Control settings
        const val PREF_CONTENT_RATING_SYSTEMS = "pref.content_rating_systems"
        const val PREF_CONTENT_RATING_LEVEL = "pref.content_rating_level"
        const val CONTENT_RATING_LEVEL_UNKNOWN = -1
        const val CONTENT_RATING_LEVEL_NONE = 0
        const val CONTENT_RATING_LEVEL_HIGH = 1
        const val CONTENT_RATING_LEVEL_MEDIUM = 2
        const val CONTENT_RATING_LEVEL_LOW = 3
        const val CONTENT_RATING_LEVEL_CUSTOM = 4

        /**
         * The rating and all of its sub-ratings are blocked.
         */
        const val RATING_BLOCKED = 0

        /**
         * The rating is blocked but not all of its sub-ratings are blocked.
         */
        const val RATING_BLOCKED_PARTIAL = 1

        /**
         * The rating is not blocked.
         */
        const val RATING_NOT_BLOCKED = 2
    }

    override fun setTvInputInterface(tvInputInterface: TvInputInterface) {
        this.mTvInputInterface = tvInputInterface
    }

    private var anokiRatingLevel = 0
    private var temporary = false
    override fun setAnokiRatingLevel(level: Int, temporary: Boolean) {
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
        fastDataProvider.updateRating((level + 1).toString())
    }

    override fun getAnokiRatingLevel(): Int {
        if (!temporary) {

            val sharedPreferences = context.getSharedPreferences(UtilsInterface.PREFS_TAG, Context.MODE_PRIVATE)

            // Writing default Anoki parental control level if it is not exists
            if(!sharedPreferences.contains(Constants.SharedPrefsConstants.ANOKI_PARENTAL_CONTROLS_LEVEL_TAG) && fastDataProvider.getFastRatingList().isNotEmpty()){
                val editor = sharedPreferences.edit()
                editor.putInt(Constants.SharedPrefsConstants.ANOKI_PARENTAL_CONTROLS_LEVEL_TAG, fastDataProvider.getFastRatingList().size - 1)
                editor.apply()
            }

            anokiRatingLevel =
                context.getSharedPreferences(UtilsInterface.PREFS_TAG, Context.MODE_PRIVATE)
                    .getInt(Constants.SharedPrefsConstants.ANOKI_PARENTAL_CONTROLS_LEVEL_TAG, fastDataProvider.getFastRatingList().size - 1)
        }
        return anokiRatingLevel
    }

    override fun getAnokiRatingList(): MutableList<String> {
        var ratingList = mutableListOf<String>()
        fastDataProvider.getFastRatingList().forEach {
            ratingList.add(it.name)
        }
        return ratingList
    }

    override fun isParentalControlsEnabled(): Boolean {
        return mTvInputManager.isParentalControlsEnabled
    }

    //TODO set this in the beggining
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
    override fun setAnokiParentalControlsEnabled(enabled: Boolean) {
        // Set parental control enabled shared prefs value Anoki parental case
        context.getSharedPreferences(UtilsInterface.PREFS_TAG, Context.MODE_PRIVATE).edit()
            .putBoolean(Constants.SharedPrefsConstants.PARENTAL_CONTROLS_ENABLED_TAG, enabled)
            .apply()
        if (enabled) {
            anokiRatingLevel =
                context.getSharedPreferences(UtilsInterface.PREFS_TAG, Context.MODE_PRIVATE)
                    .getInt(Constants.SharedPrefsConstants.ANOKI_PARENTAL_CONTROLS_LEVEL_TAG, fastDataProvider.getFastRatingList().size - 1)
            fastDataProvider.updateRating((anokiRatingLevel + 1).toString())
        } else {
            fastDataProvider.updateRating((fastDataProvider.getFastRatingList().size).toString())
        }
    }
    override fun isAnokiParentalControlsEnabled(): Boolean {
        return context.getSharedPreferences(UtilsInterface.PREFS_TAG, Context.MODE_PRIVATE)
            .getBoolean(Constants.SharedPrefsConstants.PARENTAL_CONTROLS_ENABLED_TAG, false)
    }

    @SuppressLint("NewApi")
    override fun setContentRatingSystemEnabled(
        tvInputInterface: TvInputInterface,
        contentRatingSystem: ContentRatingSystem,
        enabled: Boolean
    ) {
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

    override fun isContentRatingSystemEnabled(contentRatingSystem: ContentRatingSystem): Boolean {
        return hasContentRatingSystem(
            context,
            contentRatingSystem.id
        )
    }

    override fun hasContentRatingSystemSet(): Boolean {
        return hasContentRatingSystem(context)
    }

    @SuppressLint("NewApi")
    override fun loadRatings() {
        mRatings = HashSet(mTvInputManager.blockedRatings)
        if (mRatings != null) {
            Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG, "mRatings size =" + mRatings.size + " mRating " + mRatings)
        }
    }


    @RequiresApi(Build.VERSION_CODES.P)
    override fun getRatings(): MutableSet<TvContentRating> {
        return HashSet(mTvInputManager.blockedRatings)
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

    @RequiresApi(Build.VERSION_CODES.P)
    private fun updateRatingsForCurrentLevel(tvInputInterface: TvInputInterface) {
        val currentLevel = contentRatingLevel
        if (currentLevel != CONTENT_RATING_LEVEL_CUSTOM) {
            mRatings =
                ContentRatingLevelPolicy.getRatingsForLevel(this, tvInputInterface, currentLevel)
            if (currentLevel != CONTENT_RATING_LEVEL_NONE) {
                // UNRATED contents should be blocked unless the rating level is none or custom
                mRatings.add(TvContentRating.UNRATED)
            }
            storeRatings()
        }
    }

    override fun clearRatingBeforeSetRating() {
        mRatings.clear()
        storeRatings()
    }


    @RequiresApi(Build.VERSION_CODES.P)
    override fun setContentRatingLevel(
        tvInputInterface: TvInputInterface?, level: Int
    ) {
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
            if (level != CONTENT_RATING_LEVEL_NONE /*&& Boolean.TRUE.equals(Experiments.ENABLE_UNRATED_CONTENT_SETTINGS.get())*/) {
                // UNRATED contents should be blocked unless the rating level is none or custom
                mRatings.add(TvContentRating.UNRATED)
            } else {
                mRatings.clear()
            }
        }
        storeRatings()
    }

    @get:SuppressLint("WrongConstant")
    open val contentRatingLevel: Int
        get() {
            var currentLevel = getContentRatingLevel(context)
            if (currentLevel > CONTENT_RATING_LEVEL_CUSTOM) {
                currentLevel = 0
            }
            return currentLevel
        }

    /**
     * Sets the blocked status of a given content rating.
     *
     *
     * Note that a call to this method automatically changes the current rating level to `CONTENT_RATING_LEVEL_CUSTOM` if needed.
     *
     * @param contentRatingSystem The content rating system where the given rating belongs.
     * @param rating              The content rating to set.
     * @return `true` if changed, `false` otherwise.
     * @see .setSubRatingBlocked
     */
    override fun setRatingBlocked(
        contentRatingSystem: ContentRatingSystem,
        rating: ContentRatingSystem.Rating,
        blocked: Boolean
    ): Boolean {
        return setRatingBlockedInternal(contentRatingSystem, rating, null, blocked)
    }

    /**
     * Checks whether any of given ratings is blocked.
     *
     * @param ratings The array of ratings to check
     * @return `true` if a rating is blocked, `false` otherwise.
     */
    override fun isRatingBlocked(ratings: Array<TvContentRating?>?): Boolean {
        return getBlockedRating(ratings) != null
    }

    /**
     * Checks whether any of given ratings is blocked and returns the first blocked rating.
     *
     * @param ratings The array of ratings to check
     * @return The [TvContentRating] that is blocked.
     */
    private fun getBlockedRating(ratings: Array<TvContentRating?>?): TvContentRating? {
        if (ratings == null || ratings.size <= 0) {
            return if (mTvInputManager.isRatingBlocked(TvContentRating.UNRATED)) TvContentRating.UNRATED else null
        }
        for (rating in ratings) {
            if (mTvInputManager.isRatingBlocked(rating!!)) {
                return rating
            }
        }
        return null
    }

    /**
     * Checks whether a given rating is blocked by the user or not.
     *
     * @param contentRatingSystem The content rating system where the given rating belongs.
     * @param rating              The content rating to check.
     * @return `true` if blocked, `false` otherwise.
     */
    override fun isRatingBlocked(
        contentRatingSystem: ContentRatingSystem,
        rating: ContentRatingSystem.Rating
    ): Boolean {
        return mRatings.contains(toTvContentRating(contentRatingSystem, rating))
    }

    /**
     * Sets the blocked status of a given content sub-rating.
     *
     *
     * Note that a call to this method automatically changes the current rating level to `CONTENT_RATING_LEVEL_CUSTOM` if needed.
     *
     * @param contentRatingSystem The content rating system where the given rating belongs.
     * @param rating              The content rating associated with the given sub-rating.
     * @param subRating           The content sub-rating to set.
     * @return `true` if changed, `false` otherwise.
     * @see .setRatingBlocked
     */
    override fun setSubRatingBlocked(
        contentRatingSystem: ContentRatingSystem,
        rating: ContentRatingSystem.Rating,
        subRating: ContentRatingSystem.SubRating?,
        blocked: Boolean
    ): Boolean {
        return setRatingBlockedInternal(contentRatingSystem, rating, subRating, blocked)
    }

    /**
     * Checks whether a given content sub-rating is blocked by the user or not.
     *
     * @param contentRatingSystem The content rating system where the given rating belongs.
     * @param rating              The content rating associated with the given sub-rating.
     * @param subRating           The content sub-rating to check.
     * @return `true` if blocked, `false` otherwise.
     */
    override fun isSubRatingEnabled(
        contentRatingSystem: ContentRatingSystem,
        rating: ContentRatingSystem.Rating,
        subRating: ContentRatingSystem.SubRating
    ): Boolean {
        return mRatings.contains(toTvContentRating(contentRatingSystem, rating, subRating))
    }

    private fun setRatingBlockedInternal(
        contentRatingSystem: ContentRatingSystem,
        rating: ContentRatingSystem.Rating,
        subRating: ContentRatingSystem.SubRating?,
        blocked: Boolean
    ): Boolean {
        val tvContentRating = subRating?.let { toTvContentRating(contentRatingSystem, rating, it) }
            ?: toTvContentRating(contentRatingSystem, rating)
        val changed: Boolean

        //if rating is none, then other ratings should be unblocked, so this code will remove all added ratings.
        if (rating.name.lowercase().contains("none") && blocked ){
            setContentRatingLevel(this.mTvInputInterface, CONTENT_RATING_LEVEL_NONE)
        }

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
        if (contentRatingLevel != CONTENT_RATING_LEVEL_CUSTOM) {
            setContentRatingLevel(
                context,
                CONTENT_RATING_LEVEL_CUSTOM
            )
        }
    }

    /**
     * Returns the blocked status of a given rating. The status can be one of the followings: [ ][.RATING_BLOCKED], [.RATING_BLOCKED_PARTIAL] and [.RATING_NOT_BLOCKED]
     */
    override fun getBlockedStatus(
        contentRatingSystem: ContentRatingSystem,
        rating: ContentRatingSystem.Rating
    ): Int {
        if (isRatingBlocked(contentRatingSystem, rating)) {
            return RATING_BLOCKED
        }
        for (subRating in rating.subRatings) {
            if (isSubRatingEnabled(contentRatingSystem, rating, subRating)) {
                return RATING_BLOCKED_PARTIAL
            }
        }
        return RATING_NOT_BLOCKED
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

    override fun setRelativeRatingsEnabled(
        contentRatingSystem: ContentRatingSystem,
        selectRating: ContentRatingSystem.Rating?,
        enabled: Boolean
    ) {
        val orders = contentRatingSystem.orders
        if (orders == null || orders.isEmpty()) {
            return
        }
        //Order order=orders.get(0);
        var curOrder: ContentRatingSystem.Order? = null
        for (order in orders) {
            if (order.getRatingIndex(selectRating) != -1) {
                curOrder = order
            }
        }
        if (curOrder == null) {
            return
        }
        for (rating in contentRatingSystem.ratings) {
            val selectedRatingOrderIndex = curOrder.getRatingIndex(selectRating)
            val ratingOrderIndex = curOrder.getRatingIndex(rating)
            if (ratingOrderIndex != -1 && selectedRatingOrderIndex != -1 && (ratingOrderIndex > selectedRatingOrderIndex && enabled
                        || ratingOrderIndex < selectedRatingOrderIndex && !enabled)
            ) {
                setRatingBlocked(contentRatingSystem, rating, enabled)
                setRelativesetSubRatingEnabled(contentRatingSystem, enabled, rating)
            }
            //if none rating is selected then it should be removed from blocked states.
            if (rating.name.lowercase().contains("none") && ratingOrderIndex == -1 && selectedRatingOrderIndex != -1 ){
                setRatingBlocked(contentRatingSystem, rating,false)
            }
        }
    }

    private fun setRelativesetSubRatingEnabled(
        contentRatingSystem: ContentRatingSystem,
        enabled: Boolean,
        rating: ContentRatingSystem.Rating
    ) {
        for (subRating in rating.subRatings) {
            setSubRatingBlocked(contentRatingSystem, rating, subRating, enabled)
        }
    }


    override fun setRelativeRating2SubRatingEnabled(
        contentRatingSystem: ContentRatingSystem,
        enabled: Boolean,
        relativeRating: ContentRatingSystem.Rating?,
        subRating: ContentRatingSystem.SubRating
    ) {
        val orders = contentRatingSystem.orders
        if (orders == null || orders.isEmpty()) {
            return
        }
        var orderContainsRating: ContentRatingSystem.Order? = null
        for (i in orders.indices) {
            val order = orders[i]
            if (order != null) {
                val index = order.getRatingIndex(relativeRating)
                if (index != -1) {
                    orderContainsRating = order
                    break
                }
            }
        }
        if (orderContainsRating != null) {
            for (rating in contentRatingSystem.ratings) {
                val selectedRatingOrderIndex = orderContainsRating.getRatingIndex(relativeRating)
                val ratingOrderIndex = orderContainsRating.getRatingIndex(rating)
                if (ratingOrderIndex != -1 && selectedRatingOrderIndex != -1 && (ratingOrderIndex > selectedRatingOrderIndex && enabled
                            || ratingOrderIndex < selectedRatingOrderIndex && !enabled)
                ) {
                    val subRatingslist = rating.subRatings
                    if (subRatingslist.contains(subRating)) {
                        val relativeSub = subRatingslist[subRatingslist.indexOf(subRating)]
                        if (isSubRatingEnabled(
                                contentRatingSystem,
                                rating,
                                relativeSub
                            ) == enabled
                        ) {
                            continue
                        }
                        setSubRatingBlocked(contentRatingSystem, rating, relativeSub, enabled)
                    }
                }
            }
        }
    }

    // Parental Control settings
    protected fun addContentRatingSystem(context: Context?, id: String) {
        val contentRatingSystemSet = getContentRatingSystemSet(context)
        if (contentRatingSystemSet.add(id)) {
            PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putStringSet(PREF_CONTENT_RATING_SYSTEMS, contentRatingSystemSet)
                .apply()
        }
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

    override fun removeContentRatingSystem(context: Context?) {
        PreferenceManager.getDefaultSharedPreferences(context)
            .edit()
            .remove(PREF_CONTENT_RATING_SYSTEMS)
            .apply()
    }

    private fun hasContentRatingSystem(context: Context?, id: String): Boolean {
        return getContentRatingSystemSet(context).contains(id)
    }

    private fun hasContentRatingSystem(context: Context?): Boolean {
        return !getContentRatingSystemSet(context).isEmpty()
    }

    /**
     * Returns whether the content rating system is ever set. Returns `false` only when the
     * user changes parental control settings for the first time.
     */
    override fun isContentRatingSystemSet(context: Context?): Boolean {
        return (PreferenceManager.getDefaultSharedPreferences(context)
            .getStringSet(PREF_CONTENT_RATING_SYSTEMS, null)
                != null)
    }

    //TODO check this
    private fun getContentRatingSystemSet(context: Context?): MutableSet<String> {
        return HashSet(
            PreferenceManager.getDefaultSharedPreferences(context)
                .getStringSet(PREF_CONTENT_RATING_SYSTEMS, emptySet())!!
        )
    }

    override fun getContentRatingLevel(context: Context?): Int {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getInt(PREF_CONTENT_RATING_LEVEL, CONTENT_RATING_LEVEL_NONE)
    }

    override fun getContentRatingLevelIndex(): Int {
        return contentRatingLevel
    }

    override fun setContentRatingLevel(context: Context?, level: Int) {
        PreferenceManager.getDefaultSharedPreferences(context)
            .edit()
            .putInt(PREF_CONTENT_RATING_LEVEL, level)
            .apply()
    }

    override fun getGlobalRestrictionValue(value: Int): String {
        return when (value) {
            0 -> "menu_arrays_None"
            1 -> "option_rating_high"
            2 -> "option_rating_medium"
            3 -> "option_rating_low"
            4 -> "option_rating_custom"
            else -> "menu_arrays_None"
        }
    }

    override fun getGlobalRestrictionsArray(): MutableList<String> {
        val restrictions: MutableList<String> = ArrayList()
        restrictions.add("menu_arrays_None")
        restrictions.add("option_rating_high")
        restrictions.add("option_rating_medium")
        restrictions.add("option_rating_low")
        restrictions.add("option_rating_custom")
        return restrictions
    }

    override fun getRatingsPerRatingLevelMap(): HashMap<String?, String?> {
        return ContentRatingLevelPolicy.getContentRatingSystemNameToRatingListMap()
    }

    override fun getRatingsSubratingsPerRatingLevelMap(): HashMap<String?, String?> {
        return ContentRatingLevelPolicy.getRatingToSubRatingListMap()
    }


    override fun blockTvInputCount(blockedInputs: MutableList<InputSourceData>): Int {
        return 0
    }

    override fun blockInput(selected: Boolean, item: InputSourceData) {}

    override fun isBlockSource(hardwareId: Int): Boolean {
        return false
    }

    override fun getBlockUnrated(): Int {
        return 0
    }

    override fun setBlockUnrated(isBlockUnrated: Boolean) {}

    override fun getRRT5Regions(): MutableList<String> {
        return mutableListOf()
    }

    override fun getRRT5Dim(index: Int): MutableList<String> {
        return mutableListOf()
    }

    override fun getRRT5CrsInfo(regionName: String): MutableList<ContentRatingSystem> {
        return mutableListOf()
    }

    override fun getRRT5Level(countryIndex: Int, dimIndex: Int): MutableList<String> {
        return mutableListOf()
    }

    override fun getSelectedItemsForRRT5Level(): java.util.HashMap<Int, Int> {
        return hashMapOf()
    }

    override fun rrt5BlockedList(regionPosition: Int, dimIndex: Int): java.util.HashMap<Int, Int> {
        return hashMapOf()
    }

    override fun setSelectedItemsForRRT5Level(
        regionIndex: Int,
        dimIndex: Int,
        levelIndex: Int
    ) {
    }

    override fun resetRRT5() {}
    override fun getRRT5LevelInfo(): MutableList<String> {
        return mutableListOf()
    }

    override fun isEventLocked(tvEvent: TvEvent?): Boolean {
        tvEvent?.let {
            val rating = mTvInputInterface.getParentalRatingDisplayName(it.parentalRating, tvEvent)
            if (it.tvChannel.isFastChannel() && isAnokiParentalControlsEnabled()) {

                if (!rating.isNullOrEmpty()) {
                    //Anoki rating level starts with value 1 so here we decrease it by 1
                    val isEventLocked = anokiRatingLevel < rating.toInt() - 1
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "isEventLocked fast channel $isEventLocked")
                    return isEventLocked
                }
            } else if (isParentalControlsEnabled()){
                //ATSC 18+ content R or NC-17
            }
        }

        return false
    }

}