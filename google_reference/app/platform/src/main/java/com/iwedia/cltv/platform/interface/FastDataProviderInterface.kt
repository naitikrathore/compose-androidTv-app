package com.iwedia.cltv.platform.`interface`

import com.iwedia.cltv.platform.model.FastRatingListItem
import com.iwedia.cltv.platform.model.IAsyncCallback
import com.iwedia.cltv.platform.model.PromotionItem
import com.iwedia.cltv.platform.model.RecommendationRow

interface FastDataProviderInterface {

    fun getPromotionList(): ArrayList<PromotionItem>

    fun getRecommendationRows(): ArrayList<RecommendationRow>

    fun getGenreList(): ArrayList<String>

    fun getFastFavoriteList(): ArrayList<String>

    fun updateFavoriteList(channelId : String, addToFavorite: Boolean, callback:IAsyncCallback)

    fun getAnokiUID() : String

    fun updateDNT(enableDNT: Boolean, callback: IAsyncCallback)

    fun getDNT() : Int

    fun getTosOptIn(): Int

    fun updateTosOptIn(value: Int)

    fun getFastRatingList(): ArrayList<FastRatingListItem>

    fun updateRating(rating: String)

    fun deleteAllFastData(inputId: String)
}