@file:Suppress("INLINE_FROM_HIGHER_PLATFORM")

package stablediffusion.commands

import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.converters.impl.attachment
import com.kotlindiscord.kord.extensions.extensions.publicSlashCommand
import com.kotlindiscord.kord.extensions.types.respond
import com.kotlindiscord.kord.extensions.utils.download
import dev.kord.rest.builder.message.create.embed
import discordbot.respondWithError
import io.ktor.client.request.forms.*
import io.ktor.utils.io.*
import stablediffusion.StableDiffusionExtension

suspend fun StableDiffusionExtension.pngInfo() {
    publicSlashCommand(::PngInfoArgs) {
        name = "pnginfo"
        description = "Get stable diffusion information from an image"
        action {
            respond {
                val image = arguments.image.download()
                stableDiffusionNetwork.sdPngInfo(image)
                    .onSuccess {
                        embed {
                            title = "Png Info"
                            description = it.info
                        }
                        addFile(arguments.image.filename, ChannelProvider { ByteReadChannel(image) })
                    }
                    .respondWithError()
            }
        }
    }
}

private class PngInfoArgs : Arguments() {
    val image by attachment {
        name = "image"
        description = "The image to get information about"
    }
}