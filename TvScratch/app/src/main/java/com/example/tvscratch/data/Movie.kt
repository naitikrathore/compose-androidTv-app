package com.example.tvscratch.data

data class Movie (
    val id:Long=0,
    var title:String,
    var description:String=" ",
    var img:Int,
    var isFav:Int=0,
    var isRecent:Int=0
)