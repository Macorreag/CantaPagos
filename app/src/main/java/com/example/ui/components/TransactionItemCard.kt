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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PaymentTransaction
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate850
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TransactionItemCard(
    transaction: PaymentTransaction,
    modifier: Modifier = Modifier
) {
    val (badgeColor, textColor) = getWalletBadgeColors(transaction.wallet)
    val timeFormat = SimpleDateFormat("hh:mm:ss a", Locale.getDefault())
    val formattedTime = timeFormat.format(Date(transaction.timestamp))

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, Slate800, RoundedCornerShape(16.dp))
            .testTag("transaction_item_${transaction.id}"),
        color = Slate850,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Wallet Chip
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = badgeColor.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, badgeColor.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(badgeColor)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = transaction.wallet,
                            style = MaterialTheme.typography.labelSmall,
                            color = textColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Timestamp
                Text(
                    text = formattedTime,
                    style = MaterialTheme.typography.labelSmall,
                    color = Slate400
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = transaction.formattedAmount,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )

                    val pkgId = com.example.parser.WalletNotificationParser.resolveDisplayPackageId(
                        transaction.sender ?: "",
                        transaction.wallet
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "ID App: $pkgId",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        ),
                        color = Slate400,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Verified Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(EmeraldPrimary.copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Pago Cantado",
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Cantado",
                        style = MaterialTheme.typography.labelSmall,
                        color = EmeraldPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (transaction.rawNotificationText.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = transaction.rawNotificationText,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = Slate400.copy(alpha = 0.8f),
                    maxLines = 2
                )
            }
        }
    }
}
