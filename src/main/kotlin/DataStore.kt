import androidx.datastore.preferences.core.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapNotNull
import okio.Path.Companion.toPath

internal val BOT_TOKEN = stringPreferencesKey("BOT_TOKEN")
internal val SCROLL_TO_BOTTOM = booleanPreferencesKey("SCROLL_TO_BOTTOM")
internal val SEND_START_UP = booleanPreferencesKey("SEND_START_UP")
internal val SEND_SHUTDOWN = booleanPreferencesKey("SEND_SHUTDOWN")

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

    val sendStartup = DataStoreType(SEND_START_UP)
    val sendShutdown = DataStoreType(SEND_SHUTDOWN)

    private suspend fun <T> updatePref(key: Preferences.Key<T>, value: T) = dataStore.edit { it[key] = value }

    class DataStoreType<T>(
        private val key: Preferences.Key<T>,
    ) {
        val flow: Flow<T> = dataStore.data
            .mapNotNull { it[key] }
            .distinctUntilChanged()

        suspend fun update(value: T) {
            dataStore.edit { it[key] = value }
        }
    }
}