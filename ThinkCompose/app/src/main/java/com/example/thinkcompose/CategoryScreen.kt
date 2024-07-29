package com.example.thinkcompose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
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
fun PreviewCategoryScreen() {
    // Create a mock NavHostController
    val navController = rememberNavController()

    // Call your CategoryScreen composable with the mock NavController
    CategoryScreen(navHostController = navController)
}
@Composable

fun CategoryScreen(navHostController: NavHostController) {
    Surface() {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(color = primaryColor)
                .padding(top = 15.dp, bottom = 50.dp)
        ) {
            Column() {
                MainToolBar(title = "Category") {

                }
                LazyColumn() {
                    items(getList()) { item ->
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(85.dp)
                                .padding(start = 15.dp, end = 15.dp, top = 15.dp)
                                .clickable {
                                    navHostController.navigate(ThinkSealed.detailScreen.route + "/${item.title}")
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = primaryLight
                            )
                        )
                        {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = item.title.toString(),
                                    style = TextStyle(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 17.sp
                                    ),
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

            }
        }
    }
}