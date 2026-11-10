package com.karin.kaminplay.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.karin.kaminplay.ui.components.KaminCard
import com.karin.kaminplay.ui.components.KaminLabel
import com.karin.kaminplay.ui.components.SectionHeader
import com.karin.kaminplay.ui.theme.Kamin

/** Placeholder until the watch-party step: says what is coming without fake controls. */
@Composable
fun RoomsScreen(contentPadding: PaddingValues) {
    val colors = Kamin.colors
    LazyColumn(Modifier.fillMaxSize(), contentPadding = contentPadding, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            SectionHeader("Salas")
        }
        item {
            Text("Una sala privada para ver la misma película a la vez, cada quien en su teléfono.", style = Kamin.type.body, color = colors.muted)
        }
        item {
            KaminCard {
                KaminLabel("En construcción")
                Text("Las salas llegan después del reproductor. Así van a funcionar:", style = Kamin.type.body, color = colors.fg)
            }
        }
        item {
            KaminCard {
                Text("Cualquiera controla", style = Kamin.type.title, color = colors.fg)
                Text("Si alguien pausa, adelanta o retrocede, cambia para toda la sala.", style = Kamin.type.small, color = colors.muted)
            }
        }
        item {
            KaminCard {
                Text("Un solo reloj", style = Kamin.type.title, color = colors.fg)
                Text("Cada teléfono calcula su posición con la hora del servidor y corrige los desfases sin que se note.", style = Kamin.type.small, color = colors.muted)
            }
        }
        item {
            KaminCard {
                Text("Entrada con código", style = Kamin.type.title, color = colors.fg)
                Text("Solo entra quien tenga el código de la sala. Pensadas para dos personas, con espacio para un grupo pequeño.", style = Kamin.type.small, color = colors.muted)
            }
        }
    }
}
