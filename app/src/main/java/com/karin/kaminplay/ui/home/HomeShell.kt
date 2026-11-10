package com.karin.kaminplay.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.karin.kaminplay.core.Era
import com.karin.kaminplay.core.Film
import com.karin.kaminplay.core.Genre
import com.karin.kaminplay.core.UserProfile
import com.karin.kaminplay.ui.components.KaminIcons
import com.karin.kaminplay.ui.theme.Kamin

enum class KaminTab(val label: String, val icon: ImageVector) {
    INICIO("Inicio", KaminIcons.Home),
    BUSCAR("Buscar", KaminIcons.Search),
    SALAS("Salas", KaminIcons.Rooms),
    PERFIL("Perfil", KaminIcons.Profile),
}

/** Search text and filters. Hoisted so "Ver todas" on the home tab can preset them. */
data class SearchState(val query: String = "", val genre: Genre? = null, val era: Era? = null)

@Composable
fun HomeShell(
    tab: KaminTab,
    onTabChange: (KaminTab) -> Unit,
    user: UserProfile,
    films: List<Film>,
    search: SearchState,
    onSearchChange: (SearchState) -> Unit,
    results: List<Film>,
    rooms: RoomsUiState,
    versionName: String,
    onOpenFilm: (Film) -> Unit,
    onJoinRoom: (String) -> Unit,
    onSignOut: () -> Unit,
) {
    val colors = Kamin.colors
    Scaffold(
        containerColor = colors.bg,
        contentColor = colors.fg,
        bottomBar = { KaminTabBar(tab, onTabChange) },
    ) { padding ->
        val contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = padding.calculateTopPadding() + 20.dp,
            bottom = padding.calculateBottomPadding() + 24.dp,
        )
        when (tab) {
            KaminTab.INICIO -> HomeScreen(
                films = films,
                contentPadding = contentPadding,
                onOpenFilm = onOpenFilm,
                onOpenSearch = { preset -> onSearchChange(preset); onTabChange(KaminTab.BUSCAR) },
            )
            KaminTab.BUSCAR -> SearchScreen(search, onSearchChange, results, contentPadding, onOpenFilm)
            KaminTab.SALAS -> RoomsScreen(rooms, contentPadding, onJoinRoom)
            KaminTab.PERFIL -> ProfileScreen(user, versionName, contentPadding, onSignOut)
        }
    }
}

@Composable
private fun KaminTabBar(selected: KaminTab, onSelect: (KaminTab) -> Unit) {
    val colors = Kamin.colors
    val shape = RoundedCornerShape(20.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .background(colors.bg)
            .windowInsetsPadding(ScaffoldDefaults.contentWindowInsets.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            Modifier
                .widthIn(max = 440.dp)
                .fillMaxWidth()
                .clip(shape)
                .background(colors.surface)
                .border(1.dp, colors.line, shape)
                .padding(6.dp),
        ) {
            for (tab in KaminTab.entries) {
                val isSelected = tab == selected
                val tint = if (isSelected) colors.fg else colors.muted
                Column(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isSelected) colors.surface2 else colors.surface)
                        .clickable(role = Role.Tab) { onSelect(tab) }
                        .semantics { this.selected = isSelected }
                        .padding(vertical = 8.dp, horizontal = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Icon(tab.icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
                    Text(tab.label, style = Kamin.type.small, color = tint, maxLines = 1)
                }
            }
        }
    }
}
