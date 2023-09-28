package stablediffusion

import DiscordBotCompileSettings
import com.kotlindiscord.kord.extensions.builders.ExtensibleBotBuilder

object StableDiffusion {
    context (ExtensibleBotBuilder.ExtensionsBuilder)
    fun addToKordExtensions(
        stableDiffusionNetwork: StableDiffusionNetwork = StableDiffusionNetwork(
            DiscordBotCompileSettings.STABLE_DIFFUSION_URL
        ),
    ) {
        add { StableDiffusionExtension(stableDiffusionNetwork) }
    }

    context (ExtensibleBotBuilder.ExtensionsBuilder)
    fun addToKordExtensions(url: String = DiscordBotCompileSettings.STABLE_DIFFUSION_URL) {
        add { StableDiffusionExtension(StableDiffusionNetwork(url)) }
    }
}