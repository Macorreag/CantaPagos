package com.example.service

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.text.TextUtils
import android.util.Log
import com.example.audio.AudioNotificationEngine
import com.example.data.repository.PaymentRepository
import com.example.parser.WalletNotificationParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class SoundboxListenerService : NotificationListenerService() {

    private val tag = "SoundboxListener"
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private lateinit var repository: PaymentRepository
    private lateinit var audioEngine: AudioNotificationEngine

    override fun onCreate() {
        super.onCreate()
        repository = PaymentRepository.getInstance(applicationContext)
        audioEngine = AudioNotificationEngine.getInstance(applicationContext)
        instance = this
        Log.d(tag, "SoundboxListenerService created.")
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        instance = this
        Log.i(tag, "NotificationListener connected and listening for payments.")
        scanCurrentActiveNotifications()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        if (instance === this) {
            instance = null
        }
        Log.w(tag, "NotificationListener disconnected.")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return
        processStatusBarNotification(sbn, isFromActiveScan = false)
    }

    fun scanCurrentActiveNotifications() {
        try {
            val activeList = activeNotifications ?: return
            for (sbn in activeList) {
                // Only scan active notifications belonging to the current shift
                if (sbn.postTime >= repository.shiftCutoff.value) {
                    processStatusBarNotification(sbn, isFromActiveScan = true)
                }
            }
        } catch (e: Exception) {
            Log.w(tag, "Unable to scan active notifications: ${e.message}")
        }
    }

    private fun processStatusBarNotification(sbn: StatusBarNotification, isFromActiveScan: Boolean) {
        if (!repository.isServiceActive.value) {
            return
        }

        val packageName = sbn.packageName ?: return
        val notification = sbn.notification ?: return
        val extras = notification.extras ?: return

        val cfg = repository.config.value
        val appSource = WalletNotificationParser.identifyAppByPackage(packageName)

        // Step 1: Fast Package ID filter before parsing
        if (cfg.strictPackageFilter && appSource == null) {
            return
        }

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_TITLE_BIG)?.toString()
            ?: ""

        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: ""
        val textLines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
            ?.mapNotNull { it?.toString() }
            ?.joinToString(" ")
            ?: ""

        val fullBody = when {
            bigText.isNotBlank() && bigText.length >= text.length -> bigText
            text.isNotBlank() -> text
            textLines.isNotBlank() -> textLines
            else -> ""
        }

        if (title.isBlank() && fullBody.isBlank()) return

        // Run parser with package-first identification
        val parsed = WalletNotificationParser.parse(
            packageName = packageName,
            title = title,
            text = fullBody,
            strictPackageFilter = cfg.strictPackageFilter
        )

        if (parsed != null) {
            if (!repository.isWalletEnabled(parsed.wallet)) {
                repository.logDiagnostic(
                    packageName = packageName,
                    title = title,
                    body = fullBody,
                    status = "⏭️ Billetera ${parsed.wallet} desactivada en ajustes",
                    isPaymentMatched = false
                )
                return
            }

            // Build deduplication signature so the exact same notification isn't announced twice
            val signature = "${packageName}|${sbn.postTime / 4000}|${parsed.amount}|${parsed.sender ?: ""}"
            if (!repository.markNotificationIfNew(signature)) {
                Log.d(tag, "Duplicate notification skipped: $signature")
                return
            }

            Log.i(tag, "✅ VERIFIED PAYMENT DETECTED! Pkg=$packageName, Wallet=${parsed.wallet}, Amount=${parsed.amount}, Sender=${parsed.sender}")

            repository.logDiagnostic(
                packageName = packageName,
                title = title,
                body = fullBody,
                status = "✅ Pago cantado: ${parsed.formattedAmount} (${parsed.wallet})",
                isPaymentMatched = true
            )

            // 1. Save in local Room Database with App Package ID (no personal names)
            serviceScope.launch {
                repository.recordPayment(
                    wallet = parsed.wallet,
                    amount = parsed.amount,
                    rawNotificationText = parsed.rawText,
                    sender = parsed.packageName
                )
            }

            // 2. Play chime + speak out loud adhering to user preferences
            audioEngine.announcePayment(parsed, cfg)
        } else if (!isFromActiveScan && appSource != null) {
            // Log why a notification from a known wallet package was ignored (e.g., promo, outbound)
            repository.logDiagnostic(
                packageName = packageName,
                title = title,
                body = fullBody,
                status = "⏭️ Ignorada (No es abono/ingreso)",
                isPaymentMatched = false
            )
        }
    }

    override fun onDestroy() {
        if (instance === this) {
            instance = null
        }
        super.onDestroy()
        serviceScope.cancel()
    }

    companion object {
        @Volatile
        private var instance: SoundboxListenerService? = null

        fun isNotificationAccessGranted(context: Context): Boolean {
            val pkgName = context.packageName
            val flat = Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners"
            )
            if (!TextUtils.isEmpty(flat)) {
                val names = flat.split(":")
                for (name in names) {
                    val cn = ComponentName.unflattenFromString(name)
                    if (cn != null && TextUtils.equals(pkgName, cn.packageName)) {
                        return true
                    }
                }
            }
            return false
        }

        fun ensureServiceActiveAndScan(context: Context) {
            if (!isNotificationAccessGranted(context)) return
            val current = instance
            if (current != null) {
                current.scanCurrentActiveNotifications()
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                try {
                    requestRebind(ComponentName(context, SoundboxListenerService::class.java))
                } catch (e: Exception) {
                    Log.w("SoundboxListener", "Failed to requestRebind: ${e.message}")
                }
            }
        }
    }
}
