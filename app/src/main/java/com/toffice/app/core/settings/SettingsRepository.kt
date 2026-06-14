package com.toffice.app.core.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.toffice.app.feature.editor.model.PageSettings
import com.toffice.app.feature.editor.model.pageSizeById
import com.toffice.app.feature.editor.model.withSize
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val keyPageSize = stringPreferencesKey("default_page_size")
    private val keyLandscape = booleanPreferencesKey("default_landscape")

    val defaultPageSizeId: Flow<String> =
        context.settingsDataStore.data.map { it[keyPageSize] ?: "A4" }

    val defaultLandscape: Flow<Boolean> =
        context.settingsDataStore.data.map { it[keyLandscape] ?: false }

    suspend fun setDefaultPageSize(id: String) {
        context.settingsDataStore.edit { it[keyPageSize] = id }
    }

    suspend fun setDefaultLandscape(landscape: Boolean) {
        context.settingsDataStore.edit { it[keyLandscape] = landscape }
    }

    /** إعدادات صفحة افتراضية للمستندات الجديدة. */
    suspend fun defaultPageSettings(): PageSettings {
        val id = defaultPageSizeId.first()
        val landscape = defaultLandscape.first()
        return PageSettings().withSize(pageSizeById(id), landscape)
    }
}
