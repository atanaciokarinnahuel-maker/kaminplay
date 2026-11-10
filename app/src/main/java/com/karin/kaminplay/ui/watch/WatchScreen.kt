package com.karin.kaminplay.ui.watch

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.karin.kaminplay.core.Film
import com.karin.kaminplay.core.rooms.ChatMessage
import com.karin.kaminplay.core.rooms.RoomMember
import com.karin.kaminplay.core.rooms.formatClock
import com.karin.kaminplay.ui.components.KaminButton
import com.karin.kaminplay.ui.components.KaminIcons
import com.karin.kaminplay.ui.components.KaminLabel
import com.karin.kaminplay.ui.theme.Kamin
import kotlin.math.abs

sealed interface SourceUi {
    /** Looking the film up on Internet Archive (and, in a room, joining it). */
    data object Loading : SourceUi
    data object Ready : SourceUi
    data class Failed(val message: String) : SourceUi
}

@Immutable
data class PlaybackUi(
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val positionMs: Long = 0,
    /** 0 until the player knows it. */
    val durationMs: Long = 0,
)

@Immutable
data class RoomUi(
    val code: String,
    val selfUid: String,
    val members: List<RoomMember> = emptyList(),
    val messages: List<ChatMessage> = emptyList(),
    val isConnected: Boolean = false,
    /** Local position minus the room's; null until the engine has run. */
    val driftMs: Long? = null,
    val problem: String? = null,
)

@Immutable
data class WatchUiState(
    val film: Film? = null,
    val source: SourceUi = SourceUi.Loading,
    val playback: PlaybackUi = PlaybackUi(),
    /** Null when watching alone. */
    val room: RoomUi? = null,
)

/**
 * Player screen, alone or in a room. [video] is the platform video surface; it is a slot so
 * this file stays free of Android dependencies.
 */
@Composable
fun WatchScreen(
    state: WatchUiState,
    video: @Composable (Modifier) -> Unit,
    onBack: () -> Unit,
    onTogglePlay: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSeekBy: (Long) -> Unit,
    onRetry: () -> Unit,
    onSendMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Kamin.colors
    Scaffold(modifier = modifier, containerColor = colors.bg, contentColor = colors.fg) { padding ->
        BoxWithConstraints(Modifier.fillMaxSize().padding(padding)) {
            if (maxWidth > maxHeight) {
                // Landscape: video on the left, everything else in a side column.
                Row(Modifier.fillMaxSize()) {
                    VideoBox(state, video, onRetry, Modifier.weight(1.7f).fillMaxHeight())
                    Column(Modifier.weight(1f).fillMaxHeight()) {
                        TitleRow(state, onBack)
                        Transport(state, onTogglePlay, onSeekTo, onSeekBy)
                        Details(state, onSendMessage, Modifier.weight(1f))
                    }
                }
            } else {
                Column(Modifier.fillMaxSize()) {
                    TitleRow(state, onBack)
                    VideoBox(state, video, onRetry, Modifier.fillMaxWidth().aspectRatio(16f / 9f))
                    Transport(state, onTogglePlay, onSeekTo, onSeekBy)
                    Details(state, onSendMessage, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun TitleRow(state: WatchUiState, onBack: () -> Unit) {
    val colors = Kamin.colors
    Row(
        Modifier.fillMaxWidth().padding(start = 6.dp, end = 16.dp, top = 4.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(48.dp).clip(CircleShape).clickable(role = Role.Button, onClickLabel = "Salir", onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(KaminIcons.Back, contentDescription = "Salir", tint = colors.fg, modifier = Modifier.size(22.dp))
        }
        Text(
            state.film?.title ?: if (state.room != null) "Sala ${state.room.code}" else "",
            style = Kamin.type.title,
            color = colors.fg,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun VideoBox(state: WatchUiState, video: @Composable (Modifier) -> Unit, onRetry: () -> Unit, modifier: Modifier) {
    // The picture area stays black in both themes.
    val onVideo = Kamin.colors.posterLight
    Box(modifier.background(Color.Black), contentAlignment = Alignment.Center) {
        video(Modifier.fillMaxSize())
        when (val source = state.source) {
            SourceUi.Loading -> Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                CircularProgressIndicator(Modifier.size(28.dp), color = onVideo, strokeWidth = 3.dp)
                Text("Buscando la copia en Internet Archive…", style = Kamin.type.small, color = onVideo)
            }
            is SourceUi.Failed -> Column(
                Modifier.padding(horizontal = 24.dp).widthIn(max = 420.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(source.message, style = Kamin.type.small, color = onVideo, textAlign = TextAlign.Center)
                KaminButton("Reintentar", onClick = onRetry)
            }
            SourceUi.Ready -> if (state.playback.isBuffering) {
                CircularProgressIndicator(Modifier.size(28.dp), color = onVideo, strokeWidth = 3.dp)
            }
        }
    }
}

@Composable
private fun Transport(state: WatchUiState, onTogglePlay: () -> Unit, onSeekTo: (Long) -> Unit, onSeekBy: (Long) -> Unit) {
    val colors = Kamin.colors
    val playback = state.playback
    val enabled = state.source == SourceUi.Ready
    val canSeek = enabled && playback.durationMs > 0
    // While the thumb is being dragged the slider follows the finger, not the player.
    var dragFraction by remember { mutableStateOf<Float?>(null) }
    val fraction = dragFraction ?: if (playback.durationMs > 0) playback.positionMs.toFloat() / playback.durationMs else 0f

    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).alpha(if (enabled) 1f else 0.5f)) {
        Slider(
            value = fraction.coerceIn(0f, 1f),
            onValueChange = { dragFraction = it },
            onValueChangeFinished = {
                dragFraction?.let { onSeekTo((it * playback.durationMs).toLong()) }
                dragFraction = null
            },
            enabled = canSeek,
            colors = SliderDefaults.colors(thumbColor = colors.accent, activeTrackColor = colors.accent, inactiveTrackColor = colors.surface2),
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            val shown = dragFraction?.let { (it * playback.durationMs).toLong() } ?: playback.positionMs
            Text(formatClock(shown), style = Kamin.type.mono, color = colors.fg, maxLines = 1, softWrap = false)
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
                SkipButton("−10 s", "Retroceder 10 segundos", canSeek) { onSeekBy(-10_000) }
                Box(
                    Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(colors.accent)
                        .clickable(enabled = enabled, role = Role.Button, onClick = onTogglePlay),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (playback.isPlaying) KaminIcons.Pause else KaminIcons.Play,
                        contentDescription = if (playback.isPlaying) "Pausa" else "Reproducir",
                        tint = colors.accentInk,
                        modifier = Modifier.size(26.dp),
                    )
                }
                SkipButton("+10 s", "Avanzar 10 segundos", canSeek) { onSeekBy(10_000) }
            }
            Text(if (playback.durationMs > 0) formatClock(playback.durationMs) else "–:––", style = Kamin.type.mono, color = colors.muted, maxLines = 1, softWrap = false)
        }
    }
}

@Composable
private fun SkipButton(label: String, description: String, enabled: Boolean, onClick: () -> Unit) {
    val colors = Kamin.colors
    val shape = RoundedCornerShape(12.dp)
    Box(
        Modifier
            .clip(shape)
            .background(colors.surface)
            .border(1.dp, colors.line, shape)
            .clickable(enabled = enabled, role = Role.Button, onClickLabel = description, onClick = onClick)
            .heightIn(min = 48.dp)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = Kamin.type.mono, color = colors.fg, maxLines = 1, softWrap = false)
    }
}

@Composable
private fun Details(state: WatchUiState, onSendMessage: (String) -> Unit, modifier: Modifier) {
    val room = state.room
    if (room != null) {
        RoomPanel(room, onSendMessage, modifier)
    } else {
        SoloInfo(state.film, modifier)
    }
}

@Composable
private fun SoloInfo(film: Film?, modifier: Modifier) {
    val colors = Kamin.colors
    LazyColumn(modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (film != null) {
            item { Text("${film.director} · ${film.year} · ${film.minutes} min", style = Kamin.type.small, color = colors.muted) }
            item { Text(film.synopsis, style = Kamin.type.body, color = colors.fg) }
            item { Text("Para verla en compañía, vuelve a la ficha y crea una sala.", style = Kamin.type.small, color = colors.muted) }
        }
    }
}

@Composable
private fun RoomPanel(room: RoomUi, onSendMessage: (String) -> Unit, modifier: Modifier) {
    val colors = Kamin.colors
    val clipboard = LocalClipboardManager.current
    Column(modifier.fillMaxWidth().padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                KaminLabel("Código de sala")
                Text(room.code, style = Kamin.type.h2.copy(fontFamily = Kamin.fonts.mono, fontSize = 24.sp, letterSpacing = 0.2.em), color = colors.fg)
            }
            KaminButton("Copiar", onClick = { clipboard.setText(AnnotatedString(room.code)) }, primary = false, icon = KaminIcons.Copy)
        }
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            for (member in room.members) {
                MemberChip(if (member.uid == room.selfUid) "Tú" else member.name.ifBlank { "Sin nombre" }, member.online)
            }
        }
        Text(syncStatus(room), style = Kamin.type.small, color = if (room.problem != null) colors.rose else colors.muted, modifier = Modifier.padding(horizontal = 16.dp))
        Chat(room, Modifier.weight(1f))
        MessageInput(enabled = room.isConnected, onSend = onSendMessage)
    }
}

private fun syncStatus(room: RoomUi): String {
    val drift = room.driftMs
    return when {
        room.problem != null -> room.problem
        !room.isConnected -> "Conectando con la sala…"
        drift == null -> "Conectada. Esperando el vídeo…"
        abs(drift) <= 150 -> "En sincronía"
        else -> "Ajustando: ${"%.1f".format(abs(drift) / 1000.0)} s ${if (drift > 0) "por delante" else "por detrás"}"
    }
}

@Composable
private fun MemberChip(name: String, online: Boolean) {
    val colors = Kamin.colors
    val shape = RoundedCornerShape(50)
    Row(
        Modifier.clip(shape).background(colors.surface).border(1.dp, colors.line, shape).padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(if (online) colors.green else colors.line))
        Text(name, style = Kamin.type.small, color = if (online) colors.fg else colors.muted, maxLines = 1)
        if (!online) Text("fuera", style = Kamin.type.small, color = colors.muted)
    }
}

@Composable
private fun Chat(room: RoomUi, modifier: Modifier) {
    val colors = Kamin.colors
    if (room.messages.isEmpty()) {
        Box(modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
            Text("Aún no hay mensajes. Comparte el código para que entre la otra persona.", style = Kamin.type.small, color = colors.muted, textAlign = TextAlign.Center)
        }
        return
    }
    // Reversed so the newest message sits at the bottom and stays in view.
    LazyColumn(
        modifier.fillMaxWidth(),
        reverseLayout = true,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(room.messages.asReversed(), key = { it.id }) { message ->
            val mine = message.uid == room.selfUid
            Column(Modifier.fillMaxWidth(), horizontalAlignment = if (mine) Alignment.End else Alignment.Start) {
                if (!mine) Text(message.name, style = Kamin.type.small, color = colors.muted, modifier = Modifier.padding(start = 4.dp, bottom = 2.dp))
                Text(
                    message.text,
                    style = Kamin.type.body,
                    color = if (mine) colors.accentInk else colors.fg,
                    modifier = Modifier
                        .widthIn(max = 300.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (mine) colors.accent else colors.surface)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun MessageInput(enabled: Boolean, onSend: (String) -> Unit) {
    val colors = Kamin.colors
    var text by rememberSaveable { mutableStateOf("") }
    val canSend = enabled && text.isNotBlank()
    fun send() {
        if (canSend) {
            onSend(text)
            text = ""
        }
    }
    val shape = RoundedCornerShape(50)
    Row(
        Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.weight(1f).clip(shape).background(colors.surface).border(1.dp, colors.line, shape).heightIn(min = 48.dp).padding(horizontal = 16.dp, vertical = 12.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            BasicTextField(
                value = text,
                onValueChange = { text = it.take(ChatMessage.MAX_LENGTH) },
                modifier = Modifier.fillMaxWidth(),
                enabled = enabled,
                maxLines = 3,
                textStyle = Kamin.type.body.copy(color = colors.fg),
                cursorBrush = SolidColor(colors.accentText),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { send() }),
                decorationBox = { inner ->
                    if (text.isEmpty()) Text("Escribe un mensaje", style = Kamin.type.body, color = colors.muted)
                    inner()
                },
            )
        }
        Box(
            Modifier
                .size(48.dp)
                .alpha(if (canSend) 1f else 0.5f)
                .clip(CircleShape)
                .background(colors.accent)
                .clickable(enabled = canSend, role = Role.Button, onClickLabel = "Enviar mensaje") { send() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(KaminIcons.Send, contentDescription = "Enviar mensaje", tint = colors.accentInk, modifier = Modifier.size(20.dp))
        }
    }
}
