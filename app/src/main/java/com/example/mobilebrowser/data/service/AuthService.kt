package com.example.mobilebrowser.data.service

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.mobilebrowser.data.util.UserDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject
import javax.inject.Singleton
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Singleton
class AuthService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userDataStore: UserDataStore
) {
    private val TAG = "AuthService"
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    // Authentication server URL
    private val authServerUrl = "https://nimbus-browser-backend-production.up.railway.app/?mobile=true"

    /**
     * Opens the login page in the browser
     * This is usually called from elsewhere in the app (not from GeckoSessionManager)
     */
    fun openLoginPage() {
        // This is typically handled by navigating to the URL in your browser
        Log.d(TAG, "Login page URL: $authServerUrl")
        // Navigation is handled by the calling component
    }

    /**
     * Signs out the current user by clearing token data
     */
    suspend fun signOut() {
        try {
            // Get the tokens first so we can make an API call to invalidate them
            val accessToken = userDataStore.accessToken.first()
            val refreshToken = userDataStore.refreshToken.first()

            // If we have valid tokens, make an API call to invalidate them on the server
            if (accessToken.isNotBlank() && refreshToken.isNotBlank()) {
                try {
                    // TODO: Make API call to logout endpoint to invalidate tokens
                    // For now, just log the attempt
                    Log.d(TAG, "Attempting to invalidate tokens on server")
                } catch (e: Exception) {
                    Log.e(TAG, "Error invalidating tokens: ${e.message}")
                }
            }

            // Clear the local token data regardless of server response
            userDataStore.clearUserAuthData()
            Log.d(TAG, "User signed out")
        } catch (e: Exception) {
            Log.e(TAG, "Error during sign out: ${e.message}")
            throw e
        }
    }

    /**
     * Process authentication data from OAuth callback
     * This method should be called from GeckoSessionManager when it detects the OAuth callback URL
     */
    fun processAuthCallback(
        accessToken: String,
        refreshToken: String,
        userId: String?,
        displayName: String?,
        email: String?
    ) {
        coroutineScope.launch {
            try {
                userDataStore.saveUserAuthData(
                    accessToken = accessToken,
                    refreshToken = refreshToken,
                    userId = userId ?: "",
                    displayName = displayName ?: email ?: "User",
                    email = email ?: "",
                    deviceId = null
                )
                Log.d(TAG, "Authentication data saved to DataStore")
            } catch (e: Exception) {
                Log.e(TAG, "Error saving auth data to DataStore: ${e.message}")
            }
        }
    }

    fun checkAuthState() {
        coroutineScope.launch {
            // Assuming your UserDataStore provides a StateFlow<Boolean> for sign-in status
            val isSignedIn = userDataStore.isSignedIn.first()
            Log.d(TAG, "User authentication state: $isSignedIn")
            // Add additional logic as needed
        }
    }

}