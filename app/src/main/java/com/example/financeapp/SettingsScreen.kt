package com.example.financeapp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.Switch
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.statusBarsPadding

// Displays the application's Settings screen.
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,

    // Receives the Dark Mode state from MainActivity.
    isDarkMode: Boolean,

    // Sends switch changes back to MainActivity.
    onDarkModeChange: (Boolean) -> Unit
) {
    //Surface providers  the themed background and
    //the corresponding foreground content colors
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground
    ) {
        //Main Container for the settings page
        Column(
            modifier = Modifier
                //Makes the Colum fill the available screen
                .fillMaxSize()

                // Moves the content below the Android status bar.
                .statusBarsPadding()

                //Adds spacing around the content
                .padding(16.dp),

            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Top
        ) {

        //Setings Page Heading
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium
        )

        //Top Border
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 16.dp)
        )

        //Preferences Section
        Text(
            text = "PREFERENCES",
            style = MaterialTheme.typography.titleMedium
        )
        // Places the label and switch horizontally.
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {

            // Label describing the setting.
            Text("Dark Mode")

            // Takes up the remaining horizontal space.
            Spacer(modifier = Modifier.weight(1f))

            // Switch controlled by our state variable.
            // Displays the current state from MainActivity.
            // Calls the parent callback when the user changes the switch.
            Switch(
                checked = isDarkMode,
                onCheckedChange = onDarkModeChange
            )
        }

        Text(
            text = "Currency",
            modifier = Modifier.padding(vertical = 8.dp)
        )

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 16.dp)
        )

        // Security section.
        Text(
            text = "SECURITY",
            style = MaterialTheme.typography.titleMedium
        )

        Text(
            text = "Change Password",
            modifier = Modifier.padding(vertical = 8.dp)
        )

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 16.dp)
        )

        // About section.
        Text(
            text = "ABOUT",
            style = MaterialTheme.typography.titleMedium
        )

        Text(
            text = "About TFinance",
            modifier = Modifier.padding(vertical = 8.dp)
        )
            // Returns to the HomeScreen.
            Button(
                onClick = onBackClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp)
            ) {
                Text("Back")
            }

        } // Closes Column
    }     // Closes Surface
}         // Closes SettingsScreen function