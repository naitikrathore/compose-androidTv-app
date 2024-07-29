package tv.anoki.components.text

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TextVariantsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setUp() {
        composeTestRule.setContent {    // setting our composable as content for test
            MaterialTheme {
                Box {
                    TitleText(text = "TitleText")
                }
            }
        }
    }

    @Test
    fun titleText_itemShown() {
        composeTestRule.onNodeWithText("TitleText").assertIsDisplayed()
    }
}