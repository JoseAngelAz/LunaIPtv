package com.lunaiptv.core.update

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.OkHttpClient
import com.lunaiptv.BuildConfig

/**
 * In-app update checker — LunaIPtv fork does NOT check for updates from the original OwnTV repo.
 * Always reports UpToDate so the user never sees a misleading "update available" dialog.
 */
class UpdateManager(
    private val context: Context,
    private val client: OkHttpClient,
) {
    data class UpdateInfo(val version: String, val notes: String, val apkUrl: String)

    sealed interface State {
        data object Idle : State
        data object Checking : State
        data object UpToDate : State
        data class Available(val info: UpdateInfo) : State
        data class Downloading(val percent: Int) : State
        data class Failed(val message: String) : State
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state.asStateFlow()

    val currentVersion: String = BuildConfig.VERSION_NAME

    /** LunaIPtv fork — no remote update checks. Always reports UpToDate. */
    fun check() {
        if (_state.value is State.Checking || _state.value is State.Downloading) return
        _state.value = State.UpToDate
    }

    /** No-op — this is a personal fork. */
    fun downloadAndInstall() { /* no-op */ }

    fun reset() {
        if (_state.value !is State.Downloading) _state.value = State.Idle
    }
}
