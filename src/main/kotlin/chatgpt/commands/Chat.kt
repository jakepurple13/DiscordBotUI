@file:Suppress("INLINE_FROM_HIGHER_PLATFORM")

package chatgpt.commands

import chatgpt.ChatGPTExtension
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.converters.impl.string
import com.kotlindiscord.kord.extensions.extensions.ephemeralSlashCommand
import com.kotlindiscord.kord.extensions.extensions.publicSlashCommand
import com.kotlindiscord.kord.extensions.types.respond
import dev.kord.core.behavior.channel.withTyping
import dev.kord.core.behavior.interaction.response.createEphemeralFollowup
import dev.kord.rest.builder.message.create.embed
import discordbot.Red

internal suspend fun ChatGPTExtension.chat() {
    ephemeralSlashCommand(::ChatGPTArgs) {
        name = "silentchatgpt"
        description = "Chat with ChatGPT!"
        action {
            channel.withTyping {
                chatGPTNetwork.chatCompletion(arguments.prompt)
                    .onSuccess {
                        interactionResponse.createEphemeralFollowup {
                            content = it.choices.firstOrNull()?.message?.content
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

    publicSlashCommand(::ChatGPTArgs) {
        name = "chatgpt"
        description = "Chat with ChatGPT!"
        action {
            channel.withTyping {
                chatGPTNetwork.chatCompletion(arguments.prompt)
                    .onSuccess {
                        respond {
                            content = it.choices.firstOrNull()?.message?.content
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

private class ChatGPTArgs : Arguments() {
    val prompt by string {
        name = "prompt"
        description = "The prompt!"
    }
}