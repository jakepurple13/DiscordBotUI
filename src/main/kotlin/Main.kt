@file:Suppress("INLINE_FROM_HIGHER_PLATFORM")

import androidx.compose.animation.Crossfade
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.window.application
import kotlinx.coroutines.launch

@Composable
@Preview
fun App(vm: DiscordBotViewModel) {
    val scope = rememberCoroutineScope()

    Crossfade(vm.bot) { target ->
        if (target != null) {
            DiscordBotView(vm)
        } else {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Column {
                    Row {
                        TextField(
                            value = vm.tokenForBot,
                            onValueChange = { vm.tokenForBot = it },
                            visualTransformation = PasswordVisualTransformation()
                        )
                        IconButton(
                            onClick = { vm.setToken() }
                        ) { Icon(Icons.Default.Save, null) }
                    }
                    Button(
                        onClick = { scope.launch { vm.startBot() } }
                    ) {
                        Text("Start Bot")
                    }
                }
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
