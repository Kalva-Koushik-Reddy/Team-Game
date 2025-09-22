package com.example.team_game

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.team_game.ui.theme.TeamGameTheme
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.sqrt

data class Team(val name: String, val score: Int = 0)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TeamGameTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // --- Configuration ---
                    // Change this value to set the number of teams.
                    val numberOfTeams = 3
                    val teams = remember { (1..numberOfTeams).map { Team("Team $it") } }
                    // --- End Configuration ---

                    Box(modifier = Modifier.padding(innerPadding)) {
                        TeamHomePage(teams = teams)
                    }
                }
            }
        }
    }
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun TeamHomePage(teams: List<Team>) {
    if (teams.isEmpty()) return

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val screenWidth = maxWidth
        val screenHeight = maxHeight
        val n = teams.size.toDouble()
        val sqrtN = sqrt(n)

        // Possibility 1: A "wide" grid layout
        val cols1 = ceil(sqrtN).toInt()
        val rows1 = ceil(n / cols1).toInt()

        // Possibility 2: A "tall" grid layout
        val rows2 = ceil(sqrtN).toInt()
        val cols2 = ceil(n / rows2).toInt()

        // Determine which layout results in cells closer to a square
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
            verticalArrangement = Arrangement.spacedBy(4.dp) // Adds space between rows
        ) {
            teamRows.forEach { rowOfTeams ->
                Row(
                    modifier = Modifier.weight(1f), // Each Row takes up equal vertical space
                    horizontalArrangement = Arrangement.spacedBy(4.dp) // Adds space between columns
                ) {
                    rowOfTeams.forEach { team ->
                        // The weight modifier ensures each team box gets an equal share of the width.
                        Box(modifier = Modifier.weight(1f)) {
                            TeamBox(team = team, onScoreChange = { newScore ->
                                println("${team.name}'s score is now $newScore")
                            })
                        }
                    }
                    // Add spacers to fill the rest of the row if it's not full.
                    // This keeps the grid structure consistent.
                    repeat(columnCount - rowOfTeams.size) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun TeamBox(team: Team, onScoreChange: (Int) -> Unit) {
    // This state is managed locally within the TeamBox.
    var score by remember { mutableIntStateOf(team.score) }

    // Card now fills the entire space provided by the parent Box.
    Card(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .padding(8.dp) // Internal padding for the content
                .fillMaxSize(), // Center content within the card
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = team.name, fontSize = 20.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Score: $score", fontSize = 24.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(onClick = {
                    score--
                    onScoreChange(score)
                }) {
                    Text("-")
                }
                Button(onClick = {
                    score++
                    onScoreChange(score)
                }) {
                    Text("+")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TeamHomePagePreview() {
    TeamGameTheme {
        val teams = listOf(
            Team("Team 1"),
            Team("Team 2"),
            Team("Team 3"),
            Team("Team 4"),
            Team("Team 5")
        )
        TeamHomePage(teams = teams)
    }
}
