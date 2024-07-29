package com.iwedia.cltv.platform.refplus5.parental

import com.mediatek.dtv.tvinput.framework.tifextapi.atsc.view.rating.MtkTvRRTRatingRegionInfo

class RatingPara {
    companion object{
        var rrt5Ratings: ArrayList<MtkTvRRTRatingRegionInfo>? = null
        var regionIndex: Int? = 0
        var dimIndex: Int? = 0
        var levelIndex: Int? = 0
    }
}