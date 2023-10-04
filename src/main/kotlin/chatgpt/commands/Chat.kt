@file:Suppress("INLINE_FROM_HIGHER_PLATFORM")

package chatgpt.commands

import chatgpt.ChatGPTExtension
import chatgpt.Message
import chatgpt.Role
import chatgpt.toRole
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.converters.impl.string
import com.kotlindiscord.kord.extensions.extensions.ephemeralSlashCommand
import com.kotlindiscord.kord.extensions.extensions.publicSlashCommand
import com.kotlindiscord.kord.extensions.types.respond
import com.kotlindiscord.kord.extensions.utils.respond
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.channel.withTyping
import dev.kord.core.behavior.interaction.response.createEphemeralFollowup
import dev.kord.rest.builder.message.create.embed
import discordbot.Red
import foldWithFirst
import dev.kord.core.entity.Message as DiscordMessage

internal suspend fun ChatGPTExtension.chat() {
    ephemeralSlashCommand(::ChatGPTArgs) {
        name = "silentchatgpt"
        description = "Chat with ChatGPT!"
        action {
            channel.withTyping {
                chatGPTNetwork.generate(arguments.prompt)
                    .onSuccess {
                        it.chunked(2000).forEach { c ->
                            interactionResponse.createEphemeralFollowup { content = c }
                        }
                        /*interactionResponse.createEphemeralFollowup {
                            content = it//it.choices.firstOrNull()?.message?.content
                        }*/
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
        val userMap: MutableMap<Snowflake, MutableList<Message>> = mutableMapOf()
        action {
            val snowflake = channel.asChannelOrNull()?.id ?: user.id //TODO: Check that this is not null for dms
            userMap.putIfAbsent(snowflake, mutableListOf())
            userMap[snowflake]?.add(
                Message(
                    role = Role.User,
                    content = arguments.prompt
                )
            )
            channel.withTyping {
                chatGPTNetwork.chatCompletion(userMap[snowflake].orEmpty())
                    .onSuccess { response ->
                        response.choices.forEach {
                            userMap[snowflake]?.add(
                                Message(
                                    role = it["role"].toRole(),
                                    content = it["content"].orEmpty()
                                )
                            )

                            /*it["content"]?.chunked(2000)?.fold(
                                respond { content = "Here is your message!" }.message
                            ) { r, s -> r.respond(s) }*/
                            it["content"]
                                ?.chunked(2000)
                                ?.foldWithFirst(
                                    initial = { s -> respond { content = s }.message },
                                ) { r: DiscordMessage, s: String -> r.respond(s) }
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