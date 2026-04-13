/*
 * LocalShare - Share files locally
 * Copyright (C) 2024 defname
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.defname.localshare.ui.screens.info

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.defname.localshare.BuildConfig
import com.defname.localshare.R
import com.defname.localshare.ui.theme.LocalShareTheme

@Composable
fun InfoScreen() {
    val context = LocalContext.current

    val appName = stringResource(id = R.string.app_name)
    val appIcon = remember {
        ContextCompat.getDrawable(context, R.drawable.ic_launcher_foreground)
    }
    
    // Zugriff auf das von Gradle generierte BuildConfig
    val versionName = BuildConfig.VERSION_NAME
    val versionCode = BuildConfig.VERSION_CODE
    val license = stringResource(id = R.string.app_license)
    val srcUrl = stringResource(id = R.string.app_src_url)


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Spacer(modifier = Modifier.height(16.dp))

        // Toolbar Bereich
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
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
                InfoRow(label = "License", value = license)
                InfoRow(label = "Source", value = buildAnnotatedString {
                    withLink(
                        LinkAnnotation.Url(
                            url = srcUrl,
                            styles = TextLinkStyles(
                                style = SpanStyle(
                                    color = MaterialTheme.colorScheme.primary,
                                    textDecoration = TextDecoration.Underline
                                )
                            )
                        )
                    ) {
                        append(srcUrl)
                    }
                })
            }
        }
        Spacer(Modifier.height(32.dp))

        // Lizenzen & Credits
        Text("Credits", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))

        val papirusUrl = stringResource(R.string.icon_theme_url)
        val annotatedDescription = buildAnnotatedString {
            append("Licensed under ${stringResource(R.string.icon_theme_license)}\n")
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
    InfoRow(label, AnnotatedString(value))
}
@Composable
fun InfoRow(label: String, value: AnnotatedString) {
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
        Text(value, style = MaterialTheme.typography.bodyMedium, overflow = TextOverflow.Ellipsis)
    }
}

@Preview(showBackground = true)
@Composable
fun InfoRowPreview() {
    LocalShareTheme {
        InfoRow(
            label = "Version",
            value = "1.0.0"
        )
    }
}

@Preview(showBackground = true)
@Composable
fun InfoScreenPreview() {
    LocalShareTheme {
        InfoScreen()
    }
}
