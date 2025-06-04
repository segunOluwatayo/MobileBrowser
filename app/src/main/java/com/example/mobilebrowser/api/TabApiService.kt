package com.example.mobilebrowser.api

import com.example.mobilebrowser.data.dto.ApiResponse
import com.example.mobilebrowser.data.dto.TabDto
import retrofit2.http.*

interface TabApiService {

//     Adds a new tab to the server.
    @POST("tabs")
    suspend fun addTab(
        @Header("Authorization") authorization: String,
        @Body tab: TabDto
    ): ApiResponse<TabDto>


//     Updates an existing tab on the server.

    @PUT("tabs/{id}")
    suspend fun updateTab(
        @Header("Authorization") authorization: String,
        @Path("id") id: String,
        @Body tab: TabDto
    ): ApiResponse<TabDto>

//     * Deletes a tab from the server.
    @DELETE("tabs/{id}")
    suspend fun deleteTab(
        @Header("Authorization") authorization: String,
        @Path("id") id: String
    ): ApiResponse<Any>

//     Gets all tabs for the authenticated user.
    @GET("tabs")
    suspend fun getAllTabs(
        @Header("Authorization") authorization: String
    ): ApiResponse<List<TabDto>>
}