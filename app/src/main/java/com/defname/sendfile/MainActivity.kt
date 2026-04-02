package com.defname.sendfile

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.format.Formatter
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import com.defname.sendfile.ui.theme.SendFileTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.SvgDecoder

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
        }

        // Prüfen, ob die App durch eine "Teilen"-Aktion gestartet wurde
        if (intent != null) {
            onNewIntent(intent)
        }

        ServerRepository.updateLocalIpAddresses()

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
            if (intent.hasExtra(Intent.EXTRA_STREAM)) {
                val uri = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
                uri?.let { ServerRepository.addFile(this, it) }
            }
        } else if (intent.action == Intent.ACTION_SEND_MULTIPLE) {
            if (intent.hasExtra(Intent.EXTRA_STREAM)) {
                val uris = IntentCompat.getParcelableArrayListExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
                uris?.let { ServerRepository.addFiles(this, it) }
            }
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
            ServerControlScreen()
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


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileCarousel(
    selectedFiles: Set<Uri>,
    onToggleSelection: (Uri) -> Unit
) {
    val state by ServerRepository.state.collectAsState()
    val context = LocalContext.current
    val svgImageLoader = remember {
        ImageLoader.Builder(context)
            .components { add(SvgDecoder.Factory())}
            .build()
    }

    HorizontalMultiBrowseCarousel(
        state = rememberCarouselState { state.fileList.size },
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(top = 16.dp, bottom = 16.dp),
        preferredItemWidth = 48.dp,
        itemSpacing = 8.dp,
        maxSmallItemWidth = 48.dp,
        minSmallItemWidth = 48.dp,
        contentPadding = PaddingValues(horizontal = 8.dp)
    ) { i ->
        val item = state.fileList[i]
        val isSelected = selectedFiles.contains(item.uri)

        Card(
            Modifier
                .maskClip(MaterialTheme.shapes.medium)
                .combinedClickable(
                    onClick = { if (selectedFiles.isNotEmpty()) onToggleSelection(item.uri) },
                    onLongClick = { onToggleSelection(item.uri) }
                )
        ) {
            if (item.filePreview != null) {
                Image(
                    modifier = Modifier
                        .size(48.dp)
                        .maskClip(MaterialTheme.shapes.small)
                        .alpha(if (isSelected) 0.3f else 1f),
                    bitmap = item.filePreview.asImageBitmap(),
                    contentDescription = item.name,
                    contentScale = ContentScale.Crop,
                    colorFilter = if (isSelected) {
                        ColorFilter.tint(MaterialTheme.colorScheme.error.copy(alpha = 0.5f), BlendMode.SrcAtop)
                    } else null
                )
            } else {
                val iconFilename = item.iconFile
                val assetPath = "file:///android_asset/fileicons/$iconFilename"

                Box(
                    Modifier
                        .size(48.dp)
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.surfaceVariant
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = assetPath,
                        imageLoader = svgImageLoader, // Hier wird der SVG-Decoder genutzt
                        contentDescription = item.name,
                        modifier = Modifier.size(32.dp),
                    )
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IpAddressSelector() {
    val state by ServerRepository.state.collectAsState()
    var expanded by remember { mutableStateOf(false) }

    // Wir zeigen die aktuell gewählte IP oder "All Interfaces" an
    val selectedOption = state.selectedIp ?: "All Interfaces (0.0.0.0)"

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = {
            expanded = !expanded
           if (it) {
               ServerRepository.updateLocalIpAddresses()
           }
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        OutlinedTextField(
            value = selectedOption,
            onValueChange = {},
            readOnly = true, // Verhindert Tastatureingabe
            label = { Text("Bind Server to IP") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            enabled = !state.isRunning
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            // Option 1: Alle Interfaces
            DropdownMenuItem(
                text = { Text("All Interfaces (0.0.0.0)") },
                onClick = {
                    ServerRepository.setSelectedIp(null)
                    expanded = false
                },
                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
            )

            // Dynamische Optionen aus der IP-Liste
            state.localIpAddresses.forEach { netInfo ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(netInfo.ip)
                            Text(netInfo.interfaceName, style = MaterialTheme.typography.labelSmall)
                        }
                    },
                    onClick = {
                        ServerRepository.setSelectedIp(netInfo.ip)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }
        }
    }
}

@Composable
fun LogList(maxEntries: Int = 0) {
    val state by ServerRepository.state.collectAsState()
    var dropDownIdx by remember { mutableStateOf<Int>(-1) }

    val logsToShow = if (maxEntries > 0) {
        state.logs.reversed().take(maxEntries)
    } else {
        state.logs.reversed()
    }

    Column {
        logsToShow.forEachIndexed { idx, log ->
            Box {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(8.dp, 4.dp)
                        .combinedClickable(
                            onClick = { },
                            onLongClick = { dropDownIdx = idx }
                        )
                ) {
                    Text("${log.status}", color = if (log.status >= 400) Color.Red else Color.Green)
                    Spacer(Modifier.width(8.dp))
                    Text(log.method)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        log.path,
                        modifier = Modifier.weight(1f),
                        //style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.StartEllipsis
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        log.clientIp,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (ServerRepository.isBlacklisted(log.clientIp)) Color.Red else if (ServerRepository.isWhitelisted(log.clientIp)) Color.Green else MaterialTheme.colorScheme.outline
                    )
                }

                DropdownMenu(
                    expanded = dropDownIdx == idx,
                    onDismissRequest = { dropDownIdx = -1 }
                ) {
                    if (ServerRepository.isWhitelisted(log.clientIp)) {
                        DropdownMenuItem(
                            text = { Text("Remove ${log.clientIp} from whitelist") },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Block,
                                    contentDescription = null,
                                    //tint = Color.Red
                                )
                            },
                            onClick = {
                                ServerRepository.removeFromWhitelist(log.clientIp)
                                dropDownIdx = -1
                            }
                        )
                    }
                    if (ServerRepository.isBlacklisted(log.clientIp)) {
                        DropdownMenuItem(
                            text = { Text("Remove ${log.clientIp} from blacklist") },
                            leadingIcon = {
                                Icon(Icons.Default.Undo, contentDescription = null, tint = Color.Green)
                            },
                            onClick = {
                                ServerRepository.removeFromBlacklist(log.clientIp)
                                dropDownIdx = -1
                            }
                        )
                    }
                    else {
                        DropdownMenuItem(
                            text = { Text("Add ${log.clientIp} to blacklist") },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Block,
                                    contentDescription = null,
                                    tint = Color.Red
                                )
                            },
                            onClick = {
                                ServerRepository.addToBlacklist(log.clientIp)
                                dropDownIdx = -1
                            }
                        )
                    }
                }
            }
        }
    }
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

    var filesToDelete by remember { mutableStateOf(setOf<Uri>()) }
    val toggleFileSelection = { uri: Uri ->
        filesToDelete = if (filesToDelete.contains(uri)) {
            filesToDelete - uri
        } else {
            filesToDelete + uri
        }
    }

    val scrollState = rememberScrollState()

    val filePickerDialog = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) {
            ServerRepository.addFiles(context, uris)
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
        ) {
            Row (verticalAlignment = Alignment.CenterVertically){
                Text(
                    "Shared Files",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.weight(1f)
                )
                if (state.fileList.isNotEmpty()) {
                    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                        IconButton(
                            onClick = { ServerRepository.clearFiles(); filesToDelete = emptySet() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            if (state.fileList.isNotEmpty()) {
                Text("Files: ${state.fileList.size}", style = MaterialTheme.typography.bodySmall)
                Text("Total Size: ${Formatter.formatFileSize(context, state.fileList.sumOf { it.size })}", style = MaterialTheme.typography.bodySmall)
                FileCarousel(filesToDelete, toggleFileSelection)
            }
            Row {
                Spacer(Modifier.weight(1f))
                if (filesToDelete.isNotEmpty()) {
                    Button(
                        modifier = Modifier
                            .padding(end = 8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ),
                        onClick = { filesToDelete.forEach { uri -> ServerRepository.removeFile(uri) }; filesToDelete = emptySet() }
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Remove")
                    }
                    Button(
                        onClick = { filesToDelete = emptySet() }
                    ) {
                        Icon(imageVector = Icons.Default.Cancel, contentDescription = "Cancel")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Cancel")
                    }
                }
                else {
                    Button(
                        onClick = { filePickerDialog.launch("*/*") }
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Add")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Files")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Server Settings", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(8.dp))

            Row(Modifier.padding(8.dp)) {
                OutlinedTextField(
                    value = state.token,
                    label = { Text("Token") },
                    singleLine = true,
                    onValueChange = { ServerRepository.setToken(it) },
                    enabled = true,
                    modifier = Modifier
                        .weight(2f)
                        .fillMaxWidth(),
                    trailingIcon = { IconButton(onClick = { ServerRepository.setRandomToken() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Generate Random Token")
                    } }
                )

            }

            IpAddressSelector()

            Spacer(modifier = Modifier.height(16.dp))
            Row (verticalAlignment = Alignment.CenterVertically){
                Text(
                    "Logs",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.weight(1f)
                )
                if (state.logs.isNotEmpty()) {
                    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                        IconButton(
                            onClick = { ServerRepository.clearLogs() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            LogList(10)

            if (state.isRunning) {

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
                                onClick = { ServerRepository.addToBlacklist(ip) }
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

                if (state.blacklist.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        "Bans (${state.blacklist.size})",
                        style = MaterialTheme.typography.headlineSmall,
                    )

                    state.blacklist.distinct().forEach { ip ->
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
                                onClick = { ServerRepository.removeFromBlacklist(ip) }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Undo,
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
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary,
                        contentColor = MaterialTheme.colorScheme.onTertiary
                    ),
                ) {
                    Text("Request Notification Permission")
                }
            } else {
                if (!state.isRunning) {
                    // Button zum Starten
                    Button(
                        onClick = { ServerRepository.startServer(context) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryFixed,
                            contentColor = MaterialTheme.colorScheme.onPrimaryFixed
                        )
                    ) {
                        Icon(imageVector = Icons.Default.Send, contentDescription = "Start Server")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Run Server")
                    }
                } else {
                    // Button zum Stoppen (wenn er schon läuft)
                    Button(
                        onClick = { ServerRepository.stopServer(context) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ),
                    ) {
                        Icon(imageVector = Icons.Default.Block, contentDescription = "Stop Server")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Stop Server")
                    }
                }
            }
        }
    }
}

