package tv.anoki.components.utils

import android.util.Log
import android.view.KeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.NativeKeyEvent
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import tv.anoki.components.constants.LogConstants
import tv.anoki.components.theme.BackgroundColor

val ParentPadding = PaddingValues(vertical = 12.dp, horizontal = 8.dp)
private const val TAG = "ModifierUtils"

@Immutable
data class Padding(
    val start: Dp,
    val top: Dp,
    val end: Dp,
    val bottom: Dp,
)

/**
 * Used to apply child padding.
 */
@Composable
fun rememberChildPadding(direction: LayoutDirection = LocalLayoutDirection.current): Padding {
    return remember {
        Padding(
            start = ParentPadding.calculateStartPadding(direction) + 8.dp,
            top = ParentPadding.calculateTopPadding(),
            end = ParentPadding.calculateEndPadding(direction) + 8.dp,
            bottom = ParentPadding.calculateBottomPadding()
        )
    }
}

/**
 * Used to apply modifiers conditionally.
 */
fun Modifier.ifElse(
    condition: () -> Boolean,
    ifTrueModifier: Modifier,
    ifFalseModifier: Modifier = Modifier
): Modifier = then(if (condition()) ifTrueModifier else ifFalseModifier)

/**
 * Used to apply modifiers conditionally.
 */
fun Modifier.ifElse(
    condition: Boolean,
    ifTrueModifier: Modifier,
    ifFalseModifier: Modifier = Modifier
): Modifier = ifElse({ condition }, ifTrueModifier, ifFalseModifier)

/**
 * Handles D-Pad Keys
 * */
fun Modifier.handleKeyEvent(
    keyAction: KeyAction
) = onKeyEvent { keyEvent ->
    Log.d(LogConstants.CLTV_TAG + TAG, "handleDPadKeyEventsExample1: ${keyEvent.nativeKeyEvent}")
    return@onKeyEvent when (keyEvent.nativeKeyEvent.action) {

        NativeKeyEvent.ACTION_DOWN -> {
            when (keyEvent.nativeKeyEvent.keyCode) {
                KeyEvent.KEYCODE_DPAD_LEFT -> {
                    keyAction.actionDown.onDpadLeft()
                }

                KeyEvent.KEYCODE_DPAD_RIGHT -> {
                    keyAction.actionDown.onDpadRight()
                }

                KeyEvent.KEYCODE_DPAD_UP -> {
                    keyAction.actionDown.onDpadUp()
                }

                KeyEvent.KEYCODE_DPAD_DOWN -> {
                    keyAction.actionDown.onDpadDown()
                }

                else -> {
                    false
                }
            }
        }

        NativeKeyEvent.ACTION_UP -> {
            when (keyEvent.nativeKeyEvent.keyCode) {
                KeyEvent.KEYCODE_BACK -> {
                    keyAction.actionUp.onBackPressed()
                }

                KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_SYSTEM_NAVIGATION_LEFT -> {
                    keyAction.actionUp.onDpadLeft()
                }

                KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_SYSTEM_NAVIGATION_RIGHT -> {
                    keyAction.actionUp.onDpadRight()
                }

                KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_SYSTEM_NAVIGATION_UP -> {
                    keyAction.actionUp.onDpadUp()
                }

                KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_SYSTEM_NAVIGATION_DOWN -> {
                    keyAction.actionUp.onDpadDown()
                }

                KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                    keyAction.actionUp.onDpadCenter()
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

/**
 * Disables the DPAD LEFT key press event.
 */
fun Modifier.disableDpadLeft(): Modifier {
    return handleKeyEvent(
        KeyAction(
            actionDown = KeyHandler.Action(
                onDpadLeft = {
                    true
                }
            )
        ))
}

/**
 * Disables the DPAD DOWN key press event.
 */
fun Modifier.disableDpadDown(): Modifier {
    return handleKeyEvent(
        KeyAction(
            actionDown = KeyHandler.Action(
                onDpadDown = {
                    true
                }
            )
        ))
}

/**
 * Disables the DPAD RIGHT key press event.
 */
fun Modifier.disableDpadRight(): Modifier {
    return handleKeyEvent(
        KeyAction(
            actionDown = KeyHandler.Action(
                onDpadRight = {
                    true
                }
            )
        )
    )
}

/**
 * Disables the DPAD CENTER key press event.
 */
fun Modifier.disableDpadCenter(): Modifier {
    return handleKeyEvent(
        KeyAction(
            actionDown = KeyHandler.Action(
                onDpadCenter = {
                    true
                }
            )
        )
    )
}

/**
 * Adds an action to be executed when the DPAD UP key is pressed.
 *
 * @param onAction The action to be executed when the DPAD UP key is pressed.
 *                 This action does not take any parameters and returns Unit.
 *
 */
fun Modifier.onDpadUp(onAction: () -> Unit): Modifier {
    return handleKeyEvent(
        KeyAction(
            actionDown = KeyHandler.Action(
                onDpadUp = {
                    onAction()
                    true
                }
            )
        )
    )
}

/**
 * Adds an action to be executed when the DPAD RIGHT key is pressed.
 *
 * @param onAction The action to be executed when the DPAD RIGHT key is pressed.
 *                 This action does not take any parameters and returns Unit.
 *
 */
fun Modifier.onDpadRight(onAction: () -> Unit): Modifier {
    return handleKeyEvent(
        KeyAction(
            actionDown = KeyHandler.Action(
                onDpadRight = {
                    onAction()
                    true
                }
            )
        )
    )
}

/**
 * Adds an action to be executed when the DPAD LEFT key is pressed.
 *
 * @param onAction The action to be executed when the DPAD LEFT key is pressed.
 *                 This action does not take any parameters and returns Unit.
 *
 */
fun Modifier.onDpadLeft(onAction: () -> Unit): Modifier {
    return handleKeyEvent(
        KeyAction(
            actionDown = KeyHandler.Action(
                onDpadLeft = {
                    onAction()
                    true
                }
            )
        )
    )
}

/**
 * Adds an action to be executed when the back button is pressed.
 *
 * @param onBackPressed The action to be executed when the back button is pressed.
 *                      This action does not take any parameters and returns Unit.
 *
 */
fun Modifier.onBackPressed(
    onBackPressed: () -> Unit
): Modifier {
    return handleKeyEvent(
        KeyAction(
            actionUp = KeyHandler.Action(
                onBackPressed = {
                    onBackPressed()
                    true
                }
            )
        )
    )
}

/**
 * An extension function for [Modifier] that sets the background color of a Composable.
 * This function applies a background color of the specified color to the Composable it is applied to.
 *
 * Example usage:
 * ```
 * Box(
 *     modifier = Modifier
 *         .size(100.dp)
 *         .setBackgroundColor()
 * )
 * ```
 *
 * @return A modified [Modifier] with the background color applied.
 */
fun Modifier.setBackgroundColor() = run { then(background(BackgroundColor)) }

/**
 * Fills max available size and only utilizes the content size for the composable. Useful for
 * cases when you need to quickly center the item on the available area.
 * */
fun Modifier.occupyScreenSize() = this
    .fillMaxSize()
    .wrapContentSize()


/**
 * Adds an action to be executed when the DPAD CENTER key is pressed with event actionUp.
 *
 * @param onAction The action to be executed when the DPAD CENTER key is pressed.
 *                 This action does not take any parameters and returns Unit.
 *
 */
fun Modifier.actionUpOnDpadCenter(onAction: () -> Unit): Modifier {
    return handleKeyEvent(
        KeyAction(
            actionUp = KeyHandler.Action(
                onDpadCenter = {
                    onAction()
                    true
                }
            )
        )
    )
}

/**
 * Adds an action to be executed when the DPAD LEFT key is pressed with event actionUp.
 *
 * @param onAction The action to be executed when the DPAD LEFT key is pressed.
 *                 This action does not take any parameters and returns Unit.
 *
 */
fun Modifier.actionUpOnDpadLeft(onAction: () -> Unit): Modifier {
    return handleKeyEvent(
        KeyAction(
            actionUp = KeyHandler.Action(
                onDpadLeft = {
                    onAction()
                    true
                }
            )
        )
    )
}

/**
 * Adds an action to be executed when the DPAD RIGHT key is pressed with event actionUp.
 *
 * @param onAction The action to be executed when the DPAD RIGHT key is pressed.
 *                 This action does not take any parameters and returns Unit.
 *
 */
fun Modifier.actionUpOnDpadRight(onAction: () -> Unit): Modifier {
    return handleKeyEvent(
        KeyAction(
            actionUp = KeyHandler.Action(
                onDpadRight  = {
                    onAction()
                    true
                }
            )
        )
    )
}


/**
 * Adds an action to be executed when the DPAD UP key is pressed with event actionUp.
 *
 * @param onAction The action to be executed when the DPAD UP key is pressed.
 *                 This action does not take any parameters and returns Unit.
 *
 */
fun Modifier.actionUpOnDpadUp(onAction: () -> Unit): Modifier {
    return handleKeyEvent(
        KeyAction(
            actionUp = KeyHandler.Action(
                onDpadUp  = {
                    onAction()
                    true
                }
            )
        )
    )
}

/**
 * Adds an action to be executed when the DPAD DOWN key is pressed with event actionUp.
 *
 * @param onAction The action to be executed when the DPAD DOWN key is pressed.
 *                 This action does not take any parameters and returns Unit.
 *
 */
fun Modifier.actionUpOnDpadDown(onAction: () -> Unit): Modifier {
    return handleKeyEvent(
        KeyAction(
            actionUp = KeyHandler.Action(
                onDpadDown  = {
                    onAction()
                    true
                }
            )
        )
    )
}

/**
 * Disables the DPAD RIGHT key press event.
 */
fun Modifier.disableDpadUp(): Modifier {
    return handleKeyEvent(
        KeyAction(
            actionDown = KeyHandler.Action(
                onDpadUp = {
                    true
                }
            )
        )
    )
}