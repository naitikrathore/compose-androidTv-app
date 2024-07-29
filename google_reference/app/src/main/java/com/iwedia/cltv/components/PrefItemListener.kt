package com.iwedia.cltv.components

import com.iwedia.cltv.platform.`interface`.TTSSetterInterface
import com.iwedia.cltv.platform.`interface`.ToastInterface
import com.iwedia.cltv.platform.model.IAsyncCallback
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.TvChannel

/**
 * this actions are used to differentiate clicks, up,down,focused
 * update - it is used for update info for items- like if radio is selected we need to update previously selected radio button
 */
enum class Action {
    UP,
    DOWN,
    LEFT,
    RIGHT,
    CLICK,
    BACK,
    FOCUSED,
    UPDATE,
    LONG_CLICK
}

/**
 * this listener listens the changes like update,focused,up, down etc
 */
interface PrefItemListener: TTSSetterInterface, ToastInterface {
    fun onAction(action: Action, id: Pref): Boolean
}

//used for switch, checkbox, radioButton
/**
 * index: gives the position of the item
 * data: gives is it true/false
 * id: it is the id of item to differentiate each item
 *
 */
interface PrefCompoundListener {
    fun onChange(data: Boolean, index: Int, id: Pref, callback: IAsyncDataCallback<Boolean>)
}

/**
 * data: data gives the progress of seekbar
 * id: it is the id of item to differentiate each item
 *
 */
interface PrefSeekBarListener {
    fun onChange(data: Int, id: Pref, callback: IAsyncDataCallback<Int>)
}

/**
 * this listener is used to listen changes in edit channel options in setup menu for konka devices,
 */
interface EditChannelListener {
    fun onMove(moveItems: ArrayList<TvChannel>, previousPosition:Int, newPosition: Int, channelMap: HashMap<Int, String>, callback: IAsyncCallback)
    fun onSwap(firstChannel: TvChannel, secondChannel: TvChannel,firstIndex:Int,secondIndex:Int, callback: IAsyncCallback)
    fun onDelete(tvChannel: TvChannel,deletedIndex:Int, callback: IAsyncCallback)
    fun onSkip(channel: TvChannel, callback: IAsyncCallback)
}