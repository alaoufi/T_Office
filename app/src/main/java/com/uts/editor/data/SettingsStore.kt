package com.uts.editor.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

enum class ThemeMode { SYSTEM, LIGHT, DARK }
enum class AppLanguage(val tag: String?) { SYSTEM(null), ARABIC("ar"), ENGLISH("en") }

data class AppSettings(
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val fontSizeSp: Float = 15f,
    val autosaveEnabled: Boolean = true,
    val syntaxEnabled: Boolean = true,
    val lineNumbers: Boolean = true,
    val wordWrap: Boolean = true,
    /** SAF tree Uri (as string) of the default folder new files are saved into. */
    val saveFolderUri: String? = null,
    /** Human-readable name of that folder, for display in settings. */
    val saveFolderName: String? = null,
)

private val Context.dataStore by preferencesDataStore(name = "uts_settings")

/** Persists user preferences and the last-used encoding per file. */
class SettingsStore(private val context: Context) {

    private object Keys {
        val THEME = stringPreferencesKey("theme")
        val FONT = floatPreferencesKey("font_size")
        val AUTOSAVE = booleanPreferencesKey("autosave")
        val SYNTAX = booleanPreferencesKey("syntax")
        val LINE_NUMBERS = booleanPreferencesKey("line_numbers")
        val WORD_WRAP = booleanPreferencesKey("word_wrap")
        val SAVE_FOLDER_URI = stringPreferencesKey("save_folder_uri")
        val SAVE_FOLDER_NAME = stringPreferencesKey("save_folder_name")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { p ->
        AppSettings(
            theme = p[Keys.THEME]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM,
            fontSizeSp = p[Keys.FONT] ?: 15f,
            autosaveEnabled = p[Keys.AUTOSAVE] ?: true,
            syntaxEnabled = p[Keys.SYNTAX] ?: true,
            lineNumbers = p[Keys.LINE_NUMBERS] ?: true,
            wordWrap = p[Keys.WORD_WRAP] ?: true,
            saveFolderUri = p[Keys.SAVE_FOLDER_URI],
            saveFolderName = p[Keys.SAVE_FOLDER_NAME],
        )
    }

    suspend fun current(): AppSettings = settings.first()

    suspend fun setTheme(mode: ThemeMode) = edit { it[Keys.THEME] = mode.name }
    suspend fun setFontSize(sp: Float) = edit { it[Keys.FONT] = sp.coerceIn(8f, 40f) }
    suspend fun setAutosave(on: Boolean) = edit { it[Keys.AUTOSAVE] = on }
    suspend fun setSyntax(on: Boolean) = edit { it[Keys.SYNTAX] = on }
    suspend fun setLineNumbers(on: Boolean) = edit { it[Keys.LINE_NUMBERS] = on }
    suspend fun setWordWrap(on: Boolean) = edit { it[Keys.WORD_WRAP] = on }
    suspend fun setSaveFolder(uri: String?, name: String?) = edit {
        if (uri == null) it.remove(Keys.SAVE_FOLDER_URI) else it[Keys.SAVE_FOLDER_URI] = uri
        if (name == null) it.remove(Keys.SAVE_FOLDER_NAME) else it[Keys.SAVE_FOLDER_NAME] = name
    }

    // --- Per-file last encoding memory ---

    suspend fun rememberEncoding(uriKey: String, encodingId: String) = edit {
        it[stringPreferencesKey("enc_" + uriKey.hashCode())] = encodingId
    }

    suspend fun lastEncodingFor(uriKey: String): String? =
        context.dataStore.data.first()[stringPreferencesKey("enc_" + uriKey.hashCode())]

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.dataStore.edit(block)
    }
}
