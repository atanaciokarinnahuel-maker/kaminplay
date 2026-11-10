package com.karin.kaminplay.core

import java.text.Normalizer

object CatalogSearch {
    private val combiningMarks = Regex("\\p{M}+")

    /** Lower-cases and strips accents, so "metropolis" finds "Metrópolis". */
    fun normalize(text: String): String =
        Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD).replace(combiningMarks, "")

    /**
     * Every word of [query] must appear in the title, original title, director, year,
     * genre or country. [genre] and [era] narrow the result further.
     */
    fun search(films: List<Film>, query: String, genre: Genre? = null, era: Era? = null): List<Film> {
        val tokens = normalize(query).split(' ', '\t', '\n').filter { it.isNotEmpty() }
        return films.filter { film ->
            (genre == null || film.genre == genre) &&
                (era == null || film.year in era.years) &&
                film.haystack().let { hay -> tokens.all { it in hay } }
        }
    }

    private fun Film.haystack(): String =
        normalize(listOf(title, originalTitle, director, year.toString(), genre.label, country).joinToString(" "))
}
