@file:Suppress("INLINE_FROM_HIGHER_PLATFORM")

import androidx.compose.animation.Crossfade
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import com.kotlindiscord.kord.extensions.ExtensibleBot
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.edit
import dev.kord.core.entity.Guild
import dev.kord.rest.builder.message.create.embed
import dev.kord.rest.builder.message.modify.actionRow
import dev.kord.rest.builder.message.modify.embed
import discordbot.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import stablediffusion.StableDiffusion
import stablediffusion.StableDiffusionNetwork

@Composable
@Preview
fun App(
    vm: DiscordBotViewModel,
    onShowSettings: () -> Unit,
) {
    Crossfade(vm.showBotScreen) { target ->
        if (target) {
            DiscordBotView(vm, onShowSettings)
        } else {
            TokenSetup(vm)
        }
    }
}

@Composable
fun TokenSetup(vm: DiscordBotViewModel) {
    val scope = rememberCoroutineScope()
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row {
                IconButton(
                    onClick = { vm.showSavedToken() }
                ) { Icon(Icons.Default.Password, null) }
                var showPassword by remember { mutableStateOf(false) }
                TextField(
                    value = vm.tokenForBot,
                    onValueChange = { vm.tokenForBot = it },
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(
                            onClick = { showPassword = !showPassword }
                        ) {
                            Icon(
                                if (showPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                null
                            )
                        }
                    }
                )
                IconButton(
                    onClick = { vm.setToken() }
                ) { Icon(Icons.Default.Save, null) }
            }
            Button(
                onClick = { scope.launch { vm.startBot() } },
                enabled = vm.canStartBot
            ) {
                Text("Start Bot")
            }
            Button(
                onClick = { vm.showBotScreen = true },
                enabled = vm.canStartBot
            ) {
                Text("Enter Without Starting")
            }
        }
    }
}

fun DiscordBotUI(
    botCreation: suspend (token: String?) -> ExtensibleBot,
    startUpMessages: suspend (Guild) -> Unit = {},
    shutdownMessages: suspend (Guild) -> Unit = {},
    customUi: @Composable ApplicationScope.() -> Unit = {},
) {
    val viewModel = DiscordBotViewModel(
        botCreation = botCreation,
        startUpMessages = startUpMessages,
        shutdownMessages = shutdownMessages
    )

    Runtime.getRuntime().addShutdownHook(
        Thread {
            runBlocking {
                println("Shutting down")
                viewModel.stopBot()
            }
        }
    )

    application {
        var showSettings by remember { mutableStateOf(false) }
        var showBotOptions by remember { mutableStateOf(false) }
        val state = rememberWindowState()

        WindowWithBar(
            onCloseRequest = ::exitApplication,
            canClose = viewModel.bot == null,
            state = state,
            frameWindowScope = {
                MenuBar {
                    Menu("Bot Options") {
                        CheckboxItem(
                            text = "Show Bot Options",
                            checked = showBotOptions,
                            onCheckedChange = { showBotOptions = it }
                        )

                        CheckboxItem(
                            text = "Show Settings",
                            checked = showSettings,
                            onCheckedChange = { showSettings = it }
                        )
                    }
                }
            }
        ) {
            App(
                vm = viewModel,
                onShowSettings = { showSettings = true }
            )
        }

        if (showBotOptions) {
            val windowPosition = state.position
            val botOptionsState = rememberWindowState(
                position = WindowPosition.Aligned(Alignment.CenterEnd),
                size = DpSize(300.dp, 600.dp)
            )

            LaunchedEffect(windowPosition) {
                snapshotFlow {
                    if (windowPosition is WindowPosition.Absolute)
                        windowPosition.copy(x = windowPosition.x + state.size.width + 25.dp)
                    else
                        WindowPosition.Aligned(Alignment.CenterEnd)
                }
                    .distinctUntilChanged()
                    .debounce(200)
                    .onEach { botOptionsState.position = it }
                    .launchIn(this)
            }

            BotOptionsViewController(
                botOptionsState = botOptionsState,
            )
        }

        if (showSettings) {
            SettingsScreen(
                onClose = { showSettings = false }
            )
        }

        customUi()
    }
}

fun main() {
    DiscordBotUI(
        botCreation = { token ->
            ExtensibleBot(token!!) {
                chatCommands {
                    enabled = true
                }

                extensions {
                    add { NekoExtension(Network(), StableDiffusionNetwork()) }
                    add { MarvelSnapExtension(Network()) }
                    StableDiffusion.addToKordExtensions()
                    help {
                        pingInReply = true
                        color { Purple }
                    }
                }

                hooks {
                    kordShutdownHook = true
                }

                errorResponse { message, type ->
                    type.error.printStackTrace()
                    println(message)
                }
            }
        },
        startUpMessages = { g ->
            g.systemChannel
                ?.createMessage {
                    suppressNotifications = true
                    content = "NekoBot is booting up...Please wait..."
                }
                ?.also { delay(500) }
                ?.edit {
                    content = "NekoBot is Online!"
                    embed {
                        title = "NekoBot is Online!"
                        description = """
                                Meow is back online!
                                
                                To get more Stable Diffusion models or loras to suggest, press on the buttons below!
                                To use Stable Diffusion, type `/stablediffusion`
                                To get a random neko image, type `/neko random`
                                To get a random cat image, type `/neko cat`
                                To view Marvel Snap cards, type `/snapcards`
                            """.trimIndent()
                        color = Emerald
                    }
                    actionRow {
                        linkButton("https://huggingface.co") { label = "Stable Diffusion Models" }
                        linkButton("https://civitai.com/") { label = "Models and Loras" }
                    }
                }
        },
        shutdownMessages = { g ->
            g.systemChannel?.createMessage {
                suppressNotifications = true
                embed {
                    title = "Shutting Down for maintenance and updates..."
                    timestamp = Clock.System.now()
                    description = "Please wait while I go through some maintenance."
                    thumbnail {
                        url = "https://media.tenor.com/YTPLqiB6gLsAAAAC/sowwy-sorry.gif"
                    }
                    color = Red
                }
            }
        },
    )
}
