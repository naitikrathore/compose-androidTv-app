package com.iwedia.cltv.platform.model

class TvConfigRegions(val region: String) {
    var hbbtvEnabled = false
    var pvrEnabled = false
    var pvrSwkeysEnabled = false
    var timeshiftEnabled = false
    var ginga = "" //what
    var ttxEnabled = false
    var ciEnabled = false
    //ci v2 ??
    var chDownupAlwaysEnable = false
    var masterPinEnable: String = ""
    var openVchipEnable = false
    var bReuseBackKeyEnable = false
    var softKeyBoardEnable = false
    var dvbtEnable = false
    var dvbcEnable = false
    var dvbsEnable = false
    var blueMute = false
    var oad = false
//    var countries: MutableList<String> = mutableListOf()
}