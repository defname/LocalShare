package com.defname.localshare.ui.screens.files

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.defname.localshare.ui.screens.files.components.FileList
import org.koin.androidx.compose.koinViewModel

@Composable
fun FilesScreen(viewModel: FilesViewModel = koinViewModel()) {
    val state = viewModel.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
    )
    {
        
        FileList(
            files = state.value.fileList,
            selectedFiles = state.value.selectedFiles,
            isSelectionMode = state.value.selectedFiles.isNotEmpty(),
            onFileSelected = { viewModel.onToggleSelection(it) }
        )

        AnimatedVisibility(
            visible = state.value.selectedFiles.isNotEmpty(),
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
            modifier = Modifier
                .align(Alignment.End)
                .padding(16.dp)
        ) {
            Surface(
                tonalElevation = 8.dp,
                shape = CircleShape, // Pillen-Design
                color = MaterialTheme.colorScheme.surfaceBright,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("${state.value.selectedFiles.size} ausgewählt", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.width(16.dp))
                    VerticalDivider(Modifier.height(24.dp))
                    IconButton(onClick = {  }) {
                        Icon(Icons.Default.Delete, "Löschen", tint = MaterialTheme.colorScheme.error)
                    }
                    IconButton(onClick = { }) {
                        Icon(Icons.Default.Close, "Abbrechen")
                    }
                }
            }
        }
    }
}