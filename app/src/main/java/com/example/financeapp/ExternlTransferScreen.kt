package com.example.financeapp

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.math.BigDecimal

// This page prepares a transfer to another GreenStack user by email.
// It is a frontend demo: no recipient lookup, payment, or Firestore write.
// Keep the existing function name so TransferScreen.kt can still call it.

// A data class groups the information we display on the review page.
// All properties are val because the draft should not change during review.
private data class EmailTransferDraft(
    val sourceId: String,
    val sourceName: String,
    val recipientEmail: String,
    val amount: BigDecimal,
    val memo: String
)

// Only USD checking and savings accounts are supported for this prototype.
private fun eligibleEmailAccount(account: Account): Boolean {
    val isChecking = account.type.equals("Checking", ignoreCase = true)
    val isSavings = account.type.equals("Savings", ignoreCase = true)
    val isUSD = account.currency.equals("USD", ignoreCase = true)
    return (isChecking || isSavings) && isUSD
}

// Search by the unique Firestore document ID, not the display name.
// Account? means that the function can return an Account or null.
private fun findEmailAccount(accounts: List<Account>, id: String?): Account? {
    for (account in accounts) {
        if (account.id == id) {
            return account
        }
    }
    return null
}

// A basic email format check for the UI, not proof that the user exists.
// The backend must resolve the email to a verified recipient account.
private fun validRecipientEmail(email: String): Boolean {
    if (email.isEmpty() || email.length > 254) {
        return false
    }
    for (character in email) {
        if (character.isWhitespace() || character.isISOControl()) {
            return false
        }
    }
    // split separates the local part and the domain at the @ character.
    val parts = email.split('@')
    if (parts.size != 2) {
        return false
    }
    val localPart = parts[0]
    val domain = parts[1]
    if (localPart.isEmpty() || localPart.length > 64 || domain.isEmpty()) {
        return false
    }
    if (localPart.startsWith('.') || localPart.endsWith('.') || localPart.contains("..")) {
        return false
    }
    // A simple domain check for ordinary internet email addresses.
    val labels = domain.split('.')
    if (labels.size < 2) {
        return false
    }
    for (label in labels) {
        if (label.isEmpty() || label.length > 63 || label.startsWith('-') || label.endsWith('-')) {
            return false
        }
        for (character in label) {
            if (character !in 'a'..'z' && character !in 'A'..'Z' &&
                character !in '0'..'9' && character != '-') {
                return false
            }
        }
    }
    // Restrict the local part to common email characters in this demo.
    for (character in localPart) {
        if (character !in 'a'..'z' && character !in 'A'..'Z' &&
            character !in '0'..'9' && character !in ".!#$%&'*+-/=?^_`{|}~") {
            return false
        }
    }
    return true
}

// Convert a positive decimal dollar amount directly from String to BigDecimal.
// Unlike Double, BigDecimal can represent ordinary decimal money exactly.
private fun parseEmailAmount(input: String): BigDecimal? {
    val text = input.trim()
    val parts = text.split('.')
    if (parts.size > 2 || parts[0].isEmpty()) {
        return null
    }
    // Reject signs, exponent notation, letters, and other non-digit input.
    for (part in parts) {
        for (character in part) {
            if (character !in '0'..'9') {
                return null
            }
        }
    }
    if (parts.size == 2 && (parts[1].isEmpty() || parts[1].length > 2)) {
        return null
    }
    val amount = text.toBigDecimalOrNull()
    if (amount == null || amount <= BigDecimal.ZERO) {
        return null
    }
    // The proposed backend request uses integer cents, so ensure they fit in a Long.
    try {
        amount.movePointRight(2).longValueExact()
    } catch (exception: ArithmeticException) {
        return null
    }
    return amount
}

// Return an error message if something is wrong, or null if all local checks pass.
private fun validateEmailTransfer(
    source: Account?,
    email: String,
    amount: BigDecimal?
): String? {
    if (source == null || !eligibleEmailAccount(source)) {
        return "Select a USD checking or savings account."
    }
    if (!validRecipientEmail(email)) {
        return "Enter a valid recipient email address."
    }
    if (amount == null) {
        return "Enter a positive USD amount with at most two decimal places, within the supported range."
    }
    // The existing Account model stores balances as Double.
    // Reject invalid numbers before converting the balance to BigDecimal.
    if (!source.balance.isFinite() || source.balance < 0) {
        return "The source balance is unavailable or invalid."
    }
    val balance = BigDecimal.valueOf(source.balance)
    if (amount > balance) {
        return "Insufficient funds."
    }
    // The backend must repeat these checks using the latest Firestore balance.
    return null
}

@Composable
fun ExternalTransferScreen(
    accounts: List<Account>, // Accounts already loaded by TransferScreen.kt.
    onBackClick: () -> Unit, // Callback to return to the transfer menu.
    accountsMessage: String = "No eligible accounts currently available."
) {
    // remember retains values during recomposition (redrawing the interface).
    // mutableStateOf lets Compose notice changes; by avoids writing .value.
    var fromAccountId by remember { mutableStateOf<String?>(null) }
    var isFromMenuExpanded by remember { mutableStateOf(false) }
    var recipientEmail by remember { mutableStateOf("") }
    var transferAmount by remember { mutableStateOf("") }
    var memo by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // null = show the form; a draft = show review; demoComplete = show completion.
    var draft by remember { mutableStateOf<EmailTransferDraft?>(null) }
    var demoComplete by remember { mutableStateOf(false) }

    // Build the dropdown list with a normal loop, like an ArrayList in Java.
    val sourceAccounts = mutableListOf<Account>()
    for (account in accounts) {
        if (eligibleEmailAccount(account)) {
            sourceAccounts.add(account)
        }
    }
    // Find the account again rather than storing an old copy of its balance.
    val selectedSource = findEmailAccount(sourceAccounts, fromAccountId)

    // Handle Android's system Back button as well as the on-screen buttons.
    BackHandler {
        if (draft != null && !demoComplete) {
            draft = null
            errorMessage = null
        } else {
            onBackClick()
        }
    }

    val scrollState = rememberScrollState()
    // Reset scrolling when we switch between form, review and completion.
    LaunchedEffect(draft != null, demoComplete) {
        scrollState.scrollTo(0)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // A local val allows Kotlin to smart-cast the nullable draft after a null check.
        val review = draft

        if (demoComplete) {
            Text("Review complete", style = MaterialTheme.typography.headlineMedium)
            Text("Demo only. No recipient was verified, no money was sent, and no Firebase data was changed.")
            Button(onClick = onBackClick, modifier = Modifier.fillMaxWidth()) {
                Text("Back to Transfer Menu")
            }
        } else if (review != null) {
            Text("Review Transfer", style = MaterialTheme.typography.headlineMedium)
            Text("Demo only. Recipient identity and receiving account have not been verified.")

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Transfer From: ${review.sourceName}")
                    Text("Recipient Email: ${review.recipientEmail}")
                    HorizontalDivider()
                    // setScale(2) displays two decimal places, e.g. 20.00.
                    Text("Amount: ${review.amount.setScale(2).toPlainString()} USD",
                        style = MaterialTheme.typography.headlineSmall)
                    if (review.memo.isNotEmpty()) {
                        Text("Memo: ${review.memo}")
                    }
                }
            }

            Button(
                onClick = {
                    // Recheck the latest account data AVAILABLE TO THIS SCREEN.
                    // A real backend must read the current balance itself.
                    val source = findEmailAccount(sourceAccounts, review.sourceId)
                    val problem = validateEmailTransfer(source, review.recipientEmail, review.amount)
                    if (problem != null) {
                        errorMessage = problem
                    } else {
                        // TODO (backend team): send an authenticated request containing
                        // source account ID, recipient email, amount in cents, memo,
                        // and an idempotency/request ID. Do not let Android write another
                        // user's account directly. Only report a real success after the
                        // backend confirms the atomic transfer.
                        // For now, finish the UI demo and clear the form.
                        recipientEmail = ""
                        transferAmount = ""
                        memo = ""
                        fromAccountId = null
                        draft = null
                        errorMessage = null
                        demoComplete = true
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Confirm Demo Transfer")
            }
            OutlinedButton(
                onClick = {
                    // Removing the draft shows the form; entered values remain in state.
                    draft = null
                    errorMessage = null
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Edit Information")
            }
            OutlinedButton(onClick = onBackClick, modifier = Modifier.fillMaxWidth()) {
                Text("Cancel / Back to Transfer Menu")
            }
        } else {
            Text("GreenStack Transfer", style = MaterialTheme.typography.headlineMedium)
            Text("Send to another GreenStack user. Demo only — use fictional information.")
            Text("Transfer From", style = MaterialTheme.typography.titleMedium)

            // Box anchors the popup menu to the account-selection button.
            Box {
                OutlinedButton(
                    onClick = { isFromMenuExpanded = true },
                    enabled = sourceAccounts.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // ?. safely accesses a nullable object; ?: supplies a fallback.
                    Text(selectedSource?.name ?: "Select an Account")
                }
                DropdownMenu(
                    expanded = isFromMenuExpanded && sourceAccounts.isNotEmpty(),
                    onDismissRequest = { isFromMenuExpanded = false }
                ) {
                    for (account in sourceAccounts) {
                        DropdownMenuItem(
                            text = { Text("${account.name} (${account.id})") },
                            onClick = {
                                fromAccountId = account.id
                                isFromMenuExpanded = false
                                errorMessage = null
                            }
                        )
                    }
                }
            }
            if (selectedSource != null) {
                Text("Current Balance")
                // Reuse the function from Accounts.kt, in the same Kotlin package.
                Text(formatAccountBalance(selectedSource.balance, selectedSource.currency),
                    style = MaterialTheme.typography.headlineSmall)
            }
            if (sourceAccounts.isEmpty()) {
                if (accounts.isEmpty()) {
                    Text(accountsMessage)
                } else {
                    Text("No eligible USD checking or savings accounts.")
                }
            }

            HorizontalDivider()
            Text("Recipient Information", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(
                value = recipientEmail,
                // onValueChange receives the entire updated String, not one character.
                onValueChange = { newValue ->
                    recipientEmail = newValue
                    errorMessage = null
                },
                label = { Text("Recipient Email") },
                placeholder = { Text("recipient@example.com") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Text("Enter the email associated with the recipient's GreenStack account.",
                style = MaterialTheme.typography.bodySmall)

            HorizontalDivider()
            Text("Transfer Details", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(
                value = transferAmount,
                onValueChange = { newValue ->
                    transferAmount = newValue
                    errorMessage = null
                },
                label = { Text("Transfer Amount (USD)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = memo,
                onValueChange = { newValue ->
                    if (newValue.length <= 140) {
                        memo = newValue
                        errorMessage = null
                    }
                },
                label = { Text("Memo (Optional)") },
                supportingText = { Text("${memo.length}/140 characters") },
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = {
                    // trim removes accidental whitespace at the beginning and end.
                    // Preserve email case until the backend defines its lookup policy.
                    val email = recipientEmail.trim()
                    val amount = parseEmailAmount(transferAmount)
                    val problem = validateEmailTransfer(selectedSource, email, amount)
                    errorMessage = problem
                    if (problem == null && selectedSource != null && amount != null) {
                        // A non-null draft causes Compose to display the review branch.
                        draft = EmailTransferDraft(
                            sourceId = selectedSource.id,
                            sourceName = selectedSource.name,
                            recipientEmail = email,
                            amount = amount,
                            memo = memo.trim()
                        )
                    }
                },
                enabled = sourceAccounts.isNotEmpty(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Review Transfer")
            }
            OutlinedButton(onClick = onBackClick, modifier = Modifier.fillMaxWidth()) {
                Text("Back to Transfer Menu")
            }
        }

        // Copy delegated state to a local val to avoid the unsafe !! operator.
        val message = errorMessage
        if (message != null) {
            Text(message, color = MaterialTheme.colorScheme.error)
        }
    }
}
