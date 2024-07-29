package com.example.tvapp.data

import java.io.Serializable

data class MovieDataType (
    var id:Long=0,
    var title:String?=null,
    var description:String?=null,
    var backgroundImage:Int?=null,
    var studio:String?=null,
    var videoURL:String?=null
):Serializable{
}