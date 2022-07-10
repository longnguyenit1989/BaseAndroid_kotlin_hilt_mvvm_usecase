package com.example.baseandroid.data.remote

import com.example.baseandroid.models.LoginResponse
import com.example.baseandroid.models.PagingResponse
import com.example.baseandroid.models.UserResponse
import retrofit2.Call
import retrofit2.http.*

interface ApiClientRefreshTokenable {
    @FormUrlEncoded
    @POST("login")
    fun callLogin(
        @Field("email") email: String,
        @Field("password") password: String
    ): Call<LoginResponse>

    @GET("user")
    fun getUserInfo(): Call<UserResponse>

    @GET("paging")
    fun getList(
        @Query("page") page: Int
    ): Call<PagingResponse>
}