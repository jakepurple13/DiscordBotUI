@file:Suppress("INLINE_FROM_HIGHER_PLATFORM")

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.kotlindiscord.kord.extensions.ExtensibleBot
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.edit
import dev.kord.rest.builder.message.create.embed
import dev.kord.rest.builder.message.modify.actionRow
import dev.kord.rest.builder.message.modify.embed
import discordbot.*
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import stablediffusion.StableDiffusion
import stablediffusion.StableDiffusionNetwork
import stablediffusionui.ModelSuggestionUI
import stablediffusionui.StableDiffusionUI

fun main() {
    when (DiscordBotCompileSettings.running) {
        RunType.DiscordBot -> DiscordBot()
        RunType.Testing -> {}
    }
}

private fun DiscordBot() {
    val stableDiffusionNetwork = StableDiffusionNetwork()
    var showStableDiffusionWindow by mutableStateOf(false)
    var showSuggestions by mutableStateOf(false)
    DiscordBotUI(
        botCreation = { token ->
            ExtensibleBot(token!!) {
                chatCommands { enabled = true }

                extensions {
                    val network = Network()
                    add { NekoExtension(network) }
                    add { MarvelSnapExtension(network) }
                    StableDiffusion.addToKordExtensions(stableDiffusionNetwork)
                    help {
                        pingInReply = true
                        color { Purple }
                    }
                }

                hooks { kordShutdownHook = true }

                errorResponse { message, type ->
                    type.error.printStackTrace()
                    println(message)
                }
            }
        },
        startUpMessages = { g ->
            g.systemChannel
                ?.createMessage {
                    suppressNotifications = true
                    content = "NekoBot is booting up...Please wait..."
                }
                ?.also { delay(500) }
                ?.edit {
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
                        linkButton("https://huchenlei.github.io/sd-webui-openpose-editor/") { label = "Poses" }
                    }
                }
        },
        shutdownMessages = { g ->
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
        },
        mainMenuOptions = {
            Menu("Stable Diffusion") {
                CheckboxItem(
                    "Show Stable Diffusion Window",
                    checked = showStableDiffusionWindow,
                    onCheckedChange = { showStableDiffusionWindow = it }
                )

                CheckboxItem(
                    text = "Show Suggestions",
                    checked = showSuggestions,
                    onCheckedChange = { showSuggestions = it }
                )
            }
        }
    ) {
        if (showStableDiffusionWindow) {
            WindowWithBar(
                onCloseRequest = { showStableDiffusionWindow = false },
                windowTitle = "Stable Diffusion",
            ) {
                StableDiffusionUI(stableDiffusionNetwork)
            }
        }

        if (showSuggestions) {
            WindowWithBar(
                onCloseRequest = { showSuggestions = false },
                windowTitle = "Model Suggestions"
            ) {
                ModelSuggestionUI()
            }
        }
    }
}