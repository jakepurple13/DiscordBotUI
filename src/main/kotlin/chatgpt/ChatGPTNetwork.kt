package chatgpt

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

private const val CHAT_GPT_URL = "http://localhost:8000/v1/"
private const val ALLOW_LOGGING = false

class ChatGPTNetwork(
    token: String = "sk-",
    private val chatGptUrl: String = CHAT_GPT_URL,
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

    suspend fun chatCompletion(prompt: String, modelId: String = "rwkv") = runCatching {
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
    }

}