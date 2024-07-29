package com.iwedia.cltv.utils

abstract class ConfigurableKey(var type: Int) {

    object Type {
        var ACTION_DOWN = 0
        var ACTION_UP = 1
    }

    abstract fun handleKey(actionType: Int): Boolean
}