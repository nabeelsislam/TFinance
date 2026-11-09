
package com.example.financeapp

import android.os.Bundle
import android.util.Log
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {

            // Tracks whether Dark Mode is enabled.
            var isDarkMode by remember {
                mutableStateOf(false)
            }

            // Stores the currency selected by the user.
            var selectedCurrency by remember {
                mutableStateOf("USD")
            }

            FinanceAppTheme(
                darkTheme = isDarkMode,
                dynamicColor = false
            ) {

                // Tracks whether the user is logged in.
                var isLoggedIn by remember {
                    mutableStateOf(false)
                }

                // Actual Firestore document ID, such as USER009.
                var currentUserId by remember {
                    mutableStateOf<String?>(null)
                }

                // Display name stored in the user_id field.
                var currentUserName by remember {
                    mutableStateOf("")
                }

                // Navigation states.
                var isSettingsOpen by remember {
                    mutableStateOf(false)
                }

                var isAboutOpen by remember {
                    mutableStateOf(false)
                }

                var isAccountsOpen by remember {
                    mutableStateOf(false)
                }

                // Tracks whether the Transfer Money screen is open.
                var isTransferOpen by remember {
                    mutableStateOf(false)
                }



                if (!isLoggedIn) {

                    // Show the login screen.
                    LoginScreen(
                        onLoginClick = { documentId, displayName ->

                            currentUserId = documentId
                            currentUserName = displayName

                            isLoggedIn = true
                        }
                    )

                } else if (isAboutOpen) {

                    // Display the About page.
                    AboutScreen(
                        onBackClick = {
                            isAboutOpen = false
                        }
                    )

                } else if (isSettingsOpen) {

                    // Display Settings.
                    SettingsScreen(
                        onBackClick = {
                            isSettingsOpen = false
                        },

                        isDarkMode = isDarkMode,

                        onDarkModeChange = { newValue ->
                            isDarkMode = newValue
                        },

                        onAboutClick = {
                            isAboutOpen = true
                        },

                        selectedCurrency = selectedCurrency,

                        onCurrencyChange = { newCurrency ->
                            selectedCurrency = newCurrency
                        }
                    )

                } else if (isAccountsOpen) {

                    // Display the user's accounts.
                    AccountsScreen(
                        userId = currentUserId ?: "",

                        onBackClick = {
                            isAccountsOpen = false
                        }
                    )
                } else if (isTransferOpen) {

                    // Display the Transfer Money screen.
                    TransferScreen(
                        onBackClick = {
                            // Return to the Home screen.
                            isTransferOpen = false
                        }
                    )
                }else {

                    // Display the Home screen.
                    HomeScreen(
                        userName = currentUserName,

                        onLogout = {

                            // Sign out of Firebase Authentication.
                            FirebaseAuth.getInstance().signOut()

                            currentUserId = null
                            currentUserName = ""

                            isLoggedIn = false
                            isSettingsOpen = false
                            isAboutOpen = false
                            isAccountsOpen = false
                            isTransferOpen = false
                        },

                        onSettingsClick = {
                            isSettingsOpen = true
                        },

                        onAccountsClick = {
                            isAccountsOpen = true
                        },

                        onTransferClick = {
                            isTransferOpen = true;
                        }
                    )
                }
            }
        }
    }
}


// -------------------------------------
// LOGIN SCREEN
// -------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginClick: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {

    var email by remember {
        mutableStateOf("")
    }

    var password by remember {
        mutableStateOf("")
    }

    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }

    var isLoading by remember {
        mutableStateOf(false)
    }

    val context = LocalContext.current

    // Firebase instances.
    val auth = remember {
        FirebaseAuth.getInstance()
    }

    val db = remember {
        FirebaseFirestore.getInstance()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Finance App - Login")
                },

                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor =
                        MaterialTheme.colorScheme.primaryContainer,

                    titleContentColor =
                        MaterialTheme.colorScheme.onPrimaryContainer
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

            // Email input.
            OutlinedTextField(
                value = email,

                onValueChange = {
                    email = it
                    errorMessage = null
                },

                label = {
                    Text("Email")
                },

                singleLine = true,

                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Password input.
            OutlinedTextField(
                value = password,

                onValueChange = {
                    password = it
                    errorMessage = null
                },

                label = {
                    Text("Password")
                },

                singleLine = true,

                visualTransformation = PasswordVisualTransformation(),

                modifier = Modifier.fillMaxWidth()
            )

            // Display login errors.
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

                        val inputEmail = email.trim()

                        // Validate input.
                        if (inputEmail.isEmpty() || password.isEmpty()) {

                            errorMessage =
                                "Please enter both email and password"

                            return@Button
                        }

                        isLoading = true
                        errorMessage = null

                        // Authenticate using Firebase.
                        auth.signInWithEmailAndPassword(
                            inputEmail,
                            password
                        )
                            .addOnSuccessListener { result ->

                                val uid = result.user?.uid

                                if (uid == null) {

                                    isLoading = false

                                    errorMessage =
                                        "Unable to identify user"

                                    auth.signOut()

                                } else {

                                    // Find the user's Firestore document.
                                    db.collection("Users")
                                        .whereEqualTo("auth_id", uid)
                                        .limit(1)
                                        .get()
                                        .addOnSuccessListener { documents ->

                                            isLoading = false

                                            if (!documents.isEmpty) {

                                                val document =
                                                    documents.documents[0]

                                                // Firestore document ID.
                                                val documentId = document.id

                                                // User's display name.
                                                val displayName =
                                                    document.getString("user_id")
                                                        ?: documentId

                                                Log.d(
                                                    "Firebase",
                                                    "Logged in as: $displayName"
                                                )

                                                Toast.makeText(
                                                    context,
                                                    "Welcome $displayName!",
                                                    Toast.LENGTH_SHORT
                                                ).show()

                                                // Send both values to MainActivity.
                                                onLoginClick(
                                                    documentId,
                                                    displayName
                                                )

                                            } else {

                                                errorMessage =
                                                    "User not found in database"

                                                auth.signOut()
                                            }
                                        }
                                        .addOnFailureListener { exception ->

                                            isLoading = false

                                            errorMessage =
                                                "Unable to load user information"

                                            Log.e(
                                                "Firebase",
                                                "Firestore error",
                                                exception
                                            )

                                            auth.signOut()
                                        }
                                }
                            }
                            .addOnFailureListener { exception ->

                                isLoading = false

                                errorMessage = exception.localizedMessage
                                    ?: "Unknown login error"

                                Log.e(
                                    "Firebase",
                                    "Login failed",
                                    exception
                                )
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


// -------------------------------------
// HOME SCREEN
// -------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    userName: String = "",
    onLogout: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onAccountsClick: () -> Unit = {},
    onTransferClick: () -> Unit = {}
) {


    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Finance App")
                },

                actions = {

                    Button(
                        onClick = onLogout,

                        modifier = Modifier.padding(end = 8.dp)

                    ) {
                        Text("Logout")
                    }
                },

                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor =
                        MaterialTheme.colorScheme.primaryContainer,

                    titleContentColor =
                        MaterialTheme.colorScheme.onPrimaryContainer
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

            // Display the user's name.
            if (userName.isNotEmpty()) {

                Text(
                    text = "Welcome, $userName!",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(16.dp))
            }

            // View Accounts button.
            Button(
                onClick = onAccountsClick,

                modifier = Modifier.fillMaxWidth()

            ) {
                Text("View Accounts")
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    // TODO: Handle Transactions
                },

                modifier = Modifier.fillMaxWidth()

            ) {
                Text("Transactions")
            }

            Spacer(modifier = Modifier.height(12.dp))


            // Opens the Transfer Money screen when clicked.
            Button(
                onClick = onTransferClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Transfer Money")
            }


            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    // TODO: Handle Budget & Analytics
                },

                modifier = Modifier.fillMaxWidth()

            ) {
                Text("Budget & Analytics")
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Settings button.
            Button(
                onClick = onSettingsClick,

                modifier = Modifier.fillMaxWidth()

            ) {
                Text("Settings")
            }
        }
    }
}

