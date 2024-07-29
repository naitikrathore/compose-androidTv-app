package android.media.tv

import android.content.Context
import android.text.TextUtils
import java.util.Locale

class Rrt5ContentRatingSystem(
    region : String?,
    name: String?,
    domain: String?,
    title: String?,
    description: String?,
    countries: MutableList<String>?,
    displayName: String?,
    ratings: MutableList<Rating>?,
    subRatings: MutableList<SubRating>?,
    orders: MutableList<Order>?,
    isCustom: Boolean
) : ContentRatingSystem(
    name,
    domain,
    title,
    description,
    countries,
    displayName,
    ratings,
    subRatings,
    orders,
    isCustom
){
    //Name of this Region for DRRT.
    public var mRatingRegionName = region


    class RtkBuilder(context: Context?) : Builder(context) {

        private var mRegionName: String? = ""

        /**
         * setRegionName
         */
        fun setRegionName(name: String) {
            mRegionName = name
        }
        override fun build(): Rrt5ContentRatingSystem {
            require(!TextUtils.isEmpty(getmName())) { "Name cannot be empty" }
            require(!TextUtils.isEmpty(getmDomain())) { "Domain cannot be empty" }
            val sb = StringBuilder()
            val mCountries = getmCountries()
            if (mCountries != null && !mCountries.isEmpty()) {
                if (mCountries.size > 1) {
                    val locale = Locale.getDefault()
                    var countryName: String? = "other_countries"
                    if (mCountries.contains(locale.country)) {
                        // Shows the country name instead of "Other countries" if the current
                        // country is one of the countries this rating system applies to.
                        countryName = locale.displayName
                    }
                    sb.append(countryName)
                } else {
                    sb.append(Locale("", mCountries[0]).displayCountry)
                }
            }
            if (!TextUtils.isEmpty(getmTitle())) {
                sb.append(" (")
                sb.append(getmTitle())
                sb.append(")")
            }
            val displayName = sb.toString()
            val subRatings: MutableList<SubRating> = ArrayList()
            if (getmSubRatingBuilders() != null) {
                for (builder in getmSubRatingBuilders()) {
                    subRatings.add(builder.build())
                }
            }
            require(!(getmRatingBuilders().size <= 0)) { "Rating isn't available." }
            val ratings: MutableList<Rating> = ArrayList()
            // Map string ID to object.
            for (builder in getmRatingBuilders()) {
                ratings.add(builder.build(subRatings))
            }
            // Soundness check.
            for (subRating in subRatings) {
                var used = false
                for (rating in ratings) {
                    if (rating.subRatings.contains(subRating)) {
                        used = true
                        break
                    }
                }
                if (!used) {
                    throw IllegalArgumentException(
                        "Subrating " + subRating.name + " isn't used by any rating"
                    )
                }
            }
            val orders: MutableList<Order> = ArrayList()
            if (getmOrderBuilders() != null) {
                for (builder in getmOrderBuilders()) {
                    orders.add(builder.build(ratings))
                }
            }
            return Rrt5ContentRatingSystem(mRegionName,
                getmName(),
                getmDomain(),
                getmTitle(),
                getmDescription(),
                mCountries,
                displayName,
                ratings,
                subRatings,
                orders,
                true
            )
        }

    }
}