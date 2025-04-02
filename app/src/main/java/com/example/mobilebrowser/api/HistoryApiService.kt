package com.example.mobilebrowser.api

import com.example.mobilebrowser.data.dto.ApiResponse
import com.example.mobilebrowser.data.dto.HistoryDto
import retrofit2.http.*

/**
 * API service for interacting with the history endpoints on the server.
 * Enhanced with methods for deleting history entries.
 */
interface HistoryApiService {
    /**
     * Adds a new history entry to the server.
     *
     * @param authorization The authentication token in format "Bearer {token}"
     * @param history The history entry to add
     * @return API response containing the added history entry with server-assigned ID
     */
    @POST("history")
    suspend fun addHistoryEntry(
        @Header("Authorization") authorization: String,
        @Body history: HistoryDto
    ): ApiResponse<HistoryDto>

    /**
     * Deletes a history entry by its server ID.
     *
     * @param authorization The authentication token in format "Bearer {token}"
     * @param id The server-assigned ID of the history entry to delete
     * @return API response indicating success or failure
     */
    @DELETE("history/{id}")
    suspend fun deleteHistoryEntry(
        @Header("Authorization") authorization: String,
        @Path("id") id: String
    ): ApiResponse<Any>

    /**
     * Deletes history entries by URL.
     * This is needed when we don't have a server ID but need to remove entries.
     *
     * @param authorization The authentication token in format "Bearer {token}"
     * @param url The URL of the history entry to delete
     * @return API response indicating success or failure
     */
    @HTTP(method = "DELETE", path = "history", hasBody = true)
    suspend fun deleteHistoryEntryByUrl(
        @Header("Authorization") authorization: String,
        @Query("url") url: String
    ): ApiResponse<Any>

    /**
     * Gets all history entries for the authenticated user.
     *
     * @param authorization The authentication token in format "Bearer {token}"
     * @return API response containing list of history entries
     */
    @GET("history")
    suspend fun getHistory(
        @Header("Authorization") authorization: String
    ): ApiResponse<List<HistoryDto>>
}