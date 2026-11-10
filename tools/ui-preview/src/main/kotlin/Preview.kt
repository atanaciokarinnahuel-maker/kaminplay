import androidx.compose.runtime.Composable
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.unit.Density
import com.karin.kaminplay.core.CatalogSearch
import com.karin.kaminplay.core.Genre
import com.karin.kaminplay.core.SeedCatalog
import com.karin.kaminplay.core.UserProfile
import com.karin.kaminplay.core.rooms.ChatMessage
import com.karin.kaminplay.core.rooms.RoomMember
import com.karin.kaminplay.ui.home.RoomsUiState
import com.karin.kaminplay.ui.watch.PlaybackUi
import com.karin.kaminplay.ui.watch.RoomUi
import com.karin.kaminplay.ui.watch.SourceUi
import com.karin.kaminplay.ui.watch.WatchScreen
import com.karin.kaminplay.ui.watch.WatchUiState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.karin.kaminplay.ui.film.FilmScreen
import com.karin.kaminplay.ui.home.HomeShell
import com.karin.kaminplay.ui.home.KaminTab
import com.karin.kaminplay.ui.home.SearchState
import com.karin.kaminplay.ui.login.LoginScreen
import com.karin.kaminplay.ui.login.LoginUiState
import com.karin.kaminplay.ui.theme.KaminFonts
import com.karin.kaminplay.ui.theme.KaminTheme
import org.jetbrains.skia.EncodedImageFormat
import java.io.File

private val fontDir = File("../../app/src/main/res/font")
private val outDir = File("build/screens")

private fun font(name: String, weight: FontWeight) = Font(File(fontDir, "$name.ttf"), weight)

private val fonts = KaminFonts(
    display = FontFamily(font("big_shoulders_display_semibold", FontWeight.SemiBold), font("big_shoulders_display_extrabold", FontWeight.ExtraBold)),
    body = FontFamily(font("instrument_sans_regular", FontWeight.Normal), font("instrument_sans_medium", FontWeight.Medium), font("instrument_sans_semibold", FontWeight.SemiBold)),
    mono = FontFamily(font("dm_mono_regular", FontWeight.Normal), font("dm_mono_medium", FontWeight.Medium)),
)

/** Renders [content] at phone size (dp) and 2x density. */
private fun shot(name: String, dark: Boolean = true, width: Int = 412, height: Int = 915, content: @Composable () -> Unit) {
    val density = 2f
    val scene = ImageComposeScene((width * density).toInt(), (height * density).toInt(), Density(density)) {
        KaminTheme(dark, fonts) { content() }
    }
    File(outDir, "$name.png").writeBytes(scene.render().encodeToData(EncodedImageFormat.PNG)!!.bytes)
    scene.close()
    println("wrote ${outDir.path}/$name.png")
}

fun main() {
    outDir.mkdirs()
    val films = SeedCatalog.films
    val user = UserProfile("me", "Karin", "karin@example.com", null)

    @Composable
    fun shell(tab: KaminTab, search: SearchState = SearchState(), rooms: RoomsUiState = RoomsUiState()) = HomeShell(
        tab = tab, onTabChange = {}, user = user, films = films, search = search, onSearchChange = {},
        results = CatalogSearch.search(films, search.query, search.genre, search.era),
        rooms = rooms, versionName = "0.2.0", onOpenFilm = {}, onJoinRoom = {}, onSignOut = {},
    )

    @Composable
    fun film(id: String, rooms: RoomsUiState = RoomsUiState()) {
        val film = SeedCatalog.byId(id)!!
        FilmScreen(film, SeedCatalog.similarTo(film), rooms, onBack = {}, onOpenFilm = {}, onWatch = {}, onCreateRoom = {})
    }

    // Stand-in for the video surface, which only exists on Android.
    val video: @Composable (Modifier) -> Unit = { modifier -> Box(modifier.background(Color(0xFF15131A))) }

    @Composable
    fun watch(state: WatchUiState) = WatchScreen(state, video, {}, {}, {}, {}, {}, {})

    shot("login") { LoginScreen(LoginUiState(), {}, {}) }
    shot("login-unconfigured") { LoginScreen(LoginUiState(isConfigured = false, canPreview = true), {}, {}) }
    shot("home", height = 1500) { shell(KaminTab.INICIO) }
    shot("home-light", dark = false, height = 1500) { shell(KaminTab.INICIO) }
    shot("search") { shell(KaminTab.BUSCAR, SearchState(genre = Genre.TERROR)) }
    shot("search-empty") { shell(KaminTab.BUSCAR, SearchState(query = "zzz")) }
    shot("rooms") { shell(KaminTab.SALAS) }
    shot("rooms-error") { shell(KaminTab.SALAS, rooms = RoomsUiState(error = "No existe ninguna sala con el código ABC234.")) }
    shot("rooms-off", dark = false) { shell(KaminTab.SALAS, rooms = RoomsUiState(isAvailable = false)) }
    shot("profile") { shell(KaminTab.PERFIL) }
    shot("film", height = 1200) { film("fantasma-opera-1925") }
    shot("film-unplayable", dark = false, height = 1200) { film("metropolis-1927") }
    shot("film-no-rooms", height = 1200) { film("detour-1945", RoomsUiState(isAvailable = false)) }

    val detour = SeedCatalog.byId("detour-1945")!!
    val playing = PlaybackUi(isPlaying = true, positionMs = 1_234_000, durationMs = 4_080_000)
    val room = RoomUi(
        code = "KM7P2X", selfUid = "me", isConnected = true, driftMs = 40,
        members = listOf(RoomMember("me", "Karin", true), RoomMember("her", "Lucía", true)),
        messages = listOf(
            ChatMessage("1", "her", "Lucía", "ya estoy, dale play", 1),
            ChatMessage("2", "me", "Karin", "va", 2),
            ChatMessage("3", "her", "Lucía", "pausa un segundo que voy por agua", 3),
        ),
    )
    shot("watch-solo") { watch(WatchUiState(detour, SourceUi.Ready, playing)) }
    shot("watch-loading") { watch(WatchUiState(detour, SourceUi.Loading)) }
    shot("watch-failed") { watch(WatchUiState(detour, SourceUi.Failed("No se pudo contactar con Internet Archive. Revisa tu conexión."))) }
    shot("watch-room") { watch(WatchUiState(detour, SourceUi.Ready, playing, room)) }
    shot("watch-room-light", dark = false) { watch(WatchUiState(detour, SourceUi.Ready, playing.copy(isPlaying = false), room.copy(driftMs = -640, members = room.members.map { if (it.uid == "her") it.copy(online = false) else it }))) }
    shot("watch-room-empty") { watch(WatchUiState(detour, SourceUi.Ready, PlaybackUi(durationMs = 4_080_000), room.copy(messages = emptyList(), members = room.members.take(1), driftMs = null))) }
    shot("watch-room-landscape", width = 915, height = 412) { watch(WatchUiState(detour, SourceUi.Ready, playing, room)) }
}
