package com.crewroster.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.crewroster.core.AppData
import com.crewroster.core.defaultAppData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "crew_roster")
private val APP_DATA_KEY = stringPreferencesKey("app_data_json")

class AppDataStore(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    val data: Flow<AppData> = context.dataStore.data.map { prefs ->
        val raw = prefs[APP_DATA_KEY]
        if (raw.isNullOrBlank()) {
            defaultAppData()
        } else {
            runCatching { json.decodeFromString(AppData.serializer(), raw) }
                .getOrElse { defaultAppData() }
        }
    }

    suspend fun save(data: AppData) {
        val encoded = json.encodeToString(AppData.serializer(), data)
        context.dataStore.edit { prefs -> prefs[APP_DATA_KEY] = encoded }
    }
}
