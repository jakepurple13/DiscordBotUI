package discordbot

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json


class Network(
    private val catApiToken: String = "",
    private val replicateToken: String = ""
) {
    private val json = Json {
        isLenient = true
        prettyPrint = true
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val client by lazy {
        HttpClient {
            install(ContentNegotiation) { json(json) }
            install(HttpTimeout)
        }
    }

    suspend fun loadRandomImage() = runCatching {
        client.get("https://api.catboys.com/img")
            .bodyAsText()
            .let { json.decodeFromString<CatboyModel>(it) }
            .let { NekoImage(it.url, it.artist) }
    }

    suspend fun catApi() = runCatching {
        client.get("https://api.thecatapi.com/v1/images/search")
            .bodyAsText()
            .let { json.decodeFromString<List<CatApiModel>>(it) }
            .first()
            .let { NekoImage(it.url, "TheCatApi") }
    }

    suspend fun showMarvelSnapCards() = runCatching {
        client.get("https://marvelsnapzone.com/getinfo/?searchtype=cards&searchcardstype=true")
            .bodyAsText()
            .let { json.decodeFromString<MarvelSnapModel>(it) }
    }

    suspend fun pixrayLoad(): Result<NekoImage> {
        fun HttpRequestBuilder.setup() {
            header("Authorization", "Token $replicateToken")
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            setBody(
                PixrayModel(
                    version = "ac732df83cea7fff18b8472768c88ad041fa750ff7682a21affe81863cbe77e4",
                    input = Prompts("Anime cat boy with purple hair, purple cat ears, purple cat tail, and green piercing eyes")
                )
            )
        }
        return runCatching {
            client.post("https://api.replicate.com/v1/predictions") { setup() }
                .bodyAsText()
                .let { json.decodeFromString<PixrayResponse>(it) }
                .urls.get
                .let { url ->
                    var res: PixrayResponse?
                    do {
                        val response = client.post(url) { setup() }
                            .bodyAsText()
                            .let { json.decodeFromString<PixrayResponse>(it) }
                        println(response)
                        res = response
                        delay(30000)
                    } while (response.status != "succeeded")

                    client.get(res!!.urls.get) {
                        header("Authorization", "Token $replicateToken")
                        contentType(ContentType.Application.Json)
                        accept(ContentType.Application.Json)
                    }
                        .bodyAsText()
                        .let { json.decodeFromString<PixrayOutput>(it) }
                        .let { NekoImage(it.output.random(), "Replicate") }
                }
        }
    }
}
