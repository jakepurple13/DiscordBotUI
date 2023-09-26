package stablediffusion

import dev.kord.rest.builder.message.create.FollowupMessageCreateBuilder
import dev.kord.rest.builder.message.create.embed
import discordbot.Emerald
import kotlinx.serialization.*
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.*

data class SDInfo(
    val prompt: String,
    val negativePrompt: String?,
    val cfgScale: Double,
    val clipSkip: Long,
    val steps: Long,
    val seed: Long,
    val samplerName: String,
    val infotexts: List<String>,
    val jobTimestamp: String,
)

context (FollowupMessageCreateBuilder, StableDiffusionExtension)
fun SDInfo.writeResponse() {
    embed {
        title = "Here is your image!"

        field("Prompt") { if (prompt.length <= 1024) prompt else "Too many characters" }
        negativePrompt?.let { field("Negative Prompt") { if (it.length <= 1024) it else "Too many characters" } }
        field("Cfg Scale") { cfgScale.toString() }
        field("Clip Skip") { clipSkip.toString() }
        field("Steps") { steps.toString() }
        field("Seed") { seed.toString() }
        field("Sampling Method") { samplerName }
        infotexts
            .firstOrNull()
            ?.let { Regex("Model: (.*?),").find(it)?.groupValues?.getOrNull(1) }
            ?.let { field("Model") { it } }

        footer {
            val t = dateTimeParser.parse(jobTimestamp)
            val z = ZonedDateTime.of(LocalDateTime.from(t), ZoneId.systemDefault())
            val duration = System.currentTimeMillis() - Instant.from(z).toEpochMilli()
            val time = dateTimeFormatter.format(
                Instant.ofEpochMilli(duration).atZone(ZoneOffset.systemDefault())
            )

            text = "Generated by Stable Diffusion - Took $time"
        }

        color = Emerald
    }
}


@OptIn(ExperimentalSerializationApi::class)
class DynamicLookupSerializer : KSerializer<Any> {
    override val descriptor: SerialDescriptor = ContextualSerializer(Any::class, null, emptyArray()).descriptor

    @OptIn(InternalSerializationApi::class)
    override fun serialize(encoder: Encoder, value: Any) {
        if (value is ArrayList<*>) {
            encoder.encodeSerializableValue(ListSerializer(DynamicLookupSerializer()), value)
            return
        }
        val actualSerializer = encoder.serializersModule.getContextual(value::class) ?: value::class.serializer()
        encoder.encodeSerializableValue(actualSerializer as KSerializer<Any>, value)
    }

    override fun deserialize(decoder: Decoder): Any {
        error("Unsupported")
    }
}

fun <K, V> mapOfNotNull(vararg pairs: Pair<K, V>?) = pairs.filterNotNull().toMap()