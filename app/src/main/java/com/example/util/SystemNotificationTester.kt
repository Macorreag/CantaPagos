package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R

object SystemNotificationTester {

    private const val CHANNEL_ID = "cantapagos_test_channel"
    private const val CHANNEL_NAME = "Pruebas de Notificaciones CantaPagos"

    data class NotificationTemplate(
        val wallet: String,
        val title: String,
        val body: String,
        val simulatedAmount: Long
    )

    val REAL_TEMPLATES = listOf(
        NotificationTemplate(
            wallet = "Nequi",
            title = "Envío",
            body = "[Cliente] te envió 1000, ¡lo mejor!",
            simulatedAmount = 1000L
        ),
        NotificationTemplate(
            wallet = "Daviplata",
            title = "DaviPlata",
            body = "Pasaron $20.000 a tu DaviPlata desde otro celular.",
            simulatedAmount = 20000L
        ),
        NotificationTemplate(
            wallet = "Bancolombia",
            title = "Bancolombia",
            body = "Bancolombia le informa recepción de transferencia por $50.000,00",
            simulatedAmount = 50000L
        ),
        NotificationTemplate(
            wallet = "SMS Bancario",
            title = "Mensaje Bancolombia (898999)",
            body = "Bancolombia le informa abono por $35.000 en cta de ahorros.",
            simulatedAmount = 35000L
        )
    )

    fun postSystemNotification(context: Context, template: NotificationTemplate) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create channel for Android 8.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Canal para pruebas de notificaciones de CantaPagos"
                enableVibration(true)
                enableLights(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(template.title)
            .setContentText(template.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(template.body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationId = (System.currentTimeMillis() % 10000).toInt()
        notificationManager.notify(notificationId, notification)
    }
}
