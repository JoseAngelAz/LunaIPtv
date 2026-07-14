package com.lunaiptv.phone.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lunaiptv.phone.di.PhoneSettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneNetworkSettingsScreen(
    vm: PhoneSettingsViewModel,
    onBack: () -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val config by vm.proxyConfig.collectAsStateWithLifecycle()

    var seeded by remember { mutableStateOf(false) }
    var enabled by remember { mutableStateOf(false) }
    var host by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("") }
    var user by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }

    LaunchedEffect(config) {
        if (!seeded) {
            enabled = config.enabled
            host = config.host
            port = if (config.port > 0) config.port.toString() else ""
            user = config.username
            pass = config.password
            seeded = true
        }
    }

    var testState by remember { mutableStateOf<ProxyTestState>(ProxyTestState.Idle) }

    val portInt = port.trim().toIntOrNull() ?: 0

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text("Network / Proxy") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Spacer(Modifier.height(8.dp))

            // ── HTTP Proxy toggle ──────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Use proxy", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        "Route all app traffic through an HTTP proxy",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = {
                        enabled = it
                        vm.saveProxy(enabled, host, portInt, user, pass)
                    },
                )
            }

            Spacer(Modifier.height(16.dp))

            // ── Host ──────────────────────────────────────────
            OutlinedTextField(
                value = host,
                onValueChange = { host = it },
                label = { Text("Host") },
                placeholder = { Text("proxy.example.com") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(12.dp))

            // ── Port ──────────────────────────────────────────
            OutlinedTextField(
                value = port,
                onValueChange = { port = it.filter { c -> c.isDigit() }.take(5) },
                label = { Text("Port") },
                placeholder = { Text("8080") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.width(160.dp),
            )

            Spacer(Modifier.height(12.dp))

            // ── Username ──────────────────────────────────────
            OutlinedTextField(
                value = user,
                onValueChange = { user = it },
                label = { Text("Username (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(12.dp))

            // ── Password ──────────────────────────────────────
            OutlinedTextField(
                value = pass,
                onValueChange = { pass = it },
                label = { Text("Password (optional)") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(20.dp))

            // ── Action buttons ────────────────────────────────
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = {
                        vm.saveProxy(enabled, host, portInt, user, pass)
                        testState = ProxyTestState.Idle
                    },
                ) {
                    Text("Save")
                }
                OutlinedButton(
                    onClick = {
                        if (host.isBlank() || portInt !in 1..65535) {
                            testState = ProxyTestState.Fail("Enter a valid host and port (1\u201365535).")
                            return@OutlinedButton
                        }
                        testState = ProxyTestState.Testing
                    },
                    enabled = testState !is ProxyTestState.Testing,
                ) {
                    Text(if (testState is ProxyTestState.Testing) "Testing\u2026" else "Test proxy")
                }
            }

            // ── Test result ───────────────────────────────────
            when (val s = testState) {
                is ProxyTestState.Testing -> {
                    LaunchedEffect(host, portInt, user, pass) {
                        val result = withContext(Dispatchers.IO) {
                            runCatching {
                                val proxy = java.net.Proxy(
                                    java.net.Proxy.Type.HTTP,
                                    java.net.InetSocketAddress.createUnresolved(host.trim(), portInt),
                                )
                                val builder = okhttp3.OkHttpClient.Builder()
                                    .proxy(proxy)
                                    .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                                    .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                                if (user.trim().isNotBlank()) {
                                    builder.proxyAuthenticator { _, response ->
                                        if (response.request.header("Proxy-Authorization") != null) return@proxyAuthenticator null
                                        response.request.newBuilder()
                                            .header("Proxy-Authorization", okhttp3.Credentials.basic(user.trim(), pass))
                                            .build()
                                    }
                                } else {
                                    builder.proxyAuthenticator(okhttp3.Authenticator.NONE)
                                }
                                val client = builder.build()
                                val request = okhttp3.Request.Builder()
                                    .url("https://www.gstatic.com/generate_204")
                                    .head()
                                    .build()
                                val start = System.currentTimeMillis()
                                client.newCall(request).execute().use { resp ->
                                    if (!resp.isSuccessful && resp.code != 204) {
                                        throw java.io.IOException("Proxy returned HTTP ${resp.code}")
                                    }
                                }
                                System.currentTimeMillis() - start
                            }
                        }
                        testState = result.fold(
                            onSuccess = { ProxyTestState.Ok(it) },
                            onFailure = { ProxyTestState.Fail(friendlyProxyError(it)) },
                        )
                    }
                }
                is ProxyTestState.Ok -> {
                    Text(
                        text = "Connected \u2713 (${s.millis} ms)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
                is ProxyTestState.Fail -> {
                    Text(
                        text = s.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
                else -> {}
            }

            Spacer(Modifier.height(24.dp))

            // ── Privacy note ──────────────────────────────────
            Text(
                "Your proxy provider can see which servers you connect to, and the contents of any non-HTTPS traffic.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "HTTP proxy only for now \u2014 SOCKS and per-source proxies aren\u2019t supported yet.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(32.dp))
        }
    }
}

// ── Local test state (mirrors TV's SettingsViewModel.ProxyTestState) ─────

private sealed interface ProxyTestState {
    data object Idle : ProxyTestState
    data object Testing : ProxyTestState
    data class Ok(val millis: Long) : ProxyTestState
    data class Fail(val message: String) : ProxyTestState
}

// ── Helpers ──────────────────────────────────────────────────────────────

private fun friendlyProxyError(t: Throwable): String = when (t) {
    is java.net.UnknownHostException -> "Can\u2019t reach the proxy host."
    is java.net.SocketTimeoutException -> "Connection to the proxy timed out."
    is java.net.ConnectException -> "Couldn\u2019t connect to the proxy (check host/port)."
    else -> t.message?.takeIf { it.isNotBlank() } ?: "Proxy test failed."
}
