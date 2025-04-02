package com.example.mobilebrowser.api

import com.example.mobilebrowser.data.dto.HistoryDto
import retrofit2.Response
import retrofit2.http.*

/**
 * Retrofit service interface for History API operations.
 * Provides endpoints to retrieve, add/update, and delete history entries.
 */
interface HistoryApiService {

    /**
     * Retrieve all history entries for the authenticated user.
     *
     * @param authHeader The authorization header in the format "Bearer <token>".
     * @param deviceId The device identification header.
     * @param userId The user identifier to filter history entries.
     * @return A Response containing a list of HistoryDto.
     */
    @GET("history")
    suspend fun getHistory(
        @Header("Authorization") authHeader: String,
        @Header("X-Device-ID") deviceId: String,
        @Query("userId") userId: String
    ): Response<List<HistoryDto>>

    /**
     * Add a new history entry or update an existing one.
     * The backend should decide whether to insert or update based on the provided data.
     *
     * @param authHeader The authorization header in the format "Bearer <token>".
     * @param deviceId The device identification header.
     * @param history The HistoryDto object representing the history entry.
     * @return A Response containing the created or updated HistoryDto.
     */
    @POST("history")
    suspend fun addOrUpdateHistory(
        @Header("Authorization") authHeader: String,
        @Header("X-Device-ID") deviceId: String,
        @Body history: HistoryDto
    ): Response<HistoryDto>

    /**
     * Delete a specific history entry from the backend.
     *
     * @param authHeader The authorization header in the format "Bearer <token>".
     * @param deviceId The device identification header.
     * @param serverId The server-assigned ID of the history entry to delete.
     * @return A Response with no content on success.
     */
    @DELETE("history/{id}")
    suspend fun deleteHistoryEntry(
        @Header("Authorization") authHeader: String,
        @Header("X-Device-ID") deviceId: String,
        @Path("id") serverId: String
    ): Response<Unit>

    /**
     * Clear all history entries for the authenticated user.
     * Maps to DELETE /api/history on the backend.
     *
     * @param authHeader The authorization header in the format "Bearer <token>".
     * @param deviceId The device identification header.
     * @param userId The user identifier to specify whose history should be cleared.
     * @return A Response with no content on success.
     */
    @DELETE("history")
    suspend fun clearHistory(
        @Header("Authorization") authHeader: String,
        @Header("X-Device-ID") deviceId: String,
        @Query("userId") userId: String
    ): Response<Unit>
}
