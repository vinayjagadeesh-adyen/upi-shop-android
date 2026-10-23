package com.demo.upishop

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.adyen.checkout.components.core.ActionComponentData
import com.adyen.checkout.components.core.Amount
import com.adyen.checkout.components.core.CheckoutConfiguration
import com.adyen.checkout.components.core.ComponentCallback
import com.adyen.checkout.components.core.ComponentError
import com.adyen.checkout.components.core.PaymentComponentData
import com.adyen.checkout.components.core.PaymentMethod
import com.adyen.checkout.components.core.PaymentMethodTypes
import com.adyen.checkout.components.core.PaymentMethodsApiResponse
import com.adyen.checkout.components.core.action.Action
import com.adyen.checkout.core.Environment
import com.adyen.checkout.redirect.RedirectComponent
import com.adyen.checkout.upi.UPIComponent
import com.adyen.checkout.upi.UPIComponentState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.Locale
import java.util.UUID

enum class Screen { SHOP, CART, CHECKOUT, RESULT }

/** Everything the UPI Component needs to render. */
data class CheckoutSetup(
    val paymentMethod: PaymentMethod,
    val configuration: CheckoutConfiguration,
    /** New for every checkout, so the SDK creates a fresh Component. */
    val componentKey: String,
    val amount: Long,
)

data class PaymentResult(
    val resultCode: String,
    val message: String?,
    val merchantReference: String?,
    val pspReference: String?,
) {
    val isSuccess get() = resultCode == "Authorised"
    val isPending get() = resultCode == "Pending" || resultCode == "Received"
}

data class UiState(
    val backendUrl: String = "",
    val loadingConfig: Boolean = false,
    val config: AppConfig? = null,
    val configError: String? = null,
    /** product id -> quantity */
    val cart: Map<String, Int> = emptyMap(),
    val screen: Screen = Screen.SHOP,
    val preparingCheckout: Boolean = false,
    val checkout: CheckoutSetup? = null,
    val checkoutError: String? = null,
    /** True while the backend is handling /payments or /payments/details. */
    val talkingToServer: Boolean = false,
    val result: PaymentResult? = null,
) {
    val cartCount get() = cart.values.sum()
    fun cartTotal(): Long = config?.items.orEmpty().sumOf { (cart[it.id] ?: 0) * it.price }
}

class ShopViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("upishop", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(
        UiState(backendUrl = prefs.getString(KEY_BACKEND_URL, null) ?: BuildConfig.BACKEND_URL),
    )
    val state: StateFlow<UiState> = _state.asStateFlow()

    /** Actions from the backend, handed to component.handleAction() by the Checkout screen. */
    private val _actions = Channel<Action>(Channel.BUFFERED)
    val actions = _actions.receiveAsFlow()

    /** Redirect returns (adyencheckout://...), handed to component.handleIntent(). */
    private val _redirectIntents = MutableSharedFlow<Intent>(extraBufferCapacity = 1)
    val redirectIntents = _redirectIntents.asSharedFlow()

    private val returnUrl = RedirectComponent.getReturnUrl(application)

    private val api get() = BackendApi(_state.value.backendUrl)

    init {
        loadConfig()
    }

    // region Catalogue and cart

    fun loadConfig() {
        if (_state.value.backendUrl.isBlank()) {
            _state.update { it.copy(configError = "Set your Vercel backend URL to start (gear icon).") }
            return
        }
        _state.update { it.copy(loadingConfig = true, configError = null) }
        viewModelScope.launch {
            try {
                val config = api.config()
                _state.update { it.copy(config = config, loadingConfig = false) }
            } catch (e: Exception) {
                _state.update { it.copy(loadingConfig = false, configError = e.message) }
            }
        }
    }

    fun setBackendUrl(url: String) {
        val clean = url.trim().trimEnd('/')
        prefs.edit().putString(KEY_BACKEND_URL, clean).apply()
        _state.update { it.copy(backendUrl = clean, config = null, cart = emptyMap()) }
        loadConfig()
    }

    fun add(productId: String) = _state.update {
        val quantity = (it.cart[productId] ?: 0) + 1
        if (quantity > MAX_QUANTITY) it else it.copy(cart = it.cart + (productId to quantity))
    }

    fun remove(productId: String) = _state.update {
        val quantity = (it.cart[productId] ?: 0) - 1
        it.copy(cart = if (quantity <= 0) it.cart - productId else it.cart + (productId to quantity))
    }

    fun open(screen: Screen) = _state.update { it.copy(screen = screen) }

    fun back() = _state.update {
        when (it.screen) {
            Screen.CART -> it.copy(screen = Screen.SHOP)
            Screen.CHECKOUT -> it.copy(screen = Screen.CART, checkout = null, checkoutError = null, talkingToServer = false)
            Screen.RESULT -> it.copy(screen = Screen.SHOP, result = null)
            Screen.SHOP -> it
        }
    }

    // endregion

    // region Checkout

    /** Pay button: fetch payment methods for this cart, then show the UPI Component. */
    fun startCheckout() {
        val current = _state.value
        val config = current.config ?: return
        if (current.cart.isEmpty()) return

        _state.update {
            it.copy(screen = Screen.CHECKOUT, preparingCheckout = true, checkout = null, checkoutError = null)
        }

        viewModelScope.launch {
            try {
                val response = PaymentMethodsApiResponse.SERIALIZER.deserialize(api.paymentMethods(current.cart))
                val methods = response.paymentMethods.orEmpty()
                // Prefer the parent "upi" method (it carries the app list for Smart Intent).
                val upi = methods.firstOrNull { it.type == PaymentMethodTypes.UPI }
                    ?: methods.firstOrNull { it.type in UPIComponent.PAYMENT_METHOD_TYPES }
                    ?: throw BackendException(
                        "UPI isn't enabled on this merchant account. Methods returned: " +
                            methods.joinToString { it.type.orEmpty() }.ifBlank { "none" },
                    )

                val total = current.cartTotal()
                val configuration = CheckoutConfiguration(
                    environment = if (config.environment == "live-in") Environment.INDIA else Environment.TEST,
                    clientKey = config.clientKey,
                    shopperLocale = Locale("en", "IN"),
                    amount = Amount(currency = config.currency, value = total),
                )

                _state.update {
                    it.copy(
                        preparingCheckout = false,
                        checkout = CheckoutSetup(upi, configuration, UUID.randomUUID().toString(), total),
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(preparingCheckout = false, checkoutError = e.message) }
            }
        }
    }

    fun onRedirectIntent(intent: Intent) {
        _redirectIntents.tryEmit(intent)
    }

    /** Callbacks from the UPI Component. */
    val componentCallback: ComponentCallback<UPIComponentState> = object : ComponentCallback<UPIComponentState> {

        // Shopper picked an app and tapped Pay.
        override fun onSubmit(state: UPIComponentState) {
            if (!isCheckingOut()) return
            val cart = _state.value.cart
            val data = PaymentComponentData.SERIALIZER.serialize(state.data)
            _state.update { it.copy(talkingToServer = true, checkoutError = null) }

            viewModelScope.launch {
                try {
                    handleBackendResponse(api.payments(cart, data, returnUrl))
                } catch (e: Exception) {
                    finish(PaymentResult("Error", e.message, null, null))
                }
            }
        }

        // The await action finished polling (or a redirect came back).
        override fun onAdditionalDetails(actionComponentData: ActionComponentData) {
            if (!isCheckingOut()) return
            val data = ActionComponentData.SERIALIZER.serialize(actionComponentData)
            _state.update { it.copy(talkingToServer = true) }

            viewModelScope.launch {
                try {
                    handleBackendResponse(api.paymentsDetails(data))
                } catch (e: Exception) {
                    finish(PaymentResult("Error", e.message, null, null))
                }
            }
        }

        override fun onError(componentError: ComponentError) {
            if (!isCheckingOut()) return
            // Includes "Payment was not completed" when polling ends without a final result.
            finish(PaymentResult("Error", componentError.errorMessage, null, null))
        }
    }

    private var lastReference: String? = null

    /**
     * A Component from a checkout the shopper left (Back) can still be polling.
     * Its late callbacks must not pull the shopper onto the Result screen.
     */
    private fun isCheckingOut() = _state.value.screen == Screen.CHECKOUT && _state.value.checkout != null

    private suspend fun handleBackendResponse(json: JSONObject) {
        json.optString("merchantReference").takeIf { it.isNotBlank() }?.let { lastReference = it }

        val actionJson = json.optJSONObject("action")
        if (!isCheckingOut()) return
        if (actionJson != null) {
            // e.g. "await" for UPI Intent: the Component opens the UPI app and polls.
            _state.update { it.copy(talkingToServer = false) }
            _actions.send(Action.SERIALIZER.deserialize(actionJson))
            return
        }

        finish(
            PaymentResult(
                resultCode = json.optString("resultCode").ifBlank { "Unknown" },
                message = json.optString("refusalReason").takeIf { it.isNotBlank() },
                merchantReference = lastReference,
                pspReference = json.optString("pspReference").takeIf { it.isNotBlank() },
            ),
        )
    }

    private fun finish(result: PaymentResult) {
        if (!isCheckingOut()) return
        _state.update {
            it.copy(
                screen = Screen.RESULT,
                result = result.copy(merchantReference = result.merchantReference ?: lastReference),
                checkout = null,
                talkingToServer = false,
                cart = if (result.isSuccess) emptyMap() else it.cart,
            )
        }
        lastReference = null
    }

    // endregion

    companion object {
        private const val KEY_BACKEND_URL = "backend_url"
        private const val MAX_QUANTITY = 10
    }
}
