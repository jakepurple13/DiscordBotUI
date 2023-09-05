import androidx.compose.runtime.*
import com.kotlindiscord.kord.extensions.ExtensibleBot
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.entity.Guild
import dev.kord.core.entity.channel.GuildChannel
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.event.Event
import dev.kord.rest.builder.message.create.embed
import discordbot.Purple
import discordbot.Red
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import moe.tlaster.precompose.viewmodel.ViewModel
import moe.tlaster.precompose.viewmodel.viewModelScope
import stablediffusion.StableDiffusion

class DiscordBotViewModel : ViewModel() {

    var bot: ExtensibleBot? by mutableStateOf(null)
        private set

    val botToken = DataStore.botToken
    
    val guildList = mutableStateListOf<Guild>()
    var selectedGuild by mutableStateOf<Guild?>(null)

    val channelList = mutableStateListOf<GuildChannel>()
    var selectedChannel by mutableStateOf<GuildChannel?>(null)

    var tokenForBot by mutableStateOf("")

    val eventList = mutableStateListOf<Event>()

    init {
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
            .onEach { eventList.clear() }
            .flatMapLatest { it.kordRef.events }
            .onEach { eventList.add(it) }
            .launchIn(viewModelScope)
    }

    suspend fun startBot() {
        bot = ExtensibleBot(botToken.first()) {
            chatCommands {
                enabled = true
            }

            extensions {
                //add { MarvelSnapExtension(network) }
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

    override fun onCleared() {
        super.onCleared()
        Runtime.getRuntime().addShutdownHook(
            Thread {
                runBlocking {
                    /*if (SHOW_STARTUP_SHUTDOWN_MESSAGES) {
                        bot?.kordRef.guilds
                            ?.onEach { g ->
                                g.systemChannel?.createMessage {
                                    suppressNotifications = true
                                    embed {
                                        title = "Shutting Down for maintenance and updates..."
                                        timestamp = Clock.System.now()
                                        description = "Please wait while I go through some maintenance."
                                        thumbnail {
                                            url = "https://media.tenor.com/YTPLqiB6gLsAAAAC/sowwy-sorry.gif"
                                        }
                                        color = Red
                                    }
                                }
                            }
                            .lastOrNull()
                    }*/
                    bot?.stop()
                }
            }
        )
    }
}