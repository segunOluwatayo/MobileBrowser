package com.example.mobilebrowser.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.mobilebrowser.ui.composables.EnhancedPasswordSection
import com.example.mobilebrowser.ui.viewmodels.PasswordViewModel
import com.example.mobilebrowser.util.BiometricAuthHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordScreen(
    onNavigateBack: () -> Unit,
    viewModel: PasswordViewModel = hiltViewModel()
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Saved Passwords") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            EnhancedPasswordSection(
                passwordViewModel = viewModel,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}