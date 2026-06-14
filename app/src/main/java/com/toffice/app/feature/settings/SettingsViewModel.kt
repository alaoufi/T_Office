package com.toffice.app.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.toffice.app.core.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val pageSizeId: String = "A4",
    val landscape: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repo: SettingsRepository,
) : ViewModel() {

    val uiState = combine(repo.defaultPageSizeId, repo.defaultLandscape) { id, land ->
        SettingsUiState(id, land)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun setPageSize(id: String) {
        viewModelScope.launch { repo.setDefaultPageSize(id) }
    }

    fun setLandscape(landscape: Boolean) {
        viewModelScope.launch { repo.setDefaultLandscape(landscape) }
    }
}
