package com.example.mobilebrowser.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mobilebrowser.data.entity.PasswordEntity
import com.example.mobilebrowser.data.repository.PasswordRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

class PasswordViewModel @Inject constructor(
    private val passwordRepository: PasswordRepository
) : ViewModel() {

    // Expose the list of stored passwords to the UI as a StateFlow
    val passwords: StateFlow<List<PasswordEntity>> = passwordRepository.getAllPasswords()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Adds a new password after encrypting it
    fun addPassword(siteUrl: String, username: String, plainPassword: String) {
        viewModelScope.launch {
            passwordRepository.addPassword(siteUrl, username, plainPassword)
        }
    }

    // Updates an existing password entry
    fun updatePassword(passwordEntity: PasswordEntity, plainPassword: String) {
        viewModelScope.launch {
            passwordRepository.updatePassword(passwordEntity, plainPassword)
        }
    }

    // Deletes a password entry
    fun deletePassword(passwordEntity: PasswordEntity) {
        viewModelScope.launch {
            passwordRepository.deletePassword(passwordEntity)
        }
    }

    // Decrypts a password (e.g., for display after successful authentication)
    suspend fun getDecryptedPassword(encryptedPassword: String): String {
        return passwordRepository.decryptPassword(encryptedPassword)
    }
}
