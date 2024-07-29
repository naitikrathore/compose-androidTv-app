package tv.anoki.ondemand.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import tv.anoki.ondemand.R

/**
 * This component is to add vertical 20dp spacing between two components
 *
 * @param modifier Modifier to be applied to the Composable
 */
@Composable
fun VerticalSpacing20(modifier: Modifier = Modifier) {
    Spacer(modifier = modifier.height(dimensionResource(id = R.dimen.dp_20)))
}

/**
 * This component is to add horizontal 20dp spacing between two components
 *
 * @param modifier Modifier to be applied to the Composable
 */
@Composable
fun HorizontalSpacing20(modifier: Modifier = Modifier) {
    Spacer(modifier = modifier.width(dimensionResource(id = R.dimen.dp_20)))
}