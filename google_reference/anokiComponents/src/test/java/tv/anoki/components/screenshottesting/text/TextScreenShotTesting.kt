package tv.anoki.components.screenshottesting.text

import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import tv.anoki.components.text.BodyText
import tv.anoki.components.text.HeadingText
import tv.anoki.components.text.TitleText

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = RobolectricDeviceQualifiers.Television4K)
class TextScreenShotTesting {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun headingTextComponent() {
        composeTestRule.setContent {
            HeadingText(text = "Loki Original series")
        }
        composeTestRule.onRoot()
            .captureRoboImage(filePath = "screenshots/text/headingTextComponent.png")
    }

    @Test
    fun headingTextComponentMaxLineOneMarquee() {
        composeTestRule.setContent {
            HeadingText(
                text = "Loki, the God of Mischief, steps out of his brother's shadow",
                modifier = Modifier.width(480.dp),
                maxLines = 1
            )
        }
        composeTestRule.onRoot()
            .captureRoboImage(filePath = "screenshots/text/headingTextComponentMaxLineOneMarquee.png")
    }

    @Test
    fun headingTextComponentCustomFontSize() {
        composeTestRule.setContent {
            HeadingText(
                text = "Loki Original series",
                fontSize = TextUnit(value = 32F, type = TextUnitType.Sp)
            )
        }
        composeTestRule.onRoot()
            .captureRoboImage(filePath = "screenshots/text/headingTextComponentCustomFontSize.png")
    }

    @Test
    fun headingTextComponentCustomFontSizeMaxLineOneMarquee() {
        composeTestRule.setContent {
            HeadingText(
                text = "Loki, the God of Mischief, steps out of his brother's shadow",
                fontSize = TextUnit(value = 32F, type = TextUnitType.Sp),
                modifier = Modifier.width(480.dp),
                maxLines = 1
            )
        }
        composeTestRule.onRoot()
            .captureRoboImage(filePath = "screenshots/text/headingTextComponentCustomFontSizeMaxLineOneMarquee.png")
    }

    @Test
    fun titleTextComponent() {
        composeTestRule.setContent {
            TitleText(text = "Top Picks For You")
        }
        composeTestRule.onRoot()
            .captureRoboImage(filePath = "screenshots/text/titleTextComponent.png")
    }

    @Test
    fun bodyTextComponent() {
        composeTestRule.setContent {
            BodyText(
                modifier = Modifier.width(480.dp),
                text = "Loki, the God of Mischief, steps out of his brother's shadow to embark on an adventure that takes place after the events of \"Avengers: Endgame.\", Loki, the God of Mischief, steps out of his brother's shadow to embark on an adventure that takes place after the events of \"Avengers: Endgame.\""
            )
        }
        composeTestRule.onRoot()
            .captureRoboImage(filePath = "screenshots/text/bodyTextComponent.png")
    }

    @Test
    fun bodyTextWithMaxLineThreeComponent() {
        composeTestRule.setContent {
            BodyText(
                modifier = Modifier.width(480.dp),
                text = "Loki, the God of Mischief, steps out of his brother's shadow to embark on an adventure that takes place after the events of \"Avengers: Endgame.\", Loki, the God of Mischief, steps out of his brother's shadow to embark on an adventure that takes place after the events of \"Avengers: Endgame.\"",
                maxLines = 3
            )
        }
        composeTestRule.onRoot()
            .captureRoboImage(filePath = "screenshots/text/bodyTextWithMaxLineThreeComponent.png")
    }
}