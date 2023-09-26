@file:Suppress("INLINE_FROM_HIGHER_PLATFORM")

package stablediffusion.commands

import com.kotlindiscord.kord.extensions.extensions.ephemeralSlashCommand
import com.kotlindiscord.kord.extensions.types.editingPaginator
import com.kotlindiscord.kord.extensions.types.respond
import stablediffusion.StableDiffusionExtension

suspend fun StableDiffusionExtension.sdHelp() {
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
                    models.chunked(10).forEach { modelList ->
                        page {
                            title = "Models"
                            description = "Here you can see what the models are and get more information about them"
                            modelList.forEach { field(it.title) }
                        }
                    }
                    loras.chunked(20).forEach { loraList ->
                        page {
                            title = "Loras"
                            description = "Here you can see what the loras are and get more information about them"
                            loraList.forEach { field(it.name, true) { it.alias } }
                        }
                    }
                    samplers.chunked(10).forEach { samplerList ->
                        page {
                            title = "Samplers"
                            description = "Here you can see what the loras are and get more information about them"
                            samplerList.forEach { field(it.name, true) }
                        }
                    }
                }.send()
            }
        }
    }
}