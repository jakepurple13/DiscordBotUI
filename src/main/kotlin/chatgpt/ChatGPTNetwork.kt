package chatgpt

import DiscordBotCompileSettings
import com.hexadevlabs.gpt4all.LLModel
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import stablediffusion.DynamicLookupSerializer
import java.nio.file.Path
import kotlin.time.Duration.Companion.minutes

private const val ALLOW_LOGGING = false

class ChatGPTNetwork(
    private val chatGptUrl: String,
    modelPath: String = "llama-2-7b-chat.ggmlv3.q8_0.bin",
    token: String = DiscordBotCompileSettings.CHAT_GPT_KEY,
    private val json: Json = Json {
        isLenient = true
        prettyPrint = true
        ignoreUnknownKeys = true
        coerceInputValues = true
        serializersModule = SerializersModule {
            contextual(Any::class, DynamicLookupSerializer())
        }
    },
    private val client: HttpClient = HttpClient {
        install(ContentNegotiation) { json(json) }
        install(HttpTimeout)
        if (ALLOW_LOGGING) {
            install(Logging) {
                logger = Logger.DEFAULT
                level = LogLevel.ALL
            }
        }
        install(Auth) {
            bearer {
                loadTokens { BearerTokens(token, "") }
            }
        }
        defaultRequest {
            url(chatGptUrl)
        }

        expectSuccess = true
    },
) {
    private val llm: LLModel by lazy { LLModel(Path.of(modelPath)) }
    /*suspend fun chatCompletion(prompt: String, modelId: String = "rwkv") = runCatching {
        client.post("chat/completions") {
            setBody(
                ChatCompletionRequest(
                    messages = listOf(
                        ChatMessage(
                            role = "user",
                            content = prompt
                        )
                    ),
                    model = modelId
                )
            )
            contentType(ContentType.Application.Json)
        }
            .body<ChatCompletionResponse>()
    }*/

    suspend fun generate(prompt: String) = runCatching {
        llm.generate(
            prompt,
            LLModel.config()
                .withNPredict(4096)
                .build(),
            false
        )
    }

    suspend fun chatCompletion(messages: List<Message>) = runCatching {
        llm.chatCompletion(
            messages.map {
                mapOf(
                    "role" to it.role.name.lowercase(),
                    "content" to it.content
                )
            },
            LLModel.config()
                .withNPredict(4096)
                .build(),
        )
    }

    suspend fun textCompletion(prompt: String, modelId: String = "rwkv") = runCatching {
        client.post("completions") {
            setBody(
                TextCompletionRequest(
                    prompt = prompt,
                    model = modelId
                )
            )
            contentType(ContentType.Application.Json)
            timeout {
                requestTimeoutMillis = 5.minutes.inWholeMilliseconds
                connectTimeoutMillis = 5.minutes.inWholeMilliseconds
            }
        }
            .body<TextCompletionResponse>()
    }
}