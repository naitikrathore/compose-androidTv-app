package com.example.developertvcompose.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.foundation.lazy.list.items
import androidx.tv.material3.Text
import com.example.developertvcompose.data.Section
import com.example.developertvcompose.screens.MovieCard

//
//@Composable
//@Preview(
//    name = "Section Preview",
//    widthDp = 1280,
//    heightDp = 720,
//    backgroundColor = 0x000000  // black color
//)
//fun PreviewSection() {
//    val sampleSection = Section(
//        title = "Popular Movies",
//        movieList = emptyList()
//    )
//    Section(section = sampleSection)
//}

@Composable
fun Section(
  section: Section,
  navHostController: NavHostController,
){
    Column(
        modifier = Modifier.padding(16.dp)
    ) {
          Text(
              text = section.title,
              style = TextStyle(fontWeight = FontWeight.SemiBold)
          )
        Spacer(modifier = Modifier.height(9.dp))
        TvLazyRow(
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
              items(section.movieList){movie->
                   MovieCard(movie = movie)
              }
        }
    }
}