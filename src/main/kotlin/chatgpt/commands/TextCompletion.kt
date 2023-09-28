@file:Suppress("INLINE_FROM_HIGHER_PLATFORM")

package chatgpt.commands

import chatgpt.ChatGPTExtension
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.converters.impl.string
import com.kotlindiscord.kord.extensions.components.components
import com.kotlindiscord.kord.extensions.components.ephemeralButton
import com.kotlindiscord.kord.extensions.extensions.ephemeralSlashCommand
import com.kotlindiscord.kord.extensions.extensions.publicSlashCommand
import com.kotlindiscord.kord.extensions.types.edit
import com.kotlindiscord.kord.extensions.types.respond
import dev.kord.common.entity.ButtonStyle
import dev.kord.core.behavior.channel.withTyping
import dev.kord.core.behavior.interaction.response.createEphemeralFollowup
import dev.kord.rest.builder.message.create.embed
import discordbot.Red
import io.ktor.client.request.forms.*
import io.ktor.utils.io.*

internal suspend fun ChatGPTExtension.textCompletion() {
    ephemeralSlashCommand(::TextCompletionGPTArgs) {
        name = "silenttextcompletion"
        description = "Have some text completed!"
        action {
            channel.withTyping {
                chatGPTNetwork.textCompletion(arguments.prompt)
                    .onSuccess {
                        interactionResponse.createEphemeralFollowup {
                            content = it.choices.firstOrNull()?.text
                        }
                    }
                    .onFailure {
                        respond {
                            it.printStackTrace()
                            content = "Error!"
                            embed {
                                title = "Something went wrong"
                                description = it.stackTraceToString()
                                color = Red
                            }
                        }
                    }
            }
        }
    }

    publicSlashCommand(::TextCompletionGPTArgs) {
        name = "textcompletion"
        description = "Have some text completed!"
        action {
            channel.withTyping {
                chatGPTNetwork.textCompletion(arguments.prompt + "\n")
                    .onSuccess {
                        respond {
                            var c = "${arguments.prompt}\n\n${it.choices.firstOrNull()?.text}"
                            addFile("output.txt", ChannelProvider { ByteReadChannel(c.toByteArray()) })
                            content = "Here you go!"
                            components {
                                ephemeralButton {
                                    style = ButtonStyle.Primary
                                    id = "continue"
                                    label = "Continue"
                                    action {
                                        edit {
                                            chatGPTNetwork.textCompletion(content.orEmpty())
                                                .onSuccess {
                                                    it.choices
                                                        .firstOrNull()
                                                        ?.text
                                                        ?.let { text ->
                                                            c += "\n\n$text"
                                                            addFile(
                                                                "output.txt",
                                                                ChannelProvider { ByteReadChannel(text.toByteArray()) }
                                                            )
                                                        }
                                                }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    .onFailure {
                        respond {
                            it.printStackTrace()
                            content = "Error!"
                            embed {
                                title = "Something went wrong"
                                description = it.stackTraceToString()
                                color = Red
                            }
                        }
                    }
            }
        }
    }
}

private class TextCompletionGPTArgs : Arguments() {
    val prompt by string {
        name = "prompt"
        description = "The prompt!"
    }
}