package com.example.devjetreplica.presenting.screens.dashboard

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.unit.dp
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Tab
import androidx.tv.material3.TabRow
import androidx.tv.material3.Text
import com.example.devjetreplica.presenting.screens.ScreenRoute

val TopBarTabs = ScreenRoute.TabRoutes
val TopBarFocusRequesters = List(size = TopBarTabs.size) { FocusRequester() }


@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun DashboardTopBar(
    modifier: Modifier = Modifier,
    selectedTabIndex: Int,
    screens: List<ScreenRoute> = TopBarTabs,
    focusRequesters: List<FocusRequester> = remember { TopBarFocusRequesters },
    onScreenSelection: (screen: ScreenRoute) -> Unit,
) {
    Log.e("Naitik", "Dashtop ${selectedTabIndex}")

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(15.dp)
            .background(MaterialTheme.colorScheme.surface)
            .focusRestorer()
    ) {
        TabRow(
            selectedTabIndex = selectedTabIndex
        ) {
            screens.forEachIndexed { index, screenRoute ->
                key(index) {
                    Log.e("Naitik", "TabRow ${selectedTabIndex}  and ${index}")
                    Tab(
                        modifier = Modifier
                            .height(32.dp)
                            .focusRequester(focusRequesters[index])
                            .then(
                                if (screenRoute == ScreenRoute.Search) {
                                    Modifier.padding(start = 100.dp)
                                } else {
                                    Modifier
                                }
                            ),
                        selected = index == selectedTabIndex,
                        onFocus = { onScreenSelection(screenRoute) },
                        onClick = { },
                    ) {
                        Text(
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .fillMaxSize()
                                .wrapContentSize(),
                            text = screenRoute.route,
                            style = MaterialTheme.typography.titleSmall.copy(
                                color = LocalContentColor.current
                            ),
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}