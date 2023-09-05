import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kotlindiscord.kord.extensions.commands.events.PublicSlashCommandInvocationEvent
import dev.kord.core.event.message.MessageCreateEvent
import kotlinx.coroutines.launch
import moe.tlaster.precompose.flow.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscordBotView(
    viewModel: DiscordBotViewModel,
    onShowSettings: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val scrollToBottom by DataStore.scrollToBottom.collectAsStateWithLifecycle(true)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Events") },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.showBotScreen = false },
                        enabled = viewModel.bot == null
                    ) { Icon(Icons.Default.ArrowBack, null) }
                },
            )
        },
        bottomBar = {
            Column {
                Surface(
                    modifier = Modifier.fillMaxWidth()
                ) { MessageRow(viewModel) }
                BottomAppBar(
                    actions = {
                        IconButton(onClick = onShowSettings) { Icon(Icons.Default.Settings, null) }
                    },
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

                        if (viewModel.bot == null) {
                            FloatingActionButton(
                                onClick = { scope.launch { viewModel.startBot() } }
                            ) { Icon(Icons.Default.PlayCircle, null) }
                        } else {
                            FloatingActionButton(
                                onClick = { showStopDialog = true }
                            ) { Icon(Icons.Default.StopCircle, null) }
                        }
                    }
                )
            }
        }
    ) { padding ->
        LaunchedEffect(viewModel.eventList.size, scrollToBottom) {
            if (scrollToBottom) listState.animateScrollToItem(viewModel.eventList.lastIndex)
        }
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            state = listState,
            contentPadding = padding
        ) {
            items(
                viewModel.eventList,
                key = { it.toString() }
            ) {
                SelectionContainer {
                    when (it) {
                        is EventType.KordEvent -> {
                            var showMore by remember { mutableStateOf(false) }
                            when (val event = it.event) {
                                is PublicSlashCommandInvocationEvent -> {
                                    OutlinedCard(
                                        onClick = { showMore = !showMore },
                                        modifier = Modifier.animateContentSize()
                                    ) {
                                        ListItem(
                                            headlineContent = {
                                                Text(
                                                    event.toString(),
                                                    maxLines = if (showMore) Int.MAX_VALUE else 3
                                                )
                                            },
                                            trailingContent = {
                                                Icon(
                                                    if (showMore) Icons.Default.ArrowDropUp
                                                    else Icons.Default.ArrowDropDown,
                                                    null
                                                )
                                            }
                                        )
                                    }
                                }

                                is MessageCreateEvent -> {
                                    OutlinedCard(
                                        onClick = { showMore = !showMore },
                                        modifier = Modifier.animateContentSize()
                                    ) {
                                        ListItem(
                                            overlineContent = {
                                                Column {
                                                    Text(event.member?.effectiveName.orEmpty())
                                                    Text(simpleDateTimeFormatter.format(event.message.timestamp.toEpochMilliseconds()))
                                                }
                                            },
                                            headlineContent = {
                                                Text(
                                                    event.message.content,
                                                    maxLines = if (showMore) Int.MAX_VALUE else 3
                                                )
                                            },
                                            trailingContent = {
                                                Icon(
                                                    if (showMore) Icons.Default.ArrowDropUp
                                                    else Icons.Default.ArrowDropDown,
                                                    null
                                                )
                                            }
                                        )
                                    }
                                }

                                else -> {
                                    Card(
                                        onClick = { showMore = !showMore },
                                        modifier = Modifier.animateContentSize()
                                    ) {
                                        ListItem(
                                            headlineContent = { Text(event::class.toString()) },
                                            supportingContent = {
                                                Text(
                                                    event.toString(),
                                                    maxLines = if (showMore) Int.MAX_VALUE else 3
                                                )
                                            },
                                            trailingContent = {
                                                Icon(
                                                    if (showMore) Icons.Default.ArrowDropUp
                                                    else Icons.Default.ArrowDropDown,
                                                    null
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        is EventType.Running -> {
                            ElevatedCard {
                                ListItem(
                                    overlineContent = { Text(it.timestamp) },
                                    headlineContent = { Text("Starting") }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}