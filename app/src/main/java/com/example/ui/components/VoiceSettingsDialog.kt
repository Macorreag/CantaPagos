package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.repository.NotificationDiagnosticEntry
import com.example.data.repository.SoundboxConfig
import com.example.ui.theme.ColorBancolombia
import com.example.ui.theme.ColorDaviplata
import com.example.ui.theme.ColorNequi
import com.example.ui.theme.ColorSms
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate850
import com.example.ui.theme.Slate900

@Composable
fun VoiceSettingsDialog(
    config: SoundboxConfig,
    recentDiagnostics: List<NotificationDiagnosticEntry> = emptyList(),
    onConfigChange: (SoundboxConfig) -> Unit,
    onTestPreview: () -> Unit,
    onDismiss: () -> Unit
) {
    // Dynamic preview phrase (anonymized - no personal names)
    val previewWallet = if (config.speakWallet) "¡Nequi! " else ""
    val previewPesos = if (config.speakPesosWord) "mil pesos" else "mil"
    val previewBell = if (config.playChime) "🔔 [Clin-ck!] " else ""
    val fullPreview = "$previewBell$previewWallet Recibiste $previewPesos"

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, Slate800, RoundedCornerShape(24.dp))
                .testTag("voice_settings_dialog"),
            color = Slate900,
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(EmeraldPrimary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Ajustes y Filtro de Apps",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Cerrar", tint = Slate400)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Live Preview Bubble
                Surface(
                    color = Slate850,
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "VISTA PREVIA DEL ANUNCIO:",
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldPrimary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "\"$fullPreview\"",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Section 1: App Package ID Filtering
                Text(
                    text = "IDENTIFICADORES DE APP (PACKAGE ID)",
                    style = MaterialTheme.typography.labelSmall,
                    color = Slate400,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                SettingToggleItem(
                    title = "Filtrar por ID Oficial de App",
                    subtitle = "Identifica primero el paquete de la app (ej. com.nequi.MobileApp) e ignora apps ajenas",
                    checked = config.strictPackageFilter,
                    onCheckedChange = { onConfigChange(config.copy(strictPackageFilter = it)) }
                )

                WalletToggleItem(
                    name = "Nequi Colombia",
                    packageId = "com.nequi.MobileApp",
                    color = ColorNequi,
                    checked = config.enableNequi,
                    onCheckedChange = { onConfigChange(config.copy(enableNequi = it)) }
                )

                WalletToggleItem(
                    name = "Daviplata",
                    packageId = "com.davivienda.daviplataapp",
                    color = ColorDaviplata,
                    checked = config.enableDaviplata,
                    onCheckedChange = { onConfigChange(config.copy(enableDaviplata = it)) }
                )

                WalletToggleItem(
                    name = "Bancolombia y A la mano",
                    packageId = "com.todo1.mobile / co.com.bancolombia*",
                    color = ColorBancolombia,
                    checked = config.enableBancolombia,
                    onCheckedChange = { onConfigChange(config.copy(enableBancolombia = it)) }
                )

                WalletToggleItem(
                    name = "SMS Bancarios",
                    packageId = "com.samsung/google.android.messaging",
                    color = ColorSms,
                    checked = config.enableSms,
                    onCheckedChange = { onConfigChange(config.copy(enableSms = it)) }
                )

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = Slate800, thickness = 1.dp)
                Spacer(modifier = Modifier.height(14.dp))

                // Section 2: Voice Elements
                Text(
                    text = "¿QUÉ DESEAS QUE DIGA LA VOZ?",
                    style = MaterialTheme.typography.labelSmall,
                    color = Slate400,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                SettingToggleItem(
                    title = "Campanita de Caja (Clin-ck)",
                    subtitle = "Sonar timbre metálico antes de hablar",
                    checked = config.playChime,
                    onCheckedChange = { onConfigChange(config.copy(playChime = it)) }
                )

                SettingToggleItem(
                    title = "Nombre de la Billetera",
                    subtitle = "Decir '¡Nequi!', '¡Daviplata!', etc.",
                    checked = config.speakWallet,
                    onCheckedChange = { onConfigChange(config.copy(speakWallet = it)) }
                )

                SettingToggleItem(
                    title = "Decir 'Pesos'",
                    subtitle = "Ej: 'mil pesos' o solo 'mil' (texto 100% anónimo sin nombres personales)",
                    checked = config.speakPesosWord,
                    onCheckedChange = { onConfigChange(config.copy(speakPesosWord = it)) }
                )

                if (recentDiagnostics.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = Slate800, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "MONITOR DE NOTIFICACIONES DETECTADAS",
                        style = MaterialTheme.typography.labelSmall,
                        color = Slate400,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    recentDiagnostics.take(5).forEach { diag ->
                        Surface(
                            color = Slate850,
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (diag.isPaymentMatched) EmeraldPrimary.copy(alpha = 0.4f) else Slate800
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "ID: ${diag.packageName}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = Slate400
                                )
                                Text(
                                    text = "${diag.title}: ${diag.body}".trim().trimStart(':').trim(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = diag.status,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                    color = if (diag.isPaymentMatched) EmeraldPrimary else Slate200
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Test Preview Button
                Button(
                    onClick = onTestPreview,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("preview_voice_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldPrimary,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Escuchar Vista Previa", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun SettingToggleItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        color = Slate850,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Slate800),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = Slate400
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
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

@Composable
private fun WalletToggleItem(
    name: String,
    packageId: String,
    color: Color,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        color = Slate850,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Slate800),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(color)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (checked) Color.White else Slate400,
                        fontWeight = if (checked) FontWeight.SemiBold else FontWeight.Normal
                    )
                    Text(
                        text = packageId,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = Slate400
                    )
                }
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = color,
                    uncheckedThumbColor = Slate400,
                    uncheckedTrackColor = Slate800
                )
            )
        }
    }
}
