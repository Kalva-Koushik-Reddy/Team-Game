package com.example.team_game

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.team_game.pages.TeamHomePage
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import com.example.team_game.ui.theme.TeamGameTheme
import org.json.JSONArray
import org.json.JSONObject

// SharedPreferences Keys
private const val PREFS_NAME = "TeamGamePrefs"
private const val TEAMS_KEY = "teams"
private const val CURRENT_NUMBER_OF_TEAMS_KEY = "current_number_of_teams"
private const val INITIAL_SETUP_DONE_KEY = "initial_setup_done"

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NumberOfTeamsInputDialog(
    title: String,
    confirmButtonText: String,
    initialValue: String,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    var text by remember(initialValue) { mutableStateOf(initialValue) }
    val keyboardController = LocalSoftwareKeyboardController.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = {
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
                })
            )
        },
        confirmButton = {
            Button(onClick = {
                val count = text.toIntOrNull()
                if (count != null && count > 0) {
                    onConfirm(count)
                }
            }) { Text(confirmButtonText) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Preview(showBackground = true, widthDp = 380, heightDp = 700)
@Composable
fun DefaultPreview() {
    TeamGameTheme {
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
