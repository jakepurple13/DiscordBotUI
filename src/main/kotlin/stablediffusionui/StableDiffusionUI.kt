@file:Suppress("INLINE_FROM_HIGHER_PLATFORM")

package stablediffusionui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import moe.tlaster.precompose.viewmodel.ViewModel
import moe.tlaster.precompose.viewmodel.viewModel
import moe.tlaster.precompose.viewmodel.viewModelScope
import org.jetbrains.compose.splitpane.ExperimentalSplitPaneApi
import org.jetbrains.compose.splitpane.HorizontalSplitPane
import org.jetbrains.compose.splitpane.rememberSplitPaneState
import stablediffusion.*
import java.awt.Cursor
import java.awt.Image
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import java.io.IOException
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.*

@OptIn(ExperimentalSplitPaneApi::class, ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun StableDiffusionUI(stableDiffusionNetwork: StableDiffusionNetwork) {
    val viewModel = viewModel { StableDiffusionViewModel(stableDiffusionNetwork) }
    val splitterState = rememberSplitPaneState(.5f)
    Scaffold(
        bottomBar = {
            BottomAppBar(
                actions = {
                    viewModel.sdInfo?.timeToGenerate?.let {
                        Text("Time taken to generate: $it")
                    }
                },
                floatingActionButton = {
                    AnimatedVisibility(
                        !viewModel.isLoading && viewModel.prompt.isNotEmpty(),
                        enter = slideInHorizontally(),
                        exit = slideOutHorizontally()
                    ) {
                        ExtendedFloatingActionButton(
                            onClick = { viewModel.generateImage() },
                            text = { Text("Generate Image") },
                            icon = { Icon(Icons.Default.PlayCircle, null) },
                            expanded = true,
                        )
                    }
                }
            )
        }
    ) { padding ->
        HorizontalSplitPane(
            splitPaneState = splitterState,
            modifier = Modifier.padding(padding)
        ) {
            second(300.dp) {
                if (viewModel.isLoading) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    viewModel.sdInfo?.let { info ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .verticalScroll(rememberScrollState())
                                .fillMaxSize()
                        ) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                info.images.forEach { LoadedImage(it) }
                            }
                            SelectionContainer {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    val sdInfo = info.stableDiffusionInfo.info

                                    sdInfo.infotexts.firstOrNull()
                                        ?.let { Regex("Model: (.*?),").find(it)?.groupValues?.getOrNull(1) }
                                        ?.let { Text("Model: $it") }

                                    Text("Seed: ${sdInfo.seed}")
                                    Text("Sampling Method: ${sdInfo.samplerName}")
                                    Text("Steps: ${sdInfo.steps}")
                                    Text("Cfg Scale: ${sdInfo.cfgScale}")
                                    Text("Clip Skip: ${sdInfo.clipSkip}")
                                }
                            }
                        }
                    }
                }
            }

            splitter {
                visiblePart {
                    Box(
                        Modifier
                            .width(1.dp)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.background)
                    )
                }
                handle {
                    Box(
                        Modifier
                            .markAsHandle()
                            .cursorForHorizontalResize()
                            .background(SolidColor(Color.Gray), alpha = 0.50f)
                            .width(9.dp)
                            .fillMaxHeight()
                    )
                }
            }

            first(300.dp) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    TextField(
                        value = viewModel.prompt,
                        onValueChange = { viewModel.prompt = it },
                        label = { Text("Prompt") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    TextField(
                        value = viewModel.negativePrompt,
                        onValueChange = { viewModel.negativePrompt = it },
                        label = { Text("Negative Prompt") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    ListItem(
                        overlineContent = { Text("Steps: ${viewModel.steps}") },
                        headlineContent = {
                            Slider(
                                value = viewModel.steps.toFloat(),
                                onValueChange = { viewModel.steps = it.toInt() },
                                steps = 150,
                                valueRange = 1f..150f
                            )
                        }
                    )

                    ListItem(
                        overlineContent = { Text("Cfg Scale: ${viewModel.cfgScale}") },
                        headlineContent = {
                            Slider(
                                value = viewModel.cfgScale.toFloat(),
                                onValueChange = { viewModel.cfgScale = it.toDouble() },
                                steps = 30,
                                valueRange = 1f..30f
                            )
                        }
                    )

                    ListItem(
                        overlineContent = { Text("Clip Skip: ${viewModel.clipSkip}") },
                        headlineContent = {
                            Slider(
                                value = viewModel.clipSkip.toFloat(),
                                onValueChange = { viewModel.clipSkip = it.toLong() },
                                steps = 12,
                                valueRange = 1f..12f
                            )
                        }
                    )

                    TextField(
                        value = viewModel.seed,
                        onValueChange = { viewModel.seed = it },
                        label = { Text("Seed") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    var showModelSelector by remember { mutableStateOf(false) }
                    OutlinedCard(
                        onClick = { showModelSelector = true }
                    ) {
                        DropdownMenu(
                            showModelSelector,
                            onDismissRequest = { showModelSelector = false }
                        ) {
                            viewModel.modelList.forEach {
                                DropdownMenuItem(
                                    text = { Text(it.modelName) },
                                    onClick = {
                                        viewModel.modelName = it
                                        showModelSelector = false
                                    },
                                    leadingIcon = { RadioButton(selected = it == viewModel.modelName, onClick = null) }
                                )
                            }
                        }
                        ListItem(
                            headlineContent = { Text(viewModel.modelName?.modelName.orEmpty()) },
                            trailingContent = {
                                IconButton(
                                    onClick = { viewModel.loadModels() },
                                ) { Icon(Icons.Default.Refresh, null) }
                            }
                        )
                    }

                    var showSamplerSelector by remember { mutableStateOf(false) }
                    OutlinedCard(
                        onClick = { showSamplerSelector = true }
                    ) {
                        DropdownMenu(
                            showSamplerSelector,
                            onDismissRequest = { showSamplerSelector = false }
                        ) {
                            viewModel.samplerList.forEach {
                                DropdownMenuItem(
                                    text = { Text(it.name) },
                                    onClick = {
                                        viewModel.sampler = it
                                        showSamplerSelector = false
                                    },
                                    leadingIcon = { RadioButton(selected = it == viewModel.sampler, onClick = null) }
                                )
                            }
                        }
                        ListItem(
                            headlineContent = { Text(viewModel.sampler?.name.orEmpty()) },
                            trailingContent = {
                                IconButton(
                                    onClick = { viewModel.loadSamplers() },
                                ) { Icon(Icons.Default.Refresh, null) }
                            }
                        )
                    }
                }
            }
        }
    }
}

private fun Modifier.cursorForHorizontalResize(): Modifier =
    pointerHoverIcon(PointerIcon(Cursor(Cursor.E_RESIZE_CURSOR)))

@Composable
private fun LoadedImage(image: org.jetbrains.skia.Image) {
    ContextMenuArea(
        items = {
            listOf(
                ContextMenuItem("Copy") {
                    val imgSel = ImageSelection(image.toComposeImageBitmap().toAwtImage())
                    Toolkit.getDefaultToolkit()
                        .systemClipboard
                        .setContents(imgSel, null)
                    Toolkit.getDefaultToolkit().beep()
                },
            )
        }
    ) {
        Image(
            bitmap = image.toComposeImageBitmap(),
            contentDescription = null
        )
    }
}

internal class StableDiffusionViewModel(
    private val stableDiffusionNetwork: StableDiffusionNetwork,
) : ViewModel() {
    var prompt by mutableStateOf("")
    var modelName by mutableStateOf<StableDiffusionModel?>(null)
    var cfgScale by mutableDoubleStateOf(7.0)
    var steps by mutableIntStateOf(20)
    var negativePrompt by mutableStateOf("")
    var sampler by mutableStateOf<StableDiffusionSamplers?>(null)
    var seed by mutableStateOf("-1")
    var clipSkip by mutableLongStateOf(1)
    var width by mutableLongStateOf(512)
    var height by mutableLongStateOf(512)
    var pose by mutableStateOf<ByteArray?>(null)

    var sdInfo by mutableStateOf<SDImageInfo?>(null)
    var isLoading by mutableStateOf(false)

    private val dateTimeFormatter = DateTimeFormatter.ofPattern("m:ss", Locale.getDefault())
    private val dateTimeParser = DateTimeFormatter
        .ofPattern("yyyyMMddHHmmss", Locale.getDefault())
        .withZone(ZoneId.systemDefault())

    val modelList = mutableStateListOf<StableDiffusionModel>()
    val samplerList = mutableStateListOf<StableDiffusionSamplers>()
    val loraList = mutableStateListOf<StableDiffusionLora>()

    init {
        loadModels()
        loadSamplers()
        loadLoras()
    }

    fun loadModels() {
        viewModelScope.launch {
            stableDiffusionNetwork.stableDiffusionModels().onSuccess {
                modelList.clear()
                modelName = it.firstOrNull()
                modelList.addAll(it)
            }
        }
    }

    fun loadSamplers() {
        viewModelScope.launch {
            stableDiffusionNetwork.stableDiffusionSamplers().onSuccess {
                samplerList.clear()
                sampler = it.firstOrNull()
                samplerList.addAll(it)
            }
        }
    }

    fun loadLoras() {
        viewModelScope.launch {
            stableDiffusionNetwork.stableDiffusionLoras().onSuccess {
                loraList.clear()
                loraList.addAll(it)
            }
        }
    }

    fun generateImage() {
        viewModelScope.launch {
            isLoading = true
            stableDiffusionNetwork.stableDiffusion(
                prompt = prompt,
                modelName = modelName?.modelName,
                cfgScale = cfgScale,
                steps = steps,
                negativePrompt = negativePrompt,
                sampler = sampler?.name,
                seed = seed.toLongOrNull(),
                clipSkip = clipSkip,
                width = width,
                height = height,
                pose = null
            ).onSuccess {
                sdInfo = SDImageInfo(
                    stableDiffusionInfo = it,
                    dateTimeFormatter = dateTimeFormatter,
                    dateTimeParser = dateTimeParser
                )
            }
            isLoading = false
        }
    }
}

class SDImageInfo(
    val stableDiffusionInfo: StableDiffusionInfo,
    val images: List<org.jetbrains.skia.Image> = stableDiffusionInfo.images.map {
        org.jetbrains.skia.Image.makeFromEncoded(Base64.getDecoder().decode(it))
    },
    dateTimeFormatter: DateTimeFormatter,
    dateTimeParser: DateTimeFormatter,
) {
    val timeToGenerate by lazy {
        val t = dateTimeParser.parse(stableDiffusionInfo.info.jobTimestamp)
        val z = ZonedDateTime.of(LocalDateTime.from(t), ZoneId.systemDefault())
        val duration = System.currentTimeMillis() - Instant.from(z).toEpochMilli()
        dateTimeFormatter.format(
            Instant.ofEpochMilli(duration).atZone(ZoneOffset.systemDefault())
        )
    }
}

// This class is used to hold an image while on the clipboard.
internal class ImageSelection(private val image: Image) : Transferable {

    // Returns supported flavors
    override fun getTransferDataFlavors(): Array<DataFlavor> {
        return arrayOf(DataFlavor.imageFlavor)
    }

    // Returns true if flavor is supported
    override fun isDataFlavorSupported(flavor: DataFlavor): Boolean {
        return DataFlavor.imageFlavor.equals(flavor)
    }

    // Returns image
    @Throws(UnsupportedFlavorException::class, IOException::class)
    override fun getTransferData(flavor: DataFlavor): Any {
        if (!DataFlavor.imageFlavor.equals(flavor)) {
            throw UnsupportedFlavorException(flavor)
        }
        return image
    }
}