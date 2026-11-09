
package com.example.financeapp

// Represents a bank account retrieved from Firebase Firestore.
// Shared by both View Accounts and Transfer Money.
data class Account(

    // Firestore document ID, such as CHK001 or SAV001.
    val id: String,

    // Display name of the account.
    val name: String,

    // Checking, Savings, Credit Card, etc.
    val type: String,

    // Current balance of the account.
    val balance: Double,

    // Currency code, such as USD or JPY.
    val currency: String,

    // Date associated with the account balance.
    val asOfDate: String,

    // Credit limit, if this is a credit account.
    val creditLimit: Double?
)
