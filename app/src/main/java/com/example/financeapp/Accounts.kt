
package com.example.financeapp

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.FirebaseFirestore
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale
import java.text.SimpleDateFormat
import java.util.Date


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(
    userId: String,
    onBackClick: () -> Unit
) {

    var accounts by remember(userId) {
        mutableStateOf<List<Account>>(emptyList())
    }


    var isLoading by remember(userId) {
        mutableStateOf(true)
    }

    var errorMessage by remember(userId) {
        mutableStateOf<String?>(null)
    }

    val db = remember { FirebaseFirestore.getInstance() }

    DisposableEffect(userId) {
        if (userId.isBlank()) {
            isLoading = false
            errorMessage = "No user selected"
            onDispose { }
        } else {
            val listener = db.collection("Users")
                .document(userId)
                .collection("accounts")
                .addSnapshotListener { snapshot, error ->

                    if (error != null) {
                        errorMessage = "Unable to load accounts"
                        isLoading = false
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        accounts = snapshot.documents.map { document ->
                            Account(
                                id = document.id,
                                name = document.getString("account_name")
                                    ?: document.id,
                                type = document.getString("account_type")
                                    ?: "Account",
                                balance = document.getDouble("current_balance")
                                    ?: 0.0,
                                currency = document.getString("currency")
                                    ?: "USD",
                                asOfDate = document.getString("as_of_date")
                                    ?: "",
                                creditLimit = document.getDouble("credit_limit")
                            )
                        }

                        errorMessage = null
                        isLoading = false
                    }
                }

            onDispose {
                listener.remove()
            }
        }
    }

    val cashAccounts = accounts.filter {
        it.type.equals("Checking", true) ||
                it.type.equals("Savings", true)
    }

    val creditAccounts = accounts.filter {
        it.type.contains("Credit", true)
    }

    val totalCash = cashAccounts.sumOf { it.balance }
    val totalCredit = creditAccounts.sumOf { it.balance }

    // Do not combine different currencies into one total.
    val cashCurrencies = cashAccounts.map { it.currency }.distinct()
    val canShowCashTotal = cashCurrencies.size <= 1
    val cashCurrency = cashCurrencies.firstOrNull() ?: "USD"

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "My Accounts",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    TextButton(onClick = onBackClick) {
                        Text("Back")
                    }
                }
            )
        }
    ) { innerPadding ->

        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            errorMessage != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(
                        top = 16.dp,
                        bottom = 24.dp
                    )
                ) {

                    // Summary card
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF1B5E20)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp)
                            ) {
                                Text(
                                    text = "TOTAL CASH BALANCE",
                                    color = Color(0xFFD8E8FF),
                                    style = MaterialTheme.typography.labelLarge
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = if (canShowCashTotal) {
                                        formatAccountBalance(
                                            totalCash,
                                            cashCurrency
                                        )
                                    } else {
                                        "Multiple currencies"
                                    },
                                    color = Color.White,
                                    style = MaterialTheme.typography.headlineLarge,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = "Across ${cashAccounts.size} cash accounts",
                                    color = Color(0xFFD8E8FF)
                                )

                                if (creditAccounts.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    HorizontalDivider(
                                        color = Color.White.copy(alpha = 0.25f)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))

                                    Text(
                                        text = "Credit accounts: ${creditAccounts.size}",
                                        color = Color.White
                                    )

                                    if (creditAccounts.map { it.currency }
                                            .distinct().size == 1) {
                                        Text(
                                            text = "Combined reported credit balance: " +
                                                    formatAccountBalance(
                                                        totalCredit,
                                                        creditAccounts.first().currency
                                                    ),
                                            color = Color(0xFFD8E8FF),
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Text(
                            text = "Your Accounts",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (accounts.isEmpty()) {
                        item {
                            Text("No accounts found.")
                        }
                    } else {
                        items(
                            items = accounts,
                            key = { it.id }
                        ) { account ->
                            AccountCard(account)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AccountCard(account: Account) {

    val isCredit = account.type.contains("Credit", true)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 3.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = account.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${account.type} • ${account.id}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(modifier = Modifier.height(18.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (isCredit) {
                    "Reported Credit Balance"
                } else {
                    "Current Balance"
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = formatAccountBalance(
                    account.balance,
                    account.currency
                ),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            if (isCredit && account.creditLimit != null) {
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Credit Limit: " +
                            formatAccountBalance(
                                account.creditLimit,
                                account.currency
                            ),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            val today = SimpleDateFormat(
                "MMMM d, yyyy",
                Locale.US
            ).format(Date())

            Text(
                text = "Balance as of: $today",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

fun formatAccountBalance(
    amount: Double,
    currencyCode: String
): String {
    return try {
        NumberFormat.getCurrencyInstance(Locale.US).apply {
            currency = Currency.getInstance(currencyCode)
        }.format(amount)
    } catch (e: IllegalArgumentException) {
        "$currencyCode ${String.format(Locale.US, "%,.2f", amount)}"
    }
}
