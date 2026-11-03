
package com.example.financeapp

// Represents a bank account retrieved from Firebase Firestore.
data class Account(

    // Firestore document ID (e.g., CHK001 or SAV001).
    val id: String,

    // Account's display name (e.g., Everyday Checking).
    val name: String,

    // Account type: Checking, Savings, or Credit Card.
    val type: String,

    // Currency used by this account (e.g., USD).
    val currency: String,

    // Current balance retrieved from Firestore.
    // Nullable because a document might not have a valid balance.
    val balance: Double?
)
