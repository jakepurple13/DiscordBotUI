@file:Suppress("INLINE_FROM_HIGHER_PLATFORM")

package chatgpt

import chatgpt.commands.chat
import chatgpt.commands.textCompletion
import com.kotlindiscord.kord.extensions.extensions.Extension

internal class ChatGPTExtension(
    val chatGPTNetwork: ChatGPTNetwork = ChatGPTNetwork(),
) : Extension() {
    override val name: String get() = "chatgpt"

    override suspend fun setup() {
        chat()
        textCompletion()
    }
}