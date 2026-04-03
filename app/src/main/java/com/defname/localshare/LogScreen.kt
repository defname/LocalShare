package com.defname.localshare

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun LogScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
        )
    {
        Spacer(modifier = Modifier.height(16.dp))
        LogList()
    }
}