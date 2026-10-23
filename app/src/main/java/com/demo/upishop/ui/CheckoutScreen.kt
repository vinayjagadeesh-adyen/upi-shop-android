package com.demo.upishop.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.adyen.checkout.components.compose.AdyenComponent
import com.adyen.checkout.components.compose.get
import com.adyen.checkout.upi.UPIComponent
import com.demo.upishop.CheckoutSetup
import com.demo.upishop.ShopViewModel
import com.demo.upishop.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    state: UiState,
    viewModel: ShopViewModel,
    onBack: () -> Unit,
    onRetry: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pay with UPI") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            val checkout = state.checkout
            when {
                state.preparingCheckout -> CircularProgressIndicator(Modifier.align(Alignment.Center))

                checkout == null -> Column(
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(state.checkoutError ?: "Couldn't start checkout.", textAlign = TextAlign.Center)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = onRetry) { Text("Try again") }
                }

                else -> Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                ) {
                    Text("Amount to pay", style = MaterialTheme.typography.labelLarge)
                    Text(formatInr(checkout.amount), style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.height(16.dp))

                    UpiComponent(checkout, viewModel)

                    if (state.talkingToServer) {
                        Spacer(Modifier.height(16.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(12.dp))
                            Text("Contacting server…")
                        }
                    }
                }
            }
        }
    }
}

/**
 * The Adyen UPI Component (Advanced flow). It shows the UPI apps it finds on
 * the phone, fires onSubmit, then handles the "await" action itself: opens
 * the chosen UPI app, polls Adyen for the result and fires onAdditionalDetails.
 */
@Composable
private fun UpiComponent(checkout: CheckoutSetup, viewModel: ShopViewModel) {
    val activity = LocalContext.current.findActivity()

    val component = UPIComponent.PROVIDER.get(
        paymentMethod = checkout.paymentMethod,
        checkoutConfiguration = checkout.configuration,
        componentCallback = viewModel.componentCallback,
        key = checkout.componentKey,
    )

    // Hand actions from /payments (and /payments/details) to the Component.
    LaunchedEffect(component) {
        viewModel.actions.collect { action -> component.handleAction(action, activity) }
    }

    // Hand redirect returns (adyencheckout://com.demo.upishop) to the Component.
    LaunchedEffect(component) {
        viewModel.redirectIntents.collect { intent -> component.handleIntent(intent) }
    }

    AdyenComponent(component = component, modifier = Modifier.fillMaxWidth())
}

private tailrec fun Context.findActivity(): Activity = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> error("UPI Component needs an Activity")
}
