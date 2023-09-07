@file:Suppress("INLINE_FROM_HIGHER_PLATFORM")

package stablediffusion

import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.application.slash.converters.ChoiceEnum
import com.kotlindiscord.kord.extensions.commands.application.slash.converters.impl.defaultingEnumChoice
import com.kotlindiscord.kord.extensions.commands.converters.impl.*
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.ephemeralSlashCommand
import com.kotlindiscord.kord.extensions.extensions.publicSlashCommand
import com.kotlindiscord.kord.extensions.types.editingPaginator
import com.kotlindiscord.kord.extensions.types.respond
import com.kotlindiscord.kord.extensions.utils.download
import dev.kord.rest.builder.message.create.actionRow
import dev.kord.rest.builder.message.create.embed
import discordbot.Emerald
import discordbot.Red
import discordbot.respondWithError
import io.ktor.client.request.forms.*
import kotlinx.datetime.Clock
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.time.Duration.Companion.seconds

class StableDiffusionExtension(
    private val stableDiffusionNetwork: StableDiffusionNetwork = StableDiffusionNetwork(),
) : Extension() {
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("m:ss", Locale.getDefault())
    private val dateTimeParser = DateTimeFormatter
        .ofPattern("yyyyMMddHHmmss", Locale.getDefault())
        .withZone(ZoneId.systemDefault())

    override val name: String = "stablediffusion"

    override suspend fun setup() {
        stableDiffusion()
        sdProgress()
        sdHelp()
        sdLinks()
    }

    private suspend fun stableDiffusion() {
        publicSlashCommand(::DiffusionArgs) {
            name = "stablediffusion"
            description = "Get an ai generated image"

            action {
                channel.type()
                respond {
                        stableDiffusionNetwork.stableDiffusion(
                            prompt = arguments.prompt,
                            modelName = arguments.model,
                            negativePrompt = arguments.negativePrompt.orEmpty(),
                            seed = arguments.seed,
                            cfgScale = arguments.cfgScale,
                            sampler = arguments.sampler,
                            steps = arguments.steps,
                            clipSkip = arguments.clipSkip,
                            width = arguments.imageSize.width,
                            height = arguments.imageSize.height,
                            pose = arguments.pose?.download()
                        )
                            .onSuccess { model ->
                                val info = model.info
                                content = "${member?.mention} your image is ready!"
                                embed {
                                    title = "Here is your neko image!"

                                    field("Prompt") { info.prompt }
                                    info.negativePrompt?.let { field("Negative Prompt") { it } }
                                    field("Cfg Scale") { info.cfgScale.toString() }
                                    field("Clip Skip") { info.clipSkip.toString() }
                                    field("Steps") { info.steps.toString() }
                                    field("Seed") { info.seed.toString() }
                                    field("Sampling Method") { info.samplerName }
                                    info.infotexts.firstOrNull()
                                        ?.let { Regex("Model: (.*?),").find(it)?.groupValues?.getOrNull(1) }
                                        ?.let { field("Model") { it } }

                                    footer {
                                        val t = dateTimeParser.parse(info.jobTimestamp)
                                        val z = ZonedDateTime.of(LocalDateTime.from(t), ZoneId.systemDefault())
                                        val duration = System.currentTimeMillis() - Instant.from(z).toEpochMilli()
                                        val time = dateTimeFormatter.format(
                                            Instant.ofEpochMilli(duration).atZone(ZoneOffset.systemDefault())
                                        )

                                        text = "Generated by Stable Diffusion - Took $time"
                                    }

                                    color = Emerald
                                }

                                model.imagesAsByteChannel().forEach {
                                    addFile("output.png", ChannelProvider { it })
                                }
                            }
                            .onFailure {
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

    private suspend fun sdHelp() {
        ephemeralSlashCommand {
            name = "sdhelp"
            description = "See what information this stable diffusion instance has access to"

            action {
                respond {
                    val loras = stableDiffusionNetwork.stableDiffusionLoras()
                        .getOrNull()
                        .orEmpty()
                    val models = stableDiffusionNetwork.stableDiffusionModels()
                        .getOrNull()
                        .orEmpty()
                    val samplers = stableDiffusionNetwork.stableDiffusionSamplers()
                        .getOrNull()
                        .orEmpty()
                    editingPaginator {
                        keepEmbed = true
                        page {
                            title = "Models"
                            description = "Here you can see what the models are and get more information about them"
                            models.forEach { field(it.title) }
                        }
                        page {
                            title = "Loras"
                            description = "Here you can see what the loras are and get more information about them"
                            loras.forEach { field(it.name, true) { it.alias } }
                        }
                        page {
                            title = "Samplers"
                            description = "Here you can see what the loras are and get more information about them"
                            samplers.forEach { field(it.name) }
                        }
                    }.send()
                }
            }
        }
    }

    class DiffusionArgs : Arguments() {
        val prompt by string {
            name = "prompt"
            description = "Give me a prompt!"
        }

        val model by optionalString {
            name = "modeltype"
            description = "If you don't want to use the default model, you can change it!"
        }

        val negativePrompt by optionalString {
            name = "negativeprompt"
            description = "If you want to include things NOT to add"
        }

        val seed by optionalLong {
            name = "seed"
            description = "A seed to generate similar images"
        }

        val sampler by optionalString {
            name = "sampler"
            description = "Which sampling method to use"
        }

        val cfgScale by defaultingDecimal {
            name = "cfgscale"
            description = "Classifier Free Guidance Scale - how strongly the image should conform to prompt"
            defaultValue = 7.0
            minValue = 1.0
            maxValue = 30.0
        }

        val steps by defaultingInt {
            name = "steps"
            description = "How many times to improve the generated image iteratively"
            defaultValue = 20
            minValue = 1
            maxValue = 150
        }

        val clipSkip by defaultingLong {
            name = "clipskip"
            description = "Controls how early the processing of the prompt should be stopped"
            defaultValue = 1
            minValue = 1
            maxValue = 12
        }

        val imageSize by defaultingEnumChoice<StableDiffusionSize> {
            name = "imagesize"
            description = "The resolution! If using an XL model, it's recommended to use 1024x1024"
            typeName = "Image Size"
            defaultValue = StableDiffusionSize.FiveTwelveByFiveTwelve
        }

        val pose by optionalAttachment {
            name = "pose"
            description = "Add if you want to use a pose using ControlNet and OpenPose"
        }
    }
}

enum class StableDiffusionSize(val height: Long, val width: Long) : ChoiceEnum {
    FiveTwelveByFiveTwelve(512, 512) {
        override val readableName: String get() = "512x512"
    },
    Ten24ByTen24(1024, 1024) {
        override val readableName: String get() = "1024x1024"
    }
}
