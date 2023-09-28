package chatgpt

import DiscordBotCompileSettings
import com.kotlindiscord.kord.extensions.builders.ExtensibleBotBuilder

object ChatGPT {
    context (ExtensibleBotBuilder.ExtensionsBuilder)
    fun addToKordExtensions(chatGPTNetwork: ChatGPTNetwork = ChatGPTNetwork(DiscordBotCompileSettings.CHAT_GPT_URL)) {
        add { ChatGPTExtension(chatGPTNetwork) }
    }

    context (ExtensibleBotBuilder.ExtensionsBuilder)
    fun addToKordExtensions(url: String = DiscordBotCompileSettings.CHAT_GPT_URL) {
        add { ChatGPTExtension(ChatGPTNetwork(url)) }
    }
}