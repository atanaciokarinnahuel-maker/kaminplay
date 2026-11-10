package com.karin.kaminplay.core

import java.net.URLEncoder
import kotlin.math.ceil
import kotlin.math.max

/** Print tints, after the silent-era habit of dyeing each scene by its mood. */
enum class Tint { AMBER, BLUE, ROSE, GREEN, VIOLET }

enum class Genre(val label: String, val tint: Tint, val tintNote: String) {
    TERROR("Terror", Tint.BLUE, "Azul noche"),
    COMEDIA("Comedia", Tint.AMBER, "Ámbar de interior"),
    DRAMA("Drama", Tint.ROSE, "Rosa de atardecer"),
    CIENCIA_FICCION("Ciencia ficción", Tint.GREEN, "Verde de lo extraño"),
    CINE_NEGRO("Cine negro", Tint.VIOLET, "Violeta de madrugada"),
}

enum class Era(val label: String, val years: IntRange) {
    MUDO("Cine mudo", 1890..1929),
    ANOS_40("Años 40", 1940..1949),
    ANOS_50("Años 50", 1950..1959),
    ANOS_60("Años 60", 1960..1969),
}

/** Abstract shape drawn on the generated poster. */
enum class Motif { LUNA, ESCALERA, RELOJ, RIEL, CIUDAD, OJO, ONDAS, PERSIANA, VENTANA }

data class Film(
    val id: String,
    val title: String,
    val originalTitle: String,
    val year: Int,
    val director: String,
    val minutes: Int,
    val country: String,
    val genre: Genre,
    val motif: Motif,
    val synopsis: String,
) {
    /** A 300 m reel of 35 mm film runs about [REEL_MINUTES] minutes at 24 fps. */
    val reels: Int get() = max(1, ceil(minutes / REEL_MINUTES.toDouble()).toInt())

    val archiveSearchUrl: String
        get() = "https://archive.org/search?query=" + URLEncoder.encode("$originalTitle $year", "UTF-8")

    companion object {
        const val REEL_MINUTES = 11
    }
}
