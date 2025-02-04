package com.example.mobilebrowser.data.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PermissionsHandler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _permissionsGranted = MutableLiveData<Boolean>()
    val permissionsGranted: LiveData<Boolean> = _permissionsGranted

    init {
        checkPermissions()
    }

    fun requiredPermissions(): List<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ uses scoped storage
            listOf(Manifest.permission.MANAGE_EXTERNAL_STORAGE)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10
            listOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        } else {
            // Android 9 and below
            listOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }
    }

    fun hasStoragePermissions(): Boolean {
        return requiredPermissions().all { permission ->
            ContextCompat.checkSelfPermission(context, permission) ==
                    PackageManager.PERMISSION_GRANTED
        }
    }

    private fun checkPermissions() {
        _permissionsGranted.value = hasStoragePermissions()
    }

    fun getStoragePermissionRationale(): String {
        return "This permission is needed to save and manage your downloaded files. " +
                "Without this permission, you won't be able to download files."
    }
}