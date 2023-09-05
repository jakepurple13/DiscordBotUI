import androidx.datastore.preferences.core.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import okio.Path.Companion.toPath

internal val BOT_TOKEN = stringPreferencesKey("BOT_TOKEN")
internal val SCROLL_TO_BOTTOM = booleanPreferencesKey("SCROLL_TO_BOTTOM")

object DataStore {
    private val dataStore = PreferenceDataStoreFactory.createWithPath { "androidx.preferences_pb".toPath() }

    val botToken: Flow<String> = dataStore.data.mapNotNull { it[BOT_TOKEN] }

    suspend fun updateToken(token: String) {
        updatePref(BOT_TOKEN, token)
    }

    val scrollToBottom: Flow<Boolean> = dataStore.data.mapNotNull { it[SCROLL_TO_BOTTOM] }

    suspend fun changeScrollToBottom(change: Boolean) {
        updatePref(SCROLL_TO_BOTTOM, change)
    }

    private suspend fun <T> updatePref(key: Preferences.Key<T>, value: T) = dataStore.edit { it[key] = value }
}