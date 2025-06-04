package com.example.mobilebrowser.api

import com.example.mobilebrowser.data.dto.ApiResponse
import com.example.mobilebrowser.data.dto.BookmarkDto
import retrofit2.http.*

interface BookmarkApiService {

//      Add a new bookmark to the server.
    @POST("bookmarks")
    suspend fun addBookmark(
        @Header("Authorization") authorization: String,
        @Body bookmark: BookmarkDto
    ): ApiResponse<BookmarkDto>

//    Updates an existing bookmark.
    @PUT("bookmarks/{id}")
    suspend fun updateBookmark(
        @Header("Authorization") authorization: String,
        @Path("id") id: String,
        @Body bookmark: BookmarkDto
    ): ApiResponse<BookmarkDto>

//     Deletes a bookmark from the server.

    @DELETE("bookmarks/{id}")
    suspend fun deleteBookmark(
        @Header("Authorization") authorization: String,
        @Path("id") id: String
    ): ApiResponse<Any>

//     * Retrieves all bookmarks for the authenticated user.

    @GET("bookmarks")
    suspend fun getAllBookmarks(
        @Header("Authorization") authorization: String
    ): ApiResponse<List<BookmarkDto>>
}
