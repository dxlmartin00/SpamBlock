package com.spamblock.data

data class BlockedCall(
    val id: Long = 0,
    val phoneNumber: String,
    val timestamp: Long,
    val reason: String,
    val simSlot: Int = -1 // 0 for SIM 1, 1 for SIM 2, -1 for unknown
)
