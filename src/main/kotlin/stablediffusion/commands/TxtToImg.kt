@file:Suppress("INLINE_FROM_HIGHER_PLATFORM")

package stablediffusion.commands

import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.application.slash.converters.ChoiceEnum
import com.kotlindiscord.kord.extensions.commands.application.slash.converters.impl.defaultingEnumChoice
import com.kotlindiscord.kord.extensions.commands.converters.impl.*
import com.kotlindiscord.kord.extensions.extensions.publicSlashCommand
import com.kotlindiscord.kord.extensions.types.respond
import com.kotlindiscord.kord.extensions.utils.download
import dev.kord.core.behavior.channel.withTyping
import dev.kord.rest.builder.message.create.embed
import discordbot.Red
import io.ktor.client.request.forms.*
import stablediffusion.SDInfo
import stablediffusion.StableDiffusionExtension
import stablediffusion.StableDiffusionResponseInfo
import stablediffusion.writeResponse

suspend fun StableDiffusionExtension.stableDiffusion() {
    publicSlashCommand(::DiffusionArgs) {
        name = "stablediffusion"
        description = "Get an ai generated image"

        action {
            channel.withTyping {
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
                        pose = arguments.pose?.download(),
                        style = arguments.style
                    )
                        .onSuccess { model ->
                            val info = model.info
                            content = "${member?.mention} your image is ready!"
                            info.toInfo().writeResponse()
                            model.imagesAsByteChannel().forEach {
                                addFile("output.png", ChannelProvider { it })
                            }
                        }
                        .onFailure {
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

private fun StableDiffusionResponseInfo.toInfo() = SDInfo(
    prompt = prompt,
    negativePrompt = negativePrompt,
    cfgScale = cfgScale,
    clipSkip = clipSkip,
    steps = steps,
    seed = seed,
    samplerName = samplerName,
    infotexts = infotexts,
    jobTimestamp = jobTimestamp
)

private class DiffusionArgs : Arguments() {
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

    val style by defaultingString {
        name = "style"
        description = "A specific style"
        defaultValue = "base"
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