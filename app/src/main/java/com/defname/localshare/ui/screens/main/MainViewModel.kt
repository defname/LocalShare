package com.defname.localshare.ui.screens.main

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow


class MainViewModel : ViewModel() {
    val _state = MutableStateFlow(MainState())
    val state = _state.asStateFlow()


}