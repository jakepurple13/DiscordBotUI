package chatgpt

import com.kotlindiscord.kord.extensions.builders.ExtensibleBotBuilder

object ChatGPT {
    context (ExtensibleBotBuilder.ExtensionsBuilder)
    fun addToKordExtensions(chatGPTNetwork: ChatGPTNetwork = ChatGPTNetwork()) {
        add { ChatGPTExtension(chatGPTNetwork) }
    }
}