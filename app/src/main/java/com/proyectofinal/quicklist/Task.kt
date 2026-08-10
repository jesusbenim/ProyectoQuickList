package com.proyectofinal.quicklist

data class Task(
    val id: String,
    val text: String,
    val status: String,
    val dueDate: String = "",
    val completedBy: String = "",
    val completedByEmail: String = "",
    val requiresEvidence: Boolean = false,
    val evidenceUrl: String = "",
    val approved: Boolean = false,
    val evidenceUploadedBy: String = "",
    val evidenceUploadedById: String = "",
    val evidenceRejected: Boolean = false

)

