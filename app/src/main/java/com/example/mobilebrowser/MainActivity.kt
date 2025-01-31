package com.example.mobilebrowser

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.mobilebrowser.browser.GeckoSessionManager
import com.example.mobilebrowser.ui.composables.BrowserContent
import com.example.mobilebrowser.ui.screens.BookmarkEditScreen
import com.example.mobilebrowser.ui.screens.BookmarkScreen
import com.example.mobilebrowser.ui.screens.TabScreen
import com.example.mobilebrowser.ui.theme.MobileBrowserTheme
import com.example.mobilebrowser.ui.viewmodels.BookmarkViewModel
import com.example.mobilebrowser.ui.viewmodels.TabViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.mozilla.geckoview.GeckoSession

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private lateinit var sessionManager: GeckoSessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sessionManager = GeckoSessionManager(this)

        setContent {
            MobileBrowserTheme {
                // State management
                var currentUrl by remember { mutableStateOf("https://www.mozilla.org") }
                var currentPageTitle by remember { mutableStateOf("") }
                var canGoBack by remember { mutableStateOf(false) }
                var canGoForward by remember { mutableStateOf(false) }
                var currentSession by remember { mutableStateOf<GeckoSession?>(null) }
                var isNavigating by remember { mutableStateOf(false) }

                val navController = rememberNavController()
                val bookmarkViewModel: BookmarkViewModel = hiltViewModel()
                val tabViewModel: TabViewModel = hiltViewModel()
                val isCurrentUrlBookmarked by bookmarkViewModel.isCurrentUrlBookmarked.collectAsState()
                val activeTab by tabViewModel.activeTab.collectAsState()
                val scope = rememberCoroutineScope()

                // Monitor active tab and update session
                LaunchedEffect(activeTab) {
                    Log.d("MainActivity", "LaunchedEffect: activeTab = $activeTab")
                    activeTab?.let { tab ->
                        Log.d("MainActivity", "activeTab url = ${tab.url}, title = ${tab.title}")
                        currentSession = sessionManager.getOrCreateSession(
                            tabId = tab.id,
                            url = tab.url,
                            onUrlChange = { url ->
                                currentUrl = url
                                bookmarkViewModel.updateCurrentUrl(url)
                                tabViewModel.updateActiveTabContent(url, currentPageTitle)
                            },
                            onCanGoBack = { canGoBack = it },
                            onCanGoForward = { canGoForward = it }
                        )
                    }
                }

                NavHost(navController = navController, startDestination = "browser") {
                    composable("browser") {
                        val tabCount by tabViewModel.tabCount.collectAsState()

                        currentSession?.let { session ->
                            BrowserContent(
                                geckoSession = session,
                                onNavigate = { url ->
                                    currentUrl = url
                                    bookmarkViewModel.updateCurrentUrl(url)
                                    tabViewModel.updateActiveTabContent(url, currentPageTitle)
                                },
                                onBack = { session.goBack() },
                                onForward = { session.goForward() },
                                onReload = { session.reload() },
                                onShowBookmarks = {
                                    navController.navigate("bookmarks")
                                },
                                onShowTabs = {
                                    navController.navigate("tabs")
                                },
                                onAddBookmark = { url, title ->
                                    bookmarkViewModel.quickAddBookmark(url, title)
                                },
                                isCurrentUrlBookmarked = isCurrentUrlBookmarked,
                                currentPageTitle = currentPageTitle,
                                canGoBack = canGoBack,
                                canGoForward = canGoForward,
                                currentUrl = currentUrl,
                                tabCount = tabCount,
                                onNewTab = {
                                    scope.launch {
                                        val newTabId = tabViewModel.createTab()
                                        val newSession = sessionManager.getOrCreateSession(
                                            tabId = newTabId,
                                            url = "https://www.mozilla.org"
                                        )
                                        currentSession = newSession
                                        tabViewModel.switchToTab(newTabId)
                                    }
                                },
                                onCloseAllTabs = {
                                    sessionManager.removeAllSessions()
                                    tabViewModel.closeAllTabs()
                                },
                                onCanGoBackChange = { canGoBack = it },
                                onCanGoForwardChange = { canGoForward = it }
                            )
                        }
                    }

                    composable("bookmarks") {
                        BookmarkScreen(
                            onNavigateToEdit = { bookmarkId ->
                                navController.navigate("bookmark/edit/$bookmarkId")
                            },
                            onNavigateBack = {
                                navController.popBackStack()
                            },
                            onNavigateToUrl = { url ->
                                if (!isNavigating) {
                                    scope.launch {
                                        try {
                                            isNavigating = true

                                            // Update the active tab's content
                                            tabViewModel.updateActiveTabContent(url, "Loading...")

                                            // Get or create session for the active tab
                                            activeTab?.let { tab ->
                                                currentSession = sessionManager.getOrCreateSession(
                                                    tabId = tab.id,
                                                    url = url,
                                                    onUrlChange = { newUrl ->
                                                        currentUrl = newUrl
                                                        bookmarkViewModel.updateCurrentUrl(newUrl)
                                                        tabViewModel.updateActiveTabContent(newUrl, currentPageTitle)
                                                    },
                                                    onCanGoBack = { canGoBack = it },
                                                    onCanGoForward = { canGoForward = it }
                                                )
                                            }

                                            // Update the current URL and load it
                                            currentUrl = url
                                            currentSession?.loadUri(url)

                                            // Navigate back to browser
                                            navController.popBackStack()
                                        } catch (e: Exception) {
                                            Log.e("MainActivity", "Error navigating to bookmark: ${e.message}")
                                        } finally {
                                            isNavigating = false
                                        }
                                    }
                                }
                            }
                        )
                    }

                    composable("bookmark/edit/{bookmarkId}") { backStackEntry ->
                        val bookmarkId = backStackEntry.arguments?.getString("bookmarkId")?.toLongOrNull()
                        BookmarkEditScreen(
                            bookmarkId = bookmarkId,
                            onNavigateBack = {
                                navController.popBackStack()
                            }
                        )
                    }

                    composable("tabs") {
                        TabScreen(
                            onNavigateBack = {
                                navController.popBackStack()
                            },
                            onTabSelected = { tabId ->
                                scope.launch {
                                    tabViewModel.switchToTab(tabId)
                                    tabViewModel.getTabById(tabId)?.let { tab ->
                                        currentUrl = tab.url
                                        currentPageTitle = tab.title
                                        bookmarkViewModel.updateCurrentUrl(tab.url)
                                        currentSession?.loadUri(tab.url)
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}