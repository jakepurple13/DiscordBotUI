@file:Suppress("INLINE_FROM_HIGHER_PLATFORM")

package stablediffusion.commands

import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.application.slash.converters.ChoiceEnum
import com.kotlindiscord.kord.extensions.commands.application.slash.converters.impl.defaultingEnumChoice
import com.kotlindiscord.kord.extensions.extensions.ephemeralSlashCommand
import com.kotlindiscord.kord.extensions.types.editingPaginator
import com.kotlindiscord.kord.extensions.types.respond
import stablediffusion.StableDiffusionExtension

suspend fun StableDiffusionExtension.sdHelp() {
    ephemeralSlashCommand(arguments = ::SdHelpArgs) {
        name = "sdhelp"
        description = "See what information this stable diffusion instance has access to"

        action {
            respond {
                fun allow(types: SdHelpTypes) = arguments.type == types || arguments.type == SdHelpTypes.All

                suspend fun <T> haveListIfAllowed(
                    types: SdHelpTypes,
                    block: suspend () -> Result<T>?,
                ) = if (allow(types)) block()?.getOrNull() else null

                val loras = haveListIfAllowed(
                    types = SdHelpTypes.Lora,
                    block = stableDiffusionNetwork::stableDiffusionLoras
                )

                val models = haveListIfAllowed(
                    types = SdHelpTypes.Model,
                    block = stableDiffusionNetwork::stableDiffusionModels
                )

                val samplers = haveListIfAllowed(
                    types = SdHelpTypes.Sampler,
                    block = stableDiffusionNetwork::stableDiffusionSamplers
                )

                val styles = haveListIfAllowed(
                    types = SdHelpTypes.Style,
                    block = stableDiffusionNetwork::stableDiffusionStyles
                )

                editingPaginator {
                    keepEmbed = true
                    models?.chunked(10)?.forEach { modelList ->
                        page {
                            title = "Models"
                            description = "Here you can see what the models are and get more information about them"
                            modelList.forEach { field(it.title) }
                        }
                    }
                    loras?.chunked(20)?.forEach { loraList ->
                        page {
                            title = "Loras"
                            description = "Here you can see what the loras are and get more information about them"
                            loraList.forEach { field(it.name, true) { it.alias } }
                        }
                    }
                    samplers?.chunked(10)?.forEach { samplerList ->
                        page {
                            title = "Samplers"
                            description = "Here you can see what the loras are and get more information about them"
                            samplerList.forEach { field(it.name, true) }
                        }
                    }
                    styles?.chunked(15)?.forEach { styleList ->
                        page {
                            title = "Styles"
                            description = "Here you can see what styles are available"
                            styleList.forEach { field(it, true) }
                        }
                    }
                }.send()
            }
        }
    }
}

private class SdHelpArgs : Arguments() {
    val type by defaultingEnumChoice<SdHelpTypes> {
        name = "type"
        description = "Show all help or just specific help"
        typeName = "SdHelp Type"
        defaultValue = SdHelpTypes.All
    }
}

private enum class SdHelpTypes(override val readableName: String) : ChoiceEnum {
    All("All"),
    Model("Models"),
    Lora("Loras"),
    Sampler("Samplers"),
    Style("Styles")
}