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
    val user = UserProfile("1", "Karin", "karin@example.com", null)

    @Composable
    fun shell(tab: KaminTab, search: SearchState = SearchState()) = HomeShell(
        tab = tab, onTabChange = {}, user = user, films = films, search = search, onSearchChange = {},
        results = CatalogSearch.search(films, search.query, search.genre, search.era),
        versionName = "0.1.0", onOpenFilm = {}, onSignOut = {},
    )

    shot("login") { LoginScreen(LoginUiState(), {}, {}) }
    shot("login-unconfigured") { LoginScreen(LoginUiState(isConfigured = false, canPreview = true), {}, {}) }
    shot("login-error") { LoginScreen(LoginUiState(error = "No se pudo iniciar sesión. Comprueba tu conexión e inténtalo de nuevo."), {}, {}) }
    shot("home", height = 1500) { shell(KaminTab.INICIO) }
    shot("home-light", dark = false, height = 1500) { shell(KaminTab.INICIO) }
    shot("search") { shell(KaminTab.BUSCAR, SearchState(genre = Genre.TERROR)) }
    shot("search-empty") { shell(KaminTab.BUSCAR, SearchState(query = "zzz")) }
    shot("rooms") { shell(KaminTab.SALAS) }
    shot("profile") { shell(KaminTab.PERFIL) }
    val film = SeedCatalog.byId("fantasma-opera-1925")!!
    shot("film", height = 1100) { FilmScreen(film, SeedCatalog.similarTo(film), {}, {}) }
    shot("film-light", dark = false, height = 1100) { FilmScreen(film, SeedCatalog.similarTo(film), {}, {}) }
}
