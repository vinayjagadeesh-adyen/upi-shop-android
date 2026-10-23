package com.demo.upishop.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.demo.upishop.Product
import com.demo.upishop.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopScreen(
    state: UiState,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit,
    onOpenCart: () -> Unit,
    onSettings: () -> Unit,
    onRetry: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("UPI Shop") },
                actions = {
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Backend settings")
                    }
                },
            )
        },
        floatingActionButton = {
            if (state.cartCount > 0) {
                ExtendedFloatingActionButton(
                    onClick = onOpenCart,
                    icon = {
                        BadgedBox(badge = { Badge { Text("${state.cartCount}") } }) {
                            Icon(Icons.Filled.ShoppingCart, contentDescription = null)
                        }
                    },
                    text = { Text("Cart · ${formatInr(state.cartTotal())}") },
                )
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                state.loadingConfig -> CircularProgressIndicator(Modifier.align(Alignment.Center))

                state.config == null -> Column(
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        state.configError ?: "Couldn't load the shop.",
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(onClick = onSettings) { Text("Backend URL") }
                        if (state.backendUrl.isNotBlank()) Button(onClick = onRetry) { Text("Retry") }
                    }
                }

                else -> LazyColumn(
                    contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 96.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(state.config.items, key = { it.id }) { product ->
                        ProductCard(
                            product = product,
                            quantity = state.cart[product.id] ?: 0,
                            onAdd = { onAdd(product.id) },
                            onRemove = { onRemove(product.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductCard(product: Product, quantity: Int, onAdd: () -> Unit, onRemove: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(48.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        product.name.take(1),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(product.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    product.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Text(formatInr(product.price), fontWeight = FontWeight.SemiBold)
            }
            if (quantity == 0) {
                FilledIconButton(onClick = onAdd) {
                    Icon(Icons.Filled.Add, contentDescription = "Add ${product.name}")
                }
            } else {
                QuantityStepper(quantity, onAdd, onRemove)
            }
        }
    }
}
