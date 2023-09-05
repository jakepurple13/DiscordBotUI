import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.kord.core.entity.Member
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatWindow(viewModel: DiscordBotViewModel) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Chat") }) },
        bottomBar = {

        }
    ) { padding ->
        Row(
            modifier = Modifier.padding(padding)
        ) {
            val memberList by produceState(
                initialValue = listOf<Member>(),
                key1 = viewModel.selectedGuild
            ) {
                value = emptyList()
                viewModel
                    .selectedGuild
                    ?.members
                    ?.onEach {
                        value = listOf(*value.toTypedArray(), it)
                    }
                    ?.launchIn(this)
            }
            LazyColumn(
                modifier = Modifier.weight(1f)
            ) {
                items(memberList) {
                    OutlinedCard {
                        ListItem(
                            headlineContent = { Text(it.effectiveName) },
                            trailingContent = {
                                it.memberAvatar?.cdnUrl?.toUrl()?.let { it1 ->
                                    KamelImage(asyncPainterResource(it1), contentDescription = null)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageRow(viewModel: DiscordBotViewModel) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .fillMaxWidth()
            .animateContentSize()
    ) {
        var showGuildDropDown by remember { mutableStateOf(false) }
        DropdownMenu(
            showGuildDropDown,
            onDismissRequest = { showGuildDropDown = false }
        ) {
            DropdownMenuItem(
                text = { Text("Select a Guild") },
                onClick = { viewModel.selectGuild(null) },
                leadingIcon = { RadioButton(selected = viewModel.selectedGuild == null, onClick = null) }
            )
            viewModel.guildList.forEach {
                DropdownMenuItem(
                    text = { Text(it.name) },
                    onClick = { viewModel.selectGuild(it) },
                    leadingIcon = { RadioButton(selected = it == viewModel.selectedGuild, onClick = null) }
                )
            }
        }
        Card(
            onClick = { showGuildDropDown = true }
        ) {
            Text(
                viewModel.selectedGuild?.name ?: "Select a Guild",
                modifier = Modifier.padding(4.dp)
            )
        }

        AnimatedVisibility(viewModel.channelList.isNotEmpty()) {
            var showChannelDropDown by remember { mutableStateOf(false) }
            DropdownMenu(
                showChannelDropDown,
                onDismissRequest = { showChannelDropDown = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Select a Guild") },
                    onClick = { viewModel.selectedChannel = null },
                    leadingIcon = {
                        RadioButton(
                            selected = viewModel.selectedChannel == null,
                            onClick = null
                        )
                    }
                )
                viewModel.channelList.forEach {
                    DropdownMenuItem(
                        text = { Text(it.name) },
                        onClick = { viewModel.selectedChannel = it },
                        leadingIcon = {
                            RadioButton(
                                selected = it == viewModel.selectedChannel,
                                onClick = null
                            )
                        }
                    )
                }
            }
            Card(
                onClick = { showChannelDropDown = true }
            ) {
                Text(
                    viewModel.selectedChannel?.name ?: "Select a Channel",
                    modifier = Modifier.padding(4.dp)
                )
            }
        }

        AnimatedVisibility(viewModel.selectedGuild != null && viewModel.selectedChannel != null) {
            var message by remember { mutableStateOf("") }
            TextField(
                value = message,
                onValueChange = { message = it },
                trailingIcon = {
                    IconButton(
                        onClick = {
                            viewModel.sendMessage(message)
                            message = ""
                        }
                    ) { Icon(Icons.Default.Send, null) }
                }
            )
        }
    }
}