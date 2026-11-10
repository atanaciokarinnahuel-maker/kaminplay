package com.karin.kaminplay.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CatalogTest {
    private val films = SeedCatalog.films

    @Test
    fun idsAreUniqueAndUrlSafe() {
        assertEquals(films.size, films.map { it.id }.toSet().size)
        assertTrue(films.all { Regex("[a-z0-9-]+").matches(it.id) })
    }

    @Test
    fun searchIgnoresAccentsAndCase() {
        assertEquals(listOf("metropolis-1927"), CatalogSearch.search(films, "METROPOLIS").map { it.id })
        assertEquals(listOf("viaje-a-la-luna-1902"), CatalogSearch.search(films, "melies").map { it.id })
    }

    @Test
    fun searchMatchesDirectorYearAndCountry() {
        assertEquals(setOf("metropolis-1927", "perversidad-1945"), CatalogSearch.search(films, "lang").map { it.id }.toSet())
        assertEquals(setOf("detour-1945", "perversidad-1945"), CatalogSearch.search(films, "1945").map { it.id }.toSet())
        assertTrue(CatalogSearch.search(films, "alemania").all { it.country == "Alemania" })
    }

    @Test
    fun everyWordMustMatch() {
        assertEquals(listOf("perversidad-1945"), CatalogSearch.search(films, "lang 1945").map { it.id })
        assertTrue(CatalogSearch.search(films, "lang 1968").isEmpty())
    }

    @Test
    fun blankQueryReturnsEverythingAndFiltersNarrow() {
        assertEquals(films.size, CatalogSearch.search(films, "   ").size)
        val noir = CatalogSearch.search(films, "", genre = Genre.CINE_NEGRO)
        assertEquals(4, noir.size)
        val silent = CatalogSearch.search(films, "", era = Era.MUDO)
        assertTrue(silent.isNotEmpty() && silent.all { it.year < 1930 })
        assertEquals(setOf("caligari-1920", "nosferatu-1922", "fantasma-opera-1925"),
            CatalogSearch.search(films, "", genre = Genre.TERROR, era = Era.MUDO).map { it.id }.toSet())
    }

    @Test
    fun reelsFollowElevenMinuteRule() {
        assertEquals(14, SeedCatalog.byId("metropolis-1927")!!.reels)
        assertEquals(2, SeedCatalog.byId("viaje-a-la-luna-1902")!!.reels)
        assertEquals(1, Film("x", "x", "x", 1900, "x", 5, "x", Genre.DRAMA, Motif.LUNA, "x").reels)
    }

    @Test
    fun lookupsAndLinks() {
        assertNull(SeedCatalog.byId(null))
        assertNull(SeedCatalog.byId("no-existe"))
        val film = assertNotNull(SeedCatalog.byId("heroe-del-rio-1928"))
        assertEquals("https://archive.org/search?query=Steamboat+Bill%2C+Jr.+1928", film.archiveSearchUrl)
        assertTrue(SeedCatalog.similarTo(film).none { it.id == film.id })
        assertTrue(SeedCatalog.similarTo(film).all { it.genre == film.genre })
    }

    @Test
    fun profileInitial() {
        assertEquals("K", UserProfile("1", " karin", null, null).initial)
        assertEquals("?", UserProfile("1", "", null, null).initial)
    }
}
