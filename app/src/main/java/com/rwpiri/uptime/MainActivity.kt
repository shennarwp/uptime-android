package com.rwpiri.uptime

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rwpiri.uptime.data.Check
import com.rwpiri.uptime.data.TargetDraft
import com.rwpiri.uptime.data.TargetWithChecks
import java.time.Instant

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NotificationChannels.ensure(this)
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100)
        }
        setContent { UptimeTheme { UptimeScreen() } }
    }
}

@Composable
private fun UptimeTheme(content: @Composable () -> Unit) {
    MaterialTheme(content = content)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UptimeScreen() {
    val application = androidx.compose.ui.platform.LocalContext.current.applicationContext as UptimeApplication
    val model: UptimeViewModel = viewModel(factory = UptimeViewModelFactory(application.container.repository))
    val state by model.state.collectAsStateWithLifecycle()
    var showLogin by rememberSaveable { mutableStateOf(false) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var editing by remember { mutableStateOf<TargetWithChecks?>(null) }
    var creating by rememberSaveable { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<TargetWithChecks?>(null) }
    LaunchedEffect(state.requiresLogin) { if (state.requiresLogin) showLogin = true }
    LaunchedEffect(state.loggedIn) { if (state.loggedIn) showLogin = false }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Uptime") },
                actions = {
                    TextButton(onClick = { showSettings = true }) { Text("Settings") }
                    TextButton(onClick = { if (state.loggedIn) model.logout() else showLogin = true }) {
                        Text(if (state.loggedIn) "Logout" else "Login")
                    }
                },
            )
        },
        floatingActionButton = {
            if (state.loggedIn) FloatingActionButton(onClick = { creating = true }) { Text("+") }
        },
    ) { padding ->
        when {
            state.loading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            state.serverUrl.isBlank() -> EmptyState("Set the server URL in Settings to connect.", Modifier.padding(padding))
            else -> Dashboard(
                targets = state.targets,
                error = state.error,
                lastSyncAt = state.lastSyncAt,
                modifier = Modifier.padding(padding),
                onRefresh = model::refresh,
                onEdit = { editing = it },
                onDelete = { deleting = it },
            )
        }
    }

    if (showLogin) LoginDialog(onDismiss = { showLogin = false }, onLogin = model::login, error = state.error)
    if (showSettings) SettingsDialog(
        initialUrl = state.serverUrl,
        onDismiss = { showSettings = false },
        onSave = { model.saveServerUrl(it); showSettings = false },
    )
    if (creating) TargetEditorSheet(null, onDismiss = { creating = false }, onSave = { model.create(it); creating = false })
    editing?.let { target -> TargetEditorSheet(target, onDismiss = { editing = null }, onSave = { model.update(target.id, it); editing = null }) }
    deleting?.let { target ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Delete ${target.name}?") },
            text = { Text("This removes the target and its server-side checks.") },
            confirmButton = { TextButton(onClick = { model.delete(target.id); deleting = null }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun Dashboard(
    targets: List<TargetWithChecks>,
    error: String?,
    lastSyncAt: String?,
    modifier: Modifier,
    onRefresh: () -> Unit,
    onEdit: (TargetWithChecks) -> Unit,
    onDelete: (TargetWithChecks) -> Unit,
) {
    Column(modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("${targets.size} targets", style = MaterialTheme.typography.titleMedium)
                lastSyncAt?.let { Text("Last sync: ${it.replace('T', ' ').substringBefore('.')}", style = MaterialTheme.typography.bodySmall) }
            }
            TextButton(onClick = onRefresh) { Text("Refresh") }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 8.dp)) }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(targets, key = { it.id }) { target -> TargetCard(target, onEdit, onDelete) }
        }
    }
}

@Composable
private fun TargetCard(target: TargetWithChecks, onEdit: (TargetWithChecks) -> Unit, onDelete: (TargetWithChecks) -> Unit) {
    val latest = target.checks.maxByOrNull { it.checkedAt }
    val isUp = latest?.isUp
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(target.name, style = MaterialTheme.typography.titleMedium)
                    Text(target.url, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
                }
                Text(statusLabel(isUp), color = statusColor(isUp))
            }
            Spacer(Modifier.height(10.dp))
            HistoryBar(target.checks)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = { onEdit(target) }) { Text("Edit") }
                TextButton(onClick = { onDelete(target) }) { Text("Delete") }
            }
        }
    }
}

@Composable
private fun HistoryBar(checks: List<Check>) {
    Row(Modifier.fillMaxWidth().height(18.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        checks.sortedBy { it.checkedAt }.takeLast(60).forEach { check ->
            Box(Modifier.weight(1f).fillMaxSize().background(if (check.isUp) Color(0xFF43A047) else Color(0xFFE53935)))
        }
    }
}

@Composable
private fun EmptyState(message: String, modifier: Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(message, modifier = Modifier.padding(32.dp)) }
}

private fun statusLabel(isUp: Boolean?) = when (isUp) { true -> "UP"; false -> "DOWN"; null -> "UNKNOWN" }
private fun statusColor(isUp: Boolean?) = when (isUp) { true -> Color(0xFF2E7D32); false -> Color(0xFFC62828); null -> Color.Gray }

@Composable
private fun LoginDialog(onDismiss: () -> Unit, onLogin: (String) -> Unit, error: String?) {
    var token by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Login") },
        text = {
            Column {
                OutlinedTextField(token, { token = it }, label = { Text("API token") }, singleLine = true)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp)) }
            }
        },
        confirmButton = { TextButton(enabled = token.isNotBlank(), onClick = { onLogin(token) }) { Text("Verify") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun SettingsDialog(initialUrl: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var url by rememberSaveable(initialUrl) { mutableStateOf(initialUrl) }
    var error by rememberSaveable(initialUrl) { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Settings") },
        text = {
            Column {
                OutlinedTextField(url, { url = it }, label = { Text("Server URL") }, placeholder = { Text("https://example.com") }, singleLine = true)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp)) }
            }
        },
        confirmButton = {
            TextButton(enabled = url.isNotBlank(), onClick = {
                error = com.rwpiri.uptime.data.InputValidation.serverUrl(url)
                if (error == null) onSave(url)
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TargetEditorSheet(target: TargetWithChecks?, onDismiss: () -> Unit, onSave: (TargetDraft) -> Unit) {
    val sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by rememberSaveable(target?.id) { mutableStateOf(target?.name.orEmpty()) }
    var url by rememberSaveable(target?.id) { mutableStateOf(target?.url.orEmpty()) }
    var schedule by rememberSaveable(target?.id) { mutableStateOf(target?.schedule ?: "0 */5 * * * *") }
    var error by rememberSaveable(target?.id) { mutableStateOf<String?>(null) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(if (target == null) "Add target" else "Edit target", style = MaterialTheme.typography.headlineSmall)
            OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            if (target == null) OutlinedTextField(url, { url = it }, label = { Text("URL") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(schedule, { schedule = it }, label = { Text("Cron schedule") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(
                enabled = name.isNotBlank() && schedule.isNotBlank() && (target != null || url.isNotBlank()),
                onClick = {
                    val draft = TargetDraft(name, url, schedule)
                    error = com.rwpiri.uptime.data.InputValidation.targetDraft(draft, target == null)
                    if (error == null) onSave(draft)
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Save") }
            Spacer(Modifier.height(24.dp))
        }
    }
}
