@file:Suppress("INLINE_FROM_HIGHER_PLATFORM")

package chatgpt

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalScrollbarStyle
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import moe.tlaster.precompose.viewmodel.ViewModel
import moe.tlaster.precompose.viewmodel.viewModel
import moe.tlaster.precompose.viewmodel.viewModelScope

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatGPTUi(chatGPTNetwork: ChatGPTNetwork) {
    val viewModel = viewModel { ChatGPTViewModel(chatGPTNetwork) }
    val listState = rememberLazyListState()

    Scaffold(
        bottomBar = {
            BottomAppBar(
                actions = {
                    OutlinedTextField(
                        value = viewModel.message,
                        onValueChange = { viewModel.message = it },
                        label = { Text("Send a Message!") },
                        enabled = !viewModel.isLoading,
                        modifier = Modifier
                            .fillMaxWidth(0.75f)
                            .padding(bottom = 6.dp, start = 4.dp)
                    )
                },
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = viewModel::sendMessage,
                    ) { Icon(Icons.Default.Send, null) }
                }
            )
        }
    ) { padding ->
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
                items(viewModel.messages) { model ->
                    Column(
                        horizontalAlignment = when (model.role) {
                            Role.User -> Alignment.End
                            Role.Assistant -> Alignment.Start
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        MessageCard(
                            message = model,
                            modifier = Modifier.animateItemPlacement()
                        )
                    }
                }
                if (viewModel.isLoading) {
                    item {
                        CircularProgressIndicator()
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

@Composable
private fun MessageCard(
    message: Message,
    modifier: Modifier = Modifier,
) {
    when (message.role) {
        Role.User -> {
            OutlinedCard(
                shape = RoundedCornerShape(12.0.dp).copy(bottomEnd = CornerSize(4.dp)),
                modifier = modifier.wrapContentWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        message.role.name,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        message.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Role.Assistant -> {
            Card(
                shape = RoundedCornerShape(12.0.dp).copy(bottomStart = CornerSize(4.dp)),
                modifier = modifier.wrapContentWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        message.role.name,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        message.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

private class ChatGPTViewModel(
    private val chatGPTNetwork: ChatGPTNetwork,
) : ViewModel() {

    var isLoading by mutableStateOf(false)
    var message by mutableStateOf("")

    val messages = mutableStateListOf<Message>()

    fun sendMessage() {
        viewModelScope.launch {
            isLoading = true
            messages.add(Message(Role.User, message))
            chatGPTNetwork.chatCompletion(message)
                .onSuccess { response ->
                    messages.addAll(response.choices.map { it.message })
                }
                .onFailure {

                }
            message = ""
            isLoading = false
        }
    }
}

