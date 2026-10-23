package com.demo.upishop.ui

import androidx.activity.compose.BackHandler
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.demo.upishop.Screen
import com.demo.upishop.ShopViewModel

private val Green = Color(0xFF0ABF53)

@Composable
fun ShopApp(viewModel: ShopViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showSettings by remember { mutableStateOf(false) }
    var showLogs by remember { mutableStateOf(false) }
    val onLogs = { showLogs = true }

    // Light only, so Compose matches the Adyen Component's views.
    val colors = lightColorScheme(
        primary = Green,
        onPrimary = Color.White,
        primaryContainer = Color(0xFFDDF7E6),
        onPrimaryContainer = Color(0xFF00391A),
        background = Color.White,
        onBackground = Color(0xFF111418),
        surface = Color.White,
        onSurface = Color(0xFF111418),
        surfaceVariant = Color(0xFFF2F4F5),
        onSurfaceVariant = Color(0xFF4A5157),
        tertiary = Color(0xFFB26A00),
        error = Color(0xFFC62828),
    )

    MaterialTheme(colorScheme = colors) {
        BackHandler(enabled = state.screen != Screen.SHOP) { viewModel.back() }

        when (state.screen) {
            Screen.SHOP -> ShopScreen(
                state = state,
                onAdd = viewModel::add,
                onRemove = viewModel::remove,
                onOpenCart = { viewModel.open(Screen.CART) },
                onSettings = { showSettings = true },
                onRetry = viewModel::loadConfig,
                onLogs = onLogs,
            )
            Screen.CART -> CartScreen(
                state = state,
                onAdd = viewModel::add,
                onRemove = viewModel::remove,
                onBack = viewModel::back,
                onPay = viewModel::startCheckout,
                onLogs = onLogs,
            )
            Screen.CHECKOUT -> CheckoutScreen(
                state = state,
                viewModel = viewModel,
                onBack = viewModel::back,
                onRetry = viewModel::startCheckout,
                onLogs = onLogs,
            )
            Screen.RESULT -> ResultScreen(
                state = state,
                onDone = viewModel::back,
                onLogs = onLogs,
            )
        }

        if (showLogs) {
            LogsDialog(onDismiss = { showLogs = false })
        }

        if (showSettings) {
            SettingsDialog(
                current = state.backendUrl,
                onSave = {
                    viewModel.setBackendUrl(it)
                    showSettings = false
                },
                onDismiss = { showSettings = false },
            )
        }
    }
}
