package com.karin.kaminplay

import android.app.Application
import com.karin.kaminplay.auth.AuthRepository
import com.karin.kaminplay.auth.FirebaseAuthRepository
import com.karin.kaminplay.player.ArchiveResolver
import com.karin.kaminplay.rooms.FirebaseRoomRepository
import com.karin.kaminplay.rooms.RoomRepository

/** Manual dependency container: one instance of each app-wide service. */
class AppContainer(app: Application) {
    val authRepository: AuthRepository by lazy { FirebaseAuthRepository(app, allowPreview = BuildConfig.DEBUG) }
    val roomRepository: RoomRepository by lazy { FirebaseRoomRepository(app) }
    val archiveResolver: ArchiveResolver by lazy { ArchiveResolver() }
}

class KaminApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
