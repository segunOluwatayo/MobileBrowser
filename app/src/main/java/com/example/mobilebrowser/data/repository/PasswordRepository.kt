package com.example.mobilebrowser.data.repository

import com.example.mobilebrowser.data.dao.PasswordDao
import com.example.mobilebrowser.data.entity.PasswordEntity
import com.example.mobilebrowser.data.util.EncryptionUtil
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
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
        val normalizedSiteUrl = extractDomain(siteUrl)
        val encryptedPassword = EncryptionUtil.encrypt(plainPassword)
        val passwordEntity = PasswordEntity(
            siteUrl = normalizedSiteUrl,
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

    suspend fun passwordExistsForSite(url: String): Boolean {
        // Extract domain from URL to make matching more reliable
        val domain = extractDomain(url)
        return passwordDao.passwordExistsForSite("%$domain%")
    }

    private fun extractDomain(url: String): String {
        return try {
            val uri = java.net.URI(if (url.startsWith("http")) url else "https://$url")
            var domain = uri.host ?: return url
            if (domain.startsWith("www.")) {
                domain = domain.substring(4)
            }
            domain
        } catch (e: Exception) {
            url
        }
    }

    suspend fun getCredentialsForSite(url: String): PasswordEntity? {
        val domain = extractDomain(url)
        return passwordDao.getAllPasswords().first().find {
            extractDomain(it.siteUrl) == domain
        }
    }
}
