package chatgpt

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class ChatCompletionRequest(
    val messages: List<ChatMessage>,
    val model: String,
    val stream: Boolean = false,
    val presystem: Boolean = true,
    @SerialName("max_tokens")
    val maxTokens: Int = 1000,
    val temperature: Double = 1.2,
    @SerialName("top_p")
    val topP: Double = 0.5,
    @SerialName("presence_penalty")
    val presencePenalty: Double = 0.4,
    @SerialName("frequency_penalty")
    val frequencyPenalty: Double = 0.4,
)

@Serializable
internal data class ChatMessage(
    val role: String,
    val content: String,
    val raw: Boolean = false,
)

@Serializable
data class ChatCompletionResponse(
    @SerialName("object")
    val objectField: String,
    val model: String,
    val usage: Usage,
    val choices: List<Choice>,
)

@Serializable
data class Usage(
    @SerialName("prompt_tokens")
    val promptTokens: Long,
    @SerialName("completion_tokens")
    val completionTokens: Long,
    @SerialName("total_tokens")
    val totalTokens: Long,
)

@Serializable
data class Choice(
    val message: Message,
    val index: Long,
    @SerialName("finish_reason")
    val finishReason: String,
)

@Serializable
data class Message(
    val role: Role,
    val content: String,
)

@Serializable
enum class Role {
    @SerialName("user")
    User,

    @SerialName("assistant")
    Assistant
}

@Serializable
internal data class TextCompletionRequest(
    val prompt: String,
    val model: String,
    val stream: Boolean = false,
    @SerialName("max_tokens")
    val maxTokens: Int = 1000,
    val temperature: Double = 1.2,
    @SerialName("top_p")
    val topP: Double = 0.5,
    @SerialName("presence_penalty")
    val presencePenalty: Double = 0.4,
    @SerialName("frequency_penalty")
    val frequencyPenalty: Double = 0.4,
)

@Serializable
data class TextCompletionResponse(
    @SerialName("object")
    val objectField: String,
    val model: String,
    val usage: Usage,
    val choices: List<TextChoice>,
)

@Serializable
data class TextChoice(
    val text: String,
    val index: Long,
    @SerialName("finish_reason")
    val finishReason: String,
)