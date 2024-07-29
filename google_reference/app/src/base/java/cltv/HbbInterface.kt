package com.iwedia.cltv;

import android.view.KeyEvent

class HbbInterface {

    var isFromNative = false

    constructor(activity: MainActivity) {
    }

    fun enableHbbTv() {
    }

    fun passKeyToNative(keyCode: Int, event: KeyEvent?): Boolean {
        return false
    }

    private fun isKeyValid(keyCode: Int): Boolean {
        return false
    }

    private fun getScanCode(keyCode: Int, event: KeyEvent?):Int {
        return -1
    }

    fun handlerHbbtvMessage(type: Int, message: Int) {
        return
    }

    fun isHbbTvActive(): Boolean? {
        return false
    }

    fun isHbbTvStreaming(): Boolean? {
        return false
    }

}