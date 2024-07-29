package com.example.textcompose

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp


@Preview
@Composable

fun NotificationScreen(){
    var cnt = rememberSaveable{mutableStateOf(0)}
//    var cnt by rememberSaveable { mutableStateOf(0) }
    //in this by using deligate now cnt will have int itset rather than mutablestate so
    // we can get value directly with cnt istead of cnt.value

//    var cnt: MutableState<Int> = rememberSaveable { mutableStateOf(0) }


    Column (
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize(2f)
    ){
        NotificationCounter(cnt.value ,{cnt.value++})
        MessageBar(cnt.value)
    }
}

@Composable
fun MessageBar(cnt: Int) {
   Card(
       elevation = CardDefaults.cardElevation(4.dp)
   ){
        Row(
            Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                imageVector = Icons.Outlined.Favorite,
                contentDescription = "",
                Modifier.padding(4.dp)
            )
            Text(text = "Message send so far - ${cnt}")
        }
    }
}

@Composable
fun NotificationCounter(cnt:Int, Incr: () -> Unit){
    Column(verticalArrangement = Arrangement.Center) {
        Text(text = "You have sent ${cnt}")
        Button(onClick = { Incr() }) {
            Text(text = "Send Noti")
        }
    }
}



//
//@Preview(heightDp = 500)
//@Composable
//fun PreviewItem() {
//
//    LazyColumn(content = {
//       items(getCategoryList()){ item ->
//           BlogCategory(img = item.img, title = item.title, subtitle = item.subtitle)
//       }
//    })
////    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
////
////        getCategoryList().map { item ->
////            BlogCategory(img = item.img, title = item.title, subtitle = item.subtitle)
////        }
////
////    }
//}
//
////@Preview -- it wont work for parametarsed composable
//@Composable
//fun BlogCategory(img: Int, title: String, subtitle: String) {
//    Card(elevation = CardDefaults.cardElevation(3.dp), modifier = Modifier.padding(8.dp)) {
//        Row(
//            verticalAlignment = Alignment.CenterVertically,
//            modifier = Modifier.padding(8.dp)
//        ) {
//            Image(
//                painter = painterResource(id = R.drawable.profile),
//                contentDescription = "",
//                modifier = Modifier
//                    .size(44.dp)
//                    .padding(8.dp)
//                    .weight(.2f)
//            )
//            ItemDescription(title, subtitle, modifier = Modifier.weight(.8f))
//        }
//    }
//
//}
//
//@Composable
//fun ItemDescription(title: String, subtitle: String, modifier: Modifier) {
//    Column(modifier = modifier) {
//        Text(
//            text = title,
//            style = MaterialTheme.typography.bodyLarge
//        )
//        Text(
//            text = subtitle,
//            fontWeight = FontWeight.Thin,
//            style = MaterialTheme.typography.headlineLarge,
//            fontSize = 12.sp
//        )
//    }
//}
//
//data class Category(
//    val img: Int,
//    val title: String,
//    val subtitle: String
//)
//
//fun getCategoryList(): MutableList<Category> {
//    val list = mutableListOf<Category>()
//    list.add(Category(R.drawable.profile, "Naitik", "Learn"))
//    list.add(Category(R.drawable.profile, "Rohit", "Learn"))
//    list.add(Category(R.drawable.profile, "Vivek", "Learn"))
//    list.add(Category(R.drawable.profile, "Satik", "Learn"))
//    list.add(Category(R.drawable.profile, "Naitik", "Learn"))
//    list.add(Category(R.drawable.profile, "Rohit", "Learn"))
//    list.add(Category(R.drawable.profile, "Vivek", "Learn"))
//    list.add(Category(R.drawable.profile, "Satik", "Learn"))
//    list.add(Category(R.drawable.profile, "Naitik", "Learn"))
//    list.add(Category(R.drawable.profile, "Rohit", "Learn"))
//    list.add(Category(R.drawable.profile, "Vivek", "Learn"))
//    list.add(Category(R.drawable.profile, "Satik", "Learn"))
//    list.add(Category(R.drawable.profile, "Naitik", "Learn"))
//    list.add(Category(R.drawable.profile, "Rohit", "Learn"))
//    list.add(Category(R.drawable.profile, "Vivek", "Learn"))
//    list.add(Category(R.drawable.profile, "Satik", "Learn"))
//    return list
//}

