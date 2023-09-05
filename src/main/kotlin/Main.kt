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
import androidx.compose.ui.window.application
import kotlinx.coroutines.launch

@Composable
@Preview
fun App(vm: DiscordBotViewModel) {
    Crossfade(vm.bot) { target ->
        if (target != null) {
            DiscordBotView(vm)
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

fun main() = application {
    val viewModel = remember { DiscordBotViewModel() }
    WindowWithBar(
        onCloseRequest = ::exitApplication
    ) {
        App(viewModel)
    }

    /*if (viewModel.bot != null) {
        WindowWithBar(
            onCloseRequest = {}
        ) {
            ChatWindow(viewModel)
        }
    }*/
}
