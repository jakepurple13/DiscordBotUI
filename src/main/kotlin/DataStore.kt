import androidx.datastore.preferences.core.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import okio.Path.Companion.toPath

internal val BOT_TOKEN = stringPreferencesKey("BOT_TOKEN")

object DataStore {
    private val dataStore = PreferenceDataStoreFactory.createWithPath { "androidx.preferences_pb".toPath() }

    val botToken: Flow<String> = dataStore.data.mapNotNull { it[BOT_TOKEN] }

    suspend fun updateToken(token: String) {
        updatePref(BOT_TOKEN, token)
    }

    private suspend fun <T> updatePref(key: Preferences.Key<T>, value: T) = dataStore.edit { it[key] = value }
}