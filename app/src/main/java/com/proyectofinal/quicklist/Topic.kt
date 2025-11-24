package com.proyectofinal.quicklist

//Usamos data class porque nos da automáticamente métodos útiles como toString() y copy() y
// es lo que Firestore entiende mejor
data class Topic(
    val id: String = "",
    val name: String = "",
    val members: List<String> = emptyList() // lista de emails o userIds
)
