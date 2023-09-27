@file:Suppress("INLINE_FROM_HIGHER_PLATFORM")

package discordbot

import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.ephemeralSlashCommand
import com.kotlindiscord.kord.extensions.types.respond
import dev.kord.rest.builder.message.create.actionRow
import dev.kord.rest.builder.message.create.embed

class AboutExtension : Extension() {
    override val name: String = "about"

    override suspend fun setup() {
        ephemeralSlashCommand {
            name = "about"
            description = "Learn a little about the bot"
            action {
                respond {
                    content = "I am NekoBot!"
                    embed {
                        title = "I am NekoBot!"
                        description = """
                            I was made in Kotlin using KordExtensions!
                            
                            I use Ktor for network requests and interacting with a local instance of Stable Diffusion.
                            
                            I also use Compose Multiplatform for some moderation and feedback for a beautiful experience anyone can create!
                        """.trimIndent()

                        footer {
                            text = "NekoBot was created by jakepurple13"
                        }

                        color = Purple
                    }
                    actionRow {
                        linkButton("https://github.com/Kord-Extensions/kord-extensions") { label = "Kord Extensions" }
                        linkButton("https://github.com/JetBrains/compose-multiplatform") {
                            label = "Compose Multiplatform"
                        }
                    }
                }
            }
        }
    }
}