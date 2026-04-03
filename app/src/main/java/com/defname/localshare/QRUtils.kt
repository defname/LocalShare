package com.defname.localshare

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Stream
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.core.graphics.createBitmap
import androidx.core.graphics.set
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

fun generateQRCode(data: String, size: Int = 512): Bitmap {
    val writer = QRCodeWriter()
    val bitMatrix = writer.encode(data, BarcodeFormat.QR_CODE, size, size)
    val width = bitMatrix.width
    val height = bitMatrix.height
    val bitmap = createBitmap(width, height, Bitmap.Config.RGB_565)
    for (x in 0 until width) {
        for (y in 0 until height) {
            bitmap[x, y] = if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE
        }
    }
    return bitmap
}


@Composable
fun QrCodeDialog(onDismiss: () -> Unit, fileId: String? = null) {
    val serverState by ServerRepository.state.collectAsState()
    var createDownloadLink by remember { mutableStateOf(false) }

    // Reagiert auf fileId, den Toggle UND auf Änderungen im serverState (IP/Port/Token)
    val url = remember(fileId, createDownloadLink, serverState.selectedIp, serverState.port, serverState.token) {
        ServerRepository.getServerAdress(fileId, createDownloadLink)
    }

    // Das QR-Bitmap generieren (reagiert auf die neue URL)
    val qrBitmap by remember(url) {
        mutableStateOf(generateQRCode(url))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = {},
        //title = { Text("QR Code") },
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
                Spacer(modifier = Modifier.height(16.dp))
                SingleChoiceSegmentedButtonRow {
                    SegmentedButton (
                        shape = SegmentedButtonDefaults.itemShape(
                            index = 0,
                            count = 2
                        ),
                        onClick = { createDownloadLink = true },
                        selected = createDownloadLink,
                        label = { Text("Download") },
                        icon = { Icon(imageVector = Icons.Default.Download, contentDescription = "Download") }
                    )
                    SegmentedButton (
                        shape = SegmentedButtonDefaults.itemShape(
                            index = 1,
                            count = 2
                        ),
                        onClick = { createDownloadLink = false },
                        selected = !createDownloadLink,
                        label = { Text("Stream") },
                        icon = { Icon(imageVector = Icons.Default.Stream, contentDescription = "Download") }
                    )
                }
            }
        },
    )
}