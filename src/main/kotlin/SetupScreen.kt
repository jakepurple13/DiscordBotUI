import androidx.compose.animation.Crossfade
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import dev.kord.core.entity.Guild
import kotlinx.coroutines.runBlocking

@Composable
@Preview
fun App(
    vm: DiscordBotViewModel,
    onShowSettings: () -> Unit,
    onShowSearch: () -> Unit,
) {
    Crossfade(vm.botState) { target ->
        when (target) {
            BotState.BotRunning -> DiscordBotView(
                viewModel = vm,
                onShowSettings = onShowSettings,
                onShowSearch = onShowSearch
            )

            is BotState.Error -> ErrorState(vm, target.error)
            BotState.Loading -> {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    CircularProgressIndicator()
                }
            }

            BotState.TokenSetup -> TokenSetup(vm)
        }
    }
}

@Composable
fun ErrorState(vm: DiscordBotViewModel, throwable: Throwable) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.verticalScroll(rememberScrollState())
        ) {
            Text("Something went wrong")
            Button(
                onClick = { vm.botState = BotState.TokenSetup },
            ) {
                Text("Return to Token Setup")
            }
            Text(throwable.stackTraceToString())
        }
    }
}

@Composable
fun TokenSetup(vm: DiscordBotViewModel) {
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
                    label = { Text("Bot Token Key") },
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

                var showSaveTokenDialog by remember { mutableStateOf(false) }
                if (showSaveTokenDialog) {
                    AlertDialog(
                        onDismissRequest = { showSaveTokenDialog = false },
                        title = { Text("Save Token?") },
                        text = { Text("You already have a saved token. Are you sure you want to replace it?") },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    showSaveTokenDialog = false
                                    vm.setToken()
                                },
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                )
                            ) { Text("Stop") }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = { showSaveTokenDialog = false },
                            ) { Text("Cancel") }
                        }
                    )
                }
                IconButton(
                    onClick = {
                        if (vm.canStartBot) showSaveTokenDialog = true
                        else vm.setToken()
                    }
                ) { Icon(Icons.Default.Save, null) }
            }
            Button(
                onClick = { vm.startBot() },
                enabled = vm.canStartBot && vm.botState is BotState.TokenSetup
            ) {
                Text("Start Bot")
            }
            Button(
                onClick = { vm.botState = BotState.BotRunning },
                enabled = vm.canStartBot && vm.botState is BotState.TokenSetup
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
    mainMenuOptions: @Composable MenuBarScope.() -> Unit = {},
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
        var showSearch by remember { mutableStateOf(false) }
        var showBotOptions by remember { mutableStateOf(false) }
        val state = rememberWindowState()

        WindowWithBar(
            onCloseRequest = ::exitApplication,
            windowTitle = "DiscordBot UI",
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
                    mainMenuOptions()
                }
            }
        ) {
            App(
                vm = viewModel,
                onShowSettings = { showSettings = true },
                onShowSearch = { showSearch = true }
            )
        }

        if (showBotOptions) {
            val windowPosition = state.position
            val botOptionsState = rememberWindowState(
                position = WindowPosition.Aligned(Alignment.CenterEnd),
                size = DpSize(300.dp, 600.dp)
            )

            LaunchedEffect(windowPosition) {
                botOptionsState.position =
                    if (windowPosition is WindowPosition.Absolute && state.placement != WindowPlacement.Maximized)
                        windowPosition.copy(x = windowPosition.x + state.size.width + 25.dp)
                    else
                        WindowPosition.Aligned(Alignment.CenterEnd)
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

        if (showSearch) {
            SearchWindow(
                viewModel = viewModel,
                onClose = { showSearch = false }
            )
        }

        customUi()
    }
}