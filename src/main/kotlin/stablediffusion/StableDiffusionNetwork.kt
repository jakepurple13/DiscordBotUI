package stablediffusion

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.*

private const val STABLE_DIFFUSION_URL = "http://127.0.0.1:7860/sdapi/v1"

class StableDiffusionNetwork(
    private val stableDiffusionUrl: String = STABLE_DIFFUSION_URL,
    private val json: Json = Json {
        isLenient = true
        prettyPrint = true
        ignoreUnknownKeys = true
        coerceInputValues = true
    },
    private val client: HttpClient = HttpClient {
        install(ContentNegotiation) { json(json) }
        install(HttpTimeout)
    },
) {
    suspend fun stableDiffusionLoras() = runCatching {
        client.get("$stableDiffusionUrl/loras") {
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
        }
            .bodyAsText()
            .let { json.decodeFromString<List<StableDiffusionLora>>(it) }
    }
        .onFailure { it.printStackTrace() }

    suspend fun stableDiffusionSamplers() = runCatching {
        client.get("$stableDiffusionUrl/samplers") {
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
        }
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
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
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
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            setBody(SdPngInfo(Base64.getEncoder().encodeToString(image)))
            timeout {
                requestTimeoutMillis = Long.MAX_VALUE
                connectTimeoutMillis = Long.MAX_VALUE
            }
        }
            .bodyAsText()
            .let { json.decodeFromString<PngInfo>(it) }
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
    ) = runCatching {
        client.post("$stableDiffusionUrl/txt2img") {
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            setBody(
                when {
                    pose != null -> {
                        StableDiffusionBodyControlNet(
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
                            alwaysOnScripts = createControlNets(Base64.getEncoder().encodeToString(pose))
                        )
                    }

                    else -> {
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
                            height = height
                        )
                    }
                }
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
}