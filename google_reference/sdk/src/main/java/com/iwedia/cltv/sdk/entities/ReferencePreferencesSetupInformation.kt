package com.iwedia.cltv.sdk.entities

import data_type.GList

/**
 * Reference preferences setup information
 *
 * @author Aleksandar Lazic
 */
class ReferencePreferencesSetupInformation(
    var subCategories : MutableList<PreferenceSubcategoryItem>,
    var channels: GList<ReferenceTvChannel>? = null,
    var displayMode: HashMap<Int, String>? = null,
    var defaultChannel: ReferenceTvChannel,
    var defaultChannelIndex: Int,
    var defaultDisplayMode: Int,
    var aspectRatioOptions: MutableList<String> = mutableListOf(),
    var defaultAspectRatioOption : Int

) {}