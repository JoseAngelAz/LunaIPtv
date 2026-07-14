package com.lunaiptv.phone

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel
import com.lunaiptv.phone.ui.PhoneShell
import com.lunaiptv.phone.ui.theme.LunaIPtvPhoneTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: com.lunaiptv.phone.di.PhoneViewModel = koinViewModel()
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val accent by viewModel.accent.collectAsStateWithLifecycle()
            val customAccent by viewModel.customAccent.collectAsStateWithLifecycle()

            val darkTheme = when (themeMode) {
                com.lunaiptv.ui.theme.ThemeMode.DARK -> true
                com.lunaiptv.ui.theme.ThemeMode.LIGHT -> false
                com.lunaiptv.ui.theme.ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            LunaIPtvPhoneTheme(
                darkTheme = darkTheme,
                accentColor = accent,
                customAccent = customAccent,
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    PhoneShell()
                }
            }
        }
    }
}
