package com.example.financeapp

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.FirebaseFirestore
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

// Only the name and currency are needed from the accounts collection here.
private data class TransactionAccountInfo(
    val id: String,
    val name: String,
    val currency: String?
)

// The value is used by the filter; the label is what the user sees.
// A null value represents the "All" option.
private data class TransactionFilterOption(val value: String?, val label: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(userId: String, onBackClick: () -> Unit) {

    // remember(userId) resets the state when the selected user changes.
    // mutableStateOf tells Compose to update the UI when a value changes.
    var transactions by remember(userId) { mutableStateOf<List<Transaction>>(emptyList()) }
    var accounts by remember(userId) { mutableStateOf<List<TransactionAccountInfo>>(emptyList()) }
    var loadingTransactions by remember(userId) { mutableStateOf(true) }
    var loadingAccounts by remember(userId) { mutableStateOf(true) }
    var transactionError by remember(userId) { mutableStateOf<String?>(null) }
    var accountError by remember(userId) { mutableStateOf<String?>(null) }

    var selectedAccountId by remember(userId) { mutableStateOf<String?>(null) }
    var selectedType by remember(userId) { mutableStateOf<String?>(null) }
    var selectedMonth by remember(userId) { mutableStateOf<String?>(null) }
    var selectedTransactionId by remember(userId) { mutableStateOf<String?>(null) }
    val db = remember { FirebaseFirestore.getInstance() }

    // A snapshot listener receives the initial data and later database changes.
    // DisposableEffect lets us remove the listeners when this screen closes.
    DisposableEffect(userId) {
        if (userId.isBlank()) {
            loadingTransactions = false
            loadingAccounts = false
            transactionError = "No user selected. Please log in again."
            onDispose { }
        } else {
            val userRef = db.collection("Users").document(userId)
            val transactionListener = userRef.collection("transactions")
                .addSnapshotListener { snapshot, error ->
                    loadingTransactions = false
                    if (error != null || snapshot == null) {
                        transactions = emptyList()
                        transactionError = "Unable to load transactions."
                        return@addSnapshotListener
                    }
                    // Convert each document into an ordinary Kotlin object.
                    val newTransactions = mutableListOf<Transaction>()
                    for (document in snapshot.documents) {
                        val item = Transaction(
                            // Use the actual document ID, which is unique in this collection.
                            id = document.id,
                            accountId = (document.get("account_id") as? String) ?: "",
                            amount = readTransactionNumber(document.get("amount")),
                            balanceAfter = readTransactionNumber(document.get("balance_after")),
                            category = (document.get("category") as? String) ?: "Uncategorized",
                            date = (document.get("date") as? String) ?: "",
                            description = (document.get("description") as? String) ?: "No description",
                            type = (document.get("transaction_type") as? String) ?: "Unknown",
                            userId = (document.get("user_id") as? String) ?: userId
                        )
                        newTransactions.add(item)
                    }
                    // The database uses YYYY-MM-DD strings, so string sorting puts
                    // newer dates first. IDs provide a stable order on the same date,
                    // not a verified time of day.
                    transactions = newTransactions.sortedWith(
                        compareByDescending<Transaction> { it.date }.thenByDescending { it.id }
                    )
                    transactionError = null
                }

            // A transaction has an account_id, but no name or currency field.
            // Read the current account metadata to fill in those display details.
            val accountListener = userRef.collection("accounts")
                .addSnapshotListener { snapshot, error ->
                    loadingAccounts = false
                    if (error != null || snapshot == null) {
                        accounts = emptyList()
                        accountError = "Unable to load account names and currencies."
                        return@addSnapshotListener
                    }
                    val newAccounts = mutableListOf<TransactionAccountInfo>()
                    for (document in snapshot.documents) {
                        val name = (document.get("account_name") as? String) ?: document.id
                        val currency = (document.get("currency") as? String)
                        newAccounts.add(TransactionAccountInfo(document.id, name, currency))
                    }
                    accounts = newAccounts
                    accountError = null
                }
            onDispose {
                transactionListener.remove()
                accountListener.remove()
            }
        }
    }

    // Build filter options using actual records, rather than hardcoded categories.
    val accountOptions = mutableListOf(TransactionFilterOption(null, "All accounts"))
    val knownIds = mutableSetOf<String>()
    for (account in accounts) {
        if (knownIds.add(account.id)) {
            accountOptions.add(TransactionFilterOption(account.id, "${account.name} (${account.id})"))
        }
    }
    for (transaction in transactions) {
        if (knownIds.add(transaction.accountId)) {
            accountOptions.add(TransactionFilterOption(transaction.accountId,
                if (transaction.accountId.isBlank()) "Unknown account" else transaction.accountId))
        }
    }
    val typeOptions = mutableListOf(TransactionFilterOption(null, "All types"))
    val monthOptions = mutableListOf(TransactionFilterOption(null, "All dates"))
    val knownTypes = mutableSetOf<String>()
    val knownMonths = mutableSetOf<String>()
    for (transaction in transactions) {
        if (knownTypes.add(transaction.type)) {
            typeOptions.add(TransactionFilterOption(transaction.type, transaction.type))
        }
        val month = transactionMonth(transaction.date)
        if (knownMonths.add(month)) {
            monthOptions.add(TransactionFilterOption(month, month))
        }
    }

    // These are ordinary derived values, not extra mutable state.
    // Recomposition rebuilds the list whenever the selected filters change.
    val filteredTransactions = mutableListOf<Transaction>()
    for (transaction in transactions) {
        val matchesAccount = selectedAccountId == null || transaction.accountId == selectedAccountId
        val matchesType = selectedType == null || transaction.type == selectedType
        val matchesMonth = selectedMonth == null || transactionMonth(transaction.date) == selectedMonth
        if (matchesAccount && matchesType && matchesMonth) {
            filteredTransactions.add(transaction)
        }
    }

    // Use the latest version of the selected record if Firestore updates it.
    var selectedTransaction: Transaction? = null
    for (transaction in transactions) {
        if (transaction.id == selectedTransactionId) {
            selectedTransaction = transaction
            break
        }
    }
    val details = selectedTransaction
    BackHandler {
        if (details != null) selectedTransactionId = null else onBackClick()
    }

    Scaffold(topBar = {
        TopAppBar(title = { Text("Transactions") }, navigationIcon = {
            TextButton(onClick = onBackClick) { Text("Back") }
        })
    }) { innerPadding ->
        val error = transactionError
        when {
            loadingTransactions || loadingAccounts -> {
                Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            error != null -> {
                Box(Modifier.fillMaxSize().padding(innerPadding).padding(16.dp), contentAlignment = Alignment.Center) {
                    Text(error, color = MaterialTheme.colorScheme.error)
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Spacer(Modifier.height(4.dp))
                        val warning = accountError
                        if (warning != null) Text(warning, color = MaterialTheme.colorScheme.error)
                        TransactionFilter("Account", accountOptions, selectedAccountId) { selectedAccountId = it }
                        TransactionFilter("Transaction type", typeOptions, selectedType) { selectedType = it }
                        TransactionFilter("Month", monthOptions, selectedMonth) { selectedMonth = it }
                        TextButton(onClick = {
                            selectedAccountId = null
                            selectedType = null
                            selectedMonth = null
                        }) { Text("Clear filters") }
                    }
                    item {
                        TransactionSummary(filteredTransactions, accounts)
                    }
                    item {
                        Text("Transaction History (${filteredTransactions.size})", style = MaterialTheme.typography.titleLarge)
                    }
                    if (filteredTransactions.isEmpty()) {
                        item { Text("No matching transactions.") }
                    } else {
                        // The key identifies each row even if the list changes.
                        items(filteredTransactions, key = { it.id }) { transaction ->
                            val account = findTransactionAccount(accounts, transaction.accountId)
                            TransactionCard(transaction, account) { selectedTransactionId = transaction.id }
                        }
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }
    if (details != null) {
        val account = findTransactionAccount(accounts, details.accountId)
        TransactionDetails(details, account) { selectedTransactionId = null }
    }
}

// Firestore numbers can be integers or doubles. A safe cast accepts either.
// Missing, nonnumeric or non-finite values are not silently turned into zero.
private fun readTransactionNumber(value: Any?): Double? {
    val number = value as? Number ?: return null
    val result = number.toDouble()
    if (!result.isFinite()) return null
    return result
}

private fun transactionMonth(date: String): String {
    // The confirmed date format is YYYY-MM-DD. Extract YYYY-MM for the filter.
    if (date.length == 10 && date[4] == '-' && date[7] == '-') return date.substring(0, 7)
    return "Unknown month"
}

private fun findTransactionAccount(accounts: List<TransactionAccountInfo>, id: String): TransactionAccountInfo? {
    for (account in accounts) {
        if (account.id == id) return account
    }
    return null
}

private fun knownCurrency(code: String?): String? {
    if (code.isNullOrBlank()) return null
    val normalized = code.trim().uppercase(Locale.US)
    // Currency.getInstance throws if the stored code is not recognized.
    return try {
        Currency.getInstance(normalized)
        normalized
    } catch (e: IllegalArgumentException) {
        null
    }
}

private fun transactionMoney(amount: BigDecimal?, currencyCode: String?): String {
    if (amount == null) return "Unavailable"
    val code = knownCurrency(currencyCode)
    if (code == null) return "${amount.toPlainString()} (currency unknown)"
    val currency = Currency.getInstance(code)
    val formatter = NumberFormat.getCurrencyInstance(Locale.US)
    formatter.currency = currency
    // Set the correct currency precision explicitly (JPY, for example, uses 0).
    val digits = currency.defaultFractionDigits
    if (digits >= 0) {
        formatter.minimumFractionDigits = digits
        formatter.maximumFractionDigits = digits
    }
    return formatter.format(amount) + " " + code
}

private fun transactionDecimal(amount: Double?): BigDecimal? {
    if (amount == null || !amount.isFinite()) return null
    return BigDecimal.valueOf(amount)
}

@Composable
private fun TransactionFilter(
    title: String,
    options: List<TransactionFilterOption>,
    selectedValue: String?,
    onSelection: (String?) -> Unit
) {
    // Each dropdown has its own local expanded state.
    var expanded by remember { mutableStateOf(false) }
    var selectedLabel = selectedValue ?: "All"
    for (option in options) {
        if (option.value == selectedValue) {
            selectedLabel = option.label
            break
        }
    }
    Text(title, style = MaterialTheme.typography.titleSmall)
    Box {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text(selectedLabel)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            for (option in options) {
                DropdownMenuItem(text = { Text(option.label) }, onClick = {
                    onSelection(option.value)
                    expanded = false
                })
            }
        }
    }
}

// The totals are separated by currency. Never add USD and JPY together.
private data class TransactionTotals(
    var incoming: BigDecimal = BigDecimal.ZERO,
    var outgoing: BigDecimal = BigDecimal.ZERO
)

@Composable
private fun TransactionSummary(transactions: List<Transaction>, accounts: List<TransactionAccountInfo>) {
    val totals = mutableMapOf<String, TransactionTotals>()
    var excluded = 0
    for (transaction in transactions) {
        val account = findTransactionAccount(accounts, transaction.accountId)
        val currency = knownCurrency(account?.currency)
        val amount = transactionDecimal(transaction.amount)
        if (currency == null || amount == null) {
            excluded += 1
            continue
        }
        var total = totals[currency]
        if (total == null) {
            total = TransactionTotals()
            totals[currency] = total
        }
        // Use decimal arithmetic when adding monetary values.
        if (amount.signum() > 0) total.incoming = total.incoming.add(amount)
        if (amount.signum() < 0) total.outgoing = total.outgoing.add(amount.abs())
    }
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.primaryContainer
    )) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Filtered activity", style = MaterialTheme.typography.titleLarge)
            if (totals.isEmpty()) Text("No amounts with known currencies to summarize.")
            for ((currency, total) in totals) {
                Text(currency, fontWeight = FontWeight.Bold)
                Text("Incoming: ${transactionMoney(total.incoming, currency)}")
                Text("Outgoing: ${transactionMoney(total.outgoing, currency)}")
            }
            if (excluded > 0) Text("$excluded record(s) excluded from totals: missing amount or currency.")
            Text("Recorded positive and negative amounts, including transfers. Not an income or spending report.", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun TransactionCard(transaction: Transaction, account: TransactionAccountInfo?, onClick: () -> Unit) {
    val amount = transactionDecimal(transaction.amount)
    val money = transactionMoney(amount, account?.currency)
    val sign = if (amount != null && amount.signum() > 0) "+" else ""
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(transaction.description, modifier = Modifier.weight(1f).padding(end = 12.dp), fontWeight = FontWeight.Bold)
                Text(sign + money, fontWeight = FontWeight.Bold)
            }
            Text("${transaction.category} • ${transaction.type}", style = MaterialTheme.typography.bodyMedium)
            Text("${account?.name ?: transaction.accountId.ifBlank { "Unknown account" }} • ${transaction.date.ifBlank { "Unknown date" }}", style = MaterialTheme.typography.bodySmall)
            Text("Tap for details", style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun TransactionDetails(transaction: Transaction, account: TransactionAccountInfo?, onClose: () -> Unit) {
    // AlertDialog displays details without introducing a new activity or route.
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Transaction Details") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(transaction.description, style = MaterialTheme.typography.titleMedium)
                Text("Amount: ${transactionMoney(transactionDecimal(transaction.amount), account?.currency)}")
                Text("Date: ${transaction.date.ifBlank { "Unknown date" }}")
                Text("Account: ${account?.name ?: "Unknown account"}")
                Text("Account ID: ${transaction.accountId}")
                Text("Category: ${transaction.category}")
                Text("Type: ${transaction.type}")
                Text("Reported balance after: ${transactionMoney(transactionDecimal(transaction.balanceAfter), account?.currency)}")
                Text("Transaction ID: ${transaction.id}")
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text("Close") } }
    )
}
