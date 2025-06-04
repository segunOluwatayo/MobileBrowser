package com.example.mobilebrowser.ui.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.composed
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.foundation.layout.BoxWithConstraints

/**
 * A full-screen dialog that warns the user about potentially malicious websites.
 * This appears before the site loads to protect the user from malicious content.
 *
 * @param url The potentially malicious URL.
 * @param verdict The verdict from the ML model (including score if available).
 * @param onProceed Callback for when the user decides to proceed anyway.
 * @param onGoBack Callback for when the user decides to go back.
 * @param onDismiss Callback for when the dialog is dismissed.
 */
@Composable
fun MaliciousWebsiteDialog(
    url: String,
    verdict: String,
    onProceed: () -> Unit,
    onGoBack: () -> Unit,
    onDismiss: () -> Unit
) {
    // Create animation state for entrance
    val animationState = remember {
        MutableTransitionState(false).apply {
            targetState = true
        }
    }

    // Full screen container
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF000000).copy(alpha = 0.85f),
                        Color(0xFF000000).copy(alpha = 0.95f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        val maxH = maxHeight
        val isCompact = maxH < 600.dp

        val outerPadding = if (isCompact) 16.dp else 24.dp
        val iconSize = if (isCompact) 48.dp else 64.dp
        val smallIconSize = 28.dp
        val warningIconSize = if (isCompact) (iconSize * 0.56f) else 36.dp
        val titleStyle = if (isCompact) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineSmall
        val subtitleStyle = if (isCompact) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.titleMedium
        val bodyPaddingHorizontal = if (isCompact) 4.dp else 8.dp
        val afterIconSpacer = if (isCompact) 8.dp else 20.dp
        val afterTitleSpacer = if (isCompact) 4.dp else 8.dp
        val afterSubtitleSpacer = if (isCompact) 12.dp else 16.dp
        val betweenElementsSpacer = if (isCompact) 12.dp else 16.dp
        val afterUrlCardSpacer = if (isCompact) 12.dp else 16.dp
        val betweenButtonsSpacer = if (isCompact) 8.dp else 12.dp
        val bottomSpacer = if (isCompact) 8.dp else 12.dp

        AnimatedVisibility(
            visibleState = animationState,
            enter = fadeIn(animationSpec = tween(300)) +
                    slideInVertically(
                        animationSpec = tween(500),
                        initialOffsetY = { it / 2 }
                    )
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        // Make content scrollable to avoid compression
                        .verticalScroll(rememberScrollState())
                        .padding(outerPadding),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Top security icons row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            modifier = Modifier.size(smallIconSize),
                            tint = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Box(
                            modifier = Modifier
                                .size(iconSize)
                                .background(
                                    color = MaterialTheme.colorScheme.errorContainer,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.WarningAmber,
                                contentDescription = "Security Warning",
                                modifier = Modifier.size(warningIconSize),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            modifier = Modifier.size(smallIconSize),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(afterIconSpacer))

                    // Title
                    Text(
                        text = "Security Warning",
                        style = titleStyle,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(afterTitleSpacer))

                    // Subtitle
                    Text(
                        text = "Potential security threat detected",
                        style = subtitleStyle,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(afterSubtitleSpacer))

                    // Description
                    Text(
                        text = "Our security system has identified this website as potentially harmful:",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = bodyPaddingHorizontal)
                    )

                    Spacer(modifier = Modifier.height(betweenElementsSpacer))

                    // URL Display card
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = betweenElementsSpacer),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp),
                        tonalElevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.errorContainer,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.WarningAmber,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier
                                        .padding(6.dp)
                                        .size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Text(
                                text = url,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(afterUrlCardSpacer))

                    // ML Verdict section (if available)
                    if (verdict.isNotEmpty()) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = "Why we're warning you:",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = "This website may contain harmful content or attempt to steal your personal information. Proceed with caution.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(betweenElementsSpacer))
                    }

                    // Risk explanation
                    Text(
                        text = "Continuing to this website could put your device and personal information at risk.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = bodyPaddingHorizontal)
                    )

                    Spacer(modifier = Modifier.height(if (isCompact) 16.dp else 24.dp))

                    // Action Buttons
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Primary safe action
                        Button(
                            onClick = onGoBack,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ArrowBack,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Text(
                                "Go Back",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }

                        Spacer(modifier = Modifier.height(betweenButtonsSpacer))

                        // Secondary risky action
                        OutlinedButton(
                            onClick = onProceed,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text(
                                "Proceed at My Own Risk",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(bottomSpacer))

                    // Protection note
                    Text(
                        text = "Your safety is our priority. The page has not been loaded for your protection.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = bodyPaddingHorizontal)
                    )
                }
            }
        }
    }
}
