import androidx.datastore.preferences.core.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import okio.Path.Companion.toPath

internal val BOT_TOKEN = stringPreferencesKey("BOT_TOKEN")
internal val SCROLL_TO_BOTTOM = booleanPreferencesKey("SCROLL_TO_BOTTOM")
internal val SEND_START_UP = booleanPreferencesKey("SEND_START_UP")
internal val SEND_SHUTDOWN = booleanPreferencesKey("SEND_SHUTDOWN")

object DataStore {
    private val dataStore = PreferenceDataStoreFactory.createWithPath { "androidx.preferences_pb".toPath() }

    val botToken: DataStoreType<String> = DataStoreType(BOT_TOKEN)

    val scrollToBottom = DataStoreTypeNonNull(SCROLL_TO_BOTTOM)

    val sendStartup = DataStoreTypeNonNull(SEND_START_UP)
    val sendShutdown = DataStoreTypeNonNull(SEND_SHUTDOWN)

    open class DataStoreType<T>(
        protected val key: Preferences.Key<T>,
    ) {
        open val flow: Flow<T?> = dataStore.data
            .map { it[key] }
            .distinctUntilChanged()

        open suspend fun update(value: T) {
            dataStore.edit { it[key] = value }
        }
    }

    open class DataStoreTypeNonNull<T>(
        key: Preferences.Key<T>,
    ) : DataStoreType<T>(key) {
        override val flow: Flow<T> = dataStore.data
            .mapNotNull { it[key] }
            .distinctUntilChanged()
    }
}