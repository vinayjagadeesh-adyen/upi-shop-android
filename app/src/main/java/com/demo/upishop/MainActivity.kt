package com.demo.upishop

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.adyen.checkout.redirect.RedirectComponent
import com.demo.upishop.ui.ShopApp

/**
 * Single Activity. AppCompatActivity because the Adyen Component is made of
 * Material views; it is still a ComponentActivity, so Compose works as usual.
 */
class MainActivity : AppCompatActivity() {

    private val viewModel: ShopViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { ShopApp(viewModel) }
    }

    // singleTask: a redirect back to adyencheckout://com.demo.upishop lands here.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val data = intent.data
        if (data != null && data.toString().startsWith(RedirectComponent.REDIRECT_RESULT_SCHEME)) {
            viewModel.onRedirectIntent(intent)
        }
    }
}
