package com.iwedia.cltv.platform.`interface`

import android.annotation.SuppressLint
import android.content.Context
import android.media.tv.ContentRatingSystem
import android.media.tv.TvContentRating
import android.os.Build
import androidx.annotation.RequiresApi
import com.iwedia.cltv.platform.model.TvEvent
import com.iwedia.cltv.platform.model.parental.InputSourceData


interface ParentalControlSettingsInterface {
    fun setTvInputInterface(tvInputInterface: TvInputInterface)
    fun isParentalControlsEnabled(): Boolean

    fun setParentalControlsEnabled(enabled: Boolean)

    fun isAnokiParentalControlsEnabled(): Boolean

    fun setAnokiParentalControlsEnabled(enabled: Boolean)

    @SuppressLint("NewApi")
    fun setContentRatingSystemEnabled(
        tvInputInterface: TvInputInterface,
        contentRatingSystem: ContentRatingSystem,
        enabled: Boolean
    )

    fun isContentRatingSystemEnabled(contentRatingSystem: ContentRatingSystem): Boolean

    fun hasContentRatingSystemSet(): Boolean

    fun loadRatings()

    fun getRatings(): MutableSet<TvContentRating>

    fun clearRatingBeforeSetRating()


    @RequiresApi(Build.VERSION_CODES.P)
    fun setContentRatingLevel(
        tvInputInterface: TvInputInterface?, level: Int
    )

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
    fun setRatingBlocked(
        contentRatingSystem: ContentRatingSystem,
        rating: ContentRatingSystem.Rating,
        blocked: Boolean
    ): Boolean

    /**
     * Checks whether any of given ratings is blocked.
     *
     * @param ratings The array of ratings to check
     * @return `true` if a rating is blocked, `false` otherwise.
     */
    fun isRatingBlocked(ratings: Array<TvContentRating?>?): Boolean

    /**
     * Checks whether a given rating is blocked by the user or not.
     *
     * @param contentRatingSystem The content rating system where the given rating belongs.
     * @param rating              The content rating to check.
     * @return `true` if blocked, `false` otherwise.
     */
    fun isRatingBlocked(
        contentRatingSystem: ContentRatingSystem,
        rating: ContentRatingSystem.Rating
    ): Boolean

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
    fun setSubRatingBlocked(
        contentRatingSystem: ContentRatingSystem,
        rating: ContentRatingSystem.Rating,
        subRating: ContentRatingSystem.SubRating?,
        blocked: Boolean
    ): Boolean

    /**
     * Checks whether a given content sub-rating is blocked by the user or not.
     *
     * @param contentRatingSystem The content rating system where the given rating belongs.
     * @param rating              The content rating associated with the given sub-rating.
     * @param subRating           The content sub-rating to check.
     * @return `true` if blocked, `false` otherwise.
     */
    fun isSubRatingEnabled(
        contentRatingSystem: ContentRatingSystem,
        rating: ContentRatingSystem.Rating,
        subRating: ContentRatingSystem.SubRating
    ): Boolean

    /**
     * Returns the blocked status of a given rating. The status can be one of the followings: [ ][.RATING_BLOCKED], [.RATING_BLOCKED_PARTIAL] and [.RATING_NOT_BLOCKED]
     */
    fun getBlockedStatus(
        contentRatingSystem: ContentRatingSystem,
        rating: ContentRatingSystem.Rating
    ): Int

    fun setRelativeRatingsEnabled(
        contentRatingSystem: ContentRatingSystem,
        selectRating: ContentRatingSystem.Rating?,
        enabled: Boolean
    )

    fun setRelativeRating2SubRatingEnabled(
        contentRatingSystem: ContentRatingSystem,
        enabled: Boolean,
        relativeRating: ContentRatingSystem.Rating?,
        subRating: ContentRatingSystem.SubRating
    )

    fun removeContentRatingSystem(context: Context?)

    /**
     * Returns whether the content rating system is ever set. Returns `false` only when the
     * user changes parental control settings for the first time.
     */
    fun isContentRatingSystemSet(context: Context?): Boolean

    fun getContentRatingLevel(context: Context?): Int

    fun getContentRatingLevelIndex(): Int

    fun setContentRatingLevel(context: Context?, level: Int)

    fun getGlobalRestrictionValue(value: Int): String

    fun getGlobalRestrictionsArray(): MutableList<String>

    fun getRatingsPerRatingLevelMap(): HashMap<String?, String?>

    fun getRatingsSubratingsPerRatingLevelMap(): HashMap<String?, String?>

    fun blockTvInputCount(blockedInputs: MutableList<InputSourceData>): Int

    fun blockInput(selected: Boolean, item: InputSourceData)

    fun isBlockSource(hardwareId: Int): Boolean

    fun getBlockUnrated(): Int

    fun setBlockUnrated(isBlockUnrated: Boolean)

    fun getRRT5Regions(): MutableList<String>

    fun getRRT5Dim(index: Int): MutableList<String>
    fun getRRT5CrsInfo(regionName: String): MutableList<ContentRatingSystem>

    fun getRRT5Level(countryIndex: Int, dimIndex: Int): MutableList<String>

    fun getSelectedItemsForRRT5Level(): HashMap<Int, Int>
    fun rrt5BlockedList(regionPosition :Int , position: Int): HashMap<Int, Int>
    fun setSelectedItemsForRRT5Level(
        regionIndex: Int,
        dimIndex: Int,
        levelIndex: Int)
    fun resetRRT5()
    fun getRRT5LevelInfo(): MutableList<String>
    fun setAnokiRatingLevel(level: Int, temporary: Boolean = false)
    fun getAnokiRatingLevel(): Int
    fun getAnokiRatingList():MutableList<String>
    fun isEventLocked(tvEvent: TvEvent?): Boolean
}