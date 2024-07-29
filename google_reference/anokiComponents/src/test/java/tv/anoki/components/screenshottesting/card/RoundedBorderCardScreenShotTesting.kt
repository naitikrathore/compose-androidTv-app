package tv.anoki.components.screenshottesting.card

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.tv.material3.ExperimentalTvMaterial3Api
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import tv.anoki.components.R
import tv.anoki.components.card.RoundedBorderCard

@ExperimentalTvMaterial3Api
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = RobolectricDeviceQualifiers.Television4K)
class RoundedBorderCardScreenShotTesting {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun roundedBorderCardComponent() {
        composeTestRule.setContent {
            RoundedBorderCard(
                modifier = Modifier
                    .width(276.dp)
                    .height(156.dp),
                onClick = {}
            ) {
                Image(
                    modifier = Modifier.fillMaxSize(),
                    painter = painterResource(id = R.drawable.sample),
                    contentDescription = "image description",
                    contentScale = ContentScale.Crop
                )
            }
        }
        composeTestRule.onRoot()
            .captureRoboImage(filePath = "screenshots/card/roundedBorderCardComponent.png")
    }

    @Test
    fun focusedRoundedBorderCardComponent() {
        composeTestRule.setContent {
            RoundedBorderCard(
                modifier = Modifier
                    .width(276.dp)
                    .height(156.dp),
                onClick = {}
            ) {
                Image(
                    modifier = Modifier.fillMaxSize(),
                    painter = painterResource(id = R.drawable.sample),
                    contentDescription = "image description",
                    contentScale = ContentScale.Crop
                )
            }
        }
        composeTestRule.onRoot()
            .captureRoboImage(filePath = "screenshots/card/focusedRoundedBorderCardComponent.png")
    }

}