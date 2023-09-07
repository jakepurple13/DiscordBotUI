import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import moe.tlaster.precompose.PreComposeWindow
import moe.tlaster.precompose.flow.collectAsStateWithLifecycle
import org.jetbrains.skiko.OS
import org.jetbrains.skiko.hostOs

@Composable
fun BotOptionsViewController(
    botOptionsState: WindowState,
) {
    PreComposeWindow(
        onCloseRequest = {},
        state = botOptionsState,
        title = "Bot Options",
        undecorated = true,
        transparent = true,
        focusable = false,
        resizable = false,
    ) {
        CustomMaterialTheme {
            Surface( 
                shape = when (hostOs) {
                    OS.Linux -> RoundedCornerShape(8.dp)
                    OS.Windows -> RectangleShape
                    OS.MacOS -> RoundedCornerShape(8.dp)
                    else -> RoundedCornerShape(8.dp)
                },
                modifier = Modifier.animateContentSize(),
                border = ButtonDefaults.outlinedButtonBorder,
            ) {
                BotOptionsView()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BotOptionsView() {
    val scope = rememberCoroutineScope()
    Scaffold(
        topBar = {
            Column {
                TopAppBar(title = { Text("Bot Options") })
                Divider(color = MaterialTheme.colorScheme.onSurface)
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier.padding(padding)
        ) {
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier.verticalScroll(scrollState)
            ) {
                SwitchPreference(
                    title = "Send Start Up Messages?",
                    dataStoreType = DataStore.sendStartup,
                    scope = scope
                )
                SwitchPreference(
                    title = "Send Shutting Down Messages?",
                    dataStoreType = DataStore.sendShutdown,
                    scope = scope
                )

                Divider()
            }
            VerticalScrollbar(
                adapter = rememberScrollbarAdapter(scrollState),
                style = LocalScrollbarStyle.current.copy(
                    hoverColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.50f),
                    unhoverColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f)
                ),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 4.dp)
                    .fillMaxHeight()
            )
        }
    }
}

@Composable
private fun SwitchPreference(
    title: String,
    dataStoreType: DataStore.DataStoreType<Boolean>,
    scope: CoroutineScope = rememberCoroutineScope(),
    modifier: Modifier = Modifier,
) {
    val value by dataStoreType.flow.collectAsStateWithLifecycle(false)
    ListItem(
        headlineContent = { Text(title) },
        trailingContent = {
            Switch(
                checked = value,
                onCheckedChange = null
            )
        },
        modifier = modifier.toggleable(
            value = value,
            onValueChange = { scope.launch { dataStoreType.update(it) } }
        )
    )
}