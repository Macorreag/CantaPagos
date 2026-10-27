package com.example

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.SoundboxViewModel
import com.example.ui.components.BatteryGuideDialog
import com.example.ui.components.ShiftSummaryCard
import com.example.ui.components.SimulatePaymentDialog
import com.example.ui.components.TransactionItemCard
import com.example.ui.components.VoiceSettingsDialog
import com.example.ui.theme.AmberAlert
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.RoseDanger
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate850
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950

class MainActivity : ComponentActivity() {

    private val viewModel: SoundboxViewModel by viewModels {
        val app = application as CantaPagosApp
        SoundboxViewModel.Factory(app.repository, app.audioEngine)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                MainSoundboxScreen(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainSoundboxScreen(viewModel: SoundboxViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Request POST_NOTIFICATIONS runtime permission on Android 13+
    val notificationPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Observe permission changes on activity resume
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.checkPermission(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val isPermissionGranted by viewModel.isPermissionGranted.collectAsStateWithLifecycle()
    val isServiceActive by viewModel.isServiceActive.collectAsStateWithLifecycle()
    val shiftTransactions by viewModel.shiftTransactions.collectAsStateWithLifecycle()
    val shiftTotal by viewModel.shiftTotal.collectAsStateWithLifecycle()
    val shiftCount by viewModel.shiftCount.collectAsStateWithLifecycle()
    val walletBreakdown by viewModel.walletBreakdown.collectAsStateWithLifecycle()
    val config by viewModel.config.collectAsStateWithLifecycle()

    var showSimulateDialog by remember { mutableStateOf(false) }
    var showBatteryGuideDialog by remember { mutableStateOf(false) }
    var showVoiceSettingsDialog by remember { mutableStateOf(false) }
    var showCloseShiftConfirmDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate950),
        containerColor = Slate950,
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header: Brand + Status Pill + Settings + Master Toggle
            item {
                HeaderSection(
                    isServiceActive = isServiceActive,
                    isPermissionGranted = isPermissionGranted,
                    onToggleActive = { viewModel.setServiceActive(it) },
                    onOpenSettings = { showVoiceSettingsDialog = true }
                )
            }

            // Permission Warning Banner
            if (!isPermissionGranted) {
                item {
                    PermissionRequiredBanner(onOpenSettings = {
                        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(intent)
                    })
                }
            }

            // Hero Cuadre de Caja Card
            item {
                ShiftSummaryCard(
                    totalCop = shiftTotal,
                    totalCount = shiftCount,
                    breakdown = walletBreakdown
                )
            }

            // Action Buttons Row
            item {
                ActionsRow(
                    onTestAudio = {
                        android.widget.Toast.makeText(context, "🔔 Probando altavoz ($10.000 COP)...", android.widget.Toast.LENGTH_SHORT).show()
                        viewModel.playTestChime(10000L, "Nequi")
                    },
                    onSimulatePayment = { showSimulateDialog = true },
                    onOpenVoiceSettings = { showVoiceSettingsDialog = true },
                    onCloseShift = { showCloseShiftConfirmDialog = true },
                    onBatteryGuide = { showBatteryGuideDialog = true }
                )
            }

            // Section Header: History
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "PAGOS CANTADOS EN VIVO",
                        style = MaterialTheme.typography.labelSmall,
                        color = Slate400,
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Surface(
                        color = Slate850,
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Slate800)
                    ) {
                        Text(
                            text = "${shiftTransactions.size} en este turno",
                            style = MaterialTheme.typography.labelSmall,
                            color = Slate200,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // History List or Empty State
            if (shiftTransactions.isEmpty()) {
                item {
                    EmptyHistoryCard(onSimulate = { showSimulateDialog = true })
                }
            } else {
                items(
                    items = shiftTransactions,
                    key = { it.id }
                ) { transaction ->
                    TransactionItemCard(transaction = transaction)
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showSimulateDialog) {
        SimulatePaymentDialog(
            onDismiss = { showSimulateDialog = false },
            onSimulate = { wallet, amount, sender ->
                viewModel.simulatePayment(wallet, amount, sender)
            }
        )
    }

    if (showBatteryGuideDialog) {
        BatteryGuideDialog(
            onDismiss = { showBatteryGuideDialog = false }
        )
    }

    if (showVoiceSettingsDialog) {
        val recentDiagnostics by viewModel.recentDiagnostics.collectAsStateWithLifecycle()
        VoiceSettingsDialog(
            config = config,
            recentDiagnostics = recentDiagnostics,
            onConfigChange = { viewModel.updateConfig(it) },
            onTestPreview = { viewModel.playTestChime(1000L, "Nequi") },
            onDismiss = { showVoiceSettingsDialog = false }
        )
    }

    if (showCloseShiftConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showCloseShiftConfirmDialog = false },
            title = {
                Text(
                    text = "¿Cerrar Caja de Hoy?",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White
                )
            },
            text = {
                Text(
                    text = "El contador de pagos del turno volverá a $0 COP para iniciar una nueva jornada. Los registros históricos se mantienen a salvo.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Slate200
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.closeShift()
                        showCloseShiftConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldPrimary,
                        contentColor = Color.White
                    )
                ) {
                    Text("Cerrar Caja")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showCloseShiftConfirmDialog = false }
                ) {
                    Text("Cancelar", color = Slate400)
                }
            },
            containerColor = Slate900,
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
private fun HeaderSection(
    isServiceActive: Boolean,
    isPermissionGranted: Boolean,
    onToggleActive: (Boolean) -> Unit,
    onOpenSettings: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, Slate800, RoundedCornerShape(20.dp)),
        color = Slate900,
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "CantaPagos",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    // Offline badge
                    Surface(
                        color = EmeraldPrimary.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.WifiOff,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "100% OFFLINE",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = EmeraldPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (!isPermissionGranted) "Requiere permiso de notificaciones" else if (isServiceActive) "Escuchando pagos en tiempo real" else "Lector en pausa",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (!isPermissionGranted) AmberAlert else if (isServiceActive) EmeraldPrimary else Slate400
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("open_settings_header_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Personalizar Anuncio",
                        tint = Slate200,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Master Switch
                Switch(
                    checked = isServiceActive,
                    onCheckedChange = onToggleActive,
                    modifier = Modifier.testTag("service_master_switch"),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = EmeraldPrimary,
                        uncheckedThumbColor = Slate400,
                        uncheckedTrackColor = Slate800
                    )
                )
            }
        }
    }
}

@Composable
private fun PermissionRequiredBanner(onOpenSettings: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("permission_banner"),
        colors = CardDefaults.cardColors(containerColor = AmberAlert.copy(alpha = 0.15f)),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, AmberAlert.copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.NotificationsActive,
                    contentDescription = null,
                    tint = AmberAlert,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "¡Permiso de Lectura Requerido!",
                    style = MaterialTheme.typography.titleMedium,
                    color = AmberAlert,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Para que CantaPagos lea las notificaciones de Nequi, Daviplata y Bancolombia, debes habilitar el acceso a notificaciones en los ajustes de tu celular.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onOpenSettings,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AmberAlert,
                    contentColor = Slate950
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("enable_permission_btn")
            ) {
                Text("Habilitar en Ajustes de Android", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ActionsRow(
    onTestAudio: () -> Unit,
    onSimulatePayment: () -> Unit,
    onOpenVoiceSettings: () -> Unit,
    onCloseShift: () -> Unit,
    onBatteryGuide: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Primary Quick Test Button
        Button(
            onClick = onTestAudio,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("test_audio_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = EmeraldPrimary,
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Probar Sonido ($10.000)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Settings / Personalizar
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Slate900,
                border = androidx.compose.foundation.BorderStroke(1.dp, Slate800),
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .testTag("voice_settings_button")
            ) {
                Button(
                    onClick = onOpenVoiceSettings,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = Slate200
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(vertical = 10.dp, horizontal = 4.dp)
                ) {
                    Icon(imageVector = Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("Ajustes", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                }
            }

            // Simulate Payment
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Slate900,
                border = androidx.compose.foundation.BorderStroke(1.dp, Slate800),
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .testTag("simulate_payment_button")
            ) {
                Button(
                    onClick = onSimulatePayment,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = Slate200
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(vertical = 10.dp, horizontal = 4.dp)
                ) {
                    Text("Simular", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                }
            }

            // Close Shift
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Slate900,
                border = androidx.compose.foundation.BorderStroke(1.dp, Slate800),
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .testTag("close_shift_button")
            ) {
                Button(
                    onClick = onCloseShift,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = Slate200
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(vertical = 10.dp, horizontal = 4.dp)
                ) {
                    Icon(imageVector = Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text("Cerrar", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                }
            }

            // Battery Tips
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Slate900,
                border = androidx.compose.foundation.BorderStroke(1.dp, Slate800),
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .testTag("battery_guide_button")
            ) {
                Button(
                    onClick = onBatteryGuide,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = Slate200
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(vertical = 10.dp, horizontal = 4.dp)
                ) {
                    Icon(imageVector = Icons.Default.BatteryChargingFull, contentDescription = null, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text("Batería", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun EmptyHistoryCard(onSimulate: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, Slate800, RoundedCornerShape(20.dp)),
        color = Slate900,
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(Slate850),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = null,
                    tint = Slate400,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "Listo para cantar cobros",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Cuando un cliente te transfiera por Nequi, Daviplata o Bancolombia, CantaPagos tocará la campana y cantará el monto aquí en voz alta.",
                style = MaterialTheme.typography.bodyMedium,
                color = Slate400,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(
                onClick = onSimulate,
                border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldPrimary),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Hacer una prueba ahora", color = EmeraldPrimary, fontWeight = FontWeight.Bold)
            }
        }
    }
}
