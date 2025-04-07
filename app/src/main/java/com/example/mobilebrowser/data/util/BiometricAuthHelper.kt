package com.example.mobilebrowser.util

import androidx.fragment.app.FragmentActivity
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat

/**
 * A helper class to handle biometric authentication with device credential fallback.
 * When authentication succeeds, it calls the provided onAuthenticated callback.
 * If authentication fails, it calls the onAuthenticationError callback.
 */
class BiometricAuthHelper(
    private val activity: FragmentActivity,
    private val onAuthenticated: () -> Unit,
    private val onAuthenticationError: (String) -> Unit
) {
    fun authenticate() {
        val executor = ContextCompat.getMainExecutor(activity)
        val biometricPrompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onAuthenticated()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                onAuthenticationError(errString.toString())
            }
        })

        // Use device credential fallback (passcode/PIN/pattern) if biometrics are unavailable.
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Authentication Required")
            .setSubtitle("Authenticate to view saved passwords")
            .setDeviceCredentialAllowed(true)  // Enables fallback to device credentials.
            .build()

        biometricPrompt.authenticate(promptInfo)
    }
}
