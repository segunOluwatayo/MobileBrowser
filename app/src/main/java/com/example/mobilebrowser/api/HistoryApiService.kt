package com.example.mobilebrowser.api

import com.example.mobilebrowser.data.dto.ApiResponse
import com.example.mobilebrowser.data.dto.HistoryDto
import retrofit2.http.*

/**
 * Retrofit interface for history-related API endpoints.
 */
interface HistoryApiService {
    /**
     * Retrieves all history entries for the authenticated user.
     *
     * @param authorization Bearer token for authentication
     * @return ApiResponse containing a list of history entries
     */
    @GET("history")
    suspend fun getHistory(
        @Header("Authorization") authorization: String
    ): ApiResponse<List<HistoryDto>>

    /**
     * Adds a new history entry to the server.
     *
     * @param authorization Bearer token for authentication
     * @param historyEntry The history data to upload
     * @return ApiResponse containing the saved history entry with server-assigned ID
     */
    @POST("history")
    suspend fun addHistoryEntry(
        @Header("Authorization") authorization: String,
        @Body historyEntry: HistoryDto
    ): ApiResponse<HistoryDto>

    /**
     * Updates an existing history entry on the server.
     *
     * @param authorization Bearer token for authentication
     * @param id Server ID of the history entry to update
     * @param historyEntry Updated history data
     * @return ApiResponse containing the updated history entry
     */
    @PUT("history/{id}")
    suspend fun updateHistoryEntry(
        @Header("Authorization") authorization: String,
        @Path("id") id: String,
        @Body historyEntry: HistoryDto
    ): ApiResponse<HistoryDto>

    /**
     * Deletes a history entry from the server.
     *
     * @param authorization Bearer token for authentication
     * @param id Server ID of the history entry to delete
     * @return ApiResponse with operation status
     */
    @DELETE("history/{id}")
    suspend fun deleteHistoryEntry(
        @Header("Authorization") authorization: String,
        @Path("id") id: String
    ): ApiResponse<Any>
}