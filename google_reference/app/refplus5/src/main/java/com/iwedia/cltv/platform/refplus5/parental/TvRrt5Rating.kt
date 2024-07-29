package com.iwedia.cltv.platform.refplus5.parental
 
import android.content.Context
import android.os.Bundle
import com.mediatek.dtv.tvinput.framework.tifextapi.atsc.view.rating.MtkTvRRTRatingRegionInfo
import com.mediatek.dtv.tvinput.client.rating.TvRating
import java.util.ArrayList

object TvRrt5Rating {

    fun getRRTRatingInfo(context: Context, broadcastType: Int):
            ArrayList<MtkTvRRTRatingRegionInfo> {
        var rrtList = mutableListOf<MtkTvRRTRatingRegionInfo>()
        try {
            val rating = TvRating(context, broadcastType)
            val resultGet = rating.getRRTRatingInfo()!!
            resultGet.classLoader = MtkTvRRTRatingRegionInfo::class.java.classLoader
            rrtList =
                resultGet.getParcelableArrayList(
                    com.mediatek.dtv.tvinput.framework.tifextapi.atsc.view.rating.Constants
                        .KEY_RRT_LIST
                )!!
            return rrtList as ArrayList<MtkTvRRTRatingRegionInfo>
        } catch (e: Exception) {
            return rrtList as ArrayList<MtkTvRRTRatingRegionInfo>
        }
    }

    fun setRRTRatingInfo(
        context: Context, broadcastType: Int, rrtSet: ArrayList<MtkTvRRTRatingRegionInfo>
    ) {
        val rating = TvRating(context, broadcastType)
        val resultSet = Bundle()
        resultSet.putParcelableArrayList(
            com.mediatek.dtv.tvinput.framework.tifextapi.atsc.view.rating.Constants.KEY_RRT_LIST,
            rrtSet
        )
        rating.setRRTRatingInfo(resultSet)
    }
}