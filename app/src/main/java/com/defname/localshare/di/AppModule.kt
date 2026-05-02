// SPDX-FileCopyrightText: 2026 2026 defname
//
// SPDX-License-Identifier: GPL-3.0-or-later

package com.defname.localshare.di

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import com.defname.localshare.data.ConnectionLogsRepository
import com.defname.localshare.data.DataStoreSettingsRepository
import com.defname.localshare.data.FileInfoProvider
import com.defname.localshare.data.NetworkInfoProvider
import com.defname.localshare.data.PermissionRepository
import com.defname.localshare.data.QrCodeProvider
import com.defname.localshare.data.SecurityRepository
import com.defname.localshare.data.ServerUrlProvider
import com.defname.localshare.data.ServiceRepository
import com.defname.localshare.domain.repository.SettingsRepository
import com.defname.localshare.domain.usecase.AddFilesUseCase
import com.defname.localshare.domain.usecase.AddSharedContentUseCase
import com.defname.localshare.domain.usecase.ManageServiceUseCase
import com.defname.localshare.service.ServerIdleManager
import com.defname.localshare.service.ServerSecurityHandler
import com.defname.localshare.service.notification.NotificationHelper
import com.defname.localshare.ui.screens.files.FilesViewModel
import com.defname.localshare.ui.screens.home.HomeViewModel
import com.defname.localshare.ui.screens.logs.LogsViewModel
import com.defname.localshare.ui.screens.main.MainViewModel
import com.defname.localshare.ui.screens.settings.SettingsViewModel
import com.defname.localshare.ui.screens.sharedcontent.SharedContentViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val Context.dataStore by preferencesDataStore(name = "settings")

val appModule = module {
    single { ConnectionLogsRepository() }

    single { CoroutineScope(SupervisorJob() + Dispatchers.Default) }

    single { FileInfoProvider(androidContext().contentResolver) }
    single { NetworkInfoProvider(androidContext()) }
    single { QrCodeProvider() }

    single { androidContext().dataStore }

    single { SecurityRepository(get(), get()) }
    single { PermissionRepository(androidContext()) }
    single { ServiceRepository(get()) }
    single<SettingsRepository> { DataStoreSettingsRepository(get()) }

    single { NotificationHelper(androidContext()) }

    single { ServerIdleManager(get(), get(), CoroutineScope(Dispatchers.Default + SupervisorJob())) }

    factory { AddFilesUseCase(get(), get()) }
    factory { AddSharedContentUseCase(get()) }
    factory { ManageServiceUseCase(get(), get()) }
    factory { ServerSecurityHandler(get(), get()) }
    factory { ServerUrlProvider(get(), get())}

    viewModel { MainViewModel(get(), get(), get()) }
    viewModel { FilesViewModel(get(), get()) }
    viewModel { HomeViewModel(get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    viewModel { LogsViewModel(get(), get(), get()) }
    viewModel { SettingsViewModel(get(), get()) }
    viewModel { SharedContentViewModel(get(), get()) }
}
