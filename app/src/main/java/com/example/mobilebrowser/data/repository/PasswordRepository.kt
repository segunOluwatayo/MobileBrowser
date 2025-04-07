package com.example.mobilebrowser.data.repository

import com.example.mobilebrowser.data.dao.PasswordDao
import com.example.mobilebrowser.data.entity.PasswordEntity
import com.example.mobilebrowser.data.util.EncryptionUtil
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PasswordRepository @Inject constructor(
    private val passwordDao: PasswordDao
) {
    // Retrieve all stored passwords as a Flow
    fun getAllPasswords(): Flow<List<PasswordEntity>> = passwordDao.getAllPasswords()

    // Encrypt and store a new password entry
    suspend fun addPassword(siteUrl: String, username: String, plainPassword: String): Long {
        val encryptedPassword = EncryptionUtil.encrypt(plainPassword)
        val passwordEntity = PasswordEntity(
            siteUrl = siteUrl,
            username = username,
            encryptedPassword = encryptedPassword
        )
        return passwordDao.insertPassword(passwordEntity)
    }

    // Encrypt and update an existing password entry
    suspend fun updatePassword(passwordEntity: PasswordEntity, plainPassword: String) {
        val encryptedPassword = EncryptionUtil.encrypt(plainPassword)
        val updatedEntity = passwordEntity.copy(encryptedPassword = encryptedPassword)
        passwordDao.updatePassword(updatedEntity)
    }

    // Delete a password entry
    suspend fun deletePassword(passwordEntity: PasswordEntity) {
        passwordDao.deletePassword(passwordEntity)
    }

    // Decrypt a stored password when needed (e.g., after successful authentication)
    suspend fun decryptPassword(encryptedPassword: String): String {
        return EncryptionUtil.decrypt(encryptedPassword)
    }
}
