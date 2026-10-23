package com.demo.upishop.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
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

    val colors = if (isSystemInDarkTheme()) {
        darkColorScheme(primary = Green, onPrimary = Color.White)
    } else {
        lightColorScheme(primary = Green, onPrimary = Color.White)
    }

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
            )
            Screen.CART -> CartScreen(
                state = state,
                onAdd = viewModel::add,
                onRemove = viewModel::remove,
                onBack = viewModel::back,
                onPay = viewModel::startCheckout,
            )
            Screen.CHECKOUT -> CheckoutScreen(
                state = state,
                viewModel = viewModel,
                onBack = viewModel::back,
                onRetry = viewModel::startCheckout,
            )
            Screen.RESULT -> ResultScreen(
                state = state,
                onDone = viewModel::back,
            )
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
