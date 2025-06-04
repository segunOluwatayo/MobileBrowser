package com.example.mobilebrowser.data.dao

import androidx.room.*
import com.example.mobilebrowser.data.entity.PasswordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PasswordDao {
    // Retrieve all passwords from the database, ordered by date added.
    @Query("SELECT * FROM passwords ORDER BY dateAdded DESC")
    fun getAllPasswords(): Flow<List<PasswordEntity>>

    // Insert a new password into the database.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPassword(passwordEntity: PasswordEntity): Long

    // Update an existing password in the database.
    @Update
    suspend fun updatePassword(passwordEntity: PasswordEntity)

    // Delete a password from the database.
    @Delete
    suspend fun deletePassword(passwordEntity: PasswordEntity)

    // Check if a password exists for a specific site.
    @Query("SELECT EXISTS(SELECT 1 FROM passwords WHERE siteUrl LIKE :url)")
    suspend fun passwordExistsForSite(url: String): Boolean

    @Query("SELECT * FROM passwords WHERE siteUrl = :domain LIMIT 1")
    suspend fun getPasswordByDomain(domain: String): PasswordEntity?

}
