package com.demo.upishop

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.adyen.checkout.core.AdyenLogLevel
import com.adyen.checkout.core.AdyenLogger

class UpiShopApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // Light mode only, so Compose screens and the Adyen views always match.
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        AppLog.init(this)

        // Route the Adyen SDK's own logs (component setup, status polling,
        // network errors) into the same log the Logs screen shows.
        AdyenLogger.setLogger(object : AdyenLogger {
            private var minLevel = AdyenLogLevel.DEBUG

            override fun shouldLog(level: AdyenLogLevel): Boolean = level.priority >= minLevel.priority

            override fun setLogLevel(level: AdyenLogLevel) {
                minLevel = level
            }

            override fun log(level: AdyenLogLevel, tag: String, message: String, throwable: Throwable?) {
                if (!shouldLog(level)) return
                val short = when (level) {
                    AdyenLogLevel.ERROR, AdyenLogLevel.ASSERT -> "E"
                    AdyenLogLevel.WARN -> "W"
                    AdyenLogLevel.INFO -> "I"
                    else -> "D"
                }
                AppLog.log(short, "Adyen/$tag", message, throwable)
            }
        })
        AdyenLogger.setLogLevel(AdyenLogLevel.DEBUG)

        // Write crashes to the log before the app dies, so the next session shows them.
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            AppLog.e("Crash", "Uncaught exception on ${thread.name}: ${throwable.stackTraceToString().take(4000)}")
            previous?.uncaughtException(thread, throwable)
        }

        AppLog.i("App", "Backend URL baked into this build: ${BuildConfig.BACKEND_URL.ifBlank { "(none)" }}")
    }
}
