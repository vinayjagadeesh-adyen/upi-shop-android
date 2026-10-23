package com.demo.upishop

import android.content.Context
import android.os.Build
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * App-wide log that survives restarts. Every launch starts a new session
 * block, so you can scroll back through earlier sessions. Lines also go to
 * logcat. Older lines are dropped once the log passes [MAX_LINES].
 *
 * Never log secrets here: the app has none except the client key, which
 * [maskKey] shortens before logging.
 */
object AppLog {

    private const val MAX_LINES = 4000
    private const val TAG = "UpiShop"

    private val lock = Any()
    private var file: File? = null
    private val lines = ArrayDeque<String>()
    private val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    private val _text = MutableStateFlow("")
    /** The whole log as one string, for the Logs screen. */
    val text: StateFlow<String> = _text.asStateFlow()

    fun init(context: Context) = synchronized(lock) {
        if (file != null) return
        val logFile = File(context.filesDir, "upishop-log.txt")
        file = logFile
        runCatching { if (logFile.exists()) logFile.readLines().takeLast(MAX_LINES).forEach(lines::addLast) }

        val info = runCatching { context.packageManager.getPackageInfo(context.packageName, 0) }.getOrNull()
        write("")
        write("════════ Session started ${timeFormat.format(Date())} ════════")
        write(
            "App ${info?.versionName ?: "?"} · ${Build.MANUFACTURER} ${Build.MODEL} · " +
                "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
        )
        rewriteFile()
        publish()
    }

    fun i(tag: String, message: String) = log("I", tag, message, null)
    fun w(tag: String, message: String, throwable: Throwable? = null) = log("W", tag, message, throwable)
    fun e(tag: String, message: String, throwable: Throwable? = null) = log("E", tag, message, throwable)
    fun d(tag: String, message: String) = log("D", tag, message, null)

    fun log(level: String, tag: String, message: String, throwable: Throwable?) {
        val full = buildString {
            append(message)
            if (throwable != null) {
                append(" | ").append(throwable.javaClass.simpleName).append(": ").append(throwable.message)
                throwable.cause?.let { append(" | caused by ").append(it.javaClass.simpleName).append(": ").append(it.message) }
            }
        }
        when (level) {
            "E" -> Log.e(TAG, "[$tag] $full", throwable)
            "W" -> Log.w(TAG, "[$tag] $full", throwable)
            else -> Log.i(TAG, "[$tag] $full")
        }
        synchronized(lock) {
            val time = timeFormat.format(Date()).substring(11) // HH:mm:ss.SSS
            write("$time $level/$tag: $full")
            publish()
        }
    }

    fun clear() = synchronized(lock) {
        lines.clear()
        write("════════ Log cleared ${timeFormat.format(Date())} ════════")
        rewriteFile()
        publish()
    }

    /** "live_ABCDEFGH1234…" -> "live_ABCD…(36 chars)" */
    fun maskKey(key: String?): String = when {
        key.isNullOrBlank() -> "(empty)"
        key.length <= 9 -> "***"
        else -> key.take(9) + "…(${key.length} chars)"
    }

    private val clientKeyPattern = Regex("\"clientKey\"\\s*:\\s*\"([^\"]*)\"")

    /** Masks the client key and keeps big payloads readable in the log. */
    fun clip(text: String, max: Int = 3000): String {
        val masked = clientKeyPattern.replace(text) { "\"clientKey\":\"${maskKey(it.groupValues[1])}\"" }
        return if (masked.length <= max) masked else masked.take(max) + "… [${masked.length - max} more chars]"
    }

    // Must hold [lock].
    private fun write(line: String) {
        lines.addLast(line)
        var trimmed = false
        while (lines.size > MAX_LINES) {
            lines.removeFirst()
            trimmed = true
        }
        if (trimmed) {
            rewriteFile()
        } else {
            runCatching { file?.appendText(line + "\n") }
        }
    }

    private fun rewriteFile() {
        runCatching { file?.writeText(lines.joinToString("\n", postfix = "\n")) }
    }

    private fun publish() {
        _text.value = lines.joinToString("\n")
    }
}
