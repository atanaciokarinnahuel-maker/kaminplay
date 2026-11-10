package com.karin.kaminplay.watch

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Color
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import com.karin.kaminplay.AppContainer
import com.karin.kaminplay.core.UserProfile
import com.karin.kaminplay.ui.watch.WatchScreen

/** Player screen for [request]. The view model, and with it the player, lives as long as this destination. */
@Composable
fun WatchRoute(request: WatchRequest, user: UserProfile, container: AppContainer, onBack: () -> Unit) {
    val app = LocalContext.current.applicationContext as Application
    val viewModel: WatchViewModel = viewModel(
        factory = WatchViewModel.Factory(app, request, user, container.archiveResolver, container.roomRepository),
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    val activity = LocalContext.current.findActivity()
    LifecycleStartEffect(viewModel) {
        viewModel.setInForeground(true)
        onStopOrDispose {
            // A rotation also stops the activity; only really leaving the screen should pause.
            if (activity?.isChangingConfigurations != true) viewModel.setInForeground(false)
        }
    }

    WatchScreen(
        state = state,
        video = { modifier -> VideoSurface(viewModel.player, modifier) },
        onBack = onBack,
        onTogglePlay = viewModel::togglePlay,
        onSeekTo = viewModel::seekTo,
        onSeekBy = viewModel::seekBy,
        onRetry = viewModel::load,
        onSendMessage = viewModel::sendMessage,
        modifier = Modifier.imePadding(),
    )
}

/** Media3's view without its own controls: the app draws them, so every tap is an explicit intent. */
@OptIn(UnstableApi::class)
@Composable
private fun VideoSurface(player: Player, modifier: Modifier) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            PlayerView(context).apply {
                useController = false
                keepScreenOn = true
                setShutterBackgroundColor(Color.BLACK)
                this.player = player
            }
        },
        update = { view -> view.player = player },
        onRelease = { view -> view.player = null },
    )
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
