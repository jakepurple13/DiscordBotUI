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
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

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

fun main() {
    val viewModel = DiscordBotViewModel()

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
    }
}
