package com.example.team_game

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import com.example.team_game.ui.theme.TeamGameTheme
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.sqrt

// Key for SharedPreferences
private const val PREFS_NAME = "TeamGamePrefs"
private const val TEAMS_KEY = "teams"
private const val INITIAL_NUMBER_OF_TEAMS_KEY = "initial_number_of_teams"


data class Team(val name: String, var score: Int = 0)

class MainActivity : ComponentActivity() {

    private fun saveTeams(context: Context, teams: List<Team>, initialTeamCount: Int) {
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        sharedPreferences.edit {
            val jsonArray = JSONArray()
            teams.forEach { team ->
                val jsonObject = JSONObject()
                jsonObject.put("name", team.name)
                jsonObject.put("score", team.score)
                jsonArray.put(jsonObject)
            }
            putString(TEAMS_KEY, jsonArray.toString())
            putInt(INITIAL_NUMBER_OF_TEAMS_KEY, initialTeamCount)
        }
    }

    private fun loadTeams(context: Context, defaultInitialTeamCount: Int): Pair<List<Team>, Int> {
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val savedInitialTeamCount = sharedPreferences.getInt(INITIAL_NUMBER_OF_TEAMS_KEY, defaultInitialTeamCount)
        val jsonString = sharedPreferences.getString(TEAMS_KEY, null)
        val teams = mutableListOf<Team>()

        if (jsonString != null) {
            try {
                val jsonArray = JSONArray(jsonString)
                for (i in 0 until jsonArray.length()) {
                    val jsonObject = jsonArray.getJSONObject(i)
                    teams.add(Team(jsonObject.getString("name"), jsonObject.getInt("score")))
                }
            } catch (e: Exception) {
                return Pair((1..savedInitialTeamCount).map { Team("Team $it") }, savedInitialTeamCount)
            }
        }

        if (teams.isEmpty()) {
            return Pair((1..savedInitialTeamCount).map { Team("Team $it") }, savedInitialTeamCount)
        }
        return Pair(teams, savedInitialTeamCount)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val defaultInitialNumberOfTeams = 2
        val (loadedTeamsInitial, loadedInitialTeamCountInitial) = loadTeams(this, defaultInitialNumberOfTeams)

        enableEdgeToEdge()
        setContent {
            TeamGameTheme {
                var initialNumberOfTeamsState by remember { mutableIntStateOf(loadedInitialTeamCountInitial) }
                var teamsState by remember { mutableStateOf(loadedTeamsInitial) }

                LaunchedEffect(teamsState, initialNumberOfTeamsState) {
                    saveTeams(this@MainActivity, teamsState, initialNumberOfTeamsState)
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val resetAllScores = { // Renamed for clarity
                        val newTeams = (1..initialNumberOfTeamsState).map { Team("Team $it") }
                        teamsState = newTeams
                    }

                    val updateTeamScore = { teamToUpdate: Team, newScore: Int ->
                        val newTeams = teamsState.map {
                            if (it.name == teamToUpdate.name) {
                                it.copy(score = newScore)
                            } else {
                                it
                            }
                        }
                        teamsState = newTeams
                    }

                    Box(modifier = Modifier.padding(innerPadding)) {
                        TeamHomePage(
                            teams = teamsState,
                            onResetAllScores = resetAllScores, // Pass the renamed function
                            onTeamScoreChange = updateTeamScore
                        )
                    }
                }
            }
        }
    }
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun TeamHomePage(
    teams: List<Team>,
    onResetAllScores: () -> Unit, // Updated parameter name
    onTeamScoreChange: (Team, Int) -> Unit
) {
    if (teams.isEmpty()) return

    var showResetAllDialog by remember { mutableStateOf(false) } // Renamed for clarity

    Column(modifier = Modifier.fillMaxSize()) {
        BoxWithConstraints(modifier = Modifier.weight(1f)) {
            val screenWidth = maxWidth
            val screenHeight = maxHeight
            val n = teams.size.toDouble()
            val sqrtN = sqrt(n)

            val cols1 = ceil(sqrtN).toInt()
            val rows1 = ceil(n / cols1).toInt()

            val rows2 = ceil(sqrtN).toInt()
            val cols2 = ceil(n / rows2).toInt()

            val screenAspectRatio = screenWidth / screenHeight
            val cellAspectRatio1 = (screenAspectRatio * rows1) / cols1
            val cellAspectRatio2 = (screenAspectRatio * rows2) / cols2

            val columnCount = if (abs(1 - cellAspectRatio1) < abs(1 - cellAspectRatio2)) {
                cols1
            } else {
                cols2
            }

            val teamRows = teams.chunked(columnCount)

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                teamRows.forEach { rowOfTeams ->
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        rowOfTeams.forEach { team ->
                            Box(modifier = Modifier.weight(1f)) {
                                TeamBox(
                                    team = team,
                                    onScoreChange = { newScore -> onTeamScoreChange(team, newScore) }
                                    // The individual reset is handled within TeamBox and calls onScoreChange with 0
                                )
                            }
                        }
                        repeat(columnCount - rowOfTeams.size) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        // Button to reset all scores
        Button(
            onClick = { showResetAllDialog = true },
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(16.dp)
        ) {
            Text("Reset All Scores") // Updated text
        }
    }

    if (showResetAllDialog) {
        AlertDialog(
            onDismissRequest = { showResetAllDialog = false },
            title = { Text("Reset All Scores?") }, // Updated title
            text = { Text("Are you sure you want to reset all team scores to 0?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onResetAllScores() // Call the function to reset all scores
                        showResetAllDialog = false
                    }
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showResetAllDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun TeamBox(team: Team, onScoreChange: (Int) -> Unit) {
    var score by remember(team.score) { mutableIntStateOf(team.score) }
    var showResetDialogInBox by remember { mutableStateOf(false) } // Dialog state for this specific box

    Card(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center // Center content vertically
        ) {
            Text(text = team.name, fontSize = 20.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Score: $score", fontSize = 24.sp)
            Spacer(modifier = Modifier.height(8.dp))

            // Row for +/- buttons
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(onClick = {
                    val newScore = score - 1
                    onScoreChange(newScore)
                }) {
                    Text("-")
                }
                Button(onClick = {
                    val newScore = score + 1
                    onScoreChange(newScore)
                }) {
                    Text("+")
                }
            }

            Spacer(modifier = Modifier.height(8.dp)) // Space before the reset button

            // Reset button for this specific team
            Button(onClick = { showResetDialogInBox = true }) {
                Text("Reset Score")
            }
        }
    }

    if (showResetDialogInBox) {
        AlertDialog(
            onDismissRequest = { showResetDialogInBox = false },
            title = { Text("Reset Score for ${team.name}?") },
            text = { Text("Are you sure you want to reset the score for ${team.name} to 0?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onScoreChange(0) // Call onScoreChange with 0 to reset this team's score
                        showResetDialogInBox = false
                    }
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showResetDialogInBox = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() { // Renamed Preview function for clarity
    TeamGameTheme {
        // Sample data for preview
        val sampleTeams = listOf(Team("Team Alpha", 5), Team("Team Beta", 10))
        var teamsState by remember { mutableStateOf(sampleTeams) }
        val initialTeamsState = 2

        val resetAllScores = {
            teamsState = (1..initialTeamsState).map { Team("Team $it") }
        }

        val updateTeamScore = { teamToUpdate: Team, newScore: Int ->
            teamsState = teamsState.map {
                if (it.name == teamToUpdate.name) {
                    it.copy(score = newScore)
                } else {
                    it
                }
            }
        }
        TeamHomePage(
            teams = teamsState,
            onResetAllScores = resetAllScores,
            onTeamScoreChange = updateTeamScore
        )
    }
}
