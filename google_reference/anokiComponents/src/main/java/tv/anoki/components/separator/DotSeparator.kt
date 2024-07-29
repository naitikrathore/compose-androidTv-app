package tv.anoki.components.separator

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * This component is to render solid circle. The size of this component is managed using modifier params.
 *
 * @param modifier Modifier to be applied to the Composable
 */
@Composable
fun DotSeparator(
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.padding(horizontal = 8.dp)) {
        Spacer(
            modifier = Modifier
                .background(
                    shape = RoundedCornerShape(100),
                    color = Color.Gray
                )
                .size(6.dp)
        )
    }
}

@Preview
@Composable
fun DotSeparatorPreview() {
    DotSeparator()
}