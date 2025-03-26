package com.example.mobilebrowser.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.mobilebrowser.ui.composables.SearchEngine
import com.example.mobilebrowser.ui.viewmodels.SettingsViewModel
import com.example.mobilebrowser.ui.viewmodels.AuthViewModel
import com.example.mobilebrowser.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onSelectSearchEngine: () -> Unit,
    onSelectTabManagement: () -> Unit,
    onSelectTheme: () -> Unit,
    onNavigateToHomepageSelection: () -> Unit,
    onNavigateToUrl: (String) -> Unit,
    onNavigateToAccount: () -> Unit, // New navigation callback for Account
    viewModel: SettingsViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel() // Inject AuthViewModel for account state
) {
    val searchEngines = listOf(
        SearchEngine(
            name = "Google",
            searchUrl = "https://www.google.com/search?q=",
            iconRes = R.drawable.google_icon
        ),
        SearchEngine(
            name = "Bing",
            searchUrl = "https://www.bing.com/search?q=",
            iconRes = R.drawable.bing_icon
        ),
        SearchEngine(
            name = "DuckDuckGo",
            searchUrl = "https://duckduckgo.com/?q=",
            iconRes = R.drawable.duckduckgo_icon
        ),
        SearchEngine(
            name = "Qwant",
            searchUrl = "https://www.qwant.com/?q=",
            iconRes = R.drawable.qwant_icon
        ),
        SearchEngine(
            name = "Wikipedia",
            searchUrl = "https://wikipedia.org/wiki/Special:Search?search=",
            iconRes = R.drawable.wikipedia_icon
        ),
        SearchEngine(
            name = "eBay",
            searchUrl = "https://www.ebay.com/sch/i.html?_nkw=",
            iconRes = R.drawable.ebay_icon
        )
    )

    val currentEngineUrl by viewModel.searchEngine.collectAsState()
    val currentEngine = searchEngines.find { it.searchUrl == currentEngineUrl } ?: searchEngines[0]
    val currentTabPolicy by viewModel.tabManagementPolicy.collectAsState()
    val currentThemeMode by viewModel.themeMode.collectAsState()
    val homepageEnabled by viewModel.homepageEnabled.collectAsState()

    // Convert the theme mode value to a user-friendly string.
    val themeDisplayName = when (currentThemeMode) {
        "LIGHT" -> "Light Mode"
        "DARK" -> "Dark Mode"
        else -> "System Default"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            // Sync card at the top
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
                    .clickable {
                        // Navigate to the URL within your own browser
                        onNavigateToUrl("https://nimbus-browser-backend-production.up.railway.app/")
                    },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Profile icon with gradient
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFF4285F4), // Blue at top
                                        Color(0xFF9C27B0)  // Purple at bottom
                                    )
                                ),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Profile",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = "Synchronise and save your data",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Sign in to synchronise tabs, bookmarks, passwords, and more.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Section heading
            Text(
                text = "General",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            // Search Engine setting
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectSearchEngine() }
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Search Engine",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = currentEngine.name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Tab Management Policy section
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectTabManagement() }
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Tab Management Policy",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            // Map the policy value to a friendly display string.
                            val displayPolicy = when (currentTabPolicy) {
                                "MANUAL" -> "Manually"
                                "ONE_DAY" -> "After One Day"
                                "ONE_WEEK" -> "After One Week"
                                "ONE_MONTH" -> "After One Month"
                                else -> "Manually"
                            }
                            Text(
                                text = displayPolicy,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Theme Selection section
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectTheme() }
            ) {
                Column(modifier = Modifier.padding(vertical = 16.dp)) {
                    Text(
                        text = "Theme",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = themeDisplayName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Homepage setting section (Shortcuts)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToHomepageSelection() }
            ) {
                Column(modifier = Modifier.padding(vertical = 16.dp)) {
                    Text(
                        text = "Homepage",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    // Replace "TBD" with descriptive text
                    val homepageEnabled by viewModel.homepageEnabled.collectAsState()
                    val recentTabEnabled by viewModel.recentTabEnabled.collectAsState()
                    val bookmarksEnabled by viewModel.bookmarksEnabled.collectAsState()
                    val historyEnabled by viewModel.historyEnabled.collectAsState()

                    // Count how many sections are enabled
                    val enabledSectionCount = listOf(
                        homepageEnabled,
                        recentTabEnabled,
                        bookmarksEnabled,
                        historyEnabled
                    ).count { it }

                    Text(
                        text = if (enabledSectionCount == 0) {
                            "All homepage sections are hidden"
                        } else if (enabledSectionCount == 4) {
                            "All homepage sections are visible"
                        } else {
                            "$enabledSectionCount of 4 homepage sections visible"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // New Account setting section
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToAccount() }
            ) {
                Column(modifier = Modifier.padding(vertical = 16.dp)) {
                    Text(
                        text = "Account",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    // Show user name if signed in, otherwise show "Sign in to sync"
                    val isSignedIn by authViewModel.isSignedIn.collectAsState()
                    val userName by authViewModel.userName.collectAsState()

                    if (isSignedIn) {
                        Text(
                            text = "Signed in as $userName",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = "Sign in to sync browser data across devices",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Divider line at the end of the settings list
            Divider(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.outlineVariant,
                thickness = 1.dp
            )
        }
    }
}
