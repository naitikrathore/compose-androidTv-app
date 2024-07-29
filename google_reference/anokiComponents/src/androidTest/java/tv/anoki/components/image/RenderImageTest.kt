package tv.anoki.components.image

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RenderImageTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun cropImage_glideImageRendered() {
        composeTestRule.setContent {
            MaterialTheme {
                RenderCropImage(
                    imageUrl = "https://raw.githubusercontent.com/bumptech/glide/master/static/glide_logo.png",
                    imageDescription = "glide image"
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("glide image")
            .assertExists("Image not rendered")
            .assertIsNotDisplayed()
            .assertContentDescriptionEquals("glide image")
    }

    @Test
    fun cropImage_coilImageRendered() {
        composeTestRule.setContent {
            MaterialTheme {
                RenderCropImage(
                    imageUrl = "https://coil-kt.github.io/coil/logo.svg",
                    imageDescription = "coil image"
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("coil image")
            .assertExists("Image not rendered")
            .assertIsDisplayed()
            .assertContentDescriptionEquals("coil image")
    }

}