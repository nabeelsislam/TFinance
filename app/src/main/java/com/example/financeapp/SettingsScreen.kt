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
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Switch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

// Displays the application's Settings screen.
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit
) {
    //Track whether the Dark Mode Switch is enabled
    var isDarkMode by remember{
        mutableStateOf(false)
    }
    //Main Container for the settings page
    Column(
        modifier = Modifier
            .fillMaxSize()
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
            text = "PREFERNCES",
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
            Switch(
                checked = isDarkMode,
                onCheckedChange = { newValue ->
                    isDarkMode = newValue
                }
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
        //Returns to the HomeScreen
        Button(
            onClick = onBackClick,
            modifier = Modifier.fillMaxWidth().padding(top=24.dp)
        ){
            Text("Back")
        }
    }
}