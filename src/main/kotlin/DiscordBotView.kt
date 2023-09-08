import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.LocalScrollbarStyle
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
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
    val scrollToBottom by DataStore.scrollToBottom.flow.collectAsStateWithLifecycle(true)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Events") },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.botState = BotState.TokenSetup },
                        enabled = viewModel.bot == null
                    ) { Icon(Icons.Default.ArrowBack, null) }
                },
                actions = {
                    IconButton(onClick = onShowSettings) { Icon(Icons.Default.Settings, null) }
                }
            )
        },
        bottomBar = {
            Column {
                Surface(
                    modifier = Modifier.fillMaxWidth()
                ) { MessageRow(viewModel) }
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

                        val action: () -> Unit by rememberUpdatedState(
                            if (viewModel.bot == null) {
                                { scope.launch { viewModel.startBot() } }
                            } else {
                                { showStopDialog = true }
                            }
                        )

                        ExtendedFloatingActionButton(
                            onClick = action,
                            icon = {
                                Icon(
                                    if (viewModel.bot == null) Icons.Default.PlayCircle
                                    else Icons.Default.StopCircle,
                                    null
                                )
                            },
                            text = {
                                Text(
                                    if (viewModel.bot == null) "Start Bot"
                                    else "Stop Bot"
                                )
                            },
                            expanded = listState.isScrollingUp()
                        )
                    }
                )
            }
        }
    ) { padding ->
        LaunchedEffect(viewModel.eventList.size, scrollToBottom) {
            if (scrollToBottom && viewModel.eventList.isNotEmpty())
                listState.animateScrollToItem(viewModel.eventList.lastIndex)
        }
        Box(
            modifier = Modifier.padding(padding)
        ) {
            SelectionContainer {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    state = listState,
                ) {
                    items(viewModel.eventList) {

                        when (it) {
                            is EventType.KordEvent -> {
                                var showMore by remember { mutableStateOf(false) }
                                when (val event = it.event) {
                                    is GuildChatInputCommandInteractionCreateEvent -> {
                                        OutlinedCard(
                                            onClick = { showMore = !showMore },
                                            modifier = Modifier.animateContentSize()
                                        ) {
                                            ListItem(
                                                overlineContent = {
                                                    Column {
                                                        Text("Username: " + event.interaction.user.username)
                                                        Text("Global Name: " + event.interaction.user.globalName.orEmpty())
                                                        Text("Effective Name: " + event.interaction.user.effectiveName)
                                                    }
                                                },
                                                headlineContent = { Text("Command: " + event.interaction.invokedCommandName) },
                                                supportingContent = {
                                                    Text(
                                                        event.interaction.data.data
                                                            .options
                                                            .value
                                                            .orEmpty()
                                                            .joinToString("\n\n") {
                                                                "${it.name} = ${it.value.value?.value}"
                                                            },
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
                                                        Column {
                                                            Text("Username: " + event.member?.username)
                                                            Text("Global Name: " + event.member?.globalName.orEmpty())
                                                            Text("Effective Name: " + event.member?.effectiveName)
                                                            Text(simpleDateTimeFormatter.format(event.message.timestamp.toEpochMilliseconds()))
                                                        }
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
            VerticalScrollbar(
                adapter = rememberScrollbarAdapter(listState),
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