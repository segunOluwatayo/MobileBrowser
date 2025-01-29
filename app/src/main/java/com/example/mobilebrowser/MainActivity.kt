package com.example.mobilebrowser

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.example.mobilebrowser.ui.composables.BrowserContent
import com.example.mobilebrowser.ui.screens.BookmarkScreen
import com.example.mobilebrowser.ui.screens.BookmarkEditScreen
import com.example.mobilebrowser.ui.screens.TabScreen
import com.example.mobilebrowser.ui.theme.MobileBrowserTheme
import com.example.mobilebrowser.ui.viewmodels.BookmarkViewModel
import com.example.mobilebrowser.ui.viewmodels.TabViewModel
import dagger.hilt.android.AndroidEntryPoint
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoSession
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private lateinit var geckoRuntime: GeckoRuntime
    private lateinit var geckoSession: GeckoSession

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize GeckoRuntime
        geckoRuntime = GeckoRuntime.getDefault(this)

        // Initialize GeckoSession
        geckoSession = GeckoSession().apply {
            open(geckoRuntime)
            Log.d("MainActivity", "GeckoSession opened in onCreate")

            var currentPageTitle = ""
            // Set up content delegate to capture page title
            contentDelegate = object : GeckoSession.ContentDelegate {
                override fun onTitleChange(session: GeckoSession, title: String?) {
                    currentPageTitle = title ?: ""
                }
            }
        }

        setContent {
            MobileBrowserTheme {
                // State management
                var currentUrl by remember { mutableStateOf("about:blank") }
                var currentPageTitle by remember { mutableStateOf("") }
                var canGoBack by remember { mutableStateOf(false) }
                var canGoForward by remember { mutableStateOf(false) }

                val navController = rememberNavController()
                val bookmarkViewModel: BookmarkViewModel = hiltViewModel()
                val tabViewModel: TabViewModel = hiltViewModel()
                val isCurrentUrlBookmarked by bookmarkViewModel.isCurrentUrlBookmarked.collectAsState()
                val scope = rememberCoroutineScope()

                NavHost(
                    navController = navController,
                    startDestination = "browser"
                ) {
                    // Browser screen
                    composable("browser") {
                        BrowserContent(
                            geckoSession = geckoSession,
                            onNavigate = { url ->
                                currentUrl = url
                                bookmarkViewModel.updateCurrentUrl(url)
                                tabViewModel.updateActiveTabContent(url, currentPageTitle)
                            },
                            onBack = {
                                Log.d("MainActivity", "Go Back Button Clicked")
                                geckoSession.goBack()
                            },
                            onForward = {
                                Log.d("MainActivity", "Go Forward Button Clicked")
                                geckoSession.goForward()
                            },
                            onReload = {
                                Log.d("MainActivity", "Reload Button Clicked")
                                geckoSession.reload()
                            },
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
                            onCanGoBackChange = { isBack ->
                                canGoBack = isBack
                                Log.d("MainActivity", "onCanGoBackChange: $isBack")
                            },
                            onCanGoForwardChange = { isForward ->
                                canGoForward = isForward
                                Log.d("MainActivity", "onCanGoForwardChange: $isForward")
                            }
                        )
                    }

                    // Bookmarks screen
                    composable("bookmarks") {
                        BookmarkScreen(
                            onNavigateToEdit = { bookmarkId ->
                                navController.navigate("bookmark/edit/$bookmarkId")
                            },
                            onNavigateBack = {
                                navController.popBackStack()
                            }
                        )
                    }

                    // Bookmark edit screen
                    composable(
                        route = "bookmark/edit/{bookmarkId}",
                        arguments = listOf(navArgument("bookmarkId") { type = NavType.LongType })
                    ) { backStackEntry ->
                        val bookmarkId = backStackEntry.arguments?.getLong("bookmarkId")
                        BookmarkEditScreen(
                            bookmarkId = bookmarkId,
                            onNavigateBack = {
                                navController.popBackStack()
                            }
                        )
                    }

                    // Tabs screen
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