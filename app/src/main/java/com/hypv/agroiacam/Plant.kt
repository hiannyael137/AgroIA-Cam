
package com.hypv.agroiacam

data class Plant(

    val id: Int,

    val nombre_personalizado: String,

    val tipo_planta: String,

    val estado: String,

    val humedad: String,

    val ultimo_riego: String,

    val salud: Int,

    val imagen_url: String
)