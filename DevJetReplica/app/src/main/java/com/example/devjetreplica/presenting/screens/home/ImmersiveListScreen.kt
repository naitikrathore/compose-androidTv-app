package com.example.devjetreplica.presenting.screens.home

import android.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import androidx.tv.material3.Text
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme


@Composable
fun ImmersiveListScreen(
    height: Dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
    ) {
        BackgroundShades(color = Color.CYAN)
        VODDetails(height)
    }

}

@Composable
private fun VODDetails(
    height: Dp,
    startPadding: Dp = 48.dp,
    topPadding: Dp = 48.dp
) {
    Box(
        modifier = Modifier
            .height(height)
            .padding(
                start = startPadding,
                top = topPadding
            )
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth(0.55f)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Naitik",
                fontSize = 32.sp,
                style = MaterialTheme.typography.titleLarge
            )
        }
    }
}

@Composable
fun BackgroundShades(
    color: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            color,
                            color,
                            Color.TRANSPARENT
                        ) as List<androidx.compose.ui.graphics.Color>
                    )
                )
        )

    }
}