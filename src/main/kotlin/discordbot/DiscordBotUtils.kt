package discordbot

import dev.kord.common.Color
import dev.kord.core.behavior.interaction.response.DeferredPublicMessageInteractionResponseBehavior
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.rest.builder.message.create.FollowupMessageCreateBuilder
import dev.kord.rest.builder.message.create.embed
import dev.kord.rest.builder.message.modify.FollowupMessageModifyBuilder
import dev.kord.rest.builder.message.modify.embed
import io.ktor.utils.io.*

val Purple = Color(0xFF4a148c.toInt())
val Blue = Color(0xFF42a5f5.toInt())
val Red = Color(0xFFe74c3c.toInt())
val Emerald = Color(0xFF2ecc71.toInt())

sealed class NekoImageType(
    val artist: String
)

class NekoImage(
    val url: String,
    artist: String
) : NekoImageType(artist)

class LocalNekoImage(
    val byteReadChannel: ByteReadChannel,
    artist: String
) : NekoImageType(artist)

suspend fun Result<Any>.respondWithError(response: DeferredPublicMessageInteractionResponseBehavior) = onFailure {
    response.respond {
        content = "Error!"
        embed {
            title = "Something went wrong"
            description = it.stackTraceToString()
            color = Red
        }
    }
}

context (FollowupMessageCreateBuilder)
suspend fun Result<Any>.respondWithError() = onFailure {
    content = "Error!"
    embed {
        title = "Something went wrong"
        description = it.stackTraceToString()
        color = Red
    }
}

context (FollowupMessageModifyBuilder)
suspend fun Result<Any>.respondWithError() = onFailure {
    content = "Error!"
    embed {
        title = "Something went wrong"
        description = it.stackTraceToString()
        color = Red
    }
}