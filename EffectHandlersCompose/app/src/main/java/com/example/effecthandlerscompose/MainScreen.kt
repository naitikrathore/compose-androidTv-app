package com.example.effecthandlerscompose

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")

@Preview
@Composable
fun MainScreen(){
    var state= remember { mutableStateOf(false) }
    println("Main")

    if(state.value) {
        DisposableEffect(key1 = state.value) {
            println("Entered Disposable")
            onDispose {
                println("Cleanup Done")
            }
        }
    }

    Button(
        onClick = {
            state.value=!state.value
        },
        modifier = Modifier.size(width = 200.dp, height = 100.dp),
        shape = RoundedCornerShape(0.dp)

    ) {
        Text(text = "Change State")
    }
    println("Main Exit")

}













//22222222222222222222222222
//fun MainScreen() {
//    println("Entered Main")
//    var changeValue by rememberSaveable { mutableStateOf(false) }
//    Column(
//        modifier = Modifier.fillMaxSize(),
//        horizontalAlignment = Alignment.CenterHorizontally
//    ) {
//        Button(
//            onClick = {
//                 changeValue=!changeValue
//                println("Entered Button")
//            },
//            modifier = Modifier.size(width=120.dp, height = 100.dp),
//            shape= RoundedCornerShape(0.dp),
//            colors=ButtonDefaults.buttonColors(Color.Red)
//        ) {
//            Text(text = "Toggle", fontSize = 19.sp)
//        }
//        if(changeValue){
//            println("Value Main")
//            DisposableEffectComposable()
//        }
//    }
//}
//
//@Composable
//fun  DisposableEffectComposable(){
//    println("Entering Composition")
//     DisposableEffect(key1 = true) {
//         println("Entered Effect Scope")
//         onDispose {
//             println("Do the clean up (Leaving)")
//         }
//     }
//    Text(text = "Example Text", fontSize = 25.sp)
//    println("After Compositon")
//}
//










//11111111111111111111111111111111
//fun MainScreen() {
//
////    side effect will take no params and it will execute every time composition is sucessful
//    // means when whole code execution is done of the fucntion then side effect executes  Rarely used
////    SideEffect {
////
////    }
//    println("Before composition")
//
//    var changeState by rememberSaveable { mutableStateOf(false) }
//    val snackbarHostState= remember { SnackbarHostState()}
//
//    if(changeState) {
//        LaunchedEffect(key1 = changeState) {
//            snackbarHostState.showSnackbar("Testing Done")
//            println("Launch Effect Execution")
//            //with key=true can use viewmodel.intializeLongTask()
//
//        }
//    }
//
//    Scaffold(
//        snackbarHost = { SnackbarHost(snackbarHostState)}
//    ) {
//        Button(onClick = {
//            changeState=!changeState
//        }) {
//            Text("Toggle")
//        }
//        println("Comp done")
//    }
//}