package com.example.mobilebrowser.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mobilebrowser.data.service.AuthService
import com.example.mobilebrowser.data.util.UserDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authService: AuthService,
    private val userDataStore: UserDataStore
) : ViewModel() {

    // Expose authentication state from datastore
    val isSignedIn = userDataStore.isSignedIn
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    // User display name from datastore
    val userName = userDataStore.userName
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ""
        )

    // User email from datastore
    val userEmail = userDataStore.userEmail
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ""
        )

    // Error message state
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /**
     * Initiate sign in process by opening the authentication website
     */
    fun signIn() {
        authService.openLoginPage()
    }

    /**
     * Sign out the current user
     */
    fun signOut() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                authService.signOut()
            } catch (e: Exception) {
                _errorMessage.value = "Failed to sign out: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Clear any error message
     */
    fun clearError() {
        _errorMessage.value = null
    }
}