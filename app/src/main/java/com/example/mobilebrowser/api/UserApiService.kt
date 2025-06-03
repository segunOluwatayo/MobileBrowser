package com.example.mobilebrowser.api

import com.example.mobilebrowser.data.dto.UserProfileDto
import retrofit2.http.*

interface UserApiService {
    @GET("users/me")
    suspend fun getUserProfile(
        @Header("Authorization") authorization: String
    ): UserProfileDto
}