package com.example.mobilebrowser.data.dao

import androidx.room.*
import com.example.mobilebrowser.data.entity.PasswordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PasswordDao {
    @Query("SELECT * FROM passwords ORDER BY dateAdded DESC")
    fun getAllPasswords(): Flow<List<PasswordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPassword(passwordEntity: PasswordEntity): Long

    @Update
    suspend fun updatePassword(passwordEntity: PasswordEntity)

    @Delete
    suspend fun deletePassword(passwordEntity: PasswordEntity)
}
