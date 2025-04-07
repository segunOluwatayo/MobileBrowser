package com.example.mobilebrowser.ui.composables

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.example.mobilebrowser.data.entity.PasswordEntity
import com.example.mobilebrowser.ui.viewmodels.PasswordViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun EnhancedPasswordSection(
    passwordViewModel: PasswordViewModel,
    modifier: Modifier = Modifier
) {
    // Observe the password list from the ViewModel
    val passwordList by passwordViewModel.passwords.collectAsState()
    var selectedPassword by remember { mutableStateOf<PasswordEntity?>(null) }
    var decryptedPassword by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    if (passwordList.isEmpty()) {
        EmptyPasswordsState(modifier)
    } else {
        LazyColumn(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(passwordList) { password ->
                EnhancedPasswordItem(
                    password = password,
                    onClick = {
                        selectedPassword = password
                        isPasswordVisible = false
                        coroutineScope.launch {
                            decryptedPassword = passwordViewModel.getDecryptedPassword(password.encryptedPassword)
                        }
                    }
                )
            }
        }
    }

    // Password detail dialog with expanded functionality
    if (selectedPassword != null) {
        PasswordDetailDialog(
            password = selectedPassword!!,
            decryptedPassword = decryptedPassword,
            isPasswordVisible = isPasswordVisible,
            onTogglePasswordVisibility = { isPasswordVisible = !isPasswordVisible },
            onCopyUsername = {
                copyToClipboard(context, "Username", selectedPassword!!.username)
                Toast.makeText(context, "Username copied to clipboard", Toast.LENGTH_SHORT).show()
            },
            onCopyPassword = {
                copyToClipboard(context, "Password", decryptedPassword)
                Toast.makeText(context, "Password copied to clipboard", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { selectedPassword = null },
            onDelete = {
                coroutineScope.launch {
                    passwordViewModel.deletePassword(selectedPassword!!)
                    selectedPassword = null
                }
            }
        )
    }
}

@Composable
fun EmptyPasswordsState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No saved passwords",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "When you save passwords, they'll appear here.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
        }
    }
}

@Composable
fun EnhancedPasswordItem(
    password: PasswordEntity,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Website icon with background
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                val context = LocalContext.current
                val domain = extractDomain(password.siteUrl)

                if (domain != null) {
                    // Try to load favicon using Coil with SubcomposeAsyncImage
                    SubcomposeAsyncImage(
                        model = "https://www.google.com/s2/favicons?domain=$domain&sz=128",
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        error = {
                            // Generate color and letter for domain
                            DomainIcon(domain)
                        },
//                        fallback = {
//                            // Generate color and letter for domain
//                            DomainIcon(domain)
//                        }
                    )
                } else {
                    // Default lock icon if no domain could be extracted
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                // Show domain or full URL based on format
                val displaySite = extractDomain(password.siteUrl) ?: password.siteUrl

                Text(
                    text = displaySite,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = password.username,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "View details",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun PasswordDetailDialog(
    password: PasswordEntity,
    decryptedPassword: String,
    isPasswordVisible: Boolean,
    onTogglePasswordVisibility: () -> Unit,
    onCopyUsername: () -> Unit,
    onCopyPassword: () -> Unit,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Password Details") },
        text = {
            Column(
                modifier = Modifier.padding(top = 8.dp)
            ) {
                // Site URL with better formatting
                Text(
                    text = "Site",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = password.siteUrl,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Username with copy button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Username",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = password.username,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }

                    ActionIconButton(
                        icon = Icons.Default.ContentCopy,
                        description = "Copy username",
                        onClick = onCopyUsername
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Password with copy and visibility toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Password",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )

                        AnimatedContent(
                            targetState = isPasswordVisible,
                            transitionSpec = {
                                fadeIn() with fadeOut()
                            },
                            label = "Password visibility animation"
                        ) { visible ->
                            if (visible) {
                                Text(
                                    text = decryptedPassword,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            } else {
                                Text(
                                    text = "••••••••••••",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }

                    ActionIconButton(
                        icon = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        description = if (isPasswordVisible) "Hide password" else "Show password",
                        onClick = onTogglePasswordVisibility
                    )

                    ActionIconButton(
                        icon = Icons.Default.ContentCopy,
                        description = "Copy password",
                        onClick = onCopyPassword
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDelete,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Delete")
            }
        }
    )
}

@Composable
fun ActionIconButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = MaterialTheme.colorScheme.primary
        )
    }
}

fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(label, text)
    clipboard.setPrimaryClip(clip)
}

@Composable
fun DomainIcon(domain: String) {
    // Generate a deterministic color based on the domain
    val hash = domain.hashCode()
    val hue = ((hash % 360) + 360) % 360 // Ensure positive value 0-359
    val saturation = 0.7f
    val lightness = 0.5f

    // Convert HSL to RGB
    val c = (1 - Math.abs(2 * lightness - 1)) * saturation
    val x = c * (1 - Math.abs((hue / 60) % 2 - 1))
    val m = lightness - c / 2

    val (r, g, b) = when {
        hue < 60 -> Triple(c, x, 0f)
        hue < 120 -> Triple(x, c, 0f)
        hue < 180 -> Triple(0f, c, x)
        hue < 240 -> Triple(0f, x, c)
        hue < 300 -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }

    val color = Color(r + m, g + m, b + m)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color),
        contentAlignment = Alignment.Center
    ) {
        // Get first character of domain for the icon
        val letter = domain.first().uppercase()
        Text(
            text = letter,
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            textAlign = TextAlign.Center
        )
    }
}

fun extractDomain(url: String): String? {
    return try {
        // Handle both http/https URLs and plain domains
        val uri = if (url.startsWith("http")) {
            java.net.URI(url)
        } else {
            java.net.URI("https://$url")
        }

        var domain = uri.host
        if (domain != null) {
            if (domain.startsWith("www.")) {
                domain = domain.substring(4)
            }
            return domain
        }
        null
    } catch (e: Exception) {
        // If URI parsing fails, try to extract domain manually
        try {
            var domain = url.trim()
            if (domain.startsWith("http://")) domain = domain.substring(7)
            if (domain.startsWith("https://")) domain = domain.substring(8)
            if (domain.startsWith("www.")) domain = domain.substring(4)

            // Get the part before the first slash
            val slashIndex = domain.indexOf('/')
            if (slashIndex > 0) {
                domain = domain.substring(0, slashIndex)
            }

            return if (domain.contains(".")) domain else null
        } catch (e: Exception) {
            null
        }
    }
}
