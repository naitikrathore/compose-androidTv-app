package com.iwedia.cltv.components

/**
 * @author Gaurav jain
 * base interface for each type of items like radio,switch,checkbox,seekbar etc
 */
interface PrefComponentInterface<T> {
    fun requestFocus()
    fun clearFocus()
    fun refresh(data: T, id: Pref, listener: PrefItemListener)
    fun notifyPreferenceUpdated()
}