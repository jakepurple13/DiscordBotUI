package discordbot

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PixrayModel(
    val version: String,
    val input: Prompts
)

@Serializable
data class Prompts(val prompts: String)

@Serializable
data class PixrayResponse(
    val id: String,
    val version: String,
    val input: Input,
    val logs: String,
    val status: String,
    @SerialName("created_at")
    val createdAt: String,
    val urls: Urls,
)

@Serializable
data class Input(
    val prompts: String,
)

@Serializable
data class Urls(
    val cancel: String,
    val get: String,
)

@Serializable
data class PixrayOutput(
    val id: String,
    val input: Input,
    val output: List<String>,
    val status: String,
)

@Serializable
data class CatboyModel(
    val url: String,
    val artist: String,
    @SerialName("artist_url")
    val artistUrl: String,
    @SerialName("source_url")
    val sourceUrl: String,
    val error: String,
)

@Serializable
data class CatApiModel(
    val id: String,
    val url: String,
    val width: Long,
    val height: Long,
)