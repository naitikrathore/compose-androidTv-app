package com.example.thinkcompose

import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.pm.ShortcutInfoCompat.Surface
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.thinkcompose.Routing.ThinkSealed
import com.example.thinkcompose.ui.theme.primaryColor
import com.example.thinkcompose.ui.theme.primaryLight
import com.example.thinkcompose.ui.theme.purpleColor
import kotlin.coroutines.coroutineContext


@Composable
@Preview
fun PreviewFinal() {
    val navHostController = rememberNavController()
    FinalScreen(navHostController = navHostController, stringitem = "Hello")
}

@Composable
fun FinalScreen(navHostController: NavHostController, stringitem: String?) {

    Surface() {
        val content= LocalContext.current
        val clipboardManager:ClipboardManager= LocalClipboardManager.current
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(color = primaryColor),
            contentAlignment = Alignment.Center
        ) {
            Column() {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = primaryLight),
                    border = BorderStroke(width = 2.dp, color = Color.White)
                ) {
                    Box(
                        modifier = Modifier
                    ) {
                        Text(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(25.dp),
                            text = stringitem!!,
                            color = Color.White,
                            style = TextStyle(
                                fontSize = 18.sp, fontWeight = FontWeight.Bold
                            ),
                            textAlign = TextAlign.Center
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Card(
                        modifier = Modifier
                            .size(35.dp, 40.dp)
                            .clickable {
                                val shareIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, stringitem)
                                    type = "text/plain"
                                }
                                content.startActivity(Intent.createChooser(shareIntent, "Share via"))

                            },
                        colors = CardDefaults.cardColors(containerColor = primaryLight),
                        shape = RoundedCornerShape(100.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_share),
                                contentDescription = null,
                                tint = Color.White
                            )
                        }

                    }
                    Card(
                        modifier = Modifier
                            .size(35.dp, 40.dp)
                            .clickable {
                                clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(
                                        stringitem.toString()
                                    ))
                                Toast.makeText(content, "Text copied", Toast.LENGTH_SHORT).show()
                            },
                        colors = CardDefaults.cardColors(containerColor = primaryLight),
                        shape = RoundedCornerShape(100.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_copy),
                                contentDescription = null,
                                tint = Color.White
                            )
                        }

                    }
                }


            }

        }
    }
}
