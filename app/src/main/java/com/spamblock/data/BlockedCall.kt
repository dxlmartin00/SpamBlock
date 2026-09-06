package com.spamblock.data

data class BlockedCall(
    val id: Long = 0,
    val phoneNumber: String,
    val timestamp: Long,
    val reason: String
)
