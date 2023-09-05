import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import moe.tlaster.precompose.flow.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onClose: () -> Unit,
) {
    WindowWithBar(
        onCloseRequest = onClose
    ) {
        val scope = rememberCoroutineScope()
        Scaffold(
            topBar = { TopAppBar(title = { Text("Settings") }) }
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .padding(it)
                    .verticalScroll(rememberScrollState())
            ) {
                val scrollToBottom by DataStore.scrollToBottom.collectAsStateWithLifecycle(true)

                OutlinedCard(
                    onClick = { scope.launch { DataStore.changeScrollToBottom(!scrollToBottom) } }
                ) {
                    ListItem(
                        headlineContent = { Text("Scroll to Bottom") },
                        trailingContent = { Switch(checked = scrollToBottom, onCheckedChange = null) }
                    )
                }
            }
        }
    }
}