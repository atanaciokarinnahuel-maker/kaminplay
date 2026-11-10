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
import com.karin.kaminplay.rooms.RoomsViewModel
import com.karin.kaminplay.ui.home.RoomsUiState
import com.karin.kaminplay.watch.WatchRequest
import com.karin.kaminplay.watch.WatchRoute
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
private const val ROUTE_WATCH = "watch/{id}"
private const val ROUTE_ROOM = "room/{code}"

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
            container = container,
            user = current.user,
            versionName = versionName,
            onSignOut = { authViewModel.signOut(context) },
        )
    }
}

@Composable
private fun SignedInNav(container: AppContainer, user: UserProfile, versionName: String, onSignOut: () -> Unit) {
    val navController = rememberNavController()
    val roomsViewModel: RoomsViewModel = viewModel(factory = RoomsViewModel.factory(container.roomRepository))
    val rooms by roomsViewModel.state.collectAsStateWithLifecycle()

    // A room that was just created or joined opens its player.
    LaunchedEffect(roomsViewModel, navController) {
        roomsViewModel.opened.collect { code -> navController.navigate("room/$code") }
    }

    NavHost(navController = navController, startDestination = ROUTE_HOME) {
        composable(ROUTE_HOME) {
            HomeRoute(
                user = user,
                rooms = rooms,
                versionName = versionName,
                onOpenFilm = { id -> roomsViewModel.clearError(); navController.navigate("film/$id") },
                onJoinRoom = { code -> roomsViewModel.join(code, user) },
                onSignOut = onSignOut,
            )
        }
        composable(ROUTE_FILM, arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
            val film = SeedCatalog.byId(entry.arguments?.getString("id"))
            if (film == null) {
                LaunchedEffect(Unit) { navController.popBackStack() }
            } else {
                FilmScreen(
                    film = film,
                    similar = remember(film) { SeedCatalog.similarTo(film) },
                    rooms = rooms,
                    onBack = { navController.popBackStack() },
                    onOpenFilm = { other -> roomsViewModel.clearError(); navController.navigate("film/${other.id}") },
                    onWatch = { navController.navigate("watch/${film.id}") },
                    onCreateRoom = { roomsViewModel.create(film.id, user) },
                )
            }
        }
        composable(ROUTE_WATCH, arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
            val filmId = entry.arguments?.getString("id").orEmpty()
            WatchRoute(WatchRequest.Solo(filmId), user, container, onBack = { navController.popBackStack() })
        }
        composable(ROUTE_ROOM, arguments = listOf(navArgument("code") { type = NavType.StringType })) { entry ->
            val code = entry.arguments?.getString("code").orEmpty()
            WatchRoute(WatchRequest.Party(code), user, container, onBack = { navController.popBackStack() })
        }
    }
}

@Composable
private fun HomeRoute(
    user: UserProfile,
    rooms: RoomsUiState,
    versionName: String,
    onOpenFilm: (String) -> Unit,
    onJoinRoom: (String) -> Unit,
    onSignOut: () -> Unit,
) {
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
        rooms = rooms,
        versionName = versionName,
        onOpenFilm = { onOpenFilm(it.id) },
        onJoinRoom = onJoinRoom,
        onSignOut = onSignOut,
    )
}
