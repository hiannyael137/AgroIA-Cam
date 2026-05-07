package com.hypv.agroiacam

data class Plant(
    val name: String,
    val status: String,
    val humidity: String,
    val lastWatering: String,
    val salud: Int = 100,
    val imagenUrl: String = "",
    val id: Int = 0
)