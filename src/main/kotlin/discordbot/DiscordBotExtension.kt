package discordbot

import com.kotlindiscord.kord.extensions.ExtensibleBot
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.edit
import dev.kord.rest.builder.message.create.embed
import dev.kord.rest.builder.message.modify.actionRow
import dev.kord.rest.builder.message.modify.embed
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.lastOrNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.datetime.Clock
import stablediffusion.StableDiffusion
import stablediffusion.StableDiffusionNetwork

private const val SHOW_STARTUP_SHUTDOWN_MESSAGES = false

suspend fun DiscordBotExtension(
    token: String,
    network: Network,
    stableDiffusionNetwork: StableDiffusionNetwork
) {
    val bot = ExtensibleBot(token) {
        chatCommands {
            enabled = true
        }

        extensions {
            //add { NekoExtension(network, stableDiffusionNetwork) }
            //add { MarvelSnapExtension(network) }
            StableDiffusion.addToKordExtensions(stableDiffusionNetwork)
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

    if (SHOW_STARTUP_SHUTDOWN_MESSAGES) {
        bot.kordRef.guilds
            .mapNotNull { g ->
                g.systemChannel
                    ?.createMessage {
                        suppressNotifications = true
                        content = "NekoBot is booting up...Please wait..."
                    }
            }
            .onEach {
                it.edit {
                    content = "NekoBot is Online!"
                    embed {
                        title = "NekoBot is Online!"
                        description = """
                    Meow is back online!
                    
                    To get more Stable Diffusion models or loras to suggest, press on the buttons below!
                    To use Stable Diffusion, type `/stablediffusion`
                    To get a random neko image, type `/neko random`
                    To get a random cat image, type `/neko cat`
                    To view Marvel Snap cards, type `/snapcards`
                """.trimIndent()
                        color = Emerald
                    }
                    actionRow {
                        linkButton("https://huggingface.co") { label = "Stable Diffusion Models" }
                        linkButton("https://civitai.com/") { label = "Models and Loras" }
                    }
                }
            }
            .launchIn(CoroutineScope(Dispatchers.IO + Job()))
    }

    Runtime.getRuntime().addShutdownHook(
        Thread {
            runBlocking {
                if (SHOW_STARTUP_SHUTDOWN_MESSAGES) {
                    bot.kordRef.guilds
                        .onEach { g ->
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
                }
                bot.stop()
            }
        }
    )

    bot.start()

    withContext(Dispatchers.IO) { Thread.currentThread().join() }
}