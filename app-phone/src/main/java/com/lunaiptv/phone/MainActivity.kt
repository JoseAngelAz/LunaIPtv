package com.lunaiptv.phone

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
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
            val language by viewModel.language.collectAsStateWithLifecycle()

            LaunchedEffect(language) {
                com.lunaiptv.core.util.LocaleHelper.applyLanguageToActivity(this@MainActivity, language)
            }

            val darkTheme = when (themeMode) {
                com.lunaiptv.ui.theme.ThemeMode.DARK -> true
                com.lunaiptv.ui.theme.ThemeMode.LIGHT -> false
                com.lunaiptv.ui.theme.ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            // Splash screen state
            var showSplash by remember { mutableStateOf(true) }
            val splashAlpha = remember { Animatable(0f) }

            LaunchedEffect(Unit) {
                splashAlpha.animateTo(1f, animationSpec = tween(400))
                delay(3000L)
                splashAlpha.animateTo(0f, animationSpec = tween(400))
                showSplash = false
            }

            if (showSplash) {
                LunaSplashScreen(alpha = splashAlpha.value)
                return@setContent
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

@Composable
private fun LunaSplashScreen(alpha: Float) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(alpha)
            .background(Color(0xFF0A0E27)),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(24.dp)
                .size(24.dp),
            color = Color(0xFFC0C0C0),
            strokeWidth = 2.dp,
        )
        Image(
            painter = painterResource(id = R.drawable.splash_luna),
            contentDescription = "LunaIPtv",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit,
        )
    }
}
