package com.iwedia.cltv.fti.scan_models

class TerrestrialRangeModel (frequencyMin: Int?, frequencyMax: Int?, step: Int?, bandwidth: Int?){
    private var frequencyMin: Int? = null
    private var frequencyMax: Int? = null
    private var step: Int? = null
    private var bandwidth: Int? = null

    init{
        this.frequencyMin = frequencyMin
        this.frequencyMax = frequencyMax
        this.step = step
        this.bandwidth = bandwidth
    }

    fun getRangeMinFrequency(): Int? {
        return frequencyMin
    }

    fun getRangeMaxFrequency(): Int? {
        return frequencyMax
    }

    fun getRangeStep(): Int? {
        return step
    }

    fun getRangeBandwidth(): Int? {
        return bandwidth
    }

    fun setRangeMinFrequency(minFreq: Int?) {
        frequencyMin = minFreq
    }

    fun setRangeMaxFrequency(maxFreq: Int?) {
        frequencyMax = maxFreq
    }

    fun setRangeStep(step: Int?) {
        this.step = step
    }

    fun setRangeBandwidth(bandwidth: Int?) {
        this.bandwidth = bandwidth
    }
}