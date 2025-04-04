package com.example.mobilebrowser.api

import com.example.mobilebrowser.data.dto.ApiResponse
import com.example.mobilebrowser.data.dto.BookmarkDto
import retrofit2.http.*

interface BookmarkApiService {

    /**
     * Adds a new bookmark to the server.
     * @param authorization The bearer token in the format "Bearer {token}"
     * @param bookmark The bookmark data transfer object
     * @return ApiResponse containing the created BookmarkDto
     */
    @POST("bookmarks")
    suspend fun addBookmark(
        @Header("Authorization") authorization: String,
        @Body bookmark: BookmarkDto
    ): ApiResponse<BookmarkDto>

    /**
     * Updates an existing bookmark.
     * @param authorization The bearer token
     * @param id The server-assigned identifier for the bookmark
     * @param bookmark The updated bookmark DTO
     * @return ApiResponse containing the updated BookmarkDto
     */
    @PUT("bookmarks/{id}")
    suspend fun updateBookmark(
        @Header("Authorization") authorization: String,
        @Path("id") id: String,
        @Body bookmark: BookmarkDto
    ): ApiResponse<BookmarkDto>

    /**
     * Deletes a bookmark from the server.
     * @param authorization The bearer token
     * @param id The server-assigned identifier of the bookmark to delete
     * @return ApiResponse indicating success or failure
     */
    @DELETE("bookmarks/{id}")
    suspend fun deleteBookmark(
        @Header("Authorization") authorization: String,
        @Path("id") id: String
    ): ApiResponse<Any>

    /**
     * Retrieves all bookmarks for the authenticated user.
     * @param authorization The bearer token
     * @return ApiResponse containing a list of BookmarkDto entries
     */
    @GET("bookmarks")
    suspend fun getAllBookmarks(
        @Header("Authorization") authorization: String
    ): ApiResponse<List<BookmarkDto>>
}
