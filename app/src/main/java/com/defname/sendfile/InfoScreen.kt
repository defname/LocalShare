package com.defname.sendfile

import android.content.pm.PackageManager
import android.graphics.drawable.AdaptiveIconDrawable
import android.media.Image
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@Composable
fun InfoScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    // Daten sicher abrufen
    val packageInfo = remember {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
        } catch (e: Exception) {
            null
        }
    }

    val appName = stringResource(id = R.string.app_name)
    val appIcon = remember {
        val icon = context.packageManager.getApplicationIcon(context.packageName)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && icon is AdaptiveIconDrawable) {
            icon.foreground
        } else {
            icon
        }
    }
    val versionName = packageInfo?.versionName ?: "Unknown"
    val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        packageInfo?.longVersionCode ?: 0L
    } else {
        @Suppress("DEPRECATION")
        packageInfo?.versionCode?.toLong() ?: 0L
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(MaterialTheme.colorScheme.background)
    ) {

        Spacer(modifier = Modifier.height(16.dp))

        // Toolbar Bereich
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = appIcon,
                contentDescription = "App Icon",
                modifier = Modifier
                    .size(130.dp)
                    .clip(MaterialTheme.shapes.large)
            )
            Text(appName, style = MaterialTheme.typography.headlineMedium)

        }

        Spacer(Modifier.height(32.dp))

        // App Infos
        Card(
            Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp)) {
                InfoRow(label = "Version", value = versionName)
                InfoRow(label = "Build Number", value = versionCode.toString())
                InfoRow(label = "Package Name", value = context.packageName)
            }
        }
        Spacer(Modifier.height(32.dp))

        // Lizenzen & Credits
        Text("Credits", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))

        val papirusUrl = stringResource(R.string.icon_theme_url)
        val annotatedDescription = buildAnnotatedString {
            append("Licensed  under ${stringResource(R.string.icon_theme_license)}\n")
            withLink(
                LinkAnnotation.Url(
                    url = papirusUrl,
                    styles = TextLinkStyles(
                        style = SpanStyle(
                            color = MaterialTheme.colorScheme.primary,
                            textDecoration = TextDecoration.Underline
                        )
                    )
                )
            ) {
                append(papirusUrl)
            }
        }

        Card (
            Modifier
                .fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    stringResource(R.string.icon_theme),
                    style = MaterialTheme.typography.headlineSmall,
                )
                Spacer(Modifier.height(8.dp))
                Text(annotatedDescription)
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}