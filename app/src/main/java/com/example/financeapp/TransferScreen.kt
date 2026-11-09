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


import androidx.compose.material3.OutlinedTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import java.math.BigDecimal


@Composable
fun TransferScreen(
    // Supplied by MainActivity to return to the home screen.
    onBackClick: () -> Unit
) {
    //
    // SCREEN AND DROPDOWN STATE
    //

    // Starts on the Transfer Money menu.
    var isInternalTransferOpen by remember { mutableStateOf(false) }

    // Store document IDs rather than account display names.
    var fromAccountId by remember { mutableStateOf<String?>(null) }
    var toAccountId by remember { mutableStateOf<String?>(null) }



    // Amount entered by the user.
        var transferAmount by remember {mutableStateOf("") }

    // Prevents multiple submissions while a transfer is processing.
        var isTransferring by remember {mutableStateOf(false)}

    // Displays success or error messages.
        var transferMessage by remember { mutableStateOf<String?>(null) }

    // Used to reload account balances after a successful transfer.
        var refreshAccounts by remember {mutableStateOf(0)}


    // Control whether each dropdown menu is visible.
    var isFromMenuExpanded by remember { mutableStateOf(false) }
    var isToMenuExpanded by remember { mutableStateOf(false) }

    //
    // FIREBASE AUTHENTICATION AND DATA STATE
    //

    // UID of the currently signed-in Firebase Authentication user.
    val uid = FirebaseAuth.getInstance().currentUser?.uid

    // The matching Firestore document ID, like USER001.
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

    //
    // STEP 1: FIND THE SIGNED-IN USER IN FIRESTORE
    //

    LaunchedEffect(uid) {
        firestoreUserId = null
        lookupMessage = "Looking for your account..."

        if (uid == null) {
            lookupMessage = "No user is signed in"
        } else {
            FirebaseFirestore.getInstance()
                .collection("Users")
                .whereEqualTo("auth_id", uid)
                .get()
                .addOnSuccessListener { result ->

                    // Ignore a delayed response if another user signed in.
                    if (FirebaseAuth.getInstance().currentUser?.uid != uid) {
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
                    if (FirebaseAuth.getInstance().currentUser?.uid != uid) {
                        return@addOnFailureListener
                    }

                    firestoreUserId = null
                    lookupMessage =
                        "Lookup failed: ${exception.localizedMessage}"
                }
        }
    }

    //
    // STEP 2: LOAD THAT USER'S ACCOUNT DOCUMENTS
    //

    LaunchedEffect(firestoreUserId, refreshAccounts) {
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
                        FirebaseAuth.getInstance().currentUser?.uid != uid
                    ) {
                        return@addOnSuccessListener
                    }

                    // Convert each Firestore document into our shared Account class.
                    loadedAccounts = result.documents.map { document ->
                        Account(
                            // Unique Firestore document ID.
                            id = document.id,

                            // Account's display name.
                            name = document.getString("account_name")
                                ?: document.id,

                            // Account type: Checking, Savings, etc.
                            type = document.getString("account_type")
                                ?: "Unknown",

                            // Current account balance.
                            balance = document.getDouble("current_balance")
                                ?: 0.0,

                            // Currency associated with this account.
                            currency = document.getString("currency")
                                ?: "USD",

                            // Date associated with the account balance.
                            asOfDate = document.getString("as_of_date")
                                ?: "",

                            // Credit limit, if one exists.
                            creditLimit = document.getDouble("credit_limit")
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
                        FirebaseAuth.getInstance().currentUser?.uid != uid
                    ) {
                        return@addOnFailureListener
                    }

                    loadedAccounts = emptyList()
                    accountsMessage =
                        "Account lookup failed: ${exception.localizedMessage}"
                }
        }
    }

    //
    // MAIN SCREEN: VERTICAL, SCROLLABLE LAYOUT
    //

    // Column is similar to a JavaFX VBox.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        if (isInternalTransferOpen) {
            // INTERNAL TRANSFER SCREEN
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
                val balanceText = account.balance.toString()
                Text(text = "Balance: $balanceText ${account.currency}")

                Spacer(modifier = Modifier.height(16.dp))
            }


            // TRANSFER FROM: SOURCE ACCOUNT

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


            // TRANSFER TO: DESTINATION ACCOUNT
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

            // Let the user enter the amount to transfer.
            OutlinedTextField(
                value = transferAmount,
                onValueChange = { transferAmount = it },
                label = { Text("Transfer Amount") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal
                ),
                singleLine = true,
                enabled = !isTransferring,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    // Capture the selected account IDs and user ID.
                    val userId = firestoreUserId
                    val fromId = selectedSource?.id
                    val toId = selectedDestination?.id

                    // Convert the user's input into a decimal amount.
                    val amount = transferAmount.trim().toBigDecimalOrNull()

                    // Validate the input before contacting Firestore.
                    when {
                        uid == null || userId == null -> {
                            transferMessage = "Please log in again."
                        }

                        fromId == null || toId == null -> {
                            transferMessage = "Select both accounts."
                        }

                        fromId == toId -> {
                            transferMessage = "Select two different accounts."
                        }

                        amount == null ||
                                amount <= BigDecimal.ZERO ||
                                amount.scale() > 2 -> {
                            transferMessage =
                                "Enter a positive amount with at most two decimal places."
                        }

                        else -> {
                            // Reference the two accounts belonging to this user.
                            val db = FirebaseFirestore.getInstance()
                            val userRef = db.collection("Users").document(userId)

                            val fromRef = userRef
                                .collection("accounts")
                                .document(fromId)

                            val toRef = userRef
                                .collection("accounts")
                                .document(toId)

                            isTransferring = true
                            transferMessage = null

                            // Read, validate, and update both balances atomically.
                            db.runTransaction { transaction ->

                                // All reads must happen before writes.
                                val userDoc = transaction.get(userRef)
                                val fromDoc = transaction.get(fromRef)
                                val toDoc = transaction.get(toRef)

                                // Ensure the user document matches the signed-in user.
                                require(userDoc.getString("auth_id") == uid) {
                                    "Account ownership could not be verified."
                                }

                                require(fromDoc.exists() && toDoc.exists()) {
                                    "One or both accounts no longer exist."
                                }

                                // Check that both accounts are eligible.
                                val fromType = fromDoc.getString("account_type")
                                val toType = toDoc.getString("account_type")

                                val validTypes = listOf("Checking", "Savings")

                                require(
                                    validTypes.any { it.equals(fromType, true) } &&
                                            validTypes.any { it.equals(toType, true) }
                                ) {
                                    "Only Checking and Savings transfers are supported."
                                }

                                // Make sure currencies match.
                                val fromCurrency = fromDoc.getString("currency")
                                val toCurrency = toDoc.getString("currency")

                                require(
                                    fromCurrency != null &&
                                            fromCurrency.equals(toCurrency, true) &&
                                            fromCurrency.equals("USD", true)
                                ) {
                                    "Only same-currency USD transfers are supported."
                                }

                                // Read the LATEST balances from Firestore.
                                val fromNumber =
                                    fromDoc.get("current_balance") as? Number
                                        ?: error("Source balance is unavailable.")

                                val toNumber =
                                    toDoc.get("current_balance") as? Number
                                        ?: error("Destination balance is unavailable.")

                                val fromBalance = BigDecimal(fromNumber.toString())
                                val toBalance = BigDecimal(toNumber.toString())

                                // Reject invalid balance data.
                                require(
                                    fromBalance.scale() <= 2 &&
                                            toBalance.scale() <= 2
                                ) {
                                    "Account balances must have at most two decimal places."
                                }

                                // The source must have enough funds.
                                require(fromBalance >= amount) {
                                    "Insufficient funds."
                                }

                                // Calculate the new balances.
                                val newFromBalance = fromBalance - amount
                                val newToBalance = toBalance + amount

                                // Both updates belong to the SAME transaction.
                                transaction.update(
                                    fromRef,
                                    "current_balance",
                                    newFromBalance.toDouble()
                                )

                                transaction.update(
                                    toRef,
                                    "current_balance",
                                    newToBalance.toDouble()
                                )

                                null
                            }
                                .addOnSuccessListener {
                                    isTransferring = false
                                    transferMessage = "Transfer completed successfully!"

                                    // Clear inputs.
                                    transferAmount = ""

                                    // Reload account balances from Firestore.
                                    refreshAccounts += 1
                                }
                                .addOnFailureListener { exception ->
                                    isTransferring = false
                                    transferMessage =
                                        "Transfer failed: ${exception.localizedMessage}"
                                }
                        }
                    }
                },
                enabled = !isTransferring &&
                        selectedSource != null &&
                        selectedDestination != null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isTransferring) "Processing..." else "Confirm Transfer")
            }

            // Show the result to the user.
            transferMessage?.let { message ->
                Spacer(modifier = Modifier.height(12.dp))
                Text(message)
            }


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
            // TRANSFER MONEY MENU

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
