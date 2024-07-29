package com.iwedia.cltv.platform.model

/**
 * Reference system information data
 *
 * @author Dejan Nadj
 */
class ReferenceSystemInformation(
    var rfChannelNumber: String = "",
    var ber: String = "",
    var frequency: String = "",
    var prog: String = "",
    var uec: String = "",
    var serviceId: String = "",
    var postViterbi: String = "",
    var tsId: String = "",
    var fiveS: String = "",
    var onId: String = "",
    var agc: String = "",
    var networkId: String = "",
    var networkName: String = "",
    var bandwidth: String = "",
    var signalStrength: Int = 0,
    var signalQuality: Int  = 0,
) {
}