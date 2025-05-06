package com.example.mobilebrowser

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.mobilebrowser.browser.GeckoDownloadDelegate
import com.example.mobilebrowser.browser.GeckoDownloadDelegate.DownloadRequest
import com.example.mobilebrowser.browser.GeckoSessionManager
import com.example.mobilebrowser.ui.composables.BrowserContent
import com.example.mobilebrowser.ui.screens.*
import com.example.mobilebrowser.ui.theme.MobileBrowserTheme
import com.example.mobilebrowser.ui.viewmodels.*
import com.example.mobilebrowser.worker.DynamicShortcutWorker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.mozilla.geckoview.GeckoSession
import android.view.View
import android.widget.Toast
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.mobilebrowser.classifier.UrlCnnInterpreter
import com.example.mobilebrowser.data.service.AuthService
import com.example.mobilebrowser.ui.composables.PasswordSaveDialog
import com.example.mobilebrowser.worker.SyncWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.mozilla.geckoview.Autocomplete
import org.mozilla.geckoview.BuildConfig
import org.mozilla.geckoview.GeckoResult
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

// Enum to track which overlay screen is active
enum class OverlayScreen {
    None, Tabs, Settings, Bookmarks, Downloads, History, SearchEngine,
    TabManagement, ThemeSelection, HomepageSelection, BookmarkEdit, Account, Passwords
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    //    private val bloom by lazy { Bloom(applicationContext) }
    private lateinit var scanner: UrlCnnInterpreter

    @Inject
    lateinit var authService: AuthService
    private lateinit var sessionManager: GeckoSessionManager
    private var geckoViewReference: View? = null

    private val authSuccessState = mutableStateOf(false)
    private val needToCloseCurrentTab = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sessionManager = GeckoSessionManager(this)

        // Set up the auth success callback
        sessionManager.setAuthSuccessCallback {
            Log.d("MainActivity", "Authentication success callback triggered")
            // Signal that we need to close the current tab and go to homepage
            authSuccessState.value = true
            needToCloseCurrentTab.value = true
        }
        // Schedule the dynamic shortcut worker
        DynamicShortcutWorker.schedule(this)
        SyncWorker.schedule(this)

        monitorWorkManager()

        scanner = UrlCnnInterpreter(this)

        val tests = listOf(
            "https://fantasticfilms.ru",
            "https://github.com",
            "https://google.com",
            "https://apple.com/store",
            "http://free-gift-cards.xyz",
            "http://amazonn-signin.com/login.php",
            "https://www.very.ie",
            "https://www.gardensrestaurantandcatering.com/",
            "https://www.scamadviser.com",
            "https://www.scamwatch.gov.au",
            "https://www.scamwatch.gov.au/report-a-scam",
            "https://anix.com.pl/",
            "https://tiffanycoshop.com",
            "Luvasti.com",
            "https://www.likaraoke.com",
            "https://www.vegweb.com",
            "https://en.m.wikipedia.org/wiki/Cooking"
        )

        lifecycleScope.launch {
            withContext(Dispatchers.Default) {
                tests.forEach { url ->
                    val v = scanner.verdict(url)
                    println("$url  ->  $v")
                }
            }
        }

        // Handle deep link if the activity was launched from one
        handleIncomingIntent(intent)
        setContent {
            // Obtain the SettingsViewModel via Hilt.
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            // Observe the current theme mode from DataStore.
            val themeMode by settingsViewModel.themeMode.collectAsState()
            // Get the system default dark theme value.
            val systemDarkTheme = isSystemInDarkTheme()
            // Determine darkTheme boolean based on user selection.
            val darkTheme = when (themeMode) {
                "LIGHT" -> false
                "DARK" -> true
                else -> systemDarkTheme // "SYSTEM" mode follows the system setting.
            }
            MobileBrowserTheme(darkTheme = darkTheme) {
                BrowserApp()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent) {
        Log.d("MainActivity", "handleIncomingIntent called with action: ${intent.action}, data: ${intent.data}")
        if (intent.action == Intent.ACTION_VIEW && intent.data != null) {
            val uri = intent.data
            Log.d("MainActivity", "Deep link received: $uri")

            // Log the scheme and host to confirm the correct deep link
            val scheme = uri?.scheme
            val host = uri?.host
            Log.d("MainActivity", "Scheme: $scheme, Host: $host")

            // Extract and log query parameters
            val accessToken = uri?.getQueryParameter("accessToken")
            val refreshToken = uri?.getQueryParameter("refreshToken")
            val userId = uri?.getQueryParameter("userId")
            val displayName = uri?.getQueryParameter("displayName")
            val email = uri?.getQueryParameter("email")

            Log.d(
                "MainActivity",
                "accessToken: $accessToken, refreshToken: $refreshToken, userId: $userId, displayName: $displayName, email: $email"
            )

            if (accessToken != null && refreshToken != null) {
                // Handle successful authentication here
                val sharedPrefs = getSharedPreferences("browser_prefs", Context.MODE_PRIVATE)
                sharedPrefs.edit()
                    .putString("access_token", accessToken)
                    .putString("refresh_token", refreshToken)
                    .putString("user_id", userId)
                    .putString("display_name", displayName)
                    .putString("email", email)
                    .putBoolean("is_authenticated", true)
                    .apply()

                Toast.makeText(this, "Signed in successfully", Toast.LENGTH_SHORT).show()
            } else if (uri?.getQueryParameter("error") != null) {
                Log.e("MainActivity", "Authentication error: ${uri.getQueryParameter("error")}")
                Toast.makeText(this, "Sign in failed", Toast.LENGTH_SHORT).show()
            }
        }
    }



    @Composable
    fun BrowserApp() {

        // Obtain the LifecycleOwner
        val lifecycleOwner = LocalLifecycleOwner.current
        // Get the AuthViewModel instance via Hilt
        val authViewModel: AuthViewModel = hiltViewModel()

        // Observe lifecycle events and start/stop the auto-sync accordingly
        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_RESUME -> {
                        // App enters foreground: start the auto-sync timer.
                        authViewModel.startAutoSync()
                    }
                    Lifecycle.Event.ON_PAUSE -> {
                        // App goes to background: stop the auto-sync.
                        authViewModel.stopAutoSync()
                    }
                    else -> Unit
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        }

        data class PendingLogin(
            val siteUrl: String,
            val username: String,
            val plainPassword: String,
            val confirm: (String, String) -> Unit,
            val dismiss: () -> Unit
        )

        var pendingLogin by remember { mutableStateOf<PendingLogin?>(null) }
        val passwordViewModel: PasswordViewModel = hiltViewModel()

        val authSuccess by remember { authSuccessState }
        val needToClose by remember { needToCloseCurrentTab }
        // Define UI state variables.
        var currentUrl by remember { mutableStateOf("") }
        var currentPageTitle by remember { mutableStateOf("New Tab") }
        var canGoBack by remember { mutableStateOf(false) }
        var canGoForward by remember { mutableStateOf(false) }
        var currentSession by remember { mutableStateOf<GeckoSession?>(null) }
        var isNavigating by remember { mutableStateOf(false) }

        // Track the last recorded history entry to avoid duplicate entries.
        var lastRecordedUrl by remember { mutableStateOf("") }
        var lastRecordedTitle by remember { mutableStateOf("") }

        // New state to track which screen is visible as an overlay
        var currentOverlay by remember { mutableStateOf(OverlayScreen.None) }
        // State to track bookmark edit ID
        var bookmarkEditId by remember { mutableStateOf<Long?>(null) }

        val bookmarkViewModel: BookmarkViewModel = hiltViewModel()
        val tabViewModel: TabViewModel = hiltViewModel()
        val tabsState = tabViewModel.tabs.collectAsState()
        val displayTabCount by remember {
            derivedStateOf { tabsState.value.count { it.url.isNotBlank() } }
        }
        val historyViewModel: HistoryViewModel = hiltViewModel()
        val shortcutViewModel: ShortcutViewModel = hiltViewModel()
        val downloadViewModel: DownloadViewModel = hiltViewModel()
        val isCurrentUrlBookmarked by bookmarkViewModel.isCurrentUrlBookmarked.collectAsState()
        val activeTab by tabViewModel.activeTab.collectAsState()
        val scope = rememberCoroutineScope()
        var isHomepageActive by remember { mutableStateOf(true) }

        // Handle authentication success
        LaunchedEffect(authSuccess, needToClose) {
            if (authSuccess && needToClose) {
                Log.d("MainActivity", "LaunchedEffect: Processing auth success")

                // Reset state so we don't handle it again
                authSuccessState.value = false
                needToCloseCurrentTab.value = false

                // Get current tab ID
                val currentTabId = activeTab?.id
                if (currentTabId != null) {
                    Log.d("MainActivity", "Closing authentication tab: $currentTabId")

                    // Close the authentication tab
                    val authTab = tabViewModel.getTabById(currentTabId)
                    if (authTab != null) {
                        tabViewModel.closeTab(authTab)
                        sessionManager.removeSession(currentTabId)
                    }

                    // Activate the homepage
                    isHomepageActive = true
//                    currentUrl = ""
//                    currentPageTitle = "New Tab"
                    scope.launch {
                        try {
                            // Trigger a one-time immediate sync via WorkManager
                            val immediateSync = OneTimeWorkRequestBuilder<SyncWorker>()
                                .addTag("immediate_post_login_sync")
                                .build()

                            WorkManager.getInstance(applicationContext).enqueue(immediateSync)

                            Log.d("MainActivity", "Scheduled immediate sync after login")

                            // Show success toast
                            Toast.makeText(
                                this@MainActivity,
                                "Synchronization started",
                                Toast.LENGTH_SHORT
                            ).show()
                        } catch (e: Exception) {
                            Log.e("MainActivity", "Failed to start initial sync: ${e.message}", e)
                        }
                    }
                }
            }
        }

        // Helper: normalize URL by trimming and removing trailing "/"
        fun normalizeUrl(url: String): String = url.trim().removeSuffix("/")

        // Helper: record history if the URL/title changed.
        fun recordHistory(url: String, title: String) {
            val normalizedUrl = normalizeUrl(url)

            // Skip auth URLs
            if (shouldSkipHistoryRecording(normalizedUrl)) {
                return
            }

            if (title.isNotBlank() && title != "Loading..." &&
                (normalizedUrl != lastRecordedUrl || title != lastRecordedTitle)
            ) {
                lastRecordedUrl = normalizedUrl
                lastRecordedTitle = title
                historyViewModel.addHistoryEntry(normalizedUrl, title)

                // Also update shortcut visit count if this URL is a shortcut.
                scope.launch {
                    shortcutViewModel.recordVisit(normalizedUrl)
                }
            }
        }

        // Safely captures a thumbnail when GeckoView is ready
        fun safelyCaptureThumbnail(tabId: Long) {
            val view = geckoViewReference
            if (view == null) {
                Log.d("MainActivity", "Cannot capture thumbnail: GeckoView reference is null")
                return
            }

            scope.launch {
                try {
                    // Verify the active tab matches the requested tab
                    val currentActiveTabId = activeTab?.id
                    if (currentActiveTabId != tabId) {
                        Log.d("MainActivity", "Skipping thumbnail capture - active tab has changed")
                        return@launch
                    }

                    // Much longer delay to ensure full page load
                    delay(1500)

                    // Verify AGAIN that the tab is still active (it could have changed during our delay)
                    if (activeTab?.id != tabId) {
                        Log.d("MainActivity", "Aborting thumbnail capture - tab is no longer active")
                        return@launch
                    }

                    // Additional check to match URL
                    if (activeTab?.url != currentUrl) {
                        Log.d("MainActivity", "URL mismatch, canceling thumbnail capture")
                        return@launch
                    }

                    // Wait until page is fully loaded and titled
                    if (currentPageTitle.isBlank() || currentPageTitle == "Loading...") {
                        Log.d("MainActivity", "Page not fully loaded, skipping thumbnail capture")
                        return@launch
                    }

                    Log.d("MainActivity", "Attempting to capture thumbnail for tab $tabId")
                    tabViewModel.updateTabThumbnail(tabId, view)
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error while setting up thumbnail capture", e)
                }
            }
        }

        // State for the download confirmation dialog.
        var showDownloadConfirmationDialog by remember { mutableStateOf(false) }
        var currentDownloadRequest by remember { mutableStateOf<DownloadRequest?>(null) }

        val showDownloadConfirmation: (DownloadRequest) -> Unit = { downloadRequest ->
            currentDownloadRequest = downloadRequest
            showDownloadConfirmationDialog = true
        }

        // Create a shared GeckoDownloadDelegate using remember.
        val geckoDownloadDelegate = remember {
            GeckoDownloadDelegate(
                context = this@MainActivity,
                downloadViewModel = downloadViewModel,
                scope = scope,
                showDownloadConfirmation = showDownloadConfirmation
            )
        }

        // Update session when the active tab changes.
        LaunchedEffect(activeTab) {
            Log.d("MainActivity", "LaunchedEffect: activeTab = $activeTab")
            activeTab?.let { tab ->
                delay(1000) // wait a bit for the view to be ready
                if (geckoViewReference == null) {
                    Log.d("MainActivity", "Waiting for GeckoView reference to be created")
                } else {
                    Log.d("MainActivity", "GeckoView reference already exists, will use for thumbnails")
                }
            }

            activeTab?.let { tab ->
                Log.d("MainActivity", "Active tab URL = ${tab.url}, title = ${tab.title}")
                currentSession = sessionManager.getOrCreateSession(
                    tabId = tab.id,
                    url = tab.url,
                    onUrlChange = { newUrl ->
                        currentUrl = if (isHomepageActive) "" else newUrl
                        bookmarkViewModel.updateCurrentUrl(newUrl)
                        tabViewModel.updateActiveTabContent(newUrl, currentPageTitle)
                    },
                    onTitleChange = { newTitle ->
                        Log.d("MainActivity", "onTitleChange triggered with newTitle: $newTitle")
                        if (newTitle.isNotBlank() && newTitle != "Loading...") {
                            currentPageTitle = newTitle
                            tabViewModel.updateActiveTabContent(currentUrl, newTitle)
                            recordHistory(currentUrl, newTitle)

                            // Don't try to capture thumbnails immediately on title change
                            // Instead, schedule a safe capture after the title indicates content is loaded
                            if (newTitle != "New Tab" && newTitle != "about:blank") {
                                scope.launch {
                                    delay(1000)  // Let the page finish rendering
                                    safelyCaptureThumbnail(tab.id)
                                }
                            }
                        }
                    },
                    onCanGoBack = { canGoBack = it },
                    onCanGoForward = { canGoForward = it },
                    downloadDelegate = geckoDownloadDelegate  // Pass the delegate
                )

                currentSession?.promptDelegate = object : GeckoSession.PromptDelegate {
                    override fun onLoginSave(
                        session: GeckoSession,
                        request: GeckoSession.PromptDelegate.AutocompleteRequest<Autocomplete.LoginSaveOption>
                    ): GeckoResult<GeckoSession.PromptDelegate.PromptResponse>? {
                        val option = request.options.firstOrNull()
                        val originalOption = option?.value

                        if (originalOption != null && option != null) {
                            val siteUrl = currentUrl

                            // Check if credentials already exist for this site
                            lifecycleScope.launch {
                                try {
                                    val exists = passwordViewModel.passwordExistsForSite(siteUrl)

                                    if (exists) {
                                        // Password already exists, dismiss the save dialog
                                        Log.d("MainActivity", "Credentials already exist for $siteUrl, dismissing save dialog")
                                        request.dismiss()
                                    } else {
                                        // No password exists, show the save dialog
                                        val isHandled = AtomicBoolean(false)

                                        pendingLogin = PendingLogin(
                                            siteUrl = siteUrl,
                                            username = originalOption.username,
                                            plainPassword = originalOption.password,
                                            confirm = { user, pass ->
                                                if (isHandled.compareAndSet(false, true)) {
                                                    passwordViewModel.addPassword(siteUrl, user, pass)
                                                    request.confirm(option)
                                                    pendingLogin = null
                                                }
                                            },
                                            dismiss = {
                                                if (isHandled.compareAndSet(false, true)) {
                                                    request.dismiss()
                                                    pendingLogin = null
                                                }
                                            }
                                        )
                                    }
                                } catch (e: Exception) {
                                    Log.e("MainActivity", "Error checking for existing credentials: ${e.message}")
                                    request.dismiss()
                                }
                            }
                            return null
                        }
                        request.dismiss()
                        return null
                    }

                    override fun onLoginSelect(
                        session: GeckoSession,
                        request: GeckoSession.PromptDelegate.AutocompleteRequest<Autocomplete.LoginSelectOption>
                    ): GeckoResult<GeckoSession.PromptDelegate.PromptResponse>? {
                        val geckoResult = GeckoResult<GeckoSession.PromptDelegate.PromptResponse>()
                        // Extract the domain from the currentUrl.
                        val currentDomain = extractDomain(currentUrl)
                        Log.d("PromptDelegate", "onLoginSelect invoked. Current URL: $currentUrl, extracted domain: $currentDomain")

                        lifecycleScope.launch {
                            try {
                                val credentials = passwordViewModel.getCredentialsForSite(currentUrl)
                                if (credentials != null) {
                                    // Get the decrypted password.
                                    val decryptedPassword = passwordViewModel.getDecryptedPassword(credentials.encryptedPassword)
                                    // Log the credentials being used.
                                    Log.d("PromptDelegate", "Found credentials for domain: $currentDomain. Username: ${credentials.username}")

                                    // Create the login entry to be used for autofill.
                                    val loginEntry = Autocomplete.LoginEntry.Builder()
                                        .origin(currentDomain)
                                        .username(credentials.username)
                                        .password(decryptedPassword)
                                        .build()
                                    val updatedOption = Autocomplete.LoginSelectOption(loginEntry)
                                    geckoResult.complete(request.confirm(updatedOption))
                                } else {
                                    Log.d("PromptDelegate", "No credentials found for domain: $currentDomain")
                                    geckoResult.complete(request.dismiss())
                                }
                            } catch (e: Exception) {
                                Log.e("PromptDelegate", "Error providing credentials: ${e.message}", e)
                                geckoResult.complete(request.dismiss())
                            }
                        }
                        return geckoResult
                    }


                    // Helper function to extract domain from URL
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
                }


                // Update UI state with the tab's stored URL and title.
                if (isHomepageActive) {
                    currentUrl = ""
                } else {
                    currentUrl = tab.url
                }
                currentPageTitle = tab.title
//                currentUrl = tab.url
                currentPageTitle = tab.title

                // Schedule a safe thumbnail capture after the tab is loaded
                scope.launch {
                    // Delay to allow WebView to load and render
                    delay(1500)
                    safelyCaptureThumbnail(tab.id)
                }
            }
        }

        // Main Box that contains our entire UI
        Box(modifier = Modifier.fillMaxSize()) {
            // Browser content is always present but potentially hidden
            currentSession?.let { session ->
                BrowserContent(
                    geckoSession = session,
                    onNavigate = { url ->
                        if (url.isNotBlank()) {
                            if (isHomepageActive) {
                                // We're on the homepage. Create a new tab for the search query.
                                scope.launch {
                                    // Create a new tab with the search URL.
                                    val newTabId = tabViewModel.createTab(url = url, title = "Loading...")
                                    // Create a new session for the new tab.
                                    currentSession = sessionManager.getOrCreateSession(
                                        tabId = newTabId,
                                        url = url,
                                        onUrlChange = { newUrl ->
                                            currentUrl = newUrl
                                            bookmarkViewModel.updateCurrentUrl(newUrl)
                                            tabViewModel.updateActiveTabContent(newUrl, currentPageTitle)
                                        },
                                        onTitleChange = { newTitle ->
                                            if (newTitle.isNotBlank() && newTitle != "Loading...") {
                                                currentPageTitle = newTitle
                                                tabViewModel.updateActiveTabContent(url, newTitle)
                                                recordHistory(url, newTitle)
                                                if (newTitle != "New Tab" && newTitle != "about:blank") {
                                                    scope.launch {
                                                        delay(1000)
                                                        safelyCaptureThumbnail(newTabId)
                                                    }
                                                }
                                            }
                                        },
                                        onCanGoBack = { canGoBack = it },
                                        onCanGoForward = { canGoForward = it },
                                        downloadDelegate = geckoDownloadDelegate,

                                    )
                                    tabViewModel.switchToTab(newTabId)
                                    currentUrl = url
                                    isHomepageActive = false
                                }
                            } else {
                                // Not on homepage: update the current active tab.
                                isHomepageActive = false
                                currentUrl = url
                                bookmarkViewModel.updateCurrentUrl(url)
                                tabViewModel.updateActiveTabContent(url, currentPageTitle)
                                if (currentPageTitle.isNotBlank() && currentPageTitle != "Loading...") {
                                    recordHistory(url, currentPageTitle)
                                }
                            }
                        } else {
                            // When url is blank, revert to homepage.
                            isHomepageActive = true
                            currentUrl = url
                        }
                    },
                            onBack = { session.goBack() },
                    isHomepageActive = isHomepageActive,
                    onForward = { session.goForward() },
                    onReload = { session.reload() },
                    onShowBookmarks = { currentOverlay = OverlayScreen.Bookmarks },
                    onShowHistory = { currentOverlay = OverlayScreen.History },
                    onShowTabs = { currentOverlay = OverlayScreen.Tabs },
                    onShowSettings = { currentOverlay = OverlayScreen.Settings },
                    onShowDownloads = { currentOverlay = OverlayScreen.Downloads },
                    activeTab = activeTab,
                    tabViewModel = tabViewModel,
                    onAddBookmark = { url, title ->
                        bookmarkViewModel.quickAddBookmark(url, title)
                    },
                    isCurrentUrlBookmarked = isCurrentUrlBookmarked,
                    currentPageTitle = currentPageTitle,
                    canGoBack = canGoBack,
                    canGoForward = canGoForward,
                    currentUrl = currentUrl,
//                    tabCount = tabViewModel.tabCount.collectAsState().value,
                    tabCount = displayTabCount,
                    onGeckoViewCreated = { view ->
                        Log.d("MainActivity", "Received GeckoView reference: ${view::class.java}")
                        geckoViewReference = view

                        // Use the existing scope from MainActivity
                        scope.launch {
                            activeTab?.let { tab ->
                                // Add delay to ensure the GeckoView is fully initialized
                                delay(1000)
                                Log.d("MainActivity", "Updating thumbnail for tab ${tab.id} after getting GeckoView reference")
                                safelyCaptureThumbnail(tab.id)
                            }
                        }
                    },
                    onNewTab = {
                        scope.launch {
                            // Check if a blank tab already exists.
                            val existingBlankTab = tabsState.value.find { it.url.isBlank() }
                            if (existingBlankTab != null) {
                                // Reuse the existing blank tab.
                                tabViewModel.switchToTab(existingBlankTab.id)
                                currentSession = sessionManager.getOrCreateSession(
                                    tabId = existingBlankTab.id,
                                    url = "",
                                    onUrlChange = { newUrl ->
                                        currentUrl = newUrl
                                        bookmarkViewModel.updateCurrentUrl(newUrl)
                                        tabViewModel.updateActiveTabContent(newUrl, currentPageTitle)
                                    },
                                    onTitleChange = { newTitle ->
                                        if (newTitle.isNotBlank() && newTitle != "Loading...") {
                                            currentPageTitle = newTitle
                                            tabViewModel.updateActiveTabContent(currentUrl, newTitle)
                                            recordHistory(currentUrl, newTitle)
                                            if (newTitle != "New Tab" && newTitle != "about:blank") {
                                                scope.launch {
                                                    delay(1000)
                                                    safelyCaptureThumbnail(existingBlankTab.id)
                                                }
                                            }
                                        }
                                    },
                                    onCanGoBack = { canGoBack = it },
                                    onCanGoForward = { canGoForward = it },
                                    downloadDelegate = geckoDownloadDelegate
                                )
                            } else {
                                // Otherwise, create a new blank tab.
                                val newTabId = tabViewModel.createTab(url = "", title = "New Tab")
                                val newSession = sessionManager.getOrCreateSession(
                                    tabId = newTabId,
                                    url = "",
                                    onUrlChange = { newUrl ->
                                        currentUrl = newUrl
                                        bookmarkViewModel.updateCurrentUrl(newUrl)
                                        tabViewModel.updateActiveTabContent(newUrl, currentPageTitle)
                                    },
                                    onTitleChange = { newTitle ->
                                        if (newTitle.isNotBlank() && newTitle != "Loading...") {
                                            currentPageTitle = newTitle
                                            tabViewModel.updateActiveTabContent(currentUrl, newTitle)
                                            recordHistory(currentUrl, newTitle)
                                            if (newTitle != "New Tab" && newTitle != "about:blank") {
                                                scope.launch {
                                                    delay(1000)
                                                    safelyCaptureThumbnail(newTabId)
                                                }
                                            }
                                        }
                                    },
                                    onCanGoBack = { canGoBack = it },
                                    onCanGoForward = { canGoForward = it },
                                    downloadDelegate = geckoDownloadDelegate
                                )
                                currentSession = newSession
                                tabViewModel.switchToTab(newTabId)
                            }
                            currentUrl = ""
                            currentPageTitle = "New Tab"
                            isHomepageActive = true
                        }
                    },
                    onCloseAllTabs = {
                        sessionManager.removeAllSessions()
                        tabViewModel.closeAllTabs()
                        isHomepageActive = true
                        currentUrl = ""
                        currentPageTitle = "New Tab"
                    },
                    onCanGoBackChange = { canGoBack = it },
                    onCanGoForwardChange = { canGoForward = it },
                    showDownloadConfirmationDialog = showDownloadConfirmationDialog,
                    onShowPasswords = { currentOverlay = OverlayScreen.Passwords },
                    currentDownloadRequest = currentDownloadRequest,
                    onDismissDownloadConfirmationDialog = { showDownloadConfirmationDialog = false },
                    isOverlayActive = currentOverlay != OverlayScreen.None
                )
            }

            // Animated overlay container
            AnimatedVisibility(
                visible = currentOverlay != OverlayScreen.None,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it / 3 }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 3 })
            ) {
                // Box with surface background to cover browser content
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    // Show different overlays based on state
                    when (currentOverlay) {
                        OverlayScreen.Tabs -> {
                            TabScreen(
                                onNavigateBack = { currentOverlay = OverlayScreen.None },
                                onTabSelected = { tabId ->
                                    scope.launch {
                                        tabViewModel.switchToTab(tabId)
                                        tabViewModel.getTabById(tabId)?.let { tab ->
                                            currentUrl = tab.url
                                            currentPageTitle = tab.title
                                            bookmarkViewModel.updateCurrentUrl(tab.url)

                                            // Check if this is a new tab that should show the homepage
                                            if (tab.url.isBlank() || tab.url == "about:blank") {
                                                isHomepageActive = true
                                            } else {
                                                isHomepageActive = false

                                                // Get the current time and calculate tab age
                                                val now = System.currentTimeMillis()
                                                val lastVisited = tab.lastVisited.time
                                                val tabAge = now - lastVisited

                                                // 24 hours in milliseconds
                                                val staleThreshold = 24 * 60 * 60 * 1000L
                                                val isStale = tabAge > staleThreshold

                                                // Switch to the session without explicitly reloading
                                                currentSession = sessionManager.switchToSession(tabId)

                                                // Only reload if the tab is stale (older than 24 hours)
                                                if (isStale) {
                                                    Log.d("MainActivity", "Tab $tabId is stale, reloading content")
                                                    currentSession?.loadUri(tab.url)
                                                } else {
                                                    Log.d("MainActivity", "Tab $tabId is not stale, keeping content as is")
                                                }
                                            }
                                        }
                                        currentOverlay = OverlayScreen.None
                                    }
                                },
                                onNewTabHome = {
                                    // Simply set homepage state; no new blank tab is created.
                                    isHomepageActive = true
                                    currentUrl = ""
                                    currentPageTitle = "New Tab"
                                    currentOverlay = OverlayScreen.None
                                }
                            )
                        }
                        OverlayScreen.Settings -> {
                            SettingsScreen(
                                onNavigateBack = { currentOverlay = OverlayScreen.None },
                                onSelectSearchEngine = { currentOverlay = OverlayScreen.SearchEngine },
                                onSelectTabManagement = { currentOverlay = OverlayScreen.TabManagement },
                                onSelectTheme = { currentOverlay = OverlayScreen.ThemeSelection },
                                onNavigateToHomepageSelection = { currentOverlay = OverlayScreen.HomepageSelection },
                                onNavigateToUrl = { url ->
                                    // Close settings screen
                                    currentOverlay = OverlayScreen.None
                                    scope.launch {
                                        tabViewModel.updateActiveTabContent(url, "Sync")
                                        currentSession?.loadUri(url)
                                        isHomepageActive = false
                                    }
                                },
                                onNavigateToAccount = {
                                    // Add logic to navigate to the Account screen
                                    currentOverlay = OverlayScreen.Account
                                },
                            )

                        }
                        OverlayScreen.SearchEngine -> {
                            SearchEngineSelectionScreen(
                                onNavigateBack = { currentOverlay = OverlayScreen.Settings }
                            )
                        }
                        OverlayScreen.TabManagement -> {
                            TabManagementSelectionScreen(
                                onNavigateBack = { currentOverlay = OverlayScreen.Settings }
                            )
                        }
                        OverlayScreen.ThemeSelection -> {
                            ThemeSelectionScreen(
                                onNavigateBack = { currentOverlay = OverlayScreen.Settings }
                            )
                        }
                        OverlayScreen.HomepageSelection -> {
                            HomepageSelectionScreen(
                                onNavigateBack = { currentOverlay = OverlayScreen.Settings }
                            )
                        }
                        OverlayScreen.Bookmarks -> {
                            BookmarkScreen(
                                onNavigateToEdit = { id ->
                                    bookmarkEditId = id
                                    currentOverlay = OverlayScreen.BookmarkEdit
                                },
                                onNavigateBack = { currentOverlay = OverlayScreen.None },
                                onNavigateToUrl = { url ->
                                    if (!isNavigating) {
                                        scope.launch {
                                            try {
                                                isNavigating = true
                                                // First close the overlay to ensure UI responsiveness
                                                currentOverlay = OverlayScreen.None
                                                // Short delay to allow UI to update
                                                delay(100)

                                                // Create a new tab with the bookmark URL
                                                val newTabId = tabViewModel.createTab(url = url, title = "Loading...")

                                                // Create a new session for the new tab
                                                currentSession = sessionManager.getOrCreateSession(
                                                    tabId = newTabId,
                                                    url = url,
                                                    onUrlChange = { newUrl ->
                                                        currentUrl = newUrl
                                                        bookmarkViewModel.updateCurrentUrl(newUrl)
                                                        tabViewModel.updateActiveTabContent(newUrl, currentPageTitle)
                                                    },
                                                    onTitleChange = { newTitle ->
                                                        if (newTitle.isNotBlank() && newTitle != "Loading...") {
                                                            currentPageTitle = newTitle
                                                            tabViewModel.updateActiveTabContent(currentUrl, newTitle)
                                                            recordHistory(currentUrl, newTitle)

                                                            // Schedule safe thumbnail capture
                                                            if (newTitle != "New Tab" && newTitle != "about:blank") {
                                                                scope.launch {
                                                                    delay(1000)
                                                                    safelyCaptureThumbnail(newTabId)
                                                                }
                                                            }
                                                        }
                                                    },
                                                    onCanGoBack = { canGoBack = it },
                                                    onCanGoForward = { canGoForward = it },
                                                    downloadDelegate = geckoDownloadDelegate
                                                )

                                                // Switch to the new tab
                                                tabViewModel.switchToTab(newTabId)

                                                // Update UI state
                                                currentUrl = url
                                                isHomepageActive = false

                                                // Explicitly load the URL in the new session
                                                currentSession?.loadUri(url)
                                            } catch (e: Exception) {
                                                Log.e("MainActivity", "Error navigating to bookmark in new tab: ${e.message}")
                                            } finally {
                                                isNavigating = false
                                            }
                                        }
                                    }
                                }
                            )
                        }
                        OverlayScreen.BookmarkEdit -> {
                            BookmarkEditScreen(
                                bookmarkId = bookmarkEditId,
                                onNavigateBack = {
                                    bookmarkEditId = null
                                    currentOverlay = OverlayScreen.Bookmarks
                                }
                            )
                        }
                        OverlayScreen.Downloads -> {
                            DownloadScreen(
                                onNavigateBack = { currentOverlay = OverlayScreen.None }
                            )
                        }
                        OverlayScreen.History -> {
                            HistoryScreen(
                                onNavigateBack = { currentOverlay = OverlayScreen.None },
                                onNavigateToUrl = { url ->
                                    if (!isNavigating) {
                                        scope.launch {
                                            try {
                                                isNavigating = true
                                                // First close the overlay to ensure UI responsiveness
                                                currentOverlay = OverlayScreen.None
                                                // Short delay to allow UI to update
                                                delay(100)

                                                // Set URL and load it in the session
                                                currentUrl = url
                                                isHomepageActive = false
                                                tabViewModel.updateActiveTabContent(url, currentPageTitle)
                                                activeTab?.let { tab ->
                                                    currentSession = sessionManager.getOrCreateSession(
                                                        tabId = tab.id,
                                                        url = url,
                                                        onUrlChange = { newUrl ->
                                                            currentUrl = newUrl
                                                            bookmarkViewModel.updateCurrentUrl(newUrl)
                                                            tabViewModel.updateActiveTabContent(newUrl, currentPageTitle)
                                                        },
                                                        onTitleChange = { newTitle ->
                                                            if (newTitle.isNotBlank() && newTitle != "Loading...") {
                                                                currentPageTitle = newTitle
                                                                tabViewModel.updateActiveTabContent(currentUrl, newTitle)
                                                                recordHistory(currentUrl, newTitle)

                                                                // Schedule safe thumbnail capture
                                                                if (newTitle != "New Tab" && newTitle != "about:blank") {
                                                                    scope.launch {
                                                                        delay(1000)
                                                                        safelyCaptureThumbnail(tab.id)
                                                                    }
                                                                }
                                                            }
                                                        },
                                                        onCanGoBack = { canGoBack = it },
                                                        onCanGoForward = { canGoForward = it },
                                                        downloadDelegate = geckoDownloadDelegate
                                                    )
                                                }
                                                currentUrl = url
                                                currentSession?.loadUri(url)
                                                currentOverlay = OverlayScreen.None
                                            } catch (e: Exception) {
                                                Log.e("MainActivity", "Error navigating from history: ${e.message}")
                                            } finally {
                                                isNavigating = false
                                            }
                                        }
                                    }
                                }
                            )
                        }
                        OverlayScreen.Passwords -> {
                            PasswordScreen(
                                onNavigateBack = { currentOverlay = OverlayScreen.None }
                            )
                        }
                        OverlayScreen.Account -> {
                            AuthSettingsScreen(
                                onNavigateBack = { currentOverlay = OverlayScreen.None },
                                onNavigateToUrl = { url ->
                                    // Close the account screen
                                    currentOverlay = OverlayScreen.None
                                    // Load the URL in the current browser session
                                    scope.launch {
                                        if (url.isNotBlank()) {
                                            if (isHomepageActive) {
                                                // Create a new tab for the URL if we're on homepage
                                                val newTabId = tabViewModel.createTab(url = url, title = "Account Dashboard")
                                                tabViewModel.switchToTab(newTabId)
                                                currentSession = sessionManager.getOrCreateSession(
                                                    tabId = newTabId,
                                                    url = url,
                                                    onUrlChange = { newUrl ->
                                                        currentUrl = newUrl
                                                        bookmarkViewModel.updateCurrentUrl(newUrl)
                                                        tabViewModel.updateActiveTabContent(newUrl, currentPageTitle)
                                                    },
                                                    onTitleChange = { newTitle ->
                                                        if (newTitle.isNotBlank() && newTitle != "Loading...") {
                                                            currentPageTitle = newTitle
                                                            tabViewModel.updateActiveTabContent(currentUrl, newTitle)
                                                            recordHistory(currentUrl, newTitle)
                                                        }
                                                    },
                                                    onCanGoBack = { canGoBack = it },
                                                    onCanGoForward = { canGoForward = it },
                                                    downloadDelegate = geckoDownloadDelegate
                                                )

                                                isHomepageActive = false
                                                currentUrl = url
                                            } else {
                                                // Use current tab if we're already browsing
                                                currentUrl = url
                                                currentSession?.loadUri(url)
                                                isHomepageActive = false
                                            }
                                        }
                                    }
                                }
                            )
                        }
                        else -> { /* Not showing any overlay */ }
                    }
                }
            }
        }
        pendingLogin?.let { login ->
            PasswordSaveDialog(
                siteUrl = login.siteUrl,
                username = login.username,
                plainPassword = login.plainPassword,
                onDismiss = { login.dismiss() },
                onSave = { site, user, pass ->
                    login.confirm(user, pass)
                }
            )
        }

    }
    // Helper to determine if URL should be excluded from history
    private fun shouldSkipHistoryRecording(url: String): Boolean {
        return url.contains("nimbus-browser-backend-production.up.railway.app") &&
                (url.contains("mobile=true") ||
                        url.contains("oauth-callback") ||
                        url.contains("login") ||
                        url.contains("signup")) ||
                        url.contains("dashboard")
    }

    fun checkWorkStatus() {
        val workManager = WorkManager.getInstance(applicationContext)

        workManager.getWorkInfosByTagLiveData("sync_worker")
            .observe(this) { workInfoList ->
                for (workInfo in workInfoList) {
                    Log.d("WorkStatus", "Sync worker state: ${workInfo.state}")
                }
            }
    }
    private fun monitorWorkManager() {
        val workManager = WorkManager.getInstance(applicationContext)

        // Monitor periodic sync workers
        workManager.getWorkInfosByTagLiveData("sync_worker")
            .observe(this) { workInfoList ->
                Log.d("WorkMonitor", "Found ${workInfoList.size} sync workers")
                workInfoList.forEachIndexed { index, workInfo ->
                    Log.d("WorkMonitor", "Sync worker #${index + 1}: id=${workInfo.id}, state=${workInfo.state}")
                }
            }

        // Monitor one-time sync workers
        workManager.getWorkInfosByTagLiveData("immediate_sync")
            .observe(this) { workInfoList ->
                Log.d("WorkMonitor", "Found ${workInfoList.size} immediate sync workers")
                workInfoList.forEachIndexed { index, workInfo ->
                    Log.d("WorkMonitor", "Immediate sync worker #${index + 1}: id=${workInfo.id}, state=${workInfo.state}")
                }
            }
    }
}