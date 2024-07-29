package com.example.developertvcompose.data

data class Movie (
    val id:Long=0,
    var title:String,
    var description:String=" ",
    var img:Int,
    var isFav:Int=0,
    var isRecent:Int=0
)

data class Section(
    val title: String,
    val movieList:List<Movie> = emptyList()
)
