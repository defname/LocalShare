package com.defname.localshare.di

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import com.defname.localshare.data.DataStoreSettingsRepository
import com.defname.localshare.data.FileInfoProvider
import com.defname.localshare.data.LogsRepository
import com.defname.localshare.data.NetworkInfoProvider
import com.defname.localshare.data.PermissionRepository
import com.defname.localshare.data.SecurityRepository
import com.defname.localshare.data.ServiceRepository
import com.defname.localshare.domain.repository.SettingsRepository
import com.defname.localshare.domain.usecase.AddFilesUseCase
import com.defname.localshare.domain.usecase.ManageServiceUseCase
import com.defname.localshare.service.ServerIdleManager
import com.defname.localshare.service.ServerSecurityHandler
import com.defname.localshare.service.notification.NotificationHelper
import com.defname.localshare.ui.screens.logs.LogsViewModel
import com.defname.localshare.ui.screens.main.MainViewModel
import com.defname.localshare.ui.screens.servercontrol.ServerControlViewModel
import com.defname.localshare.ui.screens.settings.SettingsViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val Context.dataStore by preferencesDataStore(name = "settings")

val appModule = module {
    single { LogsRepository() }

    single { CoroutineScope(SupervisorJob() + Dispatchers.Default) }

    single { FileInfoProvider(androidContext().contentResolver) }
    single { NetworkInfoProvider() }

    single { androidContext().dataStore }

    single { SecurityRepository(get(), get()) }
    single { PermissionRepository(androidContext()) }
    single { ServiceRepository(get()) }
    single<SettingsRepository> { DataStoreSettingsRepository(get()) }

    single { NotificationHelper(androidContext()) }

    single { ServerIdleManager(get(), get(), CoroutineScope(Dispatchers.Default + SupervisorJob())) }

    factory { AddFilesUseCase(get(), get()) }
    factory { ManageServiceUseCase(get()) }
    factory { ServerSecurityHandler(get(), get()) }

    viewModel { MainViewModel(get(), get(), get(), get(), get(), get()) }
    viewModel { ServerControlViewModel(get(), get(), get(), get(), get(), get()) }
    viewModel { LogsViewModel(get(), get(), get()) }
    viewModel { SettingsViewModel(get(), get()) }
}
