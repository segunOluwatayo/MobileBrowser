package com.example.mobilebrowser.api

import com.example.mobilebrowser.data.dto.ApiResponse
import com.example.mobilebrowser.data.dto.HistoryDto
import retrofit2.http.*

interface HistoryApiService {

//     Adds a new history entry to the server.
    @POST("history")
    suspend fun addHistoryEntry(
        @Header("Authorization") authorization: String,
        @Body history: HistoryDto
    ): ApiResponse<HistoryDto>

//     Deletes a history entry by its server ID.
    @DELETE("history/{id}")
    suspend fun deleteHistoryEntry(
        @Header("Authorization") authorization: String,
        @Path("id") id: String
    ): ApiResponse<Any>


//     Deletes history entries by URL.

//    @HTTP(method = "DELETE", path = "history", hasBody = true)
//    suspend fun deleteHistoryEntryByUrl(
//        @Header("Authorization") authorization: String,
//        @Query("url") url: String,
//        @Body deleteRequest: DeleteHistoryRequest
//    ): ApiResponse<Any>

//    data class DeleteHistoryRequest(
//        val url: String,
//        val userId: String,
//        val device: String
//    )

    @DELETE("history/url")
    suspend fun deleteHistoryEntryByUrl(
        @Header("Authorization") authorization: String,
        @Query("url") url: String
    ): ApiResponse<Any>


//     Gets all history entries for the authenticated user.
    @GET("history")
    suspend fun getHistory(
        @Header("Authorization") authorization: String
    ): ApiResponse<List<HistoryDto>>
}