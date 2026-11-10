package com.karin.kaminplay.ui.film

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.karin.kaminplay.core.Film
import com.karin.kaminplay.ui.components.KaminButton
import com.karin.kaminplay.ui.components.KaminIcons
import com.karin.kaminplay.ui.components.KaminLabel
import com.karin.kaminplay.ui.components.Poster
import com.karin.kaminplay.ui.components.SectionHeader
import com.karin.kaminplay.ui.home.GenreTag
import com.karin.kaminplay.ui.theme.Kamin

@Composable
fun FilmScreen(film: Film, similar: List<Film>, onBack: () -> Unit, onOpenFilm: (Film) -> Unit) {
    val colors = Kamin.colors
    val uriHandler = LocalUriHandler.current
    Scaffold(containerColor = colors.bg, contentColor = colors.fg) { padding ->
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 32.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item(key = "back") {
                Row(
                    Modifier.clip(RoundedCornerShape(8.dp)).clickable(role = Role.Button, onClick = onBack).padding(end = 12.dp, top = 10.dp, bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(KaminIcons.Back, contentDescription = null, tint = colors.muted, modifier = Modifier.size(20.dp))
                    Text("Volver", style = Kamin.type.body, color = colors.muted)
                }
            }
            item(key = "head") {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.Bottom) {
                    Poster(film, Modifier.width(132.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(film.title, style = Kamin.type.h1, color = colors.fg, modifier = Modifier.semantics { heading() })
                        if (film.originalTitle != film.title) {
                            Text(film.originalTitle, style = Kamin.type.smallItalic, color = colors.muted)
                        }
                        GenreTag(film.genre)
                    }
                }
            }
            item(key = "facts") {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Fact("Dirección", film.director)
                    Fact("Año", film.year.toString())
                    Fact("Duración", "${film.minutes} min")
                    Fact("País", film.country)
                    Fact("Rollos", "${film.reels} × ${Film.REEL_MINUTES} min")
                }
            }
            item(key = "synopsis") {
                Text(film.synopsis, style = Kamin.type.body, color = colors.fg)
            }
            item(key = "actions") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    KaminButton(
                        "Ver en Internet Archive",
                        onClick = { uriHandler.openUri(film.archiveSearchUrl) },
                        icon = KaminIcons.External,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        "El reproductor propio y las salas llegan en los siguientes pasos. Por ahora este botón abre la obra en Internet Archive. " +
                            "Una bobina de 300 m de película de 35 mm dura unos ${Film.REEL_MINUTES} minutos; de ahí salen los rollos.",
                        style = Kamin.type.small,
                        color = colors.muted,
                    )
                }
            }
            if (similar.isNotEmpty()) {
                item(key = "similar") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SectionHeader("Más ${film.genre.label.lowercase()}")
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(similar, key = { it.id }) { other ->
                                Poster(
                                    other,
                                    Modifier
                                        .width(112.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable(role = Role.Button, onClickLabel = "Abrir ${other.title}") { onOpenFilm(other) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Fact(label: String, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        KaminLabel(label, Modifier.width(92.dp))
        Text(value, style = Kamin.type.body, color = Kamin.colors.fg, modifier = Modifier.weight(1f))
    }
}
