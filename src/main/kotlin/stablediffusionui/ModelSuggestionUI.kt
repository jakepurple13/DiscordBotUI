@file:Suppress("INLINE_FROM_HIGHER_PLATFORM")

package stablediffusionui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalScrollbarStyle
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CopyAll
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import moe.tlaster.precompose.viewmodel.ViewModel
import moe.tlaster.precompose.viewmodel.viewModel
import moe.tlaster.precompose.viewmodel.viewModelScope
import simpleDateTimeFormatter
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ModelSuggestionUI() {
    val viewModel = viewModel { ModelSuggestionViewModel() }
    val listState = rememberLazyListState()

    Scaffold { padding ->
        Box(
            modifier = Modifier.padding(padding)
        ) {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(horizontal = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .padding(vertical = 4.dp)
                    .fillMaxSize()
            ) {
                items(viewModel.list) { model ->
                    ModelSuggestionItem(
                        modelSuggestion = model,
                        onDelete = { viewModel.deleteSuggestion(model) },
                        modifier = Modifier.animateItemPlacement()
                    )
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

@Composable
private fun ModelSuggestionItem(
    modelSuggestion: ModelSuggestion,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Are you sure you want to delete this?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete()
                    }
                ) { Text("Yes") }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false }
                ) { Text("No") }
            }
        )
    }

    OutlinedCard(
        modifier = modifier
    ) {
        ListItem(
            overlineContent = {
                Column {
                    Text(simpleDateTimeFormatter.format(modelSuggestion.timestamp))
                    Text(modelSuggestion.suggestedBy.ifBlank { modelSuggestion.uuid })
                }
            },
            headlineContent = {
                SelectionContainer { Text(modelSuggestion.link) }
            },
            supportingContent = {
                Column {
                    Text("Server: ${modelSuggestion.server}")
                    Text("Channel: ${modelSuggestion.channel}")
                    SelectionContainer { modelSuggestion.lastMessageUrl?.let { Text(it) } }
                    Divider()
                    Text(modelSuggestion.reason)
                }
            },
            leadingContent = {
                KamelImage(
                    resource = asyncPainterResource(modelSuggestion.avatar.orEmpty()),
                    contentDescription = null,
                    onFailure = { Icon(Icons.Default.AccountCircle, null) },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                )
            },
            trailingContent = {
                Column {
                    IconButton(
                        onClick = {
                            Toolkit.getDefaultToolkit().also {
                                it
                                    .systemClipboard
                                    .setContents(StringSelection(modelSuggestion.link), null)
                                it.beep()
                            }
                        }
                    ) { Icon(Icons.Default.CopyAll, null) }
                    IconButton(
                        onClick = { showDeleteDialog = true }
                    ) { Icon(Icons.Default.Delete, null, tint = Color.Red) }
                }
            }
        )
    }
}

private class ModelSuggestionViewModel(
    private val database: ModelSuggesterDatabase = ModelSuggesterDatabase.createInstance(),
) : ViewModel() {
    val list = mutableStateListOf<ModelSuggestion>()

    init {
        database.getSuggestions()
            .onEach {
                list.clear()
                list.addAll(it.list)
            }
            .launchIn(viewModelScope)
    }

    fun deleteSuggestion(modelSuggestion: ModelSuggestion) {
        viewModelScope.launch { database.removeSuggestion(modelSuggestion.uuid) }
    }
}