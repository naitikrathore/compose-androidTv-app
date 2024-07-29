package com.example.developertvcompose.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.items
import com.example.developertvcompose.data.Section
import com.example.developertvcompose.screens.Section

@Composable
fun CatalogBrowser(
    sectionList:List<Section>,
    navHostController: NavHostController,
) {
    TvLazyColumn (
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ){
        items(sectionList){ section->
            Section(
                section=section,navHostController
            )
        }
    }

}
