package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.PaymentRepository
import com.example.ui.WalletBreakdown
import com.example.ui.theme.ColorBancolombia
import com.example.ui.theme.ColorDaviplata
import com.example.ui.theme.ColorNequi
import com.example.ui.theme.ColorSms
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate850
import com.example.ui.theme.Slate900

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ShiftSummaryCard(
    totalCop: Long,
    totalCount: Int,
    breakdown: List<WalletBreakdown>,
    modifier: Modifier = Modifier
) {
    val formattedTotal = PaymentRepository.formatCop(totalCop)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        EmeraldPrimary.copy(alpha = 0.4f),
                        Slate800
                    )
                ),
                shape = RoundedCornerShape(24.dp)
            )
            .testTag("shift_summary_card"),
        color = Slate900,
        shape = RoundedCornerShape(24.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            EmeraldDark.copy(alpha = 0.15f),
                            Color.Transparent
                        ),
                        radius = 600f
                    )
                )
                .padding(20.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
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
                                imageVector = Icons.Default.PointOfSale,
                                contentDescription = "Cuadre del día",
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "CUADRE DE CAJA (HOY)",
                            style = MaterialTheme.typography.labelSmall,
                            color = Slate400,
                            letterSpacing = 1.2.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Count Badge
                    Surface(
                        color = Slate850,
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Slate800)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Payments,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$totalCount pagos",
                                style = MaterialTheme.typography.labelSmall,
                                color = Slate200,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Giant Money Total
                Text(
                    text = formattedTotal,
                    style = MaterialTheme.typography.displayLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.testTag("total_amount_text")
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Wallet breakdown chips
                AnimatedVisibility(visible = breakdown.isNotEmpty()) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        breakdown.forEach { item ->
                            val (badgeColor, textColor) = getWalletBadgeColors(item.wallet)
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = badgeColor.copy(alpha = 0.15f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, badgeColor.copy(alpha = 0.4f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(badgeColor)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${item.wallet}: ${PaymentRepository.formatCop(item.total)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = textColor,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun getWalletBadgeColors(wallet: String): Pair<Color, Color> {
    return when (wallet) {
        "Nequi" -> ColorNequi to Color(0xFFD8B4FE)
        "Daviplata" -> ColorDaviplata to Color(0xFFFCA5A5)
        "Bancolombia" -> ColorBancolombia to Color(0xFFFDE68A)
        else -> ColorSms to Color(0xFFBAE6FD)
    }
}
