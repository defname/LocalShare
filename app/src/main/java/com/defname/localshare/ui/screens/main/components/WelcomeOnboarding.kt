// SPDX-FileCopyrightText: 2026 2026 defname
//
// SPDX-License-Identifier: GPL-3.0-or-later

package com.defname.localshare.ui.screens.main.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.WavingHand
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch

data class OnboardingPage(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val color: androidx.compose.ui.graphics.Color
)

@Composable
fun WelcomeOnboarding(
    onDismiss: () -> Unit,
    onDontShowAgain: () -> Unit
) {
    val pages = listOf(
        OnboardingPage(
            title = "Welcome to LocalShare!",
            description = "Share files and text across your local network. No cloud or internet required.",
            icon = Icons.Default.WavingHand,
            color = MaterialTheme.colorScheme.primary
        ),
        OnboardingPage(
            title = "1. Add Content",
            description = "Tap the 'Add Files' button to pick files or share files or text directly from other apps via the Android share sheet.",
            icon = Icons.Default.AddCircle,
            color = MaterialTheme.colorScheme.primary
        ),
        OnboardingPage(
            title = "2. Start Sharing",
            description = "Tap 'Start Sharing' to make your content available. If someone tries to connect you will be asked for confirmation.",
            icon = Icons.Default.Send,
            color = MaterialTheme.colorScheme.primary
        ),
        OnboardingPage(
            title = "3. Connect & Share",
            description = "Other devices just need to scan the QR code or open the link in their browser. No app installation needed for them!",
            icon = Icons.Default.QrCodeScanner,
            color = MaterialTheme.colorScheme.primary
        ),
        OnboardingPage(
            title = "Need Help?",
            description = "If you're stuck, look for the (?) icon in the menu. It opens the full guide (only works while the server is running).",
            icon = Icons.Default.QuestionMark,
            color = MaterialTheme.colorScheme.error
        )
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight()
                .padding(16.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.height(320.dp)
                ) { pageIndex ->
                    val page = pages[pageIndex]
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = page.icon,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            tint = page.color
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = page.title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = page.description,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Page Indicators (Dots)
                Row(
                    modifier = Modifier.height(8.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    repeat(pages.size) { iteration ->
                        val color = if (pagerState.currentPage == iteration) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.outlineVariant
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .clip(CircleShape)
                                .background(color)
                                .size(8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDontShowAgain) {
                        Text("Don't show again")
                    }

                    if (pagerState.currentPage < pages.size - 1) {
                        Button(
                            onClick = {
                                scope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
                        ) {
                            Text("Next")
                            Spacer(Modifier.width(8.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                        }
                    } else {
                        Button(
                            onClick = onDismiss,
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                        ) {
                            Text("Got it!")
                        }
                    }
                }
            }
        }
    }
}
