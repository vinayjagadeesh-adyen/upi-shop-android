package com.demo.upishop.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.demo.upishop.UiState

@Composable
fun ResultScreen(state: UiState, onDone: () -> Unit) {
    val result = state.result ?: return

    val (icon, title, color) = when {
        result.isSuccess -> Triple(Icons.Filled.CheckCircle, "Payment successful", MaterialTheme.colorScheme.primary)
        result.isPending -> Triple(Icons.Filled.Info, "Payment processing", MaterialTheme.colorScheme.tertiary)
        else -> Triple(Icons.Filled.Warning, "Payment not completed", MaterialTheme.colorScheme.error)
    }

    val explanation = when {
        result.isSuccess -> "Thanks! Your order is confirmed."
        result.isPending -> "Your bank hasn't confirmed yet. Check the Customer Area for the final status."
        else -> result.message ?: "The payment was ${result.resultCode.lowercase()}."
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(72.dp))
            Spacer(Modifier.height(16.dp))
            Text(title, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Text(explanation, textAlign = TextAlign.Center)
            Spacer(Modifier.height(24.dp))

            Detail("Result code", result.resultCode)
            result.merchantReference?.let { Detail("Reference", it) }
            result.pspReference?.let { Detail("PSP reference", it) }

            Spacer(Modifier.height(32.dp))
            Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
                Text(if (result.isSuccess) "Back to shop" else "Back")
            }
        }
    }
}

@Composable
private fun Detail(label: String, value: String) {
    Text(
        "$label: $value",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
