package com.karin.kaminplay.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.karin.kaminplay.core.Era
import com.karin.kaminplay.core.Film
import com.karin.kaminplay.core.Genre
import com.karin.kaminplay.ui.components.KaminChip
import com.karin.kaminplay.ui.components.KaminLabel
import com.karin.kaminplay.ui.components.Poster
import com.karin.kaminplay.ui.components.SearchField
import com.karin.kaminplay.ui.components.SectionHeader
import com.karin.kaminplay.ui.theme.Kamin

@Composable
fun SearchScreen(
    state: SearchState,
    onStateChange: (SearchState) -> Unit,
    results: List<Film>,
    contentPadding: PaddingValues,
    onOpenFilm: (Film) -> Unit,
) {
    val colors = Kamin.colors
    LazyColumn(Modifier.fillMaxSize(), contentPadding = contentPadding, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item(key = "header") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionHeader("Buscar")
                SearchField(state.query, { onStateChange(state.copy(query = it)) }, placeholder = "Título, director, año o país")
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (genre in Genre.entries) {
                        KaminChip(
                            label = genre.label,
                            selected = state.genre == genre,
                            onClick = { onStateChange(state.copy(genre = if (state.genre == genre) null else genre)) },
                            dot = colors.tint(genre.tint),
                        )
                    }
                }
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (era in Era.entries) {
                        KaminChip(
                            label = era.label,
                            selected = state.era == era,
                            onClick = { onStateChange(state.copy(era = if (state.era == era) null else era)) },
                        )
                    }
                }
                KaminLabel(if (results.size == 1) "1 película" else "${results.size} películas", Modifier.padding(top = 8.dp))
            }
        }
        if (results.isEmpty()) {
            item(key = "empty") {
                val shape = RoundedCornerShape(14.dp)
                Column(
                    Modifier.fillMaxWidth().clip(shape).border(1.dp, colors.line, shape).padding(horizontal = 20.dp, vertical = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        if (state.query.isBlank()) "Ninguna película coincide" else "Ninguna película coincide con «${state.query.trim()}»",
                        style = Kamin.type.title,
                        color = colors.fg,
                    )
                    Text("Prueba con un director, un año o quita algún filtro.", style = Kamin.type.small, color = colors.muted)
                }
            }
        } else {
            items(results, key = { it.id }) { film -> FilmRow(film) { onOpenFilm(film) } }
        }
    }
}

@Composable
fun FilmRow(film: Film, onClick: () -> Unit) {
    val colors = Kamin.colors
    val shape = RoundedCornerShape(14.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surface)
            .border(1.dp, colors.line, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Poster(film, Modifier.width(72.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(film.title, style = Kamin.type.title, color = colors.fg, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (film.originalTitle != film.title) {
                Text(film.originalTitle, style = Kamin.type.smallItalic, color = colors.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text("${film.director} · ${film.year} · ${film.minutes} min", style = Kamin.type.small, color = colors.muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
            GenreTag(film.genre)
        }
    }
}

@Composable
fun GenreTag(genre: Genre, modifier: Modifier = Modifier) {
    val colors = Kamin.colors
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(colors.tint(genre.tint)))
        Text(genre.label, style = Kamin.type.small, color = colors.fg)
    }
}
