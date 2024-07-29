package tv.anoki.components.screenshottesting.button

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import tv.anoki.components.R
import tv.anoki.components.button.IconButton
import tv.anoki.components.button.IconTextButton
import tv.anoki.components.button.IconTextProgressButton

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = RobolectricDeviceQualifiers.Television4K)
class ButtonScreenShotTesting {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun iconButtonComponent() {
        composeTestRule.setContent {
            IconButton(
                icon = R.drawable.icon_play_focused,
                onClick = {}
            )
        }
        composeTestRule.onRoot()
            .captureRoboImage(filePath = "screenshots/button/iconButtonComponent.png")
    }

    @Test
    fun focusedIconButtonComponent() {
        composeTestRule.setContent {
            IconButton(
                icon = R.drawable.icon_play_focused,
                onClick = {},
                isButtonFocused = true
            )
        }
        composeTestRule.onRoot()
            .captureRoboImage(filePath = "screenshots/button/focusedIconButtonComponent.png")
    }

    @Test
    fun iconTextButtonComponent() {
        composeTestRule.setContent {
            IconTextButton(
                icon = R.drawable.icon_play_focused,
                buttonText = "Play Season 1 Episode 1",
                onClick = {}
            )
        }
        composeTestRule.onRoot()
            .captureRoboImage(filePath = "screenshots/button/iconTextButtonComponent.png")
    }

    @Test
    fun focusedIconTextButtonComponent() {
        composeTestRule.setContent {
            IconTextButton(
                icon = R.drawable.icon_play_focused,
                buttonText = "Play Season 1 Episode 1",
                onClick = {},
                isButtonFocused = true
            )
        }
        composeTestRule.onRoot()
            .captureRoboImage(filePath = "screenshots/button/focusedIconTextButtonComponent.png")
    }

    @Test
    fun iconTextProgressButtonComponent() {
        composeTestRule.setContent {
            IconTextProgressButton(
                icon = R.drawable.icon_play_focused,
                buttonText = "Play Season 1 Episode 1",
                currentProgress = 0.3F,
                onClick = {}
            )
        }
        composeTestRule.onRoot()
            .captureRoboImage(filePath = "screenshots/button/iconTextProgressButtonComponent.png")
    }

    @Test
    fun focusedIconTextProgressButtonComponent() {
        composeTestRule.setContent {
            IconTextProgressButton(
                icon = R.drawable.icon_play_focused,
                buttonText = "Play Season 1 Episode 1",
                currentProgress = 0.3F,
                onClick = {},
                isButtonFocused = true
            )
        }
        composeTestRule.onRoot()
            .captureRoboImage(filePath = "screenshots/button/focusedIconTextProgressButtonComponent.png")
    }
}