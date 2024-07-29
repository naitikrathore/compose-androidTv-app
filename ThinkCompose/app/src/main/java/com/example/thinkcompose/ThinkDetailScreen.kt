package com.example.thinkcompose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.thinkcompose.Routing.ThinkSealed
import com.example.thinkcompose.ui.theme.primaryColor
import com.example.thinkcompose.ui.theme.primaryLight

@Composable
@Preview
fun Preview() {
    val navHostController= rememberNavController()
    ThinkDetailScreen(navHostController,title = "Kotlin")
}

@Composable
fun ThinkDetailScreen(navHostController: NavHostController? = null, title: String?) {
    Surface() {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(color = primaryColor)
        ) {
            Column() {
                MainToolBar(title = title.toString()) {

                }
                val mylist = getList().filter {
                    it.title == title.toString()
                }
                val res = mylist[0]
                res.list?.let { list ->
                    LazyColumn {
                        items(list) { item ->
                            Card(modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 15.dp)
                                .clickable {
                                           navHostController?.navigate(ThinkSealed.finalScreen.route +"/$item")
                                },
                                colors = CardDefaults.cardColors(containerColor = primaryLight)
                            ) {
                                Text(
                                    text = item,
                                    color = Color.White,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(15.dp),
                                    textAlign = TextAlign.Center,
                                    style = TextStyle(
                                        fontSize = 18.sp
                                    )
                                )
                            }

                        }
                    }

                }
            }

        }

    }
}