package tv.anoki.components.utils

private const val TAG = "KeyAction"

sealed class KeyHandler(
    val onDpadUp: () -> Boolean,
    val onDpadDown: () -> Boolean,
    val onDpadRight: () -> Boolean,
    val onDpadLeft: () -> Boolean,
    val onBackPressed: () -> Boolean,
    val onDpadCenter: () -> Boolean,
) {
    class Action(
        onDpadUp: () -> Boolean = { false },
        onDpadDown: () -> Boolean = { false },
        onDpadRight: () -> Boolean = { false },
        onDpadLeft: () -> Boolean = { false },
        onBackPressed: () -> Boolean = { false },
        onDpadCenter: () -> Boolean = { false },

        // TODO ADD new actions here
    ) : KeyHandler(
        onDpadUp,
        onDpadDown,
        onDpadRight,
        onDpadLeft,
        onBackPressed,
        onDpadCenter
    )
}

/**
 * Utility class for handling key events, particularly for navigation actions.
 * It provides methods to handle key events such as D-pad navigation and back button presses.
 *
 * @property actionDown Represents the actions to be executed when a key is pressed down.
 * @property actionUp Represents the actions to be executed when a key is released.
 */
data class KeyAction(
    val actionDown: KeyHandler.Action = KeyHandler.Action(),
    val actionUp: KeyHandler.Action = KeyHandler.Action()
)