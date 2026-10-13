package com.example.financeapp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// Displays information about the TFinance application.
@Composable
fun AboutScreen(
    onBackClick: () -> Unit
) {
    // Applies the current theme's background and text colors.
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground
    ) {
        // Arranges the About page elements vertically.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(16.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Top
        ) {
            // Page heading.
            Text(
                text = "About TFinance",
                style = MaterialTheme.typography.headlineMedium
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 16.dp)
            )

            // Application name.
            Text(
                text = "TFinance",
                style = MaterialTheme.typography.headlineSmall
            )

            // Temporary version label.
            Text(
                text = "Version: Development",
                style = MaterialTheme.typography.bodyMedium
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 16.dp)
            )

            // Description of the application's purpose.
            Text(
                text = "TFinance is a personal finance management " +
                        "application designed to help users manage " +
                        "their accounts, transactions, and budgets.",
                modifier = Modifier.padding(vertical = 8.dp)
            )

            // Development attribution.
            Text(
                text = "Developed by the TFinance Team",
                modifier = Modifier.padding(top = 16.dp)
            )

            // Navigation callback to return to Settings.
            Button(
                onClick = onBackClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp)
            ) {
                Text("Back to Settings")
            }
        }
    }
}