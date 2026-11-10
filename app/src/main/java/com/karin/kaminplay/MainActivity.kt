package com.karin.kaminplay

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import com.karin.kaminplay.nav.KaminRoot
import com.karin.kaminplay.platform.AndroidKaminFonts
import com.karin.kaminplay.ui.theme.KaminTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val container = (application as KaminApp).container
        setContent {
            KaminTheme(darkTheme = isSystemInDarkTheme(), fonts = AndroidKaminFonts) {
                KaminRoot(container = container, versionName = BuildConfig.VERSION_NAME)
            }
        }
    }
}
