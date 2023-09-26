package stablediffusion

import io.ktor.utils.io.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
internal class StableDiffusionBodyImg2Img(
    val seed: Long,
    val prompt: String,
    val cfgScale: Double,
    val steps: Int,
    @SerialName("sampler_index")
    val samplerIndex: String,
    @SerialName("negative_prompt")
    val negativePrompt: String = "",
    @SerialName("batch_size")
    val batchSize: Long = 1,
    @SerialName("override_settings")
    val overrideOptions: OverriddenOptions?,
    val width: Long,
    val height: Long,
    @SerialName("init_images")
    val initImages: List<String>,
)

@Serializable
data class StableDiffusionImg2ImgResponse(
    val images: List<String>,
    val parameters: Img2ImgParameters,
    val info: String,
)

@Serializable
data class Img2ImgParameters(
    val prompt: String,
    @SerialName("negative_prompt")
    val negativePrompt: String,
    val seed: Long,
    @SerialName("batch_size")
    val batchSize: Long,
    val steps: Long,
    @SerialName("cfg_scale")
    val cfgScale: Double,
    val width: Long,
    val height: Long,
)

@Serializable
data class OverrideSettings(
    @SerialName("sd_model_checkpoint")
    val sdModelCheckpoint: String,
)

@Serializable
data class StableDiffusionImg2ImgResponseInfo(
    val prompt: String,
    @SerialName("all_prompts")
    val allPrompts: List<String>,
    @SerialName("negative_prompt")
    val negativePrompt: String?,
    @SerialName("all_negative_prompts")
    val allNegativePrompts: List<String>,
    val seed: Long,
    @SerialName("all_seeds")
    val allSeeds: List<Long>,
    val subseed: Long,
    @SerialName("all_subseeds")
    val allSubseeds: List<Long>,
    @SerialName("subseed_strength")
    val subseedStrength: Long,
    val width: Long,
    val height: Long,
    @SerialName("sampler_name")
    val samplerName: String,
    @SerialName("cfg_scale")
    val cfgScale: Double,
    val steps: Long,
    @SerialName("batch_size")
    val batchSize: Long,
    @SerialName("restore_faces")
    val restoreFaces: Boolean,
    @SerialName("sd_model_hash")
    val sdModelHash: String,
    @SerialName("seed_resize_from_w")
    val seedResizeFromW: Long,
    @SerialName("seed_resize_from_h")
    val seedResizeFromH: Long,
    @SerialName("index_of_first_image")
    val indexOfFirstImage: Long,
    val infotexts: List<String>,
    @SerialName("job_timestamp")
    val jobTimestamp: String,
    @SerialName("clip_skip")
    val clipSkip: Long,
    @SerialName("is_using_inpainting_conditioning")
    val isUsingInpaintingConditioning: Boolean,
)

@Serializable
data class StableDiffusionImg2ImgInfo(
    val images: List<String>,
    val parameters: Img2ImgParameters,
    val info: StableDiffusionImg2ImgResponseInfo,
) {
    fun imagesAsByteChannel() = images.map {
        ByteReadChannel(Base64.getDecoder().decode(it))
    }
}