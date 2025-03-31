package com.example.mobilebrowser.data.service

import android.content.Context
import android.util.Log
import com.example.mobilebrowser.data.util.UserDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import org.json.JSONObject
import java.util.Base64

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
                // If we didn't get user info in the URL params, extract it from the JWT token
                val extractedUserId = userId ?: extractUserIdFromToken(accessToken)
                val extractedEmail = email ?: ""

                // Extract a name from the JWT if possible, otherwise use a default
                val extractedName = displayName ?: extractUserNameFromToken(accessToken) ?: "User"

                Log.d(TAG, "Using extracted user data: id=$extractedUserId, name=$extractedName, email=$extractedEmail")

                userDataStore.saveUserAuthData(
                    accessToken = accessToken,
                    refreshToken = refreshToken,
                    userId = extractedUserId,
                    displayName = extractedName,
                    email = extractedEmail,
                    deviceId = null
                )
                Log.d(TAG, "Authentication data saved to DataStore")
            } catch (e: Exception) {
                Log.e(TAG, "Error saving auth data to DataStore: ${e.message}")

                // Even if extraction fails, still try to save the tokens
                try {
                    userDataStore.saveUserAuthData(
                        accessToken = accessToken,
                        refreshToken = refreshToken,
                        userId = userId ?: "unknown",
                        displayName = "Nimbus User",
                        email = email ?: "",
                        deviceId = null
                    )
                    Log.d(TAG, "Saved basic auth data with default values")
                } catch (e2: Exception) {
                    Log.e(TAG, "Failed to save even basic auth data: ${e2.message}")
                }
            }
        }
    }

    /**
     * Extract the user ID from a JWT token
     */
    private fun extractUserIdFromToken(token: String): String {
        try {
            // JWT tokens have three parts separated by dots
            val parts = token.split(".")
            if (parts.size != 3) {
                Log.e(TAG, "Invalid JWT token format")
                return "unknown"
            }

            // Decode the payload (middle part)
            val payload = String(android.util.Base64.decode(parts[1], android.util.Base64.URL_SAFE))
            val json = JSONObject(payload)

            // JWT from our server should have an "id" field
            return if (json.has("id")) json.getString("id") else "unknown"
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting user ID from token: ${e.message}")
            return "unknown"
        }
    }

    /**
     * Try to extract a username from a JWT token
     */
    private fun extractUserNameFromToken(token: String): String? {
        try {
            // JWT tokens have three parts separated by dots
            val parts = token.split(".")
            if (parts.size != 3) {
                Log.e(TAG, "Invalid JWT token format")
                return null
            }

            // Decode the payload (middle part)
            val payload = String(android.util.Base64.decode(parts[1], android.util.Base64.URL_SAFE))
            val json = JSONObject(payload)

            // Try common name fields that might be in the JWT
            return when {
                json.has("name") -> json.getString("name")
                json.has("email") -> json.getString("email").split("@")[0]
                else -> "Nimbus User"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting user name from token: ${e.message}")
            return null
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