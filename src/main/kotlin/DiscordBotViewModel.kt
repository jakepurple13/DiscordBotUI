import androidx.compose.runtime.*
import com.kotlindiscord.kord.extensions.ExtensibleBot
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.entity.Guild
import dev.kord.core.entity.channel.GuildChannel
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.event.Event
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.util.*

class DiscordBotViewModel(
    private val botCreation: suspend (token: String?) -> ExtensibleBot,
    private val startUpMessages: suspend (Guild) -> Unit = {},
    private val shutdownMessages: suspend (Guild) -> Unit = {},
) {
    private val viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val botScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    var showBotScreen by mutableStateOf(false)

    var bot: ExtensibleBot? by mutableStateOf(null)
        private set

    private val botToken = DataStore.botToken
    var canStartBot by mutableStateOf(false)
        private set

    val guildList = mutableStateListOf<Guild>()
    var selectedGuild by mutableStateOf<Guild?>(null)

    val channelList = mutableStateListOf<GuildChannel>()
    var selectedChannel by mutableStateOf<GuildChannel?>(null)

    var tokenForBot by mutableStateOf("")

    val eventList = mutableStateListOf<EventType>()

    init {
        botToken
            .onEach { canStartBot = !it.isNullOrEmpty() }
            .launchIn(viewModelScope)

        snapshotFlow { bot }
            .filterNotNull()
            .onEach { guildList.clear() }
            .flatMapLatest { it.kordRef.guilds }
            .onEach { guildList.add(it) }
            .launchIn(viewModelScope)

        snapshotFlow { selectedGuild }
            .onEach { channelList.clear() }
            .filterNotNull()
            .flatMapLatest { it.channels }
            .onEach { channelList.add(it) }
            .launchIn(viewModelScope)

        snapshotFlow { bot }
            .filterNotNull()
            .onEach { eventList.add(EventType.Running(simpleDateTimeFormatter.format(System.currentTimeMillis()))) }
            .flatMapLatest { it.kordRef.events }
            .onEach { eventList.add(EventType.KordEvent(it)) }
            .launchIn(viewModelScope)
    }

    suspend fun startBot() {
        runCatching { bot = botCreation(botToken.firstOrNull()) }
            .onSuccess {
                if (DataStore.sendStartup.flow.firstOrNull() == true) {
                    bot
                        ?.kordRef
                        ?.guilds
                        ?.onEach(startUpMessages)
                        ?.launchIn(botScope)
                }

                showBotScreen = true
                bot?.startAsync()
            }
            .onFailure { it.printStackTrace() }
    }

    suspend fun stopBot() {
        if (DataStore.sendShutdown.flow.firstOrNull() == true) {
            bot
                ?.kordRef
                ?.guilds
                ?.onEach(shutdownMessages)
                ?.lastOrNull()
        }
        botScope.cancel()
        bot?.stop()
        bot = null
    }

    fun setToken() {
        viewModelScope.launch { DataStore.updateToken(tokenForBot) }
        tokenForBot = ""
    }

    fun sendMessage(message: String) {
        viewModelScope.launch {
            (selectedChannel?.asChannelOrNull() as? TextChannel)?.createMessage {
                content = message
            }
        }
    }

    fun selectGuild(guild: Guild?) {
        selectedGuild = guild
        selectedChannel = null
    }

    fun showSavedToken() {
        viewModelScope.launch {
            tokenForBot = botToken.firstOrNull().orEmpty()
        }
    }
}

sealed class EventType {
    data class Running(val timestamp: String) : EventType()
    data class KordEvent(val event: Event) : EventType(), Event by event
}

val simpleDateTimeFormatter = SimpleDateFormat("MM/dd/yy HH:mm", Locale.getDefault())
