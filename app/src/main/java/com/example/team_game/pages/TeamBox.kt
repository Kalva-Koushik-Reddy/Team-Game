package com.example.team_game.pages

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.team_game.Team // Assuming Team.kt is in com.example.team_game

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamBox(
    team: Team,
    onScoreChange: (Int) -> Unit,
    onNameChange: (String) -> Unit
) {
    var score by remember(team.score) { mutableIntStateOf(team.score) }
    var showResetDialogInBox by remember { mutableStateOf(false) }
    var showEditNameDialog by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current

    Card(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceAround
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(Modifier.size(25.dp))
                Text(
                    text = team.name,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = { showEditNameDialog = true },
                    modifier = Modifier.size(25.dp)
                ) {
                    Icon(
                        Icons.Filled.Edit,
                        contentDescription = "Edit ${team.name}",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Text(
                text = "Score: ${team.score}",
                fontSize = 20.sp,
                textAlign = TextAlign.Center
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(onClick = { onScoreChange(score - 1) }, modifier = Modifier.weight(1f).heightIn(min = 36.dp)) { Text("-") }
                Button(onClick = { onScoreChange(score + 1) }, modifier = Modifier.weight(1f).heightIn(min = 36.dp)) { Text("+") }
            }
            Button(
                onClick = { showResetDialogInBox = true },
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .heightIn(min = 36.dp)
            ) {
                Text("Reset Score", fontSize = 12.sp)
            }
        }
    }

    if (showResetDialogInBox) {
        AlertDialog(
            onDismissRequest = { showResetDialogInBox = false },
            title = { Text("Reset Score for ${team.name}?") },
            text = { Text("Are you sure you want to reset the score for ${team.name} to 0?") },
            confirmButton = {
                TextButton(onClick = {
                    onScoreChange(0)
                    showResetDialogInBox = false
                }) { Text("Confirm") }
            },
            dismissButton = { TextButton(onClick = { showResetDialogInBox = false }) { Text("Cancel") } }
        )
    }
    if (showEditNameDialog) {
        var newNameInput by remember(team.name) { mutableStateOf(team.name) }
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            title = { Text("Edit Team Name") },
            text = {
                OutlinedTextField(
                    value = newNameInput,
                    onValueChange = { newNameInput = it },
                    label = { Text("New Name") },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        if (newNameInput.isNotBlank()) {
                            onNameChange(newNameInput)
                        }
                        showEditNameDialog = false
                        keyboardController?.hide()
                    })
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newNameInput.isNotBlank()) {
                        onNameChange(newNameInput)
                    }
                    showEditNameDialog = false
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showEditNameDialog = false }) { Text("Cancel") } }
        )
    }
}
