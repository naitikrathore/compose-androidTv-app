package com.iwedia.cltv.fti.scan_models
class DataParams(countryTag: String?, country: String?, defaultLCN: Int?, range: ArrayList<TerrestrialRangeModel>, operators: ArrayList<CableOperatorModel>, pvrEnabled: Int?){
    private var countryTag: String? = null
    private var country: String? = null
    private var defaultLCN: Int? = null
    private val range: ArrayList<TerrestrialRangeModel> = ArrayList<TerrestrialRangeModel>()
    private val operators: ArrayList<CableOperatorModel> = ArrayList<CableOperatorModel>()
    private var pvrEnabled: Int? = null

    init {
        this.countryTag = countryTag
        this.country = country
        this.defaultLCN = defaultLCN
        this.range.addAll(range!!)
        this.operators.addAll(operators!!)
        this.pvrEnabled = pvrEnabled
    }

    fun getCountrydefaultLCN(): Int? {
        return defaultLCN
    }

    fun getCountrydefaultPvr(): Int? {
        return pvrEnabled
    }

    fun getCountryTag(): String? {
        return countryTag
    }

    fun getCountryName(): String? {
        return country
    }

    fun getRangeList(): ArrayList<TerrestrialRangeModel>? {
        return range
    }

    fun getOperatorList(): ArrayList<CableOperatorModel>? {
        return operators
    }
}