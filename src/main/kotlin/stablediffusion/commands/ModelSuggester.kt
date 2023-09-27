@file:Suppress("INLINE_FROM_HIGHER_PLATFORM")

package stablediffusion.commands

import com.kotlindiscord.kord.extensions.components.forms.ModalForm
import com.kotlindiscord.kord.extensions.extensions.ephemeralSlashCommand
import com.kotlindiscord.kord.extensions.types.respond
import com.kotlindiscord.kord.extensions.utils.getJumpUrl
import dev.kord.rest.builder.message.create.embed
import discordbot.Emerald
import discordbot.Red
import stablediffusion.StableDiffusionExtension
import stablediffusionui.ModelSuggesterDatabase

suspend fun StableDiffusionExtension.modelSuggester() {
    val modelSuggesterDatabase = ModelSuggesterDatabase.createInstance()
    val modal = ModelModal("Suggest a Model")
    ephemeralSlashCommand(
        modal = { modal }
    ) {
        name = "suggestmodel"
        description = "Suggest a link"

        val linkLine = modal.lineText {
            label = "Link to the model"
            required = true
        }

        val reasonLine = modal.paragraphText {
            label = "Reason for the model"
        }

        action {
            runCatching {
                val userData = user.fetchUser()
                modelSuggesterDatabase.addSuggestion(
                    link = checkNotNull(linkLine.value),
                    reason = reasonLine.value.orEmpty(),
                    suggestedBy = userData.let { it.globalName ?: it.username },
                    avatar = userData.avatar?.cdnUrl?.toUrl(),
                    channel = channel.asChannelOrNull()?.data?.name?.value ?: "Direct Message",
                    server = guild?.asGuildOrNull()?.name ?: "Direct Message",
                    lastMessage = channel.asChannel().getLastMessage()?.getJumpUrl()
                )
            }
                .onSuccess {
                    respond {
                        content = "Successfully submitted!"
                        embed {
                            title = "Hurray!"
                            color = Emerald
                        }
                    }
                }
                .onFailure {
                    respond {
                        it.printStackTrace()
                        content = "Error!"
                        embed {
                            title = "Something went wrong"
                            color = Red
                        }
                    }
                }
        }
    }
}

private class ModelModal(override var title: String) : ModalForm()