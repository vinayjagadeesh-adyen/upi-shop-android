package com.demo.upishop

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

data class Product(
    val id: String,
    val name: String,
    val description: String,
    /** Minor units: 100 = ₹1.00 */
    val price: Long,
)

data class AppConfig(
    val clientKey: String,
    /** "live-in" or "test" */
    val environment: String,
    val currency: String,
    val items: List<Product>,
)

class BackendException(message: String) : Exception(message)

/**
 * Talks to the Vercel backend. Plain HttpURLConnection + org.json so the
 * app needs no networking libraries. The Adyen API key never touches the app.
 */
class BackendApi(private val baseUrl: String) {

    suspend fun config(): AppConfig {
        val json = request("GET", "/api/config", null)
        val items = json.getJSONArray("items")
        return AppConfig(
            clientKey = json.getString("clientKey"),
            environment = json.getString("environment"),
            currency = json.getString("currency"),
            items = (0 until items.length()).map { i ->
                val item = items.getJSONObject(i)
                Product(
                    id = item.getString("id"),
                    name = item.getString("name"),
                    description = item.optString("description"),
                    price = item.getLong("price"),
                )
            },
        )
    }

    /** Returns Adyen's raw /paymentMethods response. */
    suspend fun paymentMethods(cart: Map<String, Int>): JSONObject =
        request("POST", "/api/paymentMethods", JSONObject().put("cart", cart.toJson()))

    suspend fun payments(
        cart: Map<String, Int>,
        paymentComponentData: JSONObject,
        returnUrl: String,
    ): JSONObject = request(
        "POST",
        "/api/payments",
        JSONObject()
            .put("cart", cart.toJson())
            .put("paymentComponentData", paymentComponentData)
            .put("returnUrl", returnUrl),
    )

    /** Body is ActionComponentData.SERIALIZER output: { details, paymentData }. */
    suspend fun paymentsDetails(actionComponentData: JSONObject): JSONObject =
        request("POST", "/api/paymentsDetails", actionComponentData)

    private fun Map<String, Int>.toJson() = JSONArray().also { array ->
        forEach { (id, quantity) -> array.put(JSONObject().put("id", id).put("quantity", quantity)) }
    }

    private suspend fun request(method: String, path: String, body: JSONObject?): JSONObject =
        withContext(Dispatchers.IO) {
            if (baseUrl.isBlank()) throw BackendException("Set the backend URL first (gear icon).")

            val connection = try {
                URL(baseUrl.trimEnd('/') + path).openConnection() as HttpURLConnection
            } catch (e: Exception) {
                throw BackendException("Backend URL is not valid: $baseUrl")
            }

            try {
                connection.requestMethod = method
                connection.connectTimeout = 15_000
                connection.readTimeout = 30_000
                connection.setRequestProperty("Accept", "application/json")
                if (body != null) {
                    connection.doOutput = true
                    connection.setRequestProperty("Content-Type", "application/json")
                    connection.outputStream.use { it.write(body.toString().toByteArray()) }
                }

                val status = connection.responseCode
                val stream = if (status in 200..299) connection.inputStream else connection.errorStream
                val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
                val json = runCatching { JSONObject(text) }.getOrNull()

                if (status !in 200..299) {
                    val message = json?.optString("error")?.takeIf { it.isNotBlank() }
                        ?: "Backend returned HTTP $status"
                    throw BackendException(message)
                }
                json ?: throw BackendException("Backend sent a response that isn't JSON.")
            } catch (e: IOException) {
                throw BackendException("Can't reach the backend: ${e.message ?: e.javaClass.simpleName}")
            } finally {
                connection.disconnect()
            }
        }
}
