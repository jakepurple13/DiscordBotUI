import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.kord.core.entity.Guild
import dev.kord.core.entity.channel.GuildChannel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageRow(viewModel: DiscordBotViewModel) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .padding(horizontal = 8.dp)
            .fillMaxWidth()
            .animateContentSize()
    ) {
        var showGuildDropDown by remember { mutableStateOf(false) }
        ExposedBoxOptions(
            expanded = showGuildDropDown,
            onExpandedChange = { showGuildDropDown = it },
            currentValue = viewModel.selectedGuild?.name ?: "Select a Guild",
            options = viewModel.guildList,
            optionToString = { it.name },
            onClick = viewModel::selectGuild,
            label = "Selected Guild"
        )

        AnimatedVisibility(viewModel.channelList.isNotEmpty()) {
            var showChannelDropDown by remember { mutableStateOf(false) }
            ExposedBoxOptions(
                expanded = showChannelDropDown,
                onExpandedChange = { showChannelDropDown = it },
                currentValue = viewModel.selectedChannel?.name ?: "Select a Channel",
                options = viewModel.channelList,
                optionToString = { it.name },
                onClick = { viewModel.selectedChannel = it },
                label = "Selected Channel"
            )
        }
    }
}

@Composable
fun RowScope.ChannelTextBox(
    selectedGuild: Guild?,
    selectedChannel: GuildChannel?,
    sendMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = selectedGuild != null && selectedChannel != null,
        modifier = modifier
    ) {
        var message by remember { mutableStateOf("") }
        OutlinedTextField(
            value = message,
            onValueChange = { message = it },
            label = { Text("Send message to ${selectedChannel?.name} in the ${selectedGuild?.name} server") },
            trailingIcon = {
                IconButton(
                    onClick = {
                        sendMessage(message)
                        message = ""
                    }
                ) { Icon(Icons.Default.Send, null) }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> ExposedBoxOptions(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    currentValue: String,
    options: List<T>,
    optionToString: (T) -> String,
    onClick: (T?) -> Unit,
    label: String,
) {
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = onExpandedChange
    ) {
        OutlinedTextField(
            // The `menuAnchor` modifier must be passed to the text field for correctness.
            modifier = Modifier.menuAnchor(),
            readOnly = true,
            value = currentValue,
            onValueChange = {},
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.textFieldColors(),
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
        ) {
            options.forEach {
                DropdownMenuItem(
                    text = { Text(optionToString(it)) },
                    onClick = {
                        onClick(it)
                        onExpandedChange(false)
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
            DropdownMenuItem(
                text = { Text(label) },
                onClick = {
                    onClick(null)
                    onExpandedChange(false)
                },
                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
            )
        }
    }
}