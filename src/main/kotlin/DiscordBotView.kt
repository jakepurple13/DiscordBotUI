import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun DiscordBotView(viewModel: DiscordBotViewModel) {
    val scope = rememberCoroutineScope()
    Scaffold(
        bottomBar = {
            Column {
                MessageSender(viewModel)
                BottomAppBar(
                    actions = {},
                    floatingActionButton = {
                        var showStopDialog by remember { mutableStateOf(false) }

                        if (showStopDialog) {
                            AlertDialog(
                                onDismissRequest = { showStopDialog = false },
                                title = { Text("Are you sure you want to stop the bot?") },
                                text = { },
                                confirmButton = {
                                    TextButton(
                                        onClick = {
                                            showStopDialog = false
                                            scope.launch { viewModel.stopBot() }
                                        },
                                        colors = ButtonDefaults.textButtonColors(
                                            contentColor = MaterialTheme.colorScheme.error
                                        )
                                    ) { Text("Stop") }
                                },
                                dismissButton = {
                                    TextButton(
                                        onClick = { showStopDialog = false },
                                    ) { Text("Cancel") }
                                }
                            )
                        }

                        FloatingActionButton(
                            onClick = {
                                showStopDialog = true
                            }
                        ) { Icon(Icons.Default.StopCircle, null) }
                    }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            Column {

            }
            Row {
                Column {
                    viewModel.guildList.forEach {
                        Text(it.name)
                    }
                }

                Column {
                    viewModel.eventList.forEach {
                        Text(it.toString())
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageSender(viewModel: DiscordBotViewModel) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
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
                onClick = { viewModel.selectedGuild = null },
                leadingIcon = { RadioButton(selected = viewModel.selectedGuild == null, onClick = null) }
            )
            viewModel.guildList.forEach {
                DropdownMenuItem(
                    text = { Text(it.name) },
                    onClick = { viewModel.selectedGuild = it },
                    leadingIcon = { RadioButton(selected = it == viewModel.selectedGuild, onClick = null) }
                )
            }
        }
        Text(
            viewModel.selectedGuild?.name ?: "Select a Guild",
            modifier = Modifier.clickable { showGuildDropDown = true }
        )

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
            Text(
                viewModel.selectedChannel?.name ?: "Select a Channel",
                modifier = Modifier.clickable { showChannelDropDown = true }
            )
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