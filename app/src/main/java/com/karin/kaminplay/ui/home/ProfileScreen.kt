package com.karin.kaminplay.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.karin.kaminplay.core.UserProfile
import com.karin.kaminplay.ui.components.KaminButton
import com.karin.kaminplay.ui.components.KaminCard
import com.karin.kaminplay.ui.components.KaminLabel
import com.karin.kaminplay.ui.components.SectionHeader
import com.karin.kaminplay.ui.theme.Kamin

@Composable
fun ProfileScreen(user: UserProfile, versionName: String, contentPadding: PaddingValues, onSignOut: () -> Unit) {
    val colors = Kamin.colors
    LazyColumn(Modifier.fillMaxSize(), contentPadding = contentPadding, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { SectionHeader("Perfil") }
        item {
            KaminCard {
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(56.dp).clip(CircleShape).background(colors.accent).clearAndSetSemantics { },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(user.initial, style = Kamin.type.h2, color = colors.accentInk)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(user.displayName.ifBlank { "Sin nombre" }, style = Kamin.type.title, color = colors.fg, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        val email = user.email
                        if (email != null) {
                            Text(email, style = Kamin.type.small, color = colors.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
        item {
            KaminButton("Cerrar sesión", onClick = onSignOut, primary = false, modifier = Modifier.fillMaxWidth())
        }
        item {
            KaminLabel("KAMINPLAY $versionName · sin anuncios")
        }
    }
}
