package com.example.mobilebrowser.data.service

import android.content.Context
import android.util.Log
import com.example.mobilebrowser.data.util.UserDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
            Log.d(TAG, " signOut() called, getting current tokens")

            // Get the tokens first so I can make an API call to invalidate them
            val accessToken = userDataStore.accessToken.first()
            val refreshToken = userDataStore.refreshToken.first()

            if (accessToken.isNotBlank() && refreshToken.isNotBlank()) {
                try {
                    Log.d(TAG, " Attempting to invalidate tokens on server")
                    // Make API call to logout endpoint
                    withContext(Dispatchers.IO) {
                        try {
                            val url = "https://nimbus-browser-backend-production.up.railway.app/api/auth/logout"
                            val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
                            connection.requestMethod = "POST"
                            connection.setRequestProperty("Content-Type", "application/json")
                            connection.setRequestProperty("Authorization", "Bearer $accessToken")
                            connection.doOutput = true

                            val jsonBody = "{\"refreshToken\":\"$refreshToken\"}"
                            connection.outputStream.use { os ->
                                val input = jsonBody.toByteArray(charset("utf-8"))
                                os.write(input, 0, input.size)
                            }

                            val responseCode = connection.responseCode
                            Log.d(TAG, " Server logout response code: $responseCode")

                            connection.disconnect()
                        } catch (e: Exception) {
                            Log.e(TAG, " Error making server logout request", e)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, " Error invalidating tokens: ${e.message}")
                }
            } else {
                Log.d(TAG, " No valid tokens to invalidate, skipping server call")
            }

            // Clear the local token data regardless of server response
            Log.d(TAG, " Clearing local user data")
            userDataStore.clearUserAuthData()
            Log.d(TAG, " User signed out successfully")
        } catch (e: Exception) {
            Log.e(TAG, " Error during sign out: ${e.message}")
            // Even on error, try to clear the data
            try {
                userDataStore.clearUserAuthData()
                Log.d(TAG, " User data cleared after error")
            } catch (e2: Exception) {
                Log.e(TAG, " Error clearing user data: ${e2.message}")
            }
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
                Log.d(TAG, " Processing auth callback")
                val extractedUserId = userId ?: extractUserIdFromToken(accessToken)
                val extractedEmail = email ?: ""

                // Extract a name from the JWT if possible, otherwise use a default
                val extractedName = displayName ?: extractUserNameFromToken(accessToken) ?: "User"

                Log.d(TAG, " Using extracted user data: id=$extractedUserId, name=$extractedName, email=$extractedEmail")

                userDataStore.saveUserAuthData(
                    accessToken = accessToken,
                    refreshToken = refreshToken,
                    userId = extractedUserId,
                    displayName = extractedName,
                    email = extractedEmail,
                    deviceId = null
                )
                Log.d(TAG, " Authentication data saved to DataStore")
            } catch (e: Exception) {
                Log.e(TAG, " Error saving auth data to DataStore: ${e.message}")

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
                    Log.d(TAG, " Saved basic auth data with default values")
                } catch (e2: Exception) {
                    Log.e(TAG, " Failed to save even basic auth data: ${e2.message}")
                }
            }
        }
    }

    /**
     * Extract the user ID from a JWT token
     */
    private fun extractUserIdFromToken(token: String): String {
        try {
            val parts = token.split(".")
            if (parts.size != 3) {
                Log.e(TAG, "Invalid JWT token format")
                return "unknown"
            }

            val payload = String(android.util.Base64.decode(parts[1], android.util.Base64.URL_SAFE))
            val json = JSONObject(payload)

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
            val parts = token.split(".")
            if (parts.size != 3) {
                Log.e(TAG, "Invalid JWT token format")
                return null
            }

            val payload = String(android.util.Base64.decode(parts[1], android.util.Base64.URL_SAFE))
            val json = JSONObject(payload)

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
            val isSignedIn = userDataStore.isSignedIn.first()
            Log.d(TAG, " User authentication state: $isSignedIn")
        }
    }
}