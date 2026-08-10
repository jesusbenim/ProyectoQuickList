package com.proyectofinal.quicklist

data class RewardRequest(
    val id: String = "",
    val userId: String = "",
    val userEmail: String = "",
    val rewardName: String = "",
    val pointsRequired: Int = 0,
    val status: String = "pending",
    val usedAt: Any? = null
)