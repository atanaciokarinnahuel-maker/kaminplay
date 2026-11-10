package com.karin.kaminplay.nav

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.karin.kaminplay.AppContainer
import com.karin.kaminplay.auth.AuthViewModel
import com.karin.kaminplay.core.CatalogSearch
import com.karin.kaminplay.core.Era
import com.karin.kaminplay.core.Genre
import com.karin.kaminplay.core.SeedCatalog
import com.karin.kaminplay.core.SessionState
import com.karin.kaminplay.core.UserProfile
import com.karin.kaminplay.ui.film.FilmScreen
import com.karin.kaminplay.ui.home.HomeShell
import com.karin.kaminplay.ui.home.KaminTab
import com.karin.kaminplay.ui.home.SearchState
import com.karin.kaminplay.ui.login.LoadingScreen
import com.karin.kaminplay.ui.login.LoginScreen

private const val ROUTE_HOME = "home"
private const val ROUTE_FILM = "film/{id}"

/** Session gate: nothing behind the login is composed until Firebase reports a signed-in user. */
@Composable
fun KaminRoot(container: AppContainer, versionName: String) {
    val authViewModel: AuthViewModel = viewModel(factory = AuthViewModel.factory(container.authRepository))
    val session by authViewModel.session.collectAsStateWithLifecycle()
    val login by authViewModel.login.collectAsStateWithLifecycle()
    val context = LocalContext.current

    when (val current = session) {
        SessionState.Loading -> LoadingScreen()
        SessionState.SignedOut -> LoginScreen(
            state = login,
            onGoogleSignIn = { authViewModel.signIn(context) },
            onPreview = authViewModel::enterPreview,
        )
        is SessionState.SignedIn -> SignedInNav(
            user = current.user,
            versionName = versionName,
            onSignOut = { authViewModel.signOut(context) },
        )
    }
}

@Composable
private fun SignedInNav(user: UserProfile, versionName: String, onSignOut: () -> Unit) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = ROUTE_HOME) {
        composable(ROUTE_HOME) {
            HomeRoute(user, versionName, onOpenFilm = { id -> navController.navigate("film/$id") }, onSignOut = onSignOut)
        }
        composable(ROUTE_FILM, arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
            val film = SeedCatalog.byId(entry.arguments?.getString("id"))
            if (film == null) {
                LaunchedEffect(Unit) { navController.popBackStack() }
            } else {
                FilmScreen(
                    film = film,
                    similar = remember(film) { SeedCatalog.similarTo(film) },
                    onBack = { navController.popBackStack() },
                    onOpenFilm = { other -> navController.navigate("film/${other.id}") },
                )
            }
        }
    }
}

@Composable
private fun HomeRoute(user: UserProfile, versionName: String, onOpenFilm: (String) -> Unit, onSignOut: () -> Unit) {
    var tab by rememberSaveable { mutableStateOf(KaminTab.INICIO) }
    var query by rememberSaveable { mutableStateOf("") }
    var genre by rememberSaveable { mutableStateOf<Genre?>(null) }
    var era by rememberSaveable { mutableStateOf<Era?>(null) }
    val films = SeedCatalog.films
    val results = remember(query, genre, era) { CatalogSearch.search(films, query, genre, era) }

    // Back from another tab returns to Inicio before leaving the app.
    BackHandler(enabled = tab != KaminTab.INICIO) { tab = KaminTab.INICIO }

    HomeShell(
        tab = tab,
        onTabChange = { tab = it },
        user = user,
        films = films,
        search = SearchState(query, genre, era),
        onSearchChange = { query = it.query; genre = it.genre; era = it.era },
        results = results,
        versionName = versionName,
        onOpenFilm = { onOpenFilm(it.id) },
        onSignOut = onSignOut,
    )
}
