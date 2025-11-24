package com.proyectofinal.quicklist

data class Task(
    val id: String = "",
    val text: String = "",
    val status: String = "pending", // pending, in_progress, done
    val photoUrl: String? = null
)

