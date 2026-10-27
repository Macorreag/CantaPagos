package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.parser.WalletNotificationParser
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate850
import com.example.ui.theme.Slate900
import com.example.util.SystemNotificationTester

@Composable
fun SimulatePaymentDialog(
    onDismiss: () -> Unit,
    onSimulate: (wallet: String, amount: Long, sender: String?) -> Unit
) {
    val context = LocalContext.current
    val wallets = listOf("Nequi", "Daviplata", "Bancolombia", "SMS Bancario")
    var selectedWallet by remember { mutableStateOf("Nequi") }
    var selectedAmount by remember { mutableStateOf(1000L) }
    var customAmountText by remember { mutableStateOf("1000") }

    val presetAmounts = listOf(1000L, 5000L, 10000L, 25000L, 50000L)
    val selectedPackageId = WalletNotificationParser.resolveDisplayPackageId("", selectedWallet)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, Slate800, RoundedCornerShape(24.dp))
                .testTag("simulate_payment_dialog"),
            color = Slate900,
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Simulador de Pagos",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = Slate400
                        )
                    }
                }

                Text(
                    text = "Prueba el sonido y la lectura anónima por identificador de app.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate400
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Wallet selector
                Text(
                    text = "BILLETERA E IDENTIFICADOR DE APP",
                    style = MaterialTheme.typography.labelSmall,
                    color = Slate400,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    wallets.forEach { wallet ->
                        val isSelected = wallet == selectedWallet
                        val (color, _) = getWalletBadgeColors(wallet)
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) color.copy(alpha = 0.25f) else Slate850,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) color else Slate800
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedWallet = wallet }
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (wallet == "Bancolombia") "Bancol." else if (wallet == "SMS Bancario") "SMS" else wallet,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) Color.White else Slate400,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    color = Slate850,
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Slate800),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "ID App: $selectedPackageId",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = EmeraldPrimary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Presets
                Text(
                    text = "MONTO RÁPIDO",
                    style = MaterialTheme.typography.labelSmall,
                    color = Slate400,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presetAmounts.take(4).forEach { amount ->
                        val isSelected = amount == selectedAmount
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) EmeraldPrimary.copy(alpha = 0.2f) else Slate850,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) EmeraldPrimary else Slate800
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    selectedAmount = amount
                                    customAmountText = amount.toString()
                                }
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$${amount / 1000}k",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) EmeraldPrimary else Slate200,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Custom Amount Input
                OutlinedTextField(
                    value = customAmountText,
                    onValueChange = {
                        val filtered = it.filter { char -> char.isDigit() }
                        customAmountText = filtered
                        selectedAmount = filtered.toLongOrNull() ?: 0L
                    },
                    label = { Text("Monto en COP ($)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("simulate_amount_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldPrimary,
                        unfocusedBorderColor = Slate700,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedLabelColor = EmeraldPrimary,
                        unfocusedLabelColor = Slate400
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Button 1: Announce directly
                Button(
                    onClick = {
                        val amount = customAmountText.toLongOrNull() ?: 1000L
                        onSimulate(selectedWallet, amount, selectedPackageId)
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("confirm_simulate_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldPrimary,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Cantar Pago Directo",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Button 2: Post Real System Notification (Anonymized)
                Button(
                    onClick = {
                        val amount = customAmountText.toLongOrNull() ?: 1000L
                        val formatted = com.example.data.repository.PaymentRepository.formatCop(amount)
                        val (title, body) = when (selectedWallet) {
                            "Nequi" -> "Envío" to "[Cliente] te envió $amount, ¡lo mejor!"
                            "Daviplata" -> "DaviPlata" to "Pasaron $formatted a tu DaviPlata."
                            "Bancolombia" -> "Bancolombia" to "Bancolombia le informa recepción de transferencia por $formatted,00"
                            else -> "Mensaje Bancolombia (898999)" to "Bancolombia le informa abono por $formatted en cta de ahorros."
                        }
                        val template = SystemNotificationTester.NotificationTemplate(
                            wallet = selectedWallet,
                            title = title,
                            body = body,
                            simulatedAmount = amount
                        )
                        SystemNotificationTester.postSystemNotification(context, template)
                        if (!com.example.service.SoundboxListenerService.isNotificationAccessGranted(context)) {
                            onSimulate(selectedWallet, amount, selectedPackageId)
                        }
                        android.widget.Toast.makeText(context, "📲 Notificación '$title: $body' enviada al teléfono", android.widget.Toast.LENGTH_SHORT).show()
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("post_system_notification_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Slate850,
                        contentColor = Slate200
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "📲 Enviar Notificación Real al Teléfono",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = EmeraldPrimary
                    )
                }
            }
        }
    }
}
