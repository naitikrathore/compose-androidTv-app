package com.example.appcomp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusModifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.appcomp.ui.theme.AppCompTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {

            Column (
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)

            ){
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .padding(16.dp)
                ) {
                    val painter = painterResource(id = R.drawable.srambled_poster)
                    val desc = "Kermit in the snow"
                    val title = "Naitik Rathore"
                    ImageCard(painter = painter, desc = desc, title = title)
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .padding(16.dp)
                ) {
                    val painter = painterResource(id = R.drawable.movie7)
                    val desc = "Kermit in the snow"
                    val title = "Naitik Rathore"
                    ImageCard(painter = painter, desc = desc, title = title)
                }
            }



        }
    }
}

@Composable
fun ImageCard(
    painter: Painter,
    desc: String,
    title: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(15.dp),
        elevation = CardDefaults.cardElevation(15.dp)
    ) {
        Box(modifier = Modifier.height(200.dp)) {
            Image(
                painter = painter,
                contentDescription = desc,
                contentScale = ContentScale.Crop
            )
            Box(modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black
                        ),
                        startY = 300f
                    )
                )
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                contentAlignment = Alignment.BottomStart
            ) {
                Text(title, style = TextStyle(color = Color.White, fontSize = 16.sp))
            }

        }
    }
}
@Preview(showBackground = true)
@Composable
fun ImageCardPreview() {
    AppCompTheme {
        ImageCard(
            painter = painterResource(id = R.drawable.movie7),
            desc = "Kermit in the snow",
            title = "Naitik Rathore"
        )
    }
}


//
//
//setContent {
//    Column(
//        modifier = Modifier
//            .border(10.dp, Color.Red)
//
//            .background(Color.Blue)
//            .fillMaxHeight(0.5f)
//            .width(600.dp)  //if entered dp is larger than screen then it will adjust to max the screen size means it will not exceed
////                     .requiredWidth(800.dp)  //the dp value entered will follow at any situatuion if the screen size is smaller than required dp then it will excede
//            .padding(top=10.dp),
//        verticalArrangement = Arrangement.SpaceAround
//
//
//    ) {
//        Text(text = "naitik", Modifier.offset(50.dp,50.dp))
//        Text(text = "rathore", modifier = Modifier.clickable {
//
//        })
//        Text("map")
//        Text(text = "street")
//    }


//            Row (
//                modifier = Modifier
//                    .width(300.dp)
////                    .height(300.dp)
//                    .fillMaxHeight(0.8f)
//                    .background(Color.Yellow),
//                horizontalArrangement = Arrangement.SpaceAround,
//                verticalAlignment = Alignment.CenterVertically
//            ){
//                Text(text = "Naitik")
//                Text(text = "Nishant")
//                Text(text = "Rathore")
//            }
//
//@Composable
//fun Greeting(name: String, modifier: Modifier = Modifier) {
//    Text(
//        text = "Hello $name!",
//        modifier = modifier
//    )
//}
//
//@Preview(showBackground = true)
//@Composable
//fun GreetingPreview() {
//    AppCompTheme {
//        Greeting("Android")
//    }
//}