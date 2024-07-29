package com.iwedia.cltv.compose.util

import android.util.Log
import androidx.compose.ui.input.key.NativeKeyEvent
import com.iwedia.cltv.platform.model.Constants

private const val TAG = "KeyAction"

sealed class KeyHandler(
    val onDpadUp: () -> Boolean,
    val onDpadDown: () -> Boolean,
    val onDpadRight: () -> Boolean,
    val onDpadLeft: () -> Boolean,
    val onBackPressed: () -> Boolean
) {
    class Action(
        onDpadUp: () -> Boolean = { false },
        onDpadDown: () -> Boolean = { false },
        onDpadRight: () -> Boolean = { false },
        onDpadLeft: () -> Boolean = { false },
        onBackPressed: () -> Boolean = { false },
    ) : KeyHandler(
        onDpadUp,
        onDpadDown,
        onDpadRight,
        onDpadLeft,
        onBackPressed
    )

}

data class KeyAction(
    val actionDown: KeyHandler.Action = KeyHandler.Action(),
    val actionUp: KeyHandler.Action = KeyHandler.Action()
) {
    fun handleKeyEvent(
        keyEvent: NativeKeyEvent
    ): Boolean {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "handleKeyEvent: $keyEvent")
        return when (keyEvent.action) {
            NativeKeyEvent.ACTION_DOWN -> {
                when (keyEvent.keyCode) {
                    NativeKeyEvent.KEYCODE_DPAD_UP -> {
                        actionDown.onDpadUp()
                    }

                    NativeKeyEvent.KEYCODE_DPAD_DOWN -> {
                        actionDown.onDpadDown()
                    }

                    NativeKeyEvent.KEYCODE_DPAD_RIGHT -> {
                        actionDown.onDpadRight()
                    }

                    NativeKeyEvent.KEYCODE_DPAD_LEFT -> {
                        actionDown.onDpadLeft()
                    }

                    NativeKeyEvent.KEYCODE_BACK, NativeKeyEvent.KEYCODE_ESCAPE -> {
                        actionDown.onBackPressed()
                    }

                    else -> {
                        false
                    }
                }

            }

            NativeKeyEvent.ACTION_UP -> {
                when (keyEvent.keyCode) {
                    NativeKeyEvent.KEYCODE_BACK, NativeKeyEvent.KEYCODE_ESCAPE -> {
                        actionUp.onBackPressed()
                    }

                    else -> {
                        false
                    }
                }
            }

            else -> {
                false
            }
        }
    }
}