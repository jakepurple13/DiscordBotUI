@file:Suppress("INLINE_FROM_HIGHER_PLATFORM")

package stablediffusion.commands

import com.kotlindiscord.kord.extensions.components.forms.ModalForm
import com.kotlindiscord.kord.extensions.extensions.ephemeralSlashCommand
import com.kotlindiscord.kord.extensions.types.respond
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
                modelSuggesterDatabase.addSuggestion(
                    checkNotNull(linkLine.value),
                    reasonLine.value.orEmpty()
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