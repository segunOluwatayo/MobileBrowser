package com.example.mobilebrowser.data.repository

import android.util.Log
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
        // Normalize the domain and log it.
        val normalizedDomain = extractDomain(siteUrl)
        Log.d("PasswordRepository", "Saving password. Original siteUrl: $siteUrl, normalized domain: $normalizedDomain")

        val passwordEntity = PasswordEntity(
            siteUrl = normalizedDomain,
            username = username,
            encryptedPassword = EncryptionUtil.encrypt(plainPassword)
        )
        return passwordDao.insertPassword(passwordEntity)
    }



    // Encrypt and update an existing password entry
    suspend fun updatePassword(passwordEntity: PasswordEntity, plainPassword: String) {
        val encryptedPassword = EncryptionUtil.encrypt(plainPassword)
        val updatedEntity = passwordEntity.copy(encryptedPassword = encryptedPassword)
        passwordDao.updatePassword(updatedEntity)
    }

    suspend fun deletePassword(passwordEntity: PasswordEntity) {
        passwordDao.deletePassword(passwordEntity)
    }

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
        // Extract and log the domain from the current URL.
        val currentDomain = extractDomain(url)
        Log.d("PasswordRepository", "Looking up credentials for currentUrl: $url, extracted domain: $currentDomain")

        // Collect the stored passwords and compare each one after normalizing.
        return passwordDao.getAllPasswords().first().find { entry ->
            val storedDomain = extractDomain(entry.siteUrl)
            Log.d("PasswordRepository", "Comparing stored password entry. Stored siteUrl: ${entry.siteUrl}, normalized domain: $storedDomain")
            storedDomain == currentDomain
        }
    }


}
