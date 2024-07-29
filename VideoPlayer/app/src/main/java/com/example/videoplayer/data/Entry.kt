package com.example.videoplayer.data

data class Entry (
    val id:Long=0,
    val name: String,
    val link: String,
    val uploader: String,
    val thumbnailPath:String?,
    val profilePicture:String?,
    var isFav:Int
)