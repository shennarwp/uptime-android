package com.rwpiri.uptime

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
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
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Login
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rwpiri.uptime.data.Check
import com.rwpiri.uptime.data.TargetDraft
import com.rwpiri.uptime.data.TargetWithChecks
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

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
    val dark = isSystemInDarkTheme()
    val colors = if (dark) {
        darkColorScheme(
            primary = Color(0xFF60A5FA),
            background = Color(0xFF121212),
            surface = Color(0xFF1E1E1E),
            surfaceVariant = Color(0xFF181818),
            outline = Color(0xFF2C2C2C),
            onBackground = Color(0xFFE0E0E0),
            onSurface = Color(0xFFE0E0E0),
            onSurfaceVariant = Color(0xFF9CA3AF),
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF2563EB),
            background = Color.White,
            surface = Color.White,
            surfaceVariant = Color(0xFFF9F9F9),
            outline = Color(0xFFE5E7EB),
            onBackground = Color(0xFF111827),
            onSurface = Color(0xFF111827),
            onSurfaceVariant = Color(0xFF6B7280),
        )
    }
    MaterialTheme(colorScheme = colors, content = content)
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
    val headerDividerColor = MaterialTheme.colorScheme.outline

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(1.dp)
                    .drawBehind {
                        val y = size.height - 1.dp.toPx()
                        drawLine(
                            color = headerDividerColor,
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1.dp.toPx(),
                        )
                    },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isSystemInDarkTheme()) Color(0xFF181818) else Color.White,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface,
                ),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(R.drawable.uptime_logo),
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                        )
                        Text("Uptime", modifier = Modifier.padding(start = 8.dp), fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    HeaderIconButton(Icons.Outlined.Refresh, "Refresh", model::refresh)
                    HeaderIconButton(Icons.Outlined.Settings, "Settings") { showSettings = true }
                    HeaderIconButton(
                        if (state.loggedIn) Icons.AutoMirrored.Outlined.Logout else Icons.AutoMirrored.Outlined.Login,
                        if (state.loggedIn) "Logout" else "Login",
                    ) { if (state.loggedIn) model.logout() else showLogin = true }
                },
            )
        },
        floatingActionButton = {
            if (state.loggedIn) FloatingActionButton(
                onClick = { creating = true },
                containerColor = Color(0xFF22C55E),
                contentColor = Color.White,
            ) { Text("+") }
        },
    ) { padding ->
        when {
            state.loading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            state.serverUrl.isBlank() -> EmptyState("Set the server URL in Settings to connect.", Modifier.padding(padding))
            else -> Dashboard(
                targets = state.targets,
                error = state.error,
                lastSyncAt = state.lastSyncAt,
                refreshing = state.refreshing,
                modifier = Modifier.padding(padding),
                onEdit = { editing = it },
                onDelete = { deleting = it },
                onRefresh = { model.refresh() },
            )
        }
    }

    if (showLogin) LoginDialog(onDismiss = { showLogin = false }, onLogin = model::login, error = state.error)
    if (showSettings) SettingsSheet(
        initialUrl = state.serverUrl,
        onDismiss = { showSettings = false },
        onSave = { model.saveServerUrl(it); showSettings = false },
    )
    if (creating) TargetEditorSheet(null, onDismiss = { creating = false }, onSave = { model.create(it); creating = false })
    editing?.let { target -> TargetEditorSheet(target, onDismiss = { editing = null }, onSave = { model.update(target.id, it); editing = null }) }
    deleting?.let { target ->
        DeleteTargetSheet(
            targetName = target.name,
            onDismiss = { deleting = null },
            onConfirm = { model.delete(target.id); deleting = null },
        )
    }
}

@Composable
private fun HeaderIconButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .padding(horizontal = 2.dp)
            .clip(RoundedCornerShape(6.dp)),
    ) {
        Icon(icon, contentDescription = description, tint = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun Dashboard(
    targets: List<TargetWithChecks>,
    error: String?,
    lastSyncAt: String?,
    refreshing: Boolean,
    modifier: Modifier,
    onEdit: (TargetWithChecks) -> Unit,
    onDelete: (TargetWithChecks) -> Unit,
    onRefresh: () -> Unit,
) {
    Column(modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(vertical = 8.dp)) }
        PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = onRefresh,
            modifier = Modifier.weight(1f),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp),
            ) {
                items(targets, key = { it.id }) { target -> TargetCard(target, onEdit, onDelete) }
                item {
                    Row(
                        Modifier.fillMaxWidth().padding(top = 2.dp, end = 72.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("${targets.size} targets", style = MaterialTheme.typography.bodySmall)
                        lastSyncAt?.let {
                            Text("Last sync: ${formatDateTime(it)}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TargetCard(target: TargetWithChecks, onEdit: (TargetWithChecks) -> Unit, onDelete: (TargetWithChecks) -> Unit) {
    val latest = target.checks.maxByOrNull { it.checkedAt }
    val isUp = latest?.isUp
    val borderColor = when (isUp) {
        true -> Color(0xFF22C55E)
        false -> Color(0xFFEF4444)
        null -> MaterialTheme.colorScheme.outline
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(1.dp, MaterialTheme.shapes.medium),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(if (isUp == null) 1.dp else 2.dp, borderColor),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            target.name,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.size(8.dp))
                        StatusBadge(isUp)
                    }
                    Text(target.url, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
                }
            }
            Spacer(Modifier.height(6.dp))
            HistoryBar(target.checks)
            TargetMetadata(
                latest = latest,
                certExpiresAt = target.certExpiresAt,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                IconButton(onClick = { onEdit(target) }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Outlined.Edit, contentDescription = "Edit ${target.name}", tint = MaterialTheme.colorScheme.onSurface)
                }
                IconButton(onClick = { onDelete(target) }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Outlined.Delete, contentDescription = "Delete ${target.name}", tint = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}

@Composable
private fun TargetMetadata(
    latest: Check?,
    certExpiresAt: String?,
) {
    Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
        latest?.let { check ->
            MetadataLine(label = "Last Check: ", value = formatDateTime(check.checkedAt))
        }
        certExpiresAt?.let { expiresAt ->
            certificateExpiryInfo(expiresAt)?.let { info ->
                MetadataLine(
                    label = "Cert Expires: ",
                    value = info.label,
                    valueColor = certificateColor(info.level),
                    bold = info.level != CertificateLevel.OK,
                )
            }
        }
    }
}

@Composable
private fun MetadataLine(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    bold: Boolean = false,
) {
    Row {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, color = valueColor, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)
    }
}

private val displayDateFormatter = DateTimeFormatter
    .ofPattern("EEE, dd MMM yyyy HH:mm", Locale.ENGLISH)
    .withZone(ZoneId.systemDefault())

private fun formatDateTime(value: String): String = runCatching {
    displayDateFormatter.format(Instant.parse(value))
}.getOrElse {
    value.replace('T', ' ')
        .substringBefore('.')
        .removeSuffix("Z")
        .replace(Regex("(:\\d{2}):\\d{2}$")) { match -> match.groupValues[1] }
}

private enum class CertificateLevel { OK, WARN, CRITICAL }

private data class CertificateExpiryInfo(val label: String, val level: CertificateLevel)

private fun certificateExpiryInfo(value: String): CertificateExpiryInfo? = runCatching {
    val daysLeft = kotlin.math.ceil(
        Duration.between(Instant.now(), Instant.parse(value)).toMillis() / 86_400_000.0,
    ).toLong()
    if (daysLeft < 0) {
        CertificateExpiryInfo("expired ${-daysLeft}d ago", CertificateLevel.CRITICAL)
    } else {
        CertificateExpiryInfo(
            label = "${daysLeft}d left",
            level = when {
                daysLeft <= 10 -> CertificateLevel.CRITICAL
                daysLeft <= 30 -> CertificateLevel.WARN
                else -> CertificateLevel.OK
            },
        )
    }
}.getOrNull()

@Composable
private fun certificateColor(level: CertificateLevel): Color = when (level) {
    CertificateLevel.OK -> MaterialTheme.colorScheme.onSurface
    CertificateLevel.WARN -> if (isSystemInDarkTheme()) Color(0xFFFBBF24) else Color(0xFFB45309)
    CertificateLevel.CRITICAL -> Color(0xFFE44646)
}

@Composable
private fun HistoryBar(checks: List<Check>) {
    Row(Modifier.fillMaxWidth().height(18.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        checks.sortedBy { it.checkedAt }.takeLast(60).forEach { check ->
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (check.isUp) Color(0xFF22C55E) else Color(0xFFEF4444)),
            )
        }
    }
}

@Composable
private fun EmptyState(message: String, modifier: Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(message, modifier = Modifier.padding(32.dp)) }
}

private fun statusLabel(isUp: Boolean?) = when (isUp) { true -> "UP"; false -> "DOWN"; null -> "UNKNOWN" }
@Composable
private fun statusColor(isUp: Boolean?): Color = when (isUp) {
    true -> if (isSystemInDarkTheme()) Color(0xFFDCFCE7) else Color(0xFF166534)
    false -> if (isSystemInDarkTheme()) Color(0xFFFEE2E2) else Color(0xFF991B1B)
    null -> MaterialTheme.colorScheme.onSurfaceVariant
}

@Composable
private fun StatusBadge(isUp: Boolean?) {
    val background = when (isUp) {
        true -> if (isSystemInDarkTheme()) Color(0xFF166534) else Color(0xFFDCFCE7)
        false -> if (isSystemInDarkTheme()) Color(0xFF991B1B) else Color(0xFFFEE2E2)
        null -> MaterialTheme.colorScheme.surfaceVariant
    }
    Surface(
        color = background,
        contentColor = statusColor(isUp),
        shape = RoundedCornerShape(12.dp),
    ) {
        Text(
            statusLabel(isUp),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

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
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            ) { Text("Cancel") }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsSheet(initialUrl: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var url by rememberSaveable(initialUrl) { mutableStateOf(initialUrl) }
    var error by rememberSaveable(initialUrl) { mutableStateOf<String?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
    ) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Settings", style = MaterialTheme.typography.headlineSmall)
            OutlinedTextField(
                url,
                { url = it },
                label = { Text("Server URL") },
                placeholder = { Text("https://example.com") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF22C55E),
                    focusedLabelColor = Color(0xFF22C55E),
                    cursorColor = Color(0xFF22C55E),
                ),
            )
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(enabled = url.isNotBlank(), onClick = {
                error = com.rwpiri.uptime.data.InputValidation.serverUrl(url)
                if (error == null) onSave(url)
            }, modifier = Modifier.fillMaxWidth(), colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                containerColor = Color(0xFF22C55E),
                contentColor = Color.White,
            )) { Text("Save") }
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            ) { Text("Cancel") }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TargetEditorSheet(target: TargetWithChecks?, onDismiss: () -> Unit, onSave: (TargetDraft) -> Unit) {
    val sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by rememberSaveable(target?.id) { mutableStateOf(target?.name.orEmpty()) }
    var url by rememberSaveable(target?.id) { mutableStateOf(target?.url.orEmpty()) }
    var schedule by rememberSaveable(target?.id) { mutableStateOf(target?.schedule ?: "0 */5 * * * *") }
    var error by rememberSaveable(target?.id) { mutableStateOf<String?>(null) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
    ) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(if (target == null) "Add target" else "Edit target", style = MaterialTheme.typography.headlineSmall)
            val inputColors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF22C55E),
                focusedLabelColor = Color(0xFF22C55E),
                cursorColor = Color(0xFF22C55E),
            )
            OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth(), colors = inputColors)
            if (target == null) OutlinedTextField(url, { url = it }, label = { Text("URL") }, singleLine = true, modifier = Modifier.fillMaxWidth(), colors = inputColors)
            OutlinedTextField(schedule, { schedule = it }, label = { Text("Cron schedule") }, singleLine = true, modifier = Modifier.fillMaxWidth(), colors = inputColors)
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(
                enabled = name.isNotBlank() && schedule.isNotBlank() && (target != null || url.isNotBlank()),
                onClick = {
                    val draft = TargetDraft(name, url, schedule)
                    error = com.rwpiri.uptime.data.InputValidation.targetDraft(draft, target == null)
                    if (error == null) onSave(draft)
                },
                modifier = Modifier.fillMaxWidth(),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF22C55E),
                    contentColor = Color.White,
                ),
            ) { Text("Save") }
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            ) { Text("Cancel") }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeleteTargetSheet(targetName: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
    ) {
        Column(
            Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Delete $targetName?", style = MaterialTheme.typography.headlineSmall)
            Text("This removes the target and its server-side checks.")
            Button(
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth(),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFEF4444),
                    contentColor = Color.White,
                ),
            ) { Text("Delete") }
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            ) { Text("Cancel") }
            Spacer(Modifier.height(24.dp))
        }
    }
}
