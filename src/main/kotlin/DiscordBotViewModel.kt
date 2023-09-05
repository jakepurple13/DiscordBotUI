import androidx.compose.runtime.*
import com.kotlindiscord.kord.extensions.ExtensibleBot
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.entity.Guild
import dev.kord.core.entity.channel.GuildChannel
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.event.Event
import discordbot.MarvelSnapExtension
import discordbot.NekoExtension
import discordbot.Network
import discordbot.Purple
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import stablediffusion.StableDiffusion
import stablediffusion.StableDiffusionNetwork
import java.text.SimpleDateFormat
import java.util.*

class DiscordBotViewModel {

    private val viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

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
            .onEach { canStartBot = it.isNotEmpty() }
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
        bot = ExtensibleBot(botToken.first()) {
            chatCommands {
                enabled = true
            }

            extensions {
                add { NekoExtension(Network(), StableDiffusionNetwork()) }
                add { MarvelSnapExtension(Network()) }
                StableDiffusion.addToKordExtensions()
                help {
                    pingInReply = true
                    color { Purple }
                }
            }

            hooks {
                kordShutdownHook = true
            }

            errorResponse { message, type ->
                type.error.printStackTrace()
                println(message)
            }
        }

        showBotScreen = true
        bot?.startAsync()
    }

    suspend fun stopBot() {
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
            tokenForBot = botToken.first()
        }
    }
}

sealed class EventType {
    data class Running(val timestamp: String) : EventType()
    data class KordEvent(val event: Event) : EventType(), Event by event
}

val simpleDateTimeFormatter = SimpleDateFormat("MM/dd/yy HH:mm", Locale.getDefault())
