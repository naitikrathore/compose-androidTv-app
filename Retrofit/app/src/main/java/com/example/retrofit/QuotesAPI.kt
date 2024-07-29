package com.example.retrofit

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface QuotesAPI {
    @GET("/quotes")
    suspend fun getQuotes(@Query("page") page: Int) : Response<QuoteList>   //jsom will recive in converted form so we need to just specify the type
    //Base URL + "/quotes?page=1
}