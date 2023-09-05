@file:Suppress("INLINE_FROM_HIGHER_PLATFORM")

package discordbot

import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.publicSlashCommand
import com.kotlindiscord.kord.extensions.types.editingPaginator
import com.kotlindiscord.kord.extensions.types.respond
import dev.kord.rest.builder.message.create.embed
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class MarvelSnapExtension(
    private val network: Network
) : Extension() {
    override val name: String = "snapcards"

    override suspend fun setup() {
        publicSlashCommand {
            name = "snapcards"
            description = "Get Cards"
            action {
                network.showMarvelSnapCards()
                    .onSuccess { model ->
                        val d = model.success.cards
                        editingPaginator {
                            d.forEach { card ->
                                page {
                                    title = card.name
                                    image = card.art
                                    footer {
                                        text = "By: ${card.sketcher}"
                                    }
                                    color = Blue
                                }
                            }
                        }.send()
                    }
                    .onFailure {
                        respond {
                            content = "Error!"
                            embed {
                                title = "Something went wrong"
                                description = it.stackTraceToString()
                                color = Red
                            }
                        }
                    }
            }
        }
    }
}

@Serializable
data class MarvelSnapModel(
    @SerialName("GET")
    val get: Get,
    val success: Success,
    @SerialName("time_total")
    val timeTotal: String,
)

@Serializable
data class Get(
    val searchtype: String,
    val searchcardstype: String,
)

@Serializable
data class Success(
    val cards: List<Card>,
)

@Serializable
data class Card(
    val cid: Long,
    val name: String,
    val type: String,
    val cost: Long,
    val power: Long,
    val ability: String,
    val flavor: String,
    val art: String,
    @SerialName("alternate_art")
    val alternateArt: String,
    val url: String,
    val status: String,
    val carddefid: String,
    val variants: List<Variant>,
    val source: String,
    @SerialName("source_slug")
    val sourceSlug: String,
    val tags: List<Tag>,
    val rarity: String,
    @SerialName("rarity_slug")
    val raritySlug: String,
    val difficulty: String,
    val sketcher: String,
    val inker: String,
    val colorist: String,
)

@Serializable
data class Variant(
    val cid: Long,
    val vid: Long,
    val art: String,
    @SerialName("art_filename")
    val artFilename: String,
    val rarity: String,
    @SerialName("rarity_slug")
    val raritySlug: String,
    @SerialName("variant_order")
    val variantOrder: String,
    val status: String,
    @SerialName("full_description")
    val fullDescription: String,
    val inker: String,
    val sketcher: String,
    val colorist: String,
)

@Serializable
data class Tag(
    @SerialName("tag_id")
    val tagId: Long,
    val tag: String,
    @SerialName("tag_slug")
    val tagSlug: String,
)
