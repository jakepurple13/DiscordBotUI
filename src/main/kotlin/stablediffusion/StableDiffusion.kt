package stablediffusion

import com.kotlindiscord.kord.extensions.builders.ExtensibleBotBuilder

object StableDiffusion {
    context (ExtensibleBotBuilder.ExtensionsBuilder)
    fun addToKordExtensions(stableDiffusionNetwork: StableDiffusionNetwork = StableDiffusionNetwork()) {
        add { StableDiffusionExtension(stableDiffusionNetwork) }
    }
}