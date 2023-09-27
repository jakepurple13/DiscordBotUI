@file:Suppress("INLINE_FROM_HIGHER_PLATFORM")

package stablediffusionui

import WindowWithBar
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CopyAll
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import moe.tlaster.precompose.viewmodel.ViewModel
import moe.tlaster.precompose.viewmodel.viewModel
import moe.tlaster.precompose.viewmodel.viewModelScope
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ModelSuggestionUI(
    onClose: () -> Unit,
) {
    WindowWithBar(
        onCloseRequest = onClose
    ) {
        val viewModel = viewModel { ModelSuggestionViewModel() }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Model Suggestions") },
                    actions = { Text("${viewModel.list.size}") }
                )
            }
        ) { padding ->
            LazyColumn(
                contentPadding = padding,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(viewModel.list) { model ->
                    ModelSuggestionItem(
                        modelSuggestion = model,
                        onDelete = { viewModel.deleteSuggestion(model) },
                        modifier = Modifier.animateItemPlacement()
                    )
                }
            }
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
            overlineContent = { Text(modelSuggestion.uuid) },
            headlineContent = {
                SelectionContainer { Text(modelSuggestion.link) }
            },
            supportingContent = { Text(modelSuggestion.reason) },
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
                    ) {
                        Icon(Icons.Default.Delete, null, tint = Color.Red)
                    }
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