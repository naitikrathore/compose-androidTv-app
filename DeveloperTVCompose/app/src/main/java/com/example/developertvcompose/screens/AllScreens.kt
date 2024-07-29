package com.example.developertvcompose.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.tv.material3.Button
import androidx.tv.material3.Icon
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.example.developertvcompose.R
import com.example.developertvcompose.data.Movie
import com.example.developertvcompose.data.Section

//
@Composable
@Preview(
    widthDp = 932,
    heightDp = 541,
    backgroundColor = 0x000000
)
fun PreviewIT() {
    val navHostController = rememberNavController()
    HomeContent( sectionList = emptyList(), navHostController = navHostController)
}
@Composable
fun HomeContent(
    sectionList: List<Section>,
    navHostController: NavHostController,
    onItemSelected: (Movie) -> Unit = {}
) {

    Surface() {
        Box (modifier = Modifier.fillMaxSize()
            .background(Color.Black),
            contentAlignment = Alignment.TopCenter

        ){
            println("inside")
            Column{
                //Banner
//                MainToolBar()
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(230.dp)
                        .padding(start = 16.dp, end = 16.dp)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black,
                                    Color.Black
                                )
                            )
                        )
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.scram),
                        contentDescription = null,
                        contentScale = ContentScale.Crop, // Ensure the image covers the entire box
                        modifier = Modifier
                            .fillMaxWidth()
                            .alpha(0.7f)
                    )
                    Text(
                        text = "Welcome to  TV",
                        style = TextStyle(
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 20.dp, bottom = 50.dp)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))

                // Content Space
               CatalogBrowser(
                    sectionList = sectionList,
                    navHostController=navHostController
                )
            }
        }

    }
}

@Composable
fun DetailsContent(navHostController: NavHostController) {
    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Image(
            modifier = Modifier.fillMaxSize(),
            painter = painterResource(id = R.drawable.movie8),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alpha = 0.5f,
        )
        Column(
            modifier = Modifier
                .padding(top = 80.dp, start = 60.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0x40000000), Color(0x90000000)),
                    ),
                    shape = CircleShape
                )
                .padding(16.dp)
                .width(400.dp)
                .height(300.dp)
        ) {
            Text(
                modifier = Modifier.padding(start = 130.dp),
                text = "Movie",
                fontSize = 50.sp,
                style = TextStyle(color = Color.White, fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "In a mystical realm where magic and reality intertwine, a young adventurer named Lila discovers a hidden forest filled with mythical creatures and ancient secrets.",
                style = TextStyle(color = Color.White, fontSize = 16.sp),
                modifier = Modifier.padding(start = 10.dp,end = 10.dp)
            )
            Spacer(modifier = Modifier.height(20.dp))
            Row(
                modifier = Modifier.padding(horizontal = 10.dp)
            ) {
                Button(
                    onClick = { /* TODO */ },
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = "Play", tint = Color.White)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Play",
                        fontWeight = FontWeight.Bold,
                    )
                }
                Button(
                    onClick = { /* TODO */ },
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp)
                ) {
                    Icon(Icons.Filled.Favorite, contentDescription = "Favourite", tint = Color.White)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Favourite")
                }
            }
        }
    }
}

@Composable
fun RecentContent() {
   Text(text = "Recent")
}

@Composable
fun FavouriteContent() {
    // Your Favourite content goes here
}

@Composable
fun SearchContent() {
    // Your Search content goes here
}

@Composable
fun SettingsContent() {
    // Your Settings content goes here
}