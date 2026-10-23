package com.demo.upishop.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.demo.upishop.AppLog

/** "Logs" button for every screen's top bar. */
@Composable
fun LogsButton(onClick: () -> Unit) {
    TextButton(onClick = onClick) { Text("Logs") }
}

/**
 * Full-screen log viewer. A dialog on top of the current screen, so opening
 * it mid-checkout doesn't disturb the UPI Component underneath.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogsDialog(onDismiss: () -> Unit) {
    val text by AppLog.text.collectAsStateWithLifecycle()
    val lines = remember(text) { text.lines() }
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val listState = rememberLazyListState()
    var confirmClear by remember { mutableStateOf(false) }

    // Open at the newest entry.
    LaunchedEffect(Unit) {
        if (lines.isNotEmpty()) listState.scrollToItem(lines.lastIndex)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Surface(Modifier.fillMaxSize()) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Logs") },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Filled.Close, contentDescription = "Close logs")
                            }
                        },
                        actions = {
                            TextButton(onClick = { confirmClear = true }) { Text("Clear") }
                        },
                    )
                },
                bottomBar = {
                    Surface(tonalElevation = 3.dp) {
                        Row(Modifier.fillMaxWidth().padding(16.dp)) {
                            Button(
                                onClick = {
                                    clipboard.setText(AnnotatedString(AppLog.text.value))
                                    Toast.makeText(context, "Logs copied (${lines.size} lines)", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("Copy to clipboard")
                            }
                        }
                    }
                },
            ) { padding ->
                SelectionContainer(Modifier.padding(padding)) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                    ) {
                        itemsIndexed(lines) { _, line ->
                            val color = when {
                                line.contains(" E/") -> MaterialTheme.colorScheme.error
                                line.contains(" W/") -> MaterialTheme.colorScheme.tertiary
                                line.startsWith("════") -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                            Text(
                                text = line,
                                color = color,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                lineHeight = 15.sp,
                                softWrap = true,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 1.dp),
                            )
                        }
                    }
                }
            }

            if (confirmClear) {
                AlertDialog(
                    onDismissRequest = { confirmClear = false },
                    title = { Text("Clear all logs?") },
                    text = { Text("This removes the logs from every session on this phone.") },
                    confirmButton = {
                        TextButton(onClick = {
                            AppLog.clear()
                            confirmClear = false
                        }) { Text("Clear") }
                    },
                    dismissButton = {
                        OutlinedButton(onClick = { confirmClear = false }) { Text("Cancel") }
                    },
                )
            }
        }
    }
}
