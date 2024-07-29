package com.example.developertvcompose.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.Text
import com.example.developertvcompose.data.Movie
import org.jetbrains.annotations.Async

@Composable
fun MovieCard(
    movie: Movie,
    onClick:() ->Unit= {}
) {
   Card(onClick = onClick,
       modifier = Modifier
           .size(150.dp,170.dp),
       shape = CardDefaults.shape(RoundedCornerShape(0.dp))
   ) {
       Image(
           painter = painterResource(id = movie.img),
           contentDescription ="this is description"
       )
       Spacer(modifier = Modifier.padding(5.dp))
       Text(text = movie.title)
   }
}