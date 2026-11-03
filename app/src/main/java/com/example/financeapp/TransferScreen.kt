package com.example.financeapp



// Layout and scrolling
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

// Material Design components
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text

// Compose state and effects
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

// UI measurements
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// Firebase services
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun TransferScreen(
    // Supplied by MainActivity to return to the home screen.
    onBackClick: () -> Unit
) {
    // ------------------------------------------------
    // SCREEN AND DROPDOWN STATE
    // ------------------------------------------------

    // Starts on the Transfer Money menu.
    var isInternalTransferOpen by remember { mutableStateOf(false) }

    // Store document IDs rather than account display names.
    var fromAccountId by remember { mutableStateOf<String?>(null) }
    var toAccountId by remember { mutableStateOf<String?>(null) }

    // Control whether each dropdown menu is visible.
    var isFromMenuExpanded by remember { mutableStateOf(false) }
    var isToMenuExpanded by remember { mutableStateOf(false) }

    // ------------------------------------------------
    // FIREBASE AUTHENTICATION AND DATA STATE
    // ------------------------------------------------

    // UID of the currently signed-in Firebase Authentication user.
    val firebaseUid = FirebaseAuth.getInstance().currentUser?.uid

    // The matching Firestore document ID, such as USER001.
    var firestoreUserId by remember { mutableStateOf<String?>(null) }
    var lookupMessage by remember {
        mutableStateOf("Looking for your account...")
    }

    // Accounts loaded from Users/{userId}/accounts.
    // Account is the data class in your separate Account.kt file.
    var loadedAccounts by remember {
        mutableStateOf<List<Account>>(emptyList())
    }
    var accountsMessage by remember {
        mutableStateOf("Waiting for user lookup...")
    }

    // ------------------------------------------------
    // STEP 1: FIND THE SIGNED-IN USER IN FIRESTORE
    // ------------------------------------------------

    LaunchedEffect(firebaseUid) {
        firestoreUserId = null
        lookupMessage = "Looking for your account..."

        if (firebaseUid == null) {
            lookupMessage = "No user is signed in"
        } else {
            FirebaseFirestore.getInstance()
                .collection("Users")
                .whereEqualTo("auth_uid", firebaseUid)
                .get()
                .addOnSuccessListener { result ->
                    // Ignore a delayed response if another user signed in.
                    if (FirebaseAuth.getInstance().currentUser?.uid != firebaseUid) {
                        return@addOnSuccessListener
                    }

                    // We require exactly one matching user document.
                    val userDocument = result.documents.singleOrNull()

                    if (userDocument != null) {
                        firestoreUserId = userDocument.id
                        lookupMessage = "Found user: ${userDocument.id}"
                    } else {
                        firestoreUserId = null
                        lookupMessage = "Expected one matching user"
                    }
                }
                .addOnFailureListener { exception ->
                    if (FirebaseAuth.getInstance().currentUser?.uid != firebaseUid) {
                        return@addOnFailureListener
                    }

                    firestoreUserId = null
                    lookupMessage =
                        "Lookup failed: ${exception.localizedMessage}"
                }
        }
    }

    // ------------------------------------------------
    // STEP 2: LOAD THAT USER'S ACCOUNT DOCUMENTS
    // ------------------------------------------------

    LaunchedEffect(firestoreUserId) {
        // Clear data and selections whenever the user document changes.
        loadedAccounts = emptyList()
        fromAccountId = null
        toAccountId = null
        isFromMenuExpanded = false
        isToMenuExpanded = false

        val userId = firestoreUserId

        if (userId == null) {
            accountsMessage = "Waiting for user lookup..."
        } else {
            accountsMessage = "Loading accounts..."

            FirebaseFirestore.getInstance()
                .collection("Users")
                .document(userId)
                .collection("accounts")
                .get()
                .addOnSuccessListener { result ->
                    // Ignore an old response if the selected user changed.
                    if (firestoreUserId != userId ||
                        FirebaseAuth.getInstance().currentUser?.uid != firebaseUid
                    ) {
                        return@addOnSuccessListener
                    }

                    // Convert each Firestore document to an Account object.
                    loadedAccounts = result.documents.map { document ->
                        Account(
                            id = document.id,
                            name = document.getString("account_name")
                                ?: document.id,
                            type = document.getString("account_type")
                                ?: "Unknown",
                            currency = document.getString("currency")
                                ?: "Unknown",
                            // Nullable: do not treat missing balances as zero.
                            balance = (document.get("current_balance") as? Number)
                                ?.toDouble()
                        )
                    }

                    accountsMessage = if (loadedAccounts.isEmpty()) {
                        "No accounts found"
                    } else {
                        "Found ${loadedAccounts.size} accounts"
                    }
                }
                .addOnFailureListener { exception ->
                    if (firestoreUserId != userId ||
                        FirebaseAuth.getInstance().currentUser?.uid != firebaseUid
                    ) {
                        return@addOnFailureListener
                    }

                    loadedAccounts = emptyList()
                    accountsMessage =
                        "Account lookup failed: ${exception.localizedMessage}"
                }
        }
    }

    // ------------------------------------------------
    // MAIN SCREEN: VERTICAL, SCROLLABLE LAYOUT
    // ------------------------------------------------

    // Column is similar to a JavaFX VBox.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        if (isInternalTransferOpen) {
            // ==========================================
            // INTERNAL TRANSFER SCREEN
            // ==========================================

            Text(
                text = "Internal Transfer",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Temporary debug messages; remove for the final interface.
            Text(text = lookupMessage)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = accountsMessage)
            Spacer(modifier = Modifier.height(12.dp))

            // Display the account records read from Firestore.
            loadedAccounts.forEach { account ->
                Text(text = "${account.name} (${account.id})")
                Text(text = "Type: ${account.type}")

                // Display-only value; we'll format it as currency later.
                val balanceText = account.balance?.toString()
                    ?: "Unavailable"
                Text(text = "Balance: $balanceText ${account.currency}")

                Spacer(modifier = Modifier.height(16.dp))
            }

            // ==========================================
            // TRANSFER FROM: SOURCE ACCOUNT
            // ==========================================

            // Credit-card transfers need separate rules, so for now
            // only Checking and Savings can be used as sources.
            val sourceAccounts = loadedAccounts.filter { account ->
                account.type.equals("Checking", ignoreCase = true) ||
                        account.type.equals("Savings", ignoreCase = true)
            }

            // Find the Account object matching the chosen source ID.
            val selectedSource = sourceAccounts.find { account ->
                account.id == fromAccountId
            }

            Text(
                text = "Transfer From",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))

            Box {
                OutlinedButton(
                    onClick = { isFromMenuExpanded = true },
                    enabled = sourceAccounts.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = selectedSource?.name ?: "Select an Account")
                }

                DropdownMenu(
                    expanded = isFromMenuExpanded,
                    onDismissRequest = { isFromMenuExpanded = false }
                ) {
                    sourceAccounts.forEach { account ->
                        DropdownMenuItem(
                            text = { Text("${account.name} (${account.id})") },
                            onClick = {
                                // Save the chosen source and reset destination.
                                fromAccountId = account.id
                                toAccountId = null
                                isFromMenuExpanded = false
                                isToMenuExpanded = false
                            }
                        )
                    }
                }
            }

            // ==========================================
            // TRANSFER TO: DESTINATION ACCOUNT
            // ==========================================

            // Eligible destinations must be different accounts,
            // have the same currency, and be Checking or Savings.
            // No destination can be chosen until a source is selected.
            val destinationAccounts: List<Account> =
                if (selectedSource == null) {
                    emptyList()
                } else {
                    sourceAccounts.filter { account ->
                        account.id != selectedSource.id &&
                                account.currency.equals(
                                    selectedSource.currency,
                                    ignoreCase = true
                                )
                    }
                }

            val selectedDestination = destinationAccounts.find { account ->
                account.id == toAccountId
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Transfer To",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))

            Box {
                OutlinedButton(
                    onClick = { isToMenuExpanded = true },
                    enabled = destinationAccounts.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = selectedDestination?.name
                            ?: "Select Destination Account"
                    )
                }

                DropdownMenu(
                    expanded = isToMenuExpanded,
                    onDismissRequest = { isToMenuExpanded = false }
                ) {
                    destinationAccounts.forEach { account ->
                        DropdownMenuItem(
                            text = { Text("${account.name} (${account.id})") },
                            onClick = {
                                // Remember the destination account ID.
                                toAccountId = account.id
                                isToMenuExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    // Return to the Transfer Money menu.
                    isInternalTransferOpen = false
                    isFromMenuExpanded = false
                    isToMenuExpanded = false
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Back to Transfer Menu")
            }
        } else {
            // ==========================================
            // TRANSFER MONEY MENU
            // ==========================================

            Text(
                text = "Transfer Money",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { isInternalTransferOpen = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Internal Transfer")
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    // TODO: Implement External Transfers in a later step.
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("External Transfer")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Available from both the menu and Internal Transfer screen.
        Button(
            onClick = onBackClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back to Home")
        }
    }
}
