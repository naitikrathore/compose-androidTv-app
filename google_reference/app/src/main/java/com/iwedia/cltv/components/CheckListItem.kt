package com.iwedia.cltv.components

/**
 * CheckListItem
 *
 * this class is used for items in CheckListAdapter
 *
 * @param name is title of the button that will be displayed.
 *
 * @param isChecked is flag used to se button initially to checked or unchecked state later in the adapter.
 *
 * @param data is used to pass some additional data with CheckListItem. For example, in InfoBanner or DetailsScene when opening Audio tracks
 * they can contain information if they are Dolby/AD/HOH. This variable can be used to pass that additional data as List of Int (resource to the
 * icons that should be shown with corresponding button).
 *
 * @author Vasilisa Laganin
 */

data class CheckListItem(var name: String, var isChecked: Boolean, var data: MutableList<Int>? = null)

