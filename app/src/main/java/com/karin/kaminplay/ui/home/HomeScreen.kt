package com.karin.kaminplay.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.karin.kaminplay.core.Era
import com.karin.kaminplay.core.Film
import com.karin.kaminplay.core.Genre
import com.karin.kaminplay.ui.components.KaminIcons
import com.karin.kaminplay.ui.components.PerforationStrip
import com.karin.kaminplay.ui.components.Poster
import com.karin.kaminplay.ui.components.SectionHeader
import com.karin.kaminplay.ui.components.Wordmark
import com.karin.kaminplay.ui.theme.Kamin

@Composable
fun HomeScreen(
    films: List<Film>,
    contentPadding: PaddingValues,
    onOpenFilm: (Film) -> Unit,
    onOpenSearch: (SearchState) -> Unit,
) {
    val colors = Kamin.colors
    val silent = remember(films) { films.filter { it.year in Era.MUDO.years } }
    val noir = remember(films) { films.filter { it.genre == Genre.CINE_NEGRO } }
    val lateNight = remember(films) { films.filter { it.year >= 1950 && (it.genre == Genre.TERROR || it.genre == Genre.CIENCIA_FICCION) } }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = contentPadding, verticalArrangement = Arrangement.spacedBy(28.dp)) {
        item(key = "hero") {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                PerforationStrip()
                Wordmark()
                PerforationStrip()
                Text(
                    "${films.size} películas de dominio público, de ${films.minOf { it.year }} a ${films.maxOf { it.year }}, para ver a solas o en sala.",
                    style = Kamin.type.body,
                    color = colors.muted,
                )
            }
        }
        item(key = "search") {
            // Opens the search tab; the real text field lives there.
            val shape = RoundedCornerShape(50)
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(colors.surface)
                    .border(1.dp, colors.line, shape)
                    .clickable(role = Role.Button, onClickLabel = "Abrir la búsqueda") { onOpenSearch(SearchState()) }
                    .heightIn(min = 52.dp)
                    .padding(horizontal = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(KaminIcons.Search, contentDescription = null, tint = colors.muted, modifier = Modifier.size(20.dp))
                Text("Título, director, año o país", style = Kamin.type.body, color = colors.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        item(key = "tints") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionHeader("Por tinte")
                for (pair in Genre.entries.chunked(2)) {
                    // Intrinsic height keeps both tiles of a row the same size.
                    Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        for (genre in pair) {
                            GenreTile(
                                genre = genre,
                                count = films.count { it.genre == genre },
                                onClick = { onOpenSearch(SearchState(genre = genre)) },
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                            )
                        }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
        item(key = "silent") { FilmRail("Cine mudo imprescindible", silent, onOpenFilm) { onOpenSearch(SearchState(era = Era.MUDO)) } }
        item(key = "noir") { FilmRail("Madrugadas de cine negro", noir, onOpenFilm) { onOpenSearch(SearchState(genre = Genre.CINE_NEGRO)) } }
        item(key = "late") { FilmRail("Sesión golfa", lateNight, onOpenFilm, onSeeAll = null) }
        item(key = "note") {
            Text(
                "Los colores siguen la costumbre del cine mudo de teñir cada escena según su ambiente. " +
                    "Todas las obras están en dominio público en Estados Unidos; la situación cambia según el país y la copia.",
                style = Kamin.type.small,
                color = colors.muted,
            )
        }
    }
}

@Composable
private fun GenreTile(genre: Genre, count: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = Kamin.colors
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier
            .clip(shape)
            .background(colors.surface)
            .border(1.dp, colors.line, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Box(Modifier.width(28.dp).height(6.dp).clip(RoundedCornerShape(3.dp)).background(colors.tint(genre.tint)))
        Spacer(Modifier.height(6.dp))
        Text(genre.label, style = Kamin.type.h2.copy(fontSize = Kamin.type.title.fontSize * 1.3f, lineHeight = Kamin.type.title.lineHeight * 1.1f), color = colors.fg, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text("${genre.tintNote} · $count títulos", style = Kamin.type.small, color = colors.muted)
    }
}

@Composable
private fun FilmRail(title: String, films: List<Film>, onOpenFilm: (Film) -> Unit, onSeeAll: (() -> Unit)?) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader(title) {
            if (onSeeAll != null) {
                Text(
                    "Ver todas",
                    style = Kamin.type.small,
                    color = Kamin.colors.accentText,
                    modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable(role = Role.Button, onClick = onSeeAll).padding(horizontal = 4.dp, vertical = 8.dp),
                )
            }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(films, key = { it.id }) { film ->
                Poster(
                    film,
                    Modifier
                        .width(128.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(role = Role.Button, onClickLabel = "Abrir ${film.title}") { onOpenFilm(film) },
                )
            }
        }
    }
}
