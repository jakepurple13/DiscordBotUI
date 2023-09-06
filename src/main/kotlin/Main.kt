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
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.configureSwingGlobalsForCompose
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.window.awaitApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

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
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
fun main() = runBlocking {
    val viewModel = DiscordBotViewModel()
    if (System.getProperty("compose.application.configure.swing.globals") == "true") {
        configureSwingGlobalsForCompose()
    }

    awaitApplication {
        var showSettings by remember { mutableStateOf(false) }

        WindowWithBar(
            onCloseRequest = ::exitApplication,
            canClose = viewModel.bot == null
        ) {
            App(
                viewModel,
                onShowSettings = { showSettings = true }
            )
        }

        if (showSettings) {
            SettingsScreen(
                onClose = { showSettings = false }
            )
        }
    }

    Runtime.getRuntime().addShutdownHook(
        Thread {
            runBlocking {
                viewModel.bot?.stop()
                println("Shutting down")
            }
        }
    )

    withContext(Dispatchers.IO) { Thread.currentThread().join() }
}
