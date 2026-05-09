package com.hypv.agroiacam

/**
 * Modelo único para las plantas de AgroIA.
 *
 * Este archivo corrige los errores:
 * - Unresolved reference: ultimo_riego
 * - Unresolved reference: salud
 * - Unresolved reference: imagen_url
 * - No value passed for parameter: ultima_actividad
 * - No value passed for parameter: temperatura
 *
 * Dejamos valores por defecto para que compile aunque otra pantalla no mande todos los campos.
 */
data class Plant(
    val id: Int = 0,
    val nombre_personalizado: String = "Mi planta",
    val tipo_planta: String = "planta",
    val estado: String = "Saludable",
    val humedad: String = "--%",

    // Campos nuevos usados por MainActivity y PlantAdapter
    val ultimo_riego: String = "Sin registro",
    val salud: Int = 100,
    val imagen_url: String = "",

    // Campos viejos / compatibilidad con pantallas anteriores
    val ultima_actividad: String = "Sin actividad",
    val temperatura: String = "--"
)
