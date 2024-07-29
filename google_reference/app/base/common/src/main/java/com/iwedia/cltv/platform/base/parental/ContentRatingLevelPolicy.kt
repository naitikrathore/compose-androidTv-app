package com.iwedia.cltv.parental_controls

import android.media.tv.ContentRatingSystem
import android.media.tv.TvContentRating
import android.os.Build
import androidx.annotation.RequiresApi
import com.iwedia.cltv.platform.`interface`.ParentalControlSettingsInterface
import com.iwedia.cltv.platform.`interface`.TvInputInterface
import com.iwedia.cltv.platform.base.parental.ParentalControlSettingsInterfaceBaseImpl


object ContentRatingLevelPolicy {

    private const val AGE_THRESHOLD_FOR_LEVEL_HIGH = 6
    private const val AGE_THRESHOLD_FOR_LEVEL_MEDIUM = 12
    private const val AGE_THRESHOLD_FOR_LEVEL_LOW = -1 // Highest age for each rating system
    private val crsNameToRatingListMap: HashMap<String?, String?> = HashMap()
    private val ratingToSubRatingListMap: HashMap<String?, String?> = HashMap()


    @RequiresApi(Build.VERSION_CODES.P)
    fun getRatingsForLevel(
        settings: ParentalControlSettingsInterface,
        tvInputInterface: TvInputInterface?,
        level: Int
    ): MutableSet<TvContentRating> {
        return when (level) {
            ParentalControlSettingsInterfaceBaseImpl.CONTENT_RATING_LEVEL_NONE -> {
                getRatingForNone(settings)
            }
            ParentalControlSettingsInterfaceBaseImpl.CONTENT_RATING_LEVEL_HIGH -> {
                getRatingsForAge(settings, tvInputInterface, AGE_THRESHOLD_FOR_LEVEL_HIGH)
            }
            ParentalControlSettingsInterfaceBaseImpl.CONTENT_RATING_LEVEL_MEDIUM -> {
                getRatingsForAge(settings, tvInputInterface, AGE_THRESHOLD_FOR_LEVEL_MEDIUM)
            }
            ParentalControlSettingsInterfaceBaseImpl.CONTENT_RATING_LEVEL_LOW -> {
                getRatingsForAge(settings, tvInputInterface, AGE_THRESHOLD_FOR_LEVEL_LOW)
            }
            else -> throw IllegalArgumentException("Unexpected rating level")
        }
    }


    @RequiresApi(Build.VERSION_CODES.P)
    private fun getRatingForNone(settings: ParentalControlSettingsInterface): MutableSet<TvContentRating> {
        crsNameToRatingListMap.clear()
        return settings.getRatings()
    }

    private fun getRatingsForAge(
        settings: ParentalControlSettingsInterface, tvInputInterface: TvInputInterface?, age: Int
    ): MutableSet<TvContentRating> {
        val ratings: MutableSet<TvContentRating> = HashSet()
        crsNameToRatingListMap.clear()
        ratingToSubRatingListMap.clear()
        for (contentRatingSystem in tvInputInterface!!.getContentRatingSystems()) {
            if (!settings.isContentRatingSystemEnabled(contentRatingSystem)) {
                continue
            }
            var ageLimit = age
            if (ageLimit == AGE_THRESHOLD_FOR_LEVEL_LOW) {
                ageLimit = getMaxAge(contentRatingSystem)
            }
            val ratingStrings = StringBuilder()
            for (rating in contentRatingSystem.ratings) {
                if (rating.ageHint < ageLimit) {
                    continue
                }
                var tvContentRating = TvContentRating.createRating(
                    contentRatingSystem.domain,
                    contentRatingSystem.name,
                    rating.name
                )
                ratings.add(tvContentRating)
                ratingStrings.append(rating.title)
                ratingStrings.append(", ")
                if (!rating.subRatings.isEmpty()) {
                    val subRatingStrings = StringBuilder()
                    for (subRating in rating.subRatings) {
                        tvContentRating = TvContentRating.createRating(
                            contentRatingSystem.domain, contentRatingSystem.name,
                            rating.name, subRating.name
                        )
                        ratings.add(tvContentRating)
                        subRatingStrings.append(subRating.description)
                        subRatingStrings.append(", ")
                    }
                    subRatingStrings.setLength(Math.max(subRatingStrings.length - 2, 0))
                    val s = subRatingStrings.toString()
                    val subRatings = "All, $s"
                    ratingToSubRatingListMap[rating.title] =
                        subRatings
                }
            }
            ratingStrings.setLength(Math.max(ratingStrings.length - 2, 0))
            crsNameToRatingListMap[contentRatingSystem.name] =
                ratingStrings.toString()
        }
        return ratings
    }

    private fun getMaxAge(contentRatingSystem: ContentRatingSystem): Int {
        var maxAge = 0
        for (rating in contentRatingSystem.ratings) {
            if (maxAge < rating.ageHint) {
                maxAge = rating.ageHint
            }
        }
        return maxAge
    }

    fun getContentRatingSystemNameToRatingListMap(): HashMap<String?, String?> {
        return crsNameToRatingListMap;
    }

    fun getRatingToSubRatingListMap(): HashMap<String?, String?> {
        return ratingToSubRatingListMap;
    }
}

