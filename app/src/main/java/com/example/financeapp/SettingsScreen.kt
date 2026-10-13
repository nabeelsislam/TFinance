
package com.example.financeapp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.MetadataChanges

// Allows this function to use experimental Material 3 dropdown components.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,

    // Receives the Dark Mode state from MainActivity.
    isDarkMode: Boolean,

    // Notifies MainActivity when Dark Mode changes.
    onDarkModeChange: (Boolean) -> Unit,

    // Notifies MainActivity when About TFinance is clicked.
    onAboutClick: () -> Unit,

    //Currency state supplied by MainActivity
    selectedCurrency: String,

    //Callback invoked when the currency changes.
    onCurrencyChange: (String) -> Unit
) {


    // Controls whether the dropdown menu is expanded.
    var isCurrencyMenuExpanded by remember {
        mutableStateOf(false)
    }

    // Read-only list containing the available currencies.
    val currencies = listOf("USD", "EUR", "JPY", "GBP")

    // Provides the correct background and foreground colors.
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground
    ) {

        // Main container for the Settings page.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),

            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Top
        ) {

            // Settings page heading.
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 16.dp)
            )

            // Preferences section.
            Text(
                text = "PREFERENCES",
                style = MaterialTheme.typography.titleMedium
            )

            // Places Dark Mode label and switch horizontally.
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text("Dark Mode")

                // Pushes the switch to the right.
                Spacer(modifier = Modifier.weight(1f))

                // Sends switch changes back to MainActivity.
                Switch(
                    checked = isDarkMode,
                    onCheckedChange = onDarkModeChange
                )
            }

            // Currency dropdown menu.
            ExposedDropdownMenuBox(
                expanded = isCurrencyMenuExpanded,
                onExpandedChange = {
                    isCurrencyMenuExpanded = !isCurrencyMenuExpanded
                }
            ) {

                // Displays the currently selected currency.
                OutlinedTextField(
                    value = selectedCurrency,

                    // No manual text editing is allowed.
                    onValueChange = {},
                    readOnly = true,

                    label = {
                        Text("Currency")
                    },

                    // Displays the dropdown arrow.
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(
                            expanded = isCurrencyMenuExpanded
                        )
                    },

                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )

                // Displays the list of available currencies.
                ExposedDropdownMenu(
                    expanded = isCurrencyMenuExpanded,
                    onDismissRequest = {
                        isCurrencyMenuExpanded = false
                    }
                ) {

                    // Creates one menu item per currency.
                    currencies.forEach { currency ->

                        DropdownMenuItem(
                            text = {
                                Text(currency)
                            },

                            onClick = {
                                // Tell MainActivity which currency was selected.
                                onCurrencyChange(currency)

                                // Close the dropdown menu.
                                isCurrencyMenuExpanded = false
                            }
                        )
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 16.dp)
            )

            // Security section.
            Text(
                text = "SECURITY",
                style = MaterialTheme.typography.titleMedium
            )

            // Placeholder for a future Change Password feature.
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

            // Opens the About TFinance screen.
            Button(
                onClick = onAboutClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Text("About TFinance")
            }

            // Returns to the Home screen.
            Button(
                onClick = onBackClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp)
            ) {
                Text("Back")
            }

        } // Closes Column
    } // Closes Surface
} // Closes SettingsScreen
