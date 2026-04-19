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
package com.defname.localshare.ui.screens.logs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel
import com.defname.localshare.ui.components.LogList
import com.defname.localshare.ui.theme.LocalShareTheme

@Composable
fun LogsScreen(viewModel: LogsViewModel = koinViewModel()) {
    val state = viewModel.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
        )
    {
        Spacer(modifier = Modifier.height(16.dp))
        LogList(
            entries = state.value.entries,
            menuOpenForId = state.value.menuOpenForId,
            onContextMenuOpen = { viewModel.openMenu(it.id) },
            onContextMenuClose = { viewModel.closeMenu() },
            onAddToBlackList = { viewModel.addToBlacklist(it) },
            onRemoveFromBlackList = { viewModel.removeFromBlacklist(it) },
            onRemoveFromWhiteList = { viewModel.removeFromWhitelist(it) }
        )
    }
}

@Preview
@Composable
fun LogsScreenPreview() {
    LocalShareTheme {
        LogsScreen()
    }
}


