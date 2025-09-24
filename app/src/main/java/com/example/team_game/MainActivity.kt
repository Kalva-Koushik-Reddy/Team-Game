package com.example.team_game

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit // For SharedPreferences KTX
import com.example.team_game.ui.theme.TeamGameTheme
import org.json.JSONArray
import org.json.JSONObject

// SharedPreferences Keys
private const val PREFS_NAME = "TeamGamePrefs"
private const val TEAMS_KEY = "teams"
private const val CURRENT_NUMBER_OF_TEAMS_KEY = "current_number_of_teams"
private const val INITIAL_SETUP_DONE_KEY = "initial_setup_done"

data class Team(var name: String, var score: Int = 0)

class MainActivity : ComponentActivity() {

    private fun saveAppState(
        context: Context,
        teams: List<Team>,
        currentTeamCount: Int,
        isInitialSetupDone: Boolean
    ) {
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        sharedPreferences.edit { // Using KTX for conciseness
            val jsonArray = JSONArray()
            teams.forEach { team ->
                val jsonObject = JSONObject()
                jsonObject.put("name", team.name)
                jsonObject.put("score", team.score)
                jsonArray.put(jsonObject)
            }
            putString(TEAMS_KEY, jsonArray.toString())
            putInt(CURRENT_NUMBER_OF_TEAMS_KEY, currentTeamCount)
            putBoolean(INITIAL_SETUP_DONE_KEY, isInitialSetupDone)
        }
    }

    private fun loadAppState(
        context: Context,
        defaultTeamCount: Int
    ): Triple<List<Team>, Int, Boolean> {
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val currentTeamCount = sharedPreferences.getInt(CURRENT_NUMBER_OF_TEAMS_KEY, defaultTeamCount)
        val isInitialSetupDone = sharedPreferences.getBoolean(INITIAL_SETUP_DONE_KEY, false)
        val jsonString = sharedPreferences.getString(TEAMS_KEY, null)
        val teams = mutableListOf<Team>()

        if (jsonString != null && isInitialSetupDone) {
            try {
                val jsonArray = JSONArray(jsonString)
                for (i in 0 until minOf(jsonArray.length(), currentTeamCount)) {
                    val jsonObject = jsonArray.getJSONObject(i)
                    teams.add(Team(jsonObject.getString("name"), jsonObject.getInt("score")))
                }
                // If saved teams are fewer than currentTeamCount (e.g., count increased), add new default teams
                if (teams.size < currentTeamCount) {
                    for (i in teams.size until currentTeamCount) {
                        teams.add(Team("Team ${i + 1}"))
                    }
                }
            } catch (e: Exception) { // Handle potential JSON parsing errors
                teams.clear() // Clear any partially loaded teams
                for (i in 0 until currentTeamCount) { // Fallback to default teams
                    teams.add(Team("Team ${i + 1}"))
                }
            }
        } else if (!isInitialSetupDone) {
            // If initial setup wasn't completed, return defaults that trigger the setup dialog
            return Triple(emptyList(), defaultTeamCount, false)
        } else {
            // This case implies initial setup was done, but no teams data was found (e.g., error during save)
            // Fallback to default teams for the currentTeamCount
            for (i in 0 until currentTeamCount) {
                teams.add(Team("Team ${i + 1}"))
            }
        }
        // Ensure the returned list exactly matches currentTeamCount, handling cases where more teams might have been saved previously
        return Triple(teams.take(currentTeamCount), currentTeamCount, true)
    }

    @OptIn(ExperimentalMaterial3Api::class) // Needed for AlertDialog, OutlinedTextField etc.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val defaultNumberOfTeamsOnFirstLaunch = 2
        val (loadedTeams, loadedTeamCount, loadedInitialSetupDone) = loadAppState(
            this,
            defaultNumberOfTeamsOnFirstLaunch
        )

        enableEdgeToEdge()
        setContent {
            TeamGameTheme {
                var actualNumberOfTeams by remember { mutableIntStateOf(loadedTeamCount) }
                var teamsState by remember { mutableStateOf(loadedTeams) }
                var isInitialSetupDoneState by remember { mutableStateOf(loadedInitialSetupDone) }

                var showInitialSetupDialog by remember { mutableStateOf(!isInitialSetupDoneState) }
                var showChangeTeamCountDialog by remember { mutableStateOf(false) }
                var showRestartGameDialog by remember { mutableStateOf(false) }

                LaunchedEffect(teamsState, actualNumberOfTeams, isInitialSetupDoneState) {
                    saveAppState(
                        context = this@MainActivity,
                        teams = teamsState,
                        currentTeamCount = actualNumberOfTeams,
                        isInitialSetupDone = isInitialSetupDoneState
                    )
                }

                if (showInitialSetupDialog) {
                    NumberOfTeamsInputDialog(
                        title = "Setup Game",
                        confirmButtonText = "Start Game",
                        initialValue = actualNumberOfTeams.toString(),
                        onDismiss = {
                            // If initial setup is dismissed and no teams are set, create default teams
                            if (teamsState.isEmpty()) {
                                actualNumberOfTeams = defaultNumberOfTeamsOnFirstLaunch
                                teamsState = (1..defaultNumberOfTeamsOnFirstLaunch).map { Team("Team $it") }
                            }
                            isInitialSetupDoneState = true // Mark setup as done
                            showInitialSetupDialog = false
                        },
                        onConfirm = { count ->
                            actualNumberOfTeams = count
                            teamsState = (1..count).map { Team("Team $it") }
                            isInitialSetupDoneState = true
                            showInitialSetupDialog = false
                        }
                    )
                }

                if (showChangeTeamCountDialog) {
                    NumberOfTeamsInputDialog(
                        title = "Change Number of Teams",
                        confirmButtonText = "Confirm",
                        initialValue = actualNumberOfTeams.toString(),
                        onDismiss = { showChangeTeamCountDialog = false },
                        onConfirm = { newCount ->
                            val currentCount = actualNumberOfTeams
                            if (newCount > currentCount) {
                                val newTeamsToAdd = (currentCount + 1..newCount).map {
                                    Team("Team $it") // Name new teams sequentially
                                }
                                teamsState = teamsState + newTeamsToAdd
                            } else if (newCount < currentCount && newCount > 0) {
                                teamsState = teamsState.take(newCount) // Remove teams from the end
                            }
                            actualNumberOfTeams = newCount
                            showChangeTeamCountDialog = false
                        }
                    )
                }

                if (showRestartGameDialog) {
                    NumberOfTeamsInputDialog(
                        title = "Restart",
                        confirmButtonText = "Restart",
                        initialValue = defaultNumberOfTeamsOnFirstLaunch.toString(), // Suggest a default for restart
                        onDismiss = { showRestartGameDialog = false },
                        onConfirm = { count ->
                            actualNumberOfTeams = count
                            teamsState = (1..count).map { Team("Team $it") } // Reset all teams
                            isInitialSetupDoneState = true // Ensure setup is marked done
                            showRestartGameDialog = false
                        }
                    )
                }

                // Main UI: Only show if initial setup is complete and there are teams
                if (isInitialSetupDoneState && teamsState.isNotEmpty()) {
                    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                        Column(
                            modifier = Modifier
                                .padding(innerPadding)
                                .padding(horizontal = 8.dp, vertical = 16.dp)
                                .fillMaxSize()
                        ) {
                            val resetAllScores = {
                                teamsState = teamsState.map { it.copy(score = 0) }
                            }
                            val updateTeamScore = { teamToUpdate: Team, newScore: Int ->
                                teamsState = teamsState.map {
                                    if (it.name == teamToUpdate.name) it.copy(score = newScore) else it
                                }
                            }
                            val updateTeamName = { teamToUpdate: Team, newName: String ->
                                if (teamsState.none { it.name == newName && it.name != teamToUpdate.name }) {
                                    teamsState = teamsState.map {
                                        if (it.name == teamToUpdate.name) it.copy(name = newName) else it
                                    }
                                } else {
                                    println("Error: Team name '$newName' already exists.")
                                }
                            }

                            TeamHomePage(
                                teams = teamsState,
                                onResetAllScores = resetAllScores,
                                onTeamScoreChange = updateTeamScore,
                                onTeamNameChange = updateTeamName,
                                onChangeTeamCount = { showChangeTeamCountDialog = true },
                                onRestartGame = { showRestartGameDialog = true }
                            )
                        }
                    }
                } else if (!isInitialSetupDoneState && !showInitialSetupDialog) {
                    // This state indicates the app is waiting for initial setup.
                    // The showInitialSetupDialog should handle this.
                    // If it's dismissed without setup, this provides a fallback message.
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Button(onClick = { showInitialSetupDialog = true }) {
                            Text("Setup Game")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class) // For AlertDialog, OutlinedTextField
@Composable
fun NumberOfTeamsInputDialog(
    title: String,
    confirmButtonText: String,
    initialValue: String,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    var text by remember(initialValue) { mutableStateOf(initialValue) } // Key initialValue for recomposition if needed
    val keyboardController = LocalSoftwareKeyboardController.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = {
                    // Allow only up to 2 digits and ensure they are numbers
                    if (it.length <= 2 && it.all { char -> char.isDigit() }) {
                        text = it
                    }
                },
                label = { Text("Number of Teams") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = {
                    val count = text.toIntOrNull()
                    if (count != null && count > 0) {
                        onConfirm(count)
                        keyboardController?.hide()
                    }
                    // TODO: Provide visual feedback for invalid input if "Done" is pressed
                })
            )
        },
        confirmButton = {
            Button(onClick = {
                val count = text.toIntOrNull()
                if (count != null && count > 0) {
                    onConfirm(count)
                }
                // TODO: Provide visual feedback for invalid input on confirm button press
            }) { Text(confirmButtonText) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun TeamHomePage( // Removed @SuppressLint as BoxWithConstraints is no longer used
    teams: List<Team>,
    onResetAllScores: () -> Unit,
    onTeamScoreChange: (Team, Int) -> Unit,
    onTeamNameChange: (Team, String) -> Unit,
    onChangeTeamCount: () -> Unit,
    onRestartGame: () -> Unit
) {
    // This check is good, but MainActivity logic should prevent empty teams after setup.
    if (teams.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No teams configured. Please use game options to set up teams.")
        }
        return
    }

    var showResetAllDialog by remember { mutableStateOf(false) }
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp.dp

    // Define spacing and padding once
    val itemSpacing = 8.dp
    val horizontalGridPadding = 8.dp

    // Calculate item width for a 2-column grid
    val itemWidth = (screenWidthDp - (horizontalGridPadding * 2) - itemSpacing) / 2

    Column(modifier = Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .weight(1f) // Grid takes available space
                .fillMaxWidth(),
            contentPadding = PaddingValues(
                horizontal = horizontalGridPadding,
                vertical = 8.dp // Consistent vertical padding
            ),
            verticalArrangement = Arrangement.spacedBy(itemSpacing),
            horizontalArrangement = Arrangement.spacedBy(itemSpacing)
        ) {
            items(teams, key = { team -> team.name }) { team -> // Use team name as a key if they are unique
                Box(
                    modifier = Modifier
                        .width(itemWidth)
                        .height(itemWidth) // Enforce square aspect ratio
                ) {
                    TeamBox(
                        team = team,
                        onScoreChange = { newScore -> onTeamScoreChange(team, newScore) },
                        onNameChange = { newName -> onTeamNameChange(team, newName) }
                    )
                }
            }
        }

        // Action Buttons Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 4.dp), // Added horizontal padding for buttons
            horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterHorizontally), // Better spacing and centering
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Using weight to distribute space more evenly for buttons if needed, or fixed sizes
            Button(onClick = { showResetAllDialog = true }) { Text("Reset Scores") }
            Button(onClick = onChangeTeamCount) { Text("Change Teams") }
            Button(onClick = onRestartGame) { Text("Restart") }
        }
    }

    if (showResetAllDialog) {
        AlertDialog(
            onDismissRequest = { showResetAllDialog = false },
            title = { Text("Reset All Scores?") },
            text = { Text("Are you sure you want to reset all team scores to 0 (names will be kept)?") },
            confirmButton = {
                TextButton(onClick = {
                    onResetAllScores()
                    showResetAllDialog = false
                }) { Text("Confirm") }
            },
            dismissButton = {
                TextButton(onClick = { showResetAllDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class) // For AlertDialog, OutlinedTextField
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
            verticalArrangement = Arrangement.SpaceAround // Use SpaceAround for better vertical distribution
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Invisible Spacer to balance the IconButton, ensuring Text is truly centered
                Spacer(Modifier.size(25.dp)) // Match IconButton's tappable area

                Text(
                    text = team.name,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f) // Text takes all available space in the middle
                )

                IconButton(
                    onClick = { showEditNameDialog = true },
                    modifier = Modifier.size(25.dp) // Standard touch target size
                ) {
                    Icon(
                        Icons.Filled.Edit,
                        contentDescription = "Edit ${team.name}",
                        modifier = Modifier.size(20.dp) // Icon visual size
                    )
                }
            }

            Text(
                text = "Score: ${team.score}", // Read directly from team.score for consistency
                fontSize = 20.sp,
                textAlign = TextAlign.Center
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Make +/- buttons slightly smaller if needed, or ensure they fit
                Button(onClick = { onScoreChange(score - 1) }, modifier = Modifier.weight(1f).heightIn(min = 36.dp)) { Text("-") }
                Button(onClick = { onScoreChange(score + 1) }, modifier = Modifier.weight(1f).heightIn(min = 36.dp)) { Text("+") }
            }

            Button(
                onClick = { showResetDialogInBox = true },
                modifier = Modifier
                    .fillMaxWidth(0.8f) // Take 80% of width
                    .heightIn(min = 36.dp)
            ) {
                Text("Reset Score", fontSize = 12.sp)
            }
        }
    }

    // --- Dialogs for Reset Score and Edit Name ---
    if (showResetDialogInBox) {
        AlertDialog(
            onDismissRequest = { showResetDialogInBox = false },
            title = { Text("Reset Score for ${team.name}?") },
            text = { Text("Are you sure you want to reset the score for ${team.name} to 0?") },
            confirmButton = {
                TextButton(onClick = {
                    onScoreChange(0) // Directly update with the new score
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
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
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

@Preview(showBackground = true, widthDp = 380, heightDp = 700)
@Composable
fun DefaultPreview() {
    TeamGameTheme {
        // Using a more stable list for preview if state changes aren't directly tested here
        val sampleTeams = listOf(
            Team("The Dragons of the North", 5), Team("Team Beta", 10),
            Team("Strikers", 3), Team("Delta Force X", 8),
            Team("Victories Secret", 12), Team("Foxtrot", 2)
        )
        TeamHomePage(
            teams = sampleTeams,
            onResetAllScores = {},
            onTeamScoreChange = { _, _ -> },
            onTeamNameChange = { _, _ -> },
            onChangeTeamCount = {},
            onRestartGame = {}
        )
    }
}
