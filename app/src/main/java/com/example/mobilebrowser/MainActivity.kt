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
import com.example.mobilebrowser.ui.theme.MobileBrowserTheme
import com.example.mobilebrowser.ui.viewmodels.BookmarkViewModel
import dagger.hilt.android.AndroidEntryPoint
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoSession

// MainActivity is the entry point of the app and hosts all UI components.
// Annotated with @AndroidEntryPoint to support dependency injection via Hilt.
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    // GeckoRuntime is the environment in which GeckoSessions run.
    private lateinit var geckoRuntime: GeckoRuntime
    // GeckoSession represents a single browsing session.
    private lateinit var geckoSession: GeckoSession

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize the GeckoRuntime.
        geckoRuntime = GeckoRuntime.getDefault(this)

        // Initialize and open a GeckoSession for browsing.
        geckoSession = GeckoSession().apply {
            open(geckoRuntime)
            Log.d("MainActivity", "GeckoSession opened in onCreate")

            var currentPageTitle = ""
            // Set up a content delegate to capture the page title.
            contentDelegate = object : GeckoSession.ContentDelegate {
                override fun onTitleChange(session: GeckoSession, title: String?) {
                    currentPageTitle = title ?: "" // Update the current page title.
                }
            }
        }

        // Set up the UI content with Compose.
        setContent {
            // Apply the app's theme.
            MobileBrowserTheme {
                // Define states for the current URL, page title, and navigation capabilities.
                var currentUrl by remember { mutableStateOf("https://www.mozilla.org") }
                var currentPageTitle by remember { mutableStateOf("") }
                var canGoBack by remember { mutableStateOf(false) }
                var canGoForward by remember { mutableStateOf(false) }

                // Create a navigation controller to manage navigation between screens.
                val navController = rememberNavController()
                val bookmarkViewModel: BookmarkViewModel = hiltViewModel() // Inject the ViewModel.
                val isCurrentUrlBookmarked by bookmarkViewModel.isCurrentUrlBookmarked.collectAsState()

                // Define the navigation graph.
                NavHost(
                    navController = navController,
                    startDestination = "browser" // Start with the browser screen.
                ) {
                    // Browser screen.
                    composable("browser") {
                        BrowserContent(
                            geckoSession = geckoSession, // Pass the GeckoSession.
                            onNavigate = { url ->
                                currentUrl = url // Update the current URL.
                                bookmarkViewModel.updateCurrentUrl(url) // Check if bookmarked.
                            },
                            onBack = {
                                Log.d("MainActivity", "Go Back Button Clicked")
                                geckoSession.goBack() // Navigate back.
                            },
                            onForward = {
                                Log.d("MainActivity", "Go Forward Button Clicked")
                                geckoSession.goForward() // Navigate forward.
                            },
                            onReload = {
                                Log.d("MainActivity", "Reload Button Clicked")
                                geckoSession.reload() // Reload the page.
                            },
                            onShowBookmarks = {
                                navController.navigate("bookmarks") // Navigate to bookmarks.
                            },
                            onAddBookmark = { url, title ->
                                bookmarkViewModel.quickAddBookmark(url, title) // Add a bookmark.
                            },
                            isCurrentUrlBookmarked = isCurrentUrlBookmarked, // Check bookmark status.
                            currentPageTitle = currentPageTitle, // Pass the current page title.
                            canGoBack = canGoBack, // Can navigate back.
                            canGoForward = canGoForward, // Can navigate forward.
                            currentUrl = currentUrl, // Pass the current URL.
                            onCanGoBackChange = { isBack ->
                                canGoBack = isBack // Update back navigation state.
                                Log.d("MainActivity", "onCanGoBackChange: $isBack")
                            },
                            onCanGoForwardChange = { isForward ->
                                canGoForward = isForward // Update forward navigation state.
                                Log.d("MainActivity", "onCanGoForwardChange: $isForward")
                            }
                        )
                    }

                    // Bookmarks screen.
                    composable("bookmarks") {
                        BookmarkScreen(
                            onNavigateToEdit = { bookmarkId ->
                                navController.navigate("bookmark/edit/$bookmarkId") // Navigate to edit.
                            },
                            onNavigateBack = {
                                navController.popBackStack() // Navigate back.
                            }
                        )
                    }

                    // Bookmark edit screen.
                    composable(
                        route = "bookmark/edit/{bookmarkId}", // Define route with arguments.
                        arguments = listOf(navArgument("bookmarkId") { type = NavType.LongType }) // Pass the bookmark ID.
                    ) { backStackEntry ->
                        val bookmarkId = backStackEntry.arguments?.getLong("bookmarkId") // Extract ID.
                        BookmarkEditScreen(
                            bookmarkId = bookmarkId,
                            onNavigateBack = {
                                navController.popBackStack() // Navigate back after editing.
                            }
                        )
                    }
                }
            }
        }
    }
}
