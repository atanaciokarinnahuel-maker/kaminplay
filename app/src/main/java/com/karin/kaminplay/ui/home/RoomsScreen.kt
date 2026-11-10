package com.karin.kaminplay.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.karin.kaminplay.core.rooms.RoomCode
import com.karin.kaminplay.ui.components.KaminButton
import com.karin.kaminplay.ui.components.KaminCard
import com.karin.kaminplay.ui.components.KaminLabel
import com.karin.kaminplay.ui.components.SectionHeader
import com.karin.kaminplay.ui.theme.Kamin

@Immutable
data class RoomsUiState(
    /** False when the build has no Firebase or the project has no Realtime Database. */
    val isAvailable: Boolean = true,
    /** A room is being created or joined. */
    val isBusy: Boolean = false,
    val error: String? = null,
)

@Composable
fun RoomsScreen(state: RoomsUiState, contentPadding: PaddingValues, onJoin: (String) -> Unit) {
    val colors = Kamin.colors
    var code by rememberSaveable { mutableStateOf("") }
    val canJoin = state.isAvailable && !state.isBusy && code.isNotBlank()

    LazyColumn(Modifier.fillMaxSize(), contentPadding = contentPadding, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { SectionHeader("Salas") }
        item {
            Text("Una sala privada para ver la misma película a la vez, cada quien en su teléfono.", style = Kamin.type.body, color = colors.muted)
        }
        item {
            KaminCard {
                KaminLabel("Unirme con código")
                val shape = RoundedCornerShape(12.dp)
                Box(
                    Modifier.fillMaxWidth().clip(shape).background(colors.bg).border(1.dp, colors.line, shape).heightIn(min = 56.dp).padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    BasicTextField(
                        value = code,
                        onValueChange = { input -> code = input.uppercase().filter { it.isLetterOrDigit() }.take(RoomCode.LENGTH) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = state.isAvailable,
                        singleLine = true,
                        textStyle = Kamin.type.h2.copy(fontFamily = Kamin.fonts.mono, fontSize = 24.sp, letterSpacing = 0.2.em, color = colors.fg),
                        cursorBrush = SolidColor(colors.accentText),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Go),
                        keyboardActions = KeyboardActions(onGo = { if (canJoin) onJoin(code) }),
                        decorationBox = { inner ->
                            if (code.isEmpty()) Text("ABC234", style = Kamin.type.h2.copy(fontFamily = Kamin.fonts.mono, fontSize = 24.sp, letterSpacing = 0.2.em), color = colors.line)
                            inner()
                        },
                    )
                }
                if (state.error != null) Text(state.error, style = Kamin.type.small, color = colors.rose)
                KaminButton(if (state.isBusy) "Entrando…" else "Unirme", onClick = { onJoin(code) }, enabled = canJoin, modifier = Modifier.fillMaxWidth())
            }
        }
        if (!state.isAvailable) {
            item {
                KaminCard {
                    KaminLabel("Salas apagadas")
                    Text(
                        "Esta compilación no tiene Firebase conectado o el proyecto no tiene Realtime Database. Los pasos están en el README.",
                        style = Kamin.type.small,
                        color = colors.fg,
                    )
                }
            }
        }
        item {
            KaminCard {
                Text("Crear una sala", style = Kamin.type.title, color = colors.fg)
                Text("Abre la ficha de una película y pulsa «Crear sala». Te dará un código para compartir.", style = Kamin.type.small, color = colors.muted)
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Cualquiera controla: si alguien pausa o salta, cambia para toda la sala.", style = Kamin.type.small, color = colors.muted)
                Text("Si a alguien se le atasca el vídeo, la sala espera y sigue sola cuando vuelve.", style = Kamin.type.small, color = colors.muted)
            }
        }
    }
}
