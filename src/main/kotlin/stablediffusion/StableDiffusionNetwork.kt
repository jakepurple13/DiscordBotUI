package stablediffusion

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import java.util.*

private const val ALLOW_LOGGING = false

class StableDiffusionNetwork(
    private val stableDiffusionUrl: String,
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
    },
) {
    suspend fun stableDiffusionLoras() = runCatching {
        client.get("$stableDiffusionUrl/loras") { setup() }
            .bodyAsText()
            .let { json.decodeFromString<List<StableDiffusionLora>>(it) }
    }
        .onFailure { it.printStackTrace() }

    suspend fun stableDiffusionSamplers() = runCatching {
        client.get("$stableDiffusionUrl/samplers") { setup() }
            .bodyAsText()
            .let { json.decodeFromString<List<StableDiffusionSamplers>>(it) }
    }
        .onFailure { it.printStackTrace() }

    suspend fun stableDiffusionModels() = runCatching {
        client.get("$stableDiffusionUrl/sd-models") {
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
        }
            .bodyAsText()
            .let { json.decodeFromString<List<StableDiffusionModel>>(it) }
    }
        .onFailure { it.printStackTrace() }

    suspend fun stableDiffusionProgress() = runCatching {
        client.get("$stableDiffusionUrl/progress") {
            setup()
            timeout {
                requestTimeoutMillis = Long.MAX_VALUE
                connectTimeoutMillis = Long.MAX_VALUE
            }
        }
            .bodyAsText()
            .let { json.decodeFromString<StableDiffusionProgress>(it) }
    }
        .onFailure { it.printStackTrace() }

    suspend fun sdPngInfo(
        image: ByteArray,
    ) = runCatching {
        @Serializable
        class SdPngInfo(val image: String)

        client.post("$stableDiffusionUrl/png-info") {
            setup()
            setBody(SdPngInfo(Base64.getEncoder().encodeToString(image)))
            timeout {
                requestTimeoutMillis = Long.MAX_VALUE
                connectTimeoutMillis = Long.MAX_VALUE
            }
        }
            .bodyAsText()
            .let { json.decodeFromString<PngInfo>(it) }
    }

    private fun HttpRequestBuilder.setup() {
        contentType(ContentType.Application.Json)
        accept(ContentType.Application.Json)
    }

    suspend fun stableDiffusionStyles() = runCatching {
        client.get("$stableDiffusionUrl/script-info") {
            setup()
        }
            .bodyAsText()
            .let { json.decodeFromString<List<AlwaysOnScriptInfo>>(it) }
            .first { it.name == "style selector for sdxl 1.0" }
            .args
            .find { it.label == "Style" }
            ?.choices
            .orEmpty()
    }

    suspend fun stableDiffusion(
        prompt: String,
        modelName: String? = null,
        cfgScale: Double = 7.0,
        steps: Int = 20,
        negativePrompt: String = "",
        sampler: String? = null,
        seed: Long? = null,
        clipSkip: Long = 1,
        width: Long = 512,
        height: Long = 512,
        pose: ByteArray? = null,
        style: String = "base",
    ) = runCatching {
        @Serializable
        class Args(val args: List<@Contextual Any>)

        client.post("$stableDiffusionUrl/txt2img") {
            setup()
            setBody(
                StableDiffusionBody(
                    prompt = prompt,
                    negativePrompt = negativePrompt,
                    cfgScale = cfgScale,
                    steps = steps,
                    samplerIndex = sampler ?: "Euler a",
                    seed = seed ?: -1,
                    overrideOptions = modelName?.let {
                        OverriddenOptions(
                            sdModelCheckpoint = it,
                            clipSkip = clipSkip
                        )
                    },
                    width = width,
                    height = height,
                    alwaysOnScripts = mapOfNotNull(
                        pose?.let { "controlnet" to createControlNets(Base64.getEncoder().encodeToString(it)) },
                        "style selector for sdxl 1.0" to Args(listOf(true, false, false, false, style))
                    ),
                )
            )
            timeout {
                requestTimeoutMillis = Long.MAX_VALUE
                connectTimeoutMillis = Long.MAX_VALUE
            }
        }
            .bodyAsText()
            .let { json.decodeFromString<StableDiffusionResponse>(it) }
            .let {
                StableDiffusionInfo(
                    images = it.images,
                    parameters = it.parameters,
                    info = json.decodeFromString<StableDiffusionResponseInfo>(it.info)
                )
            }
    }

    suspend fun img2Img(
        prompt: String,
        modelName: String? = null,
        cfgScale: Double = 7.0,
        steps: Int = 20,
        negativePrompt: String = "",
        sampler: String? = null,
        seed: Long? = null,
        clipSkip: Long = 1,
        width: Long = 512,
        height: Long = 512,
        initImg: ByteArray,
    ) = runCatching {
        client.post("$stableDiffusionUrl/img2img") {
            setup()
            setBody(
                StableDiffusionBodyImg2Img(
                    prompt = prompt,
                    negativePrompt = negativePrompt,
                    cfgScale = cfgScale,
                    steps = steps,
                    samplerIndex = sampler ?: "Euler a",
                    seed = seed ?: -1,
                    overrideOptions = modelName?.let {
                        OverriddenOptions(
                            sdModelCheckpoint = it,
                            clipSkip = clipSkip
                        )
                    },
                    width = width,
                    height = height,
                    initImages = listOf(Base64.getEncoder().encodeToString(initImg))
                )
            )
            timeout {
                requestTimeoutMillis = Long.MAX_VALUE
                connectTimeoutMillis = Long.MAX_VALUE
            }
        }
            .bodyAsText()
            .let { json.decodeFromString<StableDiffusionImg2ImgResponse>(it) }
            .let {
                StableDiffusionImg2ImgInfo(
                    images = it.images,
                    parameters = it.parameters,
                    info = json.decodeFromString<StableDiffusionImg2ImgResponseInfo>(it.info)
                )
            }
    }
}