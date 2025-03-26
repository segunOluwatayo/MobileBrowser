package com.example.mobilebrowser.data.service

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.mobilebrowser.data.util.UserDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userDataStore: UserDataStore
) {
    // Base URL for your authentication web app
    private val AUTH_BASE_URL = "https://your-auth-website.com" // Replace with your auth website

    // URL for opening login page
    private val LOGIN_URL = "$AUTH_BASE_URL/login?mobile=true"

    // URL for token refresh
    private val TOKEN_REFRESH_URL = "$AUTH_BASE_URL/api/auth/token/refresh"

    /**
     * Process the authentication deep link and save user data
     */
    suspend fun processAuthDeepLink(uri: Uri): Boolean {
        return try {
            // Extract tokens and user info from URI
            val accessToken = uri.getQueryParameter("accessToken") ?: return false
            val refreshToken = uri.getQueryParameter("refreshToken") ?: return false
            val userId = uri.getQueryParameter("userId") ?: ""
            val displayName = uri.getQueryParameter("displayName") ?: ""
            val email = uri.getQueryParameter("email") ?: ""
            val deviceId = uri.getQueryParameter("deviceId")

            // Save to DataStore
            userDataStore.saveUserAuthData(
                accessToken = accessToken,
                refreshToken = refreshToken,
                userId = userId,
                displayName = displayName,
                email = email,
                deviceId = deviceId
            )

            // Return success
            true
        } catch (e: Exception) {
            // Log error and return false
            e.printStackTrace()
            false
        }
    }

    /**
     * Check if user is authenticated
     */
    suspend fun isAuthenticated(): Boolean {
        return userDataStore.isSignedIn.first()
    }

    /**
     * Open the login page in a browser
     */
    fun openLoginPage() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(LOGIN_URL))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    /**
     * Sign out the current user
     */
    suspend fun signOut() {
        userDataStore.clearUserAuthData()
    }

    /**
     * Get current user display name
     */
    suspend fun getCurrentUserName(): String {
        return userDataStore.userName.first()
    }

    /**
     * Get current user email
     */
    suspend fun getCurrentUserEmail(): String {
        return userDataStore.userEmail.first()
    }

    /**
     * Get access token for API requests
     */
    suspend fun getAccessToken(): String? {
        return userDataStore.accessToken.firstOrNull()
    }
}