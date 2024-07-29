package com.iwedia.cltv.entities

import com.iwedia.cltv.components.PreferenceSubMenuItem
/**
 * pvr timeshift information
 *@author Gaurav Jain
 */
class PreferencesPvrTimeshiftInformation(
    var subCategories: MutableList<PreferenceSubMenuItem>,
    var timeshiftModeEnabled :Boolean? = false,
    var usbDeviceList: MutableList<ReferenceDeviceItem>
)