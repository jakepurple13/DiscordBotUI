package stablediffusionui

import io.realm.kotlin.MutableRealm
import io.realm.kotlin.Realm
import io.realm.kotlin.RealmConfiguration
import io.realm.kotlin.ext.asFlow
import io.realm.kotlin.ext.realmListOf
import io.realm.kotlin.migration.AutomaticSchemaMigration
import io.realm.kotlin.types.RealmList
import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.RealmUUID
import io.realm.kotlin.types.annotations.PrimaryKey
import kotlinx.coroutines.flow.mapNotNull

internal class ModelSuggesterDatabase(name: String = Realm.DEFAULT_FILE_NAME) {

    companion object {
        var instance: ModelSuggesterDatabase? = null
            private set

        fun createInstance(name: String = Realm.DEFAULT_FILE_NAME): ModelSuggesterDatabase {
            return instance ?: ModelSuggesterDatabase(name).also { instance = it }
        }
    }

    private val realm by lazy {
        Realm.open(
            RealmConfiguration.Builder(
                setOf(
                    ModelSuggesterList::class,
                    ModelSuggestion::class,
                )
            )
                .schemaVersion(6)
                .name(name)
                .migration(AutomaticSchemaMigration { })
                .build()
        )
    }

    private val modelSuggestions = realm.initDbBlocking { ModelSuggesterList() }

    fun getSuggestions() = modelSuggestions
        .asFlow()
        .mapNotNull { it.obj }

    suspend fun addSuggestion(
        link: String,
        reason: String,
        suggestedBy: String,
        avatar: String?,
        channel: String,
        server: String,
        lastMessage: String?,
    ) {
        realm.updateInfo<ModelSuggesterList> {
            it.list.add(
                ModelSuggestion().apply {
                    this.link = link
                    this.reason = reason
                    this.suggestedBy = suggestedBy
                    this.avatar = avatar
                    this.channel = channel
                    this.server = server
                    this.lastMessageUrl = lastMessage
                }
            )
        }
    }

    suspend fun removeSuggestion(uuid: String) {
        realm.updateInfo<ModelSuggesterList> { list ->
            list.list.removeIf { it.uuid == uuid }
        }
    }
}

internal class ModelSuggesterList : RealmObject {
    var list: RealmList<ModelSuggestion> = realmListOf()
}

internal class ModelSuggestion : RealmObject {
    @PrimaryKey
    var uuid: String = RealmUUID.random().toString()
    var link: String = ""
    var reason: String = ""
    var suggestedBy: String = ""
    var avatar: String? = null
    var channel: String = ""
    var server: String = ""
    var timestamp: Long = System.currentTimeMillis()
    var lastMessageUrl: String? = null
}

private suspend inline fun <reified T : RealmObject> Realm.updateInfo(crossinline block: MutableRealm.(T) -> Unit) {
    query(T::class).first().find()?.also { info ->
        write { findLatest(info)?.let { block(it) } }
    }
}

private inline fun <reified T : RealmObject> Realm.initDbBlocking(crossinline default: () -> T): T {
    val f = query(T::class).first().find()
    return f ?: writeBlocking { copyToRealm(default()) }
}