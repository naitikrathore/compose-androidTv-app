package com.example.thinkcompose.Routing

sealed class ThinkSealed(val route: String) {
    object splashScreen : ThinkSealed("splash")
    object categoryScreen : ThinkSealed("category")
    object detailScreen : ThinkSealed("detail")
    object finalScreen : ThinkSealed("final")
}