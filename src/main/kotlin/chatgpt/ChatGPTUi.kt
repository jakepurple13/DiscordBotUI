@file:Suppress("INLINE_FROM_HIGHER_PLATFORM")

package chatgpt

import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.mikepenz.markdown.compose.Markdown
import com.mikepenz.markdown.model.markdownColor
import com.mikepenz.markdown.model.markdownTypography
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import moe.tlaster.precompose.viewmodel.ViewModel
import moe.tlaster.precompose.viewmodel.viewModel
import moe.tlaster.precompose.viewmodel.viewModelScope

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatGPTUi(chatGPTNetwork: ChatGPTNetwork) {
    val viewModel = viewModel { ChatGPTViewModel(chatGPTNetwork) }
    val listState = rememberLazyListState()

    LaunchedEffect(viewModel.messages.size) {
        if (viewModel.messages.isNotEmpty())
            listState.animateScrollToItem(viewModel.messages.lastIndex)
    }

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(viewModel.isLoading) {
        if (!viewModel.isLoading) {
            focusRequester.requestFocus()
        }
    }

    Scaffold(
        bottomBar = {
            Surface(
                color = BottomAppBarDefaults.containerColor,
                tonalElevation = BottomAppBarDefaults.ContainerElevation,
                modifier = Modifier.animateContentSize()
            ) {
                OutlinedTextField(
                    value = viewModel.message,
                    onValueChange = { viewModel.message = it },
                    label = { Text("Send a Message!") },
                    enabled = !viewModel.isLoading,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSearch = { viewModel.sendMessage() }
                    ),
                    trailingIcon = {
                        IconButton(
                            onClick = viewModel::sendMessage,
                            enabled = !viewModel.isLoading
                        ) { Icon(Icons.Default.Send, null) }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp)
                        .focusRequester(focusRequester)
                        .onPreviewKeyEvent {
                            if (it.key == Key.Enter && it.type == KeyEventType.KeyDown) {
                                if (it.isMetaPressed) {
                                    val value = viewModel.message.text + "\n"
                                    viewModel.message = TextFieldValue(
                                        text = value,
                                        selection = TextRange(value.length)
                                    )
                                } else {
                                    viewModel.sendMessage()
                                }
                                true
                            } else {
                                false
                            }
                        }
                )
            }
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
            ) { MessageCardContent(message) }
        }

        Role.Assistant -> {
            ElevatedCard(
                shape = RoundedCornerShape(12.0.dp).copy(bottomStart = CornerSize(4.dp)),
                modifier = modifier.wrapContentWidth()
            ) { MessageCardContent(message) }
        }
    }
}

@Composable
private fun MessageCardContent(
    message: Message,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(16.dp)
    ) {
        Text(
            message.role.name,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        SelectionContainer {
            Markdown(
                message.content,
                colors = markdownColor(
                    text = MaterialTheme.colorScheme.onSurface,
                    codeText = MaterialTheme.colorScheme.onSurface,
                    codeBackground = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                ),
                typography = markdownTypography(
                    h1 = MaterialTheme.typography.displayLarge,
                    h2 = MaterialTheme.typography.displayMedium,
                    h3 = MaterialTheme.typography.displaySmall,
                    h4 = MaterialTheme.typography.headlineMedium,
                    h5 = MaterialTheme.typography.headlineSmall,
                    h6 = MaterialTheme.typography.titleLarge,
                    text = MaterialTheme.typography.bodyLarge,
                    code = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    quote = MaterialTheme.typography.bodyMedium + SpanStyle(fontStyle = FontStyle.Italic),
                    paragraph = MaterialTheme.typography.bodyLarge,
                    ordered = MaterialTheme.typography.bodyLarge,
                    bullet = MaterialTheme.typography.bodyLarge,
                    list = MaterialTheme.typography.bodyLarge
                ),
                modifier = Modifier.wrapContentSize()
            )
        }
    }
}

private class ChatGPTViewModel(
    private val chatGPTNetwork: ChatGPTNetwork,
) : ViewModel() {

    var isLoading by mutableStateOf(false)
    var message by mutableStateOf(TextFieldValue())

    val messages = mutableStateListOf<Message>()

    fun sendMessage() {
        viewModelScope.launch(Dispatchers.IO) {
            isLoading = true
            messages.add(Message(Role.User, message.text))
            /*chatGPTNetwork.generate(message)
                .onSuccess { response ->
                    //messages.addAll(response.choices.map { it.message })
                    messages.add(
                        Message(
                            role = Role.Assistant,
                            content = response
                        )
                    )
                }
                .onFailure {
                    it.printStackTrace()
                    messages.add(
                        Message(
                            role = Role.Assistant,
                            content = "Something went wrong. Please try again."
                        )
                    )
                }*/
            chatGPTNetwork.chatCompletion(messages)
                .onSuccess { response ->
                    //messages.addAll(response.choices.map { it.message })
                    response.choices.forEach {
                        messages.add(
                            Message(
                                role = it["role"].toRole(),
                                content = it["content"].orEmpty()
                            )
                        )
                    }
                    /*messages.add(
                        Message(
                            role = Role.Assistant,
                            content = response
                        )
                    )*/
                }
                .onFailure {
                    it.printStackTrace()
                    messages.add(
                        Message(
                            role = Role.Assistant,
                            content = "Something went wrong. Please try again."
                        )
                    )
                }
            message = TextFieldValue("")
            isLoading = false
        }
    }
}

