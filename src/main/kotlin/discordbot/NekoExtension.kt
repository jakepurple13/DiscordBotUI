@file:Suppress("INLINE_FROM_HIGHER_PLATFORM")

package discordbot

import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.application.slash.converters.ChoiceEnum
import com.kotlindiscord.kord.extensions.commands.application.slash.converters.impl.enumChoice
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.publicSlashCommand
import com.kotlindiscord.kord.extensions.types.respond
import dev.kord.rest.builder.message.create.embed
import io.ktor.client.request.forms.*

class NekoExtension(
    private val network: Network,
) : Extension() {
    override val name: String = "neko"

    override suspend fun setup() {
        publicSlashCommand(arguments = { NekoArgs() }) {
            name = "neko"
            description = "Get a neko image"

            action {
                respond {
                    when (arguments.nekoType) {
                        NekoType.Random -> network.loadRandomImage()
                        NekoType.Cat -> network.catApi()
                    }
                        .onSuccess { model: NekoImageType ->
                            content = "Here is your neko image!"
                            when (model) {
                                is LocalNekoImage -> {
                                    addFile("output.png", ChannelProvider { model.byteReadChannel })
                                }

                                is NekoImage -> {
                                    embed {
                                        image = model.url
                                        footer {
                                            text = "By: ${model.artist}"
                                        }
                                        color = Blue
                                    }
                                }
                            }
                        }
                        .respondWithError()
                }
            }
        }
    }

    inner class NekoArgs : Arguments() {
        val nekoType by enumChoice<NekoType> {
            name = "neko"
            description = "Get neko!"
            typeName = "Neko"
            choice("Random", NekoType.Random)
            choice("Cat", NekoType.Cat)
        }
    }

    enum class NekoType(override val readableName: String) : ChoiceEnum {
        Random("Random"),
        Cat("Cat")
    }
}
