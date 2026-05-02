// SPDX-FileCopyrightText: 2026 2026 defname
//
// SPDX-License-Identifier: GPL-3.0-or-later

package com.defname.localshare

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.defname.localshare.data.PermissionRepository
import com.defname.localshare.data.ServiceRepository
import com.defname.localshare.domain.repository.SettingsRepository
import com.defname.localshare.domain.usecase.AddFilesUseCase
import com.defname.localshare.domain.usecase.AddSharedContentUseCase
import com.defname.localshare.ui.screens.main.MainScreen
import com.defname.localshare.ui.theme.LocalShareTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {
    private val settingsRepository: SettingsRepository by inject()
    private val serviceRepository: ServiceRepository by inject()
    private val addFilesUseCase: AddFilesUseCase by inject()
    private val addSharedContentUseCase: AddSharedContentUseCase by inject()
    private val permissionRepository: PermissionRepository by inject()
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        permissionRepository.updatePermissionStatus()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            splashScreen.setOnExitAnimationListener { splashScreenView ->
                splashScreenView.animate()
                    .scaleX(3f)
                    .scaleY(3f)
                    .alpha(0f)
                    .setDuration(300L)
                    .withEndAction { splashScreenView.remove() }
                    .start()
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        intent?.let { handleSendIntent(it) }

        enableEdgeToEdge()
        setContent {
            LocalShareTheme {
                MainScreen()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleSendIntent(intent)
    }

    private fun handleSendIntent(intent: Intent) {
        val action = intent.action
        if (action != Intent.ACTION_SEND && action != Intent.ACTION_SEND_MULTIPLE) return

        scope.launch {
            val settings = settingsRepository.settingsFlow.first()

            if (settings.clearFileListOnShareIntent) {
                serviceRepository.clearFiles()
            }

            val urisToGrant = mutableListOf<Uri>()
            when (action) {
                Intent.ACTION_SEND -> {
                    val uri = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
                    if (uri != null)  {
                        urisToGrant.add(uri)
                        addFilesUseCase(uri)
                    }
                    else {
                        val mime = intent.type
                        val extraText = intent.getStringExtra(Intent.EXTRA_TEXT)
                        val extraSubject = intent.getStringExtra(Intent.EXTRA_SUBJECT)
                        addSharedContentUseCase(mime, extraText, extraSubject)
                        android.widget.Toast.makeText(
                            this@MainActivity,
                            "Text added to shared texts.",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                    }
                }
                Intent.ACTION_SEND_MULTIPLE -> {
                    IntentCompat.getParcelableArrayListExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)?.let { uris ->
                        uris.forEach { uri ->
                            urisToGrant.add(uri)
                            addFilesUseCase(uri)
                        }
                    }
                }
            }

            // 4. Permissions transferieren, falls der Server bereits läuft
            if (urisToGrant.isNotEmpty() && serviceRepository.serverRunning()) {
                val serviceIntent = Intent(this@MainActivity, com.defname.localshare.service.LocalShareService::class.java).apply {
                    this.action = com.defname.localshare.service.LocalShareService.ACTION_GRANT_PERMISSION
                    this.clipData = android.content.ClipData.newRawUri("Shared Files", urisToGrant.first()).apply {
                        for (i in 1 until urisToGrant.size) {
                            addItem(android.content.ClipData.Item(urisToGrant[i]))
                        }
                    }
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startForegroundService(serviceIntent)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

}
