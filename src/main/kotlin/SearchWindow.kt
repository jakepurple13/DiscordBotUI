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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import dev.kord.core.event.message.MessageCreateEvent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchWindow(
    viewModel: DiscordBotViewModel,
    onClose: () -> Unit,
) {
    WindowWithBar(
        onCloseRequest = onClose
    ) {
        val listState = rememberLazyListState()

        var searchQuery by remember { mutableStateOf("") }
        val items by remember {
            derivedStateOf {
                viewModel.eventList.filter {
                    when (it) {
                        is EventType.KordEvent -> {
                            when (val event = it.event) {
                                is GuildChatInputCommandInteractionCreateEvent -> event.interaction.data.message.value?.content.orEmpty()
                                is MessageCreateEvent -> event.message.content
                                else -> event.toString()
                            }.contains(searchQuery, true)
                        }

                        is EventType.Running -> false
                    }
                }
            }
        }

        Scaffold(
            topBar = {
                SearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    onSearch = {},
                    active = false,
                    onActiveChange = {},
                    placeholder = { Text("Search for messages") },
                    modifier = Modifier.fillMaxWidth()
                ) {}
            },
        ) { padding ->
            Box(
                modifier = Modifier.padding(padding)
            ) {
                SelectionContainer {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        state = listState,
                    ) {
                        items(items) {
                            when (it) {
                                is EventType.KordEvent -> {
                                    var showMore by remember { mutableStateOf(false) }
                                    when (val event = it.event) {
                                        is GuildChatInputCommandInteractionCreateEvent -> {
                                            GuildChatInputCommandInteractionCreateEventItem(
                                                event = event,
                                                showMore = showMore,
                                                onShowMoreChange = { showMore = it }
                                            )
                                        }

                                        is MessageCreateEvent -> {
                                            MessageCreateEventItem(
                                                event = event,
                                                showMore = showMore,
                                                onShowMoreChange = { showMore = it }
                                            )
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
}