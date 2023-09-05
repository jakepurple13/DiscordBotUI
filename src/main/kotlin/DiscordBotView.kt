import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kotlindiscord.kord.extensions.commands.events.PublicSlashCommandInvocationEvent
import com.kotlindiscord.kord.extensions.time.TimestampType
import com.kotlindiscord.kord.extensions.time.toDiscord
import dev.kord.core.event.message.MessageCreateEvent
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscordBotView(viewModel: DiscordBotViewModel) {
    val scope = rememberCoroutineScope()
    Scaffold(
        topBar = { TopAppBar(title = { Text("Events") }) },
        bottomBar = {
            Column {
                MessageRow(viewModel)
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
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(viewModel.eventList) {
                    var showMore by remember { mutableStateOf(false) }
                    SelectionContainer {
                        when (val event = it) {
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
                                                Text(event.message.timestamp.toDiscord(TimestampType.LongDateTime))
                                            }
                                        },
                                        headlineContent = {
                                            Text(
                                                event.message.content,
                                                maxLines = if (showMore) Int.MAX_VALUE else 3
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
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Divider()
                }
            }
        }
    }
}