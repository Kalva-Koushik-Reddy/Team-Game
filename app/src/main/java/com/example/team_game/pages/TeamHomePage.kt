package com.example.team_game.pages


import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.example.team_game.Team
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
@Composable
private fun getColumnCount(): Int { // Made private as it's specific to TeamHomePage's logic here
    val configuration = LocalConfiguration.current
    return if (configuration.screenWidthDp < 600) {
        2 // Phones
    } else {
        5 // Tablets
    }
}

@Composable
fun TeamHomePage(
    teams: List<Team>,
    onResetAllScores: () -> Unit,
    onTeamScoreChange: (Team, Int) -> Unit,
    onTeamNameChange: (Team, String) -> Unit,
    onChangeTeamCount: () -> Unit,
    onRestartGame: () -> Unit
) {
    if (teams.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No teams configured. Please use game options to set up teams.")
        }
        return
    }

    var showResetAllDialog by remember { mutableStateOf(false) }
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp.dp

    val columnCount = getColumnCount()

    val itemSpacing = 8.dp
    val horizontalGridPadding = 8.dp

    val totalSpacing = itemSpacing * (columnCount - 1)
    val totalHorizontalPadding = horizontalGridPadding * 2
    val itemWidth = (screenWidthDp - totalHorizontalPadding - totalSpacing) / columnCount
    val itemHeight = itemWidth

    Column(modifier = Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(columnCount),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(
                horizontal = horizontalGridPadding,
                vertical = 8.dp
            ),
            verticalArrangement = Arrangement.spacedBy(itemSpacing),
            horizontalArrangement = Arrangement.spacedBy(itemSpacing)
        ) {
            items(teams, key = { team -> team.name }) { team ->
                Box(
                    modifier = Modifier
                        .width(itemWidth)
                        .height(itemHeight)
                ) {
                    TeamBox(
                        team = team,
                        onScoreChange = { newScore -> onTeamScoreChange(team, newScore) },
                        onNameChange = { newName -> onTeamNameChange(team, newName) }
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
