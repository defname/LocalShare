package com.defname.sendfile

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stream
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import com.defname.sendfile.ui.theme.SendFileTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import org.eclipse.jetty.alpn.ALPN

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
        }

        // Prüfen, ob die App durch eine "Teilen"-Aktion gestartet wurde
        if (intent?.action == Intent.ACTION_SEND) {
            // Die URI (der Pfad) zur Datei extrahieren
            val sharedFileUri = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
            Log.d("MainActivity", "Received shared file URI: $sharedFileUri")
            ServerRepository.setFileUri(this, sharedFileUri)
        }

        enableEdgeToEdge()
        setContent {
            SendFileTheme {
                MainScreen()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == Intent.ACTION_SEND) {
            // Die URI (der Pfad) zur Datei extrahieren
            val sharedFileUri = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
            Log.d("MainActivity", "Received shared file URI: $sharedFileUri")
            ServerRepository.setFileUri(this, sharedFileUri)
        }
    }

}

fun hasNotificationPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
}

fun requestNotificationPermission(context: Context, launcher: ActivityResultLauncher<String>) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val status = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
        if (status != PackageManager.PERMISSION_GRANTED) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            // Optional: Permission already granted, perform the notification logic
        }
    } else {
        // API < 33: Notifications are enabled by default, no runtime permission needed
    }
}


@Composable
fun MainScreen() {
    val state by ServerRepository.state.collectAsState()

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        // Wir nutzen eine Box, um das innerPadding (von enableEdgeToEdge) anzuwenden
        Box(modifier = Modifier.padding(innerPadding)) {
            if (state.fileUri != null) {
                ServerControlScreen()
            }
            else {
                Greeting()
            }
        }
    }
}
@Composable
fun Greeting() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Welcome!",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(16.dp)
        )
        Text(
            text = "Please use the send button to share a file.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(16.dp)
        )
    }
}


fun shareText(context: Context, text: String) {
    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, text)
        type = "text/plain"
    }
    val shareIntent = Intent.createChooser(sendIntent, null)
    context.startActivity(shareIntent)
}

@Composable
fun QrCodeDialog(url: String, onDismiss: () -> Unit) {
    val qrBitmap = remember(url) { generateQRCode(url) }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close")
            }
        },
        dismissButton = {
            IconButton(onClick = {shareText(context, url)}){
                Icon(imageVector = Icons.Default.Share, contentDescription = "Share")
            }
        },
        title = { Text("QR Code") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    bitmap = qrBitmap.asImageBitmap(),
                    contentDescription = "QR Code",
                    modifier = Modifier
                        .size(256.dp)
                        .padding(4.dp)
                )
                Text(url)
            }
        },
    )
}


@Composable
fun ServerControlScreen() {
    val state by ServerRepository.state.collectAsState()
    val context = LocalContext.current

    var permissionGranted by remember { mutableStateOf(hasNotificationPermission(context)) }
    val permissionLauncher = rememberLauncherForActivityResult (
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        permissionGranted = isGranted
    }

    var showQrCodeDialog by remember { mutableStateOf<String?>(null) }

    val scrollState = rememberScrollState()

    val filePickerDialog = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            ServerRepository.setFileUri(context, uri)
        }
    }

    showQrCodeDialog?.let { url ->
        QrCodeDialog(url = url, onDismiss = { showQrCodeDialog = null })
    }


    Column (
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 24.dp, top = 24.dp, end = 24.dp, bottom = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1.0f)
                .padding(bottom = 8.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Shared File", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(8.dp))

            Card {
                Row {
                    Text("${state.fileName}", Modifier
                        .padding(8.dp)
                        .weight(1f), fontWeight = FontWeight.Bold)
                    IconButton(onClick = { filePickerDialog.launch("*/*") }) {
                        Icon(imageVector = Icons.Default.FileOpen, contentDescription = "Open")
                    }
                }
                if (state.filePreview != null) {
                    Image(
                        bitmap = state.filePreview!!.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .padding(8.dp)
                            .align(Alignment.CenterHorizontally)
                    )
                }

                Row(Modifier.padding(8.dp)) {
                    Text("${state.fileUri}", Modifier.weight(1f))
                }
            }


            Spacer(modifier = Modifier.height(16.dp))
            Text("Server Settings", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(8.dp))

            Card {
                Row(Modifier.padding(8.dp)) {
                    Text("Token", Modifier.weight(1f))
                    Column(Modifier.weight(2f)) {
                        Row {
                            Text("${state.token}", Modifier.weight(2f))
                        }
                        Row {
                            TextField(
                                value = state.customToken ?: "",
                                onValueChange = { ServerRepository.setCustomToken(context, it) },
                                enabled = true,
                                modifier = Modifier.weight(2f)
                            )
                            Checkbox(
                                checked = state.useCustomToken,
                                onCheckedChange = {
                                    ServerRepository.setUseCustomToken(
                                        context,
                                        it
                                    )
                                })
                        }
                    }

                }

                Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    SingleChoiceSegmentedButtonRow {
                        listOf(
                            SegmentedButton(
                                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                                selected = !state.deliverAsStream,
                                onClick = { ServerRepository.setDeliverAsStream(false) },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.Download,
                                        contentDescription = null
                                    )
                                },
                                label = { Text("Download") }
                            ),

                            SegmentedButton(
                                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                                selected = state.deliverAsStream,
                                onClick = { ServerRepository.setDeliverAsStream(true) },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.Stream,
                                        contentDescription = null
                                    )
                                },
                                label = { Text("Stream") }
                            )
                        )
                    }
                }
            }


            if (state.isRunning) {

                if (state.localIpAddresses.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Networks (${state.localIpAddresses.size})",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(top = 16.dp)
                    )

                    state.localIpAddresses.forEach { netInfo ->
                        val url = "http://${netInfo.ip}:8080/download/${ServerRepository.getToken()}"

                        Row(
                            Modifier
                                .padding(8.dp)
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.background)
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outline,
                                    MaterialTheme.shapes.medium
                                )
                                .clickable { showQrCodeDialog = url }
                        ) {
                            Column(Modifier.padding(8.dp)) {
                                Row {
                                    Text(
                                        netInfo.ip,
                                        Modifier.weight(2f),
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(netInfo.interfaceName, fontSize = 12.sp)
                                }
                                Row(Modifier.fillMaxWidth()) {
                                    Text(
                                        url,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }


                if (state.activeClients.isNotEmpty()) {
                    Spacer(Modifier.height(16.dp))

                    Text(
                        "Clients (${state.activeClients.size})",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(top = 16.dp)
                    )

                    state.activeClients.distinct().forEach { ip ->
                        Row(
                            Modifier
                                .padding(8.dp)
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.background)
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outline,
                                    MaterialTheme.shapes.medium
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                ip, Modifier
                                    .weight(2f)
                                    .padding(start = 8.dp)
                            )
                            IconButton(
                                onClick = { ServerRepository.banIp(ip) }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Block,
                                    contentDescription = "Ban",
                                    tint = Color.Red
                                )
                            }
                        }
                    }
                }




                if (state.bannedIps.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        "Bans (${state.bannedIps.size})",
                        style = MaterialTheme.typography.headlineSmall,
                    )

                    state.bannedIps.distinct().forEach { ip ->
                        Row(
                            Modifier
                                .padding(8.dp)
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.background)
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outline,
                                    MaterialTheme.shapes.medium
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                ip, Modifier
                                    .weight(2f)
                                    .padding(start = 8.dp)
                            )
                            IconButton(
                                onClick = { ServerRepository.unbanIp(ip) }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Block,
                                    contentDescription = "Unban",
                                    tint = Color.Green
                                )
                            }
                        }
                    }
                }
            }
        }

        Surface(modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)) {
            if (!permissionGranted) {
                Button(
                    onClick = { requestNotificationPermission(context, permissionLauncher) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                ) {
                    Text("Request Notification Permission")
                }
            } else {
                if (!state.isRunning) {
                    // Button zum Starten
                    Button(onClick = { ServerRepository.startServer(context) }) {
                        Text("Run Server")
                    }
                } else {
                    // Button zum Stoppen (wenn er schon läuft)
                    Button(
                        onClick = { ServerRepository.stopServer(context) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) {
                        Text("Stop Server")
                    }
                }
            }
        }
    }
}

