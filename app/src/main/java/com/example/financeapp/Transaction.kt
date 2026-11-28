package com.example.financeapp

// One record from Users/{userId}/transactions/{documentId}.
// Nullable amounts allow us to show missing data instead of pretending it is zero.
data class Transaction(
    val id: String,
    val accountId: String,
    val amount: Double?,
    val balanceAfter: Double?,
    val category: String,
    val date: String,
    val description: String,
    val type: String,
    val userId: String
)
