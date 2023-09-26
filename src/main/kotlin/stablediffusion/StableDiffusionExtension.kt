@file:Suppress("INLINE_FROM_HIGHER_PLATFORM")

package stablediffusion

import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.ephemeralSlashCommand
import com.kotlindiscord.kord.extensions.types.respond
import dev.kord.rest.builder.message.create.actionRow
import dev.kord.rest.builder.message.create.embed
import discordbot.respondWithError
import kotlinx.datetime.Clock
import stablediffusion.commands.img2Img
import stablediffusion.commands.pngInfo
import stablediffusion.commands.sdHelp
import stablediffusion.commands.stableDiffusion
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.time.Duration.Companion.seconds

class StableDiffusionExtension(
    val stableDiffusionNetwork: StableDiffusionNetwork = StableDiffusionNetwork(),
) : Extension() {
    val dateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("m:ss", Locale.getDefault())
    val dateTimeParser: DateTimeFormatter = DateTimeFormatter
        .ofPattern("yyyyMMddHHmmss", Locale.getDefault())
        .withZone(ZoneId.systemDefault())

    override val name: String = "stablediffusion"

    override suspend fun setup() {
        stableDiffusion()
        sdProgress()
        sdHelp()
        sdLinks()
        pngInfo()
        img2Img()
    }

    private suspend fun sdProgress() {
        ephemeralSlashCommand {
            name = "sdprogress"
            description = "Get information about the current Stable Diffusion progress"

            action {
                respond {
                    stableDiffusionNetwork.stableDiffusionProgress()
                        .onSuccess { progress ->
                            embed {
                                title = "Current Stable Diffusion Progress"
                                field("Progress") { progress.progress.toString() }
                                field("ETA")
                                timestamp = Clock.System.now() + progress.etaRelative.seconds
                            }
                        }
                        .respondWithError()
                }
            }
        }
    }

    private suspend fun sdLinks() {
        ephemeralSlashCommand {
            name = "sdlinks"
            description = "Display some links that might be very useful related to Stable Diffusion"
            action {
                respond {
                    content = "Here are your links!"
                    actionRow {
                        linkButton("https://huggingface.co") { label = "Stable Diffusion Models" }
                        linkButton("https://civitai.com/") { label = "Models and Loras" }
                        linkButton("https://huchenlei.github.io/sd-webui-openpose-editor/") { label = "Poses" }
                    }
                }
            }
        }
    }
}
