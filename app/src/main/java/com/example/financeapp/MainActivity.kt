package com.example.financeapp

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.financeapp.ui.theme.FinanceAppTheme
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FinanceAppTheme(dynamicColor = false) {
                //Tracks whether the user is logged in
                var isLoggedIn by remember { mutableStateOf(false) }

                //Tracks whether the settings screen is open
                var isSettingsOpen by remember {mutableStateOf(false) }


                if (!isLoggedIn) {
                    //Show login when the user is not logged in
                    LoginScreen(onLoginClick = { isLoggedIn = true })
                }
                else if(isSettingsOpen){
                    //Show Settings when the user opens is
                    SettingsScreen(onBackClick = {isSettingsOpen = false})
                } else {
                    HomeScreen(onLogout = { isSettingsOpen = false
                        isLoggedIn = false },
                        onSettingsClick = {
                            isSettingsOpen = true
                        })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Finance App - Login") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Welcome Back",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = username,
                onValueChange = {
                    username = it
                    errorMessage = null
                },
                label = { Text("Username") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    errorMessage = null
                },
                label = { Text("Password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (isLoading) {
                CircularProgressIndicator()
            } else {
                Button(
                    onClick = {
                        val inputUsername = username.trim()
                        val inputPassword = password.trim()

                        if (inputUsername.isEmpty() || inputPassword.isEmpty()) {
                            errorMessage = "Please enter both username and password"
                            return@Button
                        }

                        isLoading = true
                        errorMessage = null

                        // Check Firestore "login" collection for matching "Username" and "Password"
                        val db = FirebaseFirestore.getInstance()
                        db.collection("login")
                            .whereEqualTo("Username", inputUsername)
                            .whereEqualTo("Password", inputPassword)
                            .get()
                            .addOnSuccessListener { querySnapshot ->
                                isLoading = false
                                if (!querySnapshot.isEmpty) {
                                    Toast.makeText(context, "Login Successful!", Toast.LENGTH_SHORT).show()
                                    onLoginClick()
                                } else {
                                    errorMessage = "Invalid Username or Password"
                                }
                            }
                            .addOnFailureListener { exception ->
                                isLoading = false
                                errorMessage = "Database Error: ${exception.localizedMessage}"
                            }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Login")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onLogout: () -> Unit = {},
    //
    onSettingsClick: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Finance App") },
                actions = {
                    Button(
                        onClick = onLogout,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("Logout")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { /* TODO: Handle View Accounts */ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("View Accounts")
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = { /* TODO: Handle Transactions */ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Transactions")
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = { /* TODO: Handle Transfer Money */ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Transfer Money")
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = { /* TODO: Handle Budget & Analytics */ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Budget & Analytics")
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onSettingsClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Settings")
            }
        }
    }
}



@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    FinanceAppTheme(dynamicColor = false) {
        LoginScreen(onLoginClick = {})
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    FinanceAppTheme(dynamicColor = false) {
        HomeScreen()
    }
}
