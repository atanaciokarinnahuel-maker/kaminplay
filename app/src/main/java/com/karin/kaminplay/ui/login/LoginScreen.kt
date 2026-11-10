package com.karin.kaminplay.ui.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.karin.kaminplay.ui.components.KaminButton
import com.karin.kaminplay.ui.components.KaminCard
import com.karin.kaminplay.ui.components.KaminLabel
import com.karin.kaminplay.ui.components.PerforationStrip
import com.karin.kaminplay.ui.components.Wordmark
import com.karin.kaminplay.ui.theme.Kamin

@Immutable
data class LoginUiState(
    /** False until Firebase is connected (google-services.json present at build time). */
    val isConfigured: Boolean = true,
    /** Debug builds without Firebase can open the app without an account. */
    val canPreview: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
)

@Composable
fun LoginScreen(state: LoginUiState, onGoogleSignIn: () -> Unit, onPreview: () -> Unit) {
    val colors = Kamin.colors
    Scaffold(containerColor = colors.bg, contentColor = colors.fg) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            Column(
                Modifier
                    .widthIn(max = 520.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                PerforationStrip()
                Wordmark()
                PerforationStrip()
                Text(
                    "Cine de dominio público y sesiones compartidas. Sin anuncios.",
                    style = Kamin.type.body,
                    color = colors.muted,
                )
                Box(Modifier.padding(top = 16.dp)) {
                    KaminButton(
                        text = if (state.isLoading) "Abriendo Google…" else "Continuar con Google",
                        onClick = onGoogleSignIn,
                        enabled = state.isConfigured && !state.isLoading,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (state.error != null) {
                    Text(state.error, style = Kamin.type.small, color = colors.rose)
                }
                if (!state.isConfigured) {
                    KaminCard {
                        KaminLabel("Falta conectar Firebase")
                        Text(
                            "Esta compilación no incluye google-services.json, así que el inicio de sesión está apagado. Los pasos están en el README.",
                            style = Kamin.type.small,
                            color = colors.fg,
                        )
                        if (state.canPreview) {
                            KaminButton("Ver la interfaz sin cuenta", onClick = onPreview, primary = false, modifier = Modifier.fillMaxWidth())
                        }
                    }
                } else {
                    Text("Solo se usa tu nombre y tu correo de Google para identificarte en las salas.", style = Kamin.type.small, color = colors.muted)
                }
            }
        }
    }
}

/** Shown while Firebase restores the stored session. */
@Composable
fun LoadingScreen() {
    val colors = Kamin.colors
    Scaffold(containerColor = colors.bg, contentColor = colors.fg) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            Column(Modifier.widthIn(max = 280.dp).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                PerforationStrip()
                Wordmark()
                PerforationStrip()
            }
        }
    }
}
