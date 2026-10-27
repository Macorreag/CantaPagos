package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.db.AppDatabase
import com.example.data.db.PaymentDao
import com.example.data.model.PaymentTransaction
import com.example.parser.WalletNotificationParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

data class SoundboxConfig(
    val playChime: Boolean = true,
    val speakWallet: Boolean = true,
    val speakSender: Boolean = false,
    val speakPesosWord: Boolean = true,
    val enableNequi: Boolean = true,
    val enableDaviplata: Boolean = true,
    val enableBancolombia: Boolean = true,
    val enableSms: Boolean = true,
    val strictPackageFilter: Boolean = true
)

data class NotificationDiagnosticEntry(
    val timestamp: Long,
    val packageName: String,
    val title: String,
    val body: String,
    val status: String,
    val isPaymentMatched: Boolean
)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class PaymentRepository(
    private val paymentDao: PaymentDao,
    private val prefs: SharedPreferences
) {
    private val repoScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _isServiceActive = MutableStateFlow(prefs.getBoolean(KEY_SERVICE_ACTIVE, true))
    val isServiceActive: StateFlow<Boolean> = _isServiceActive.asStateFlow()

    private val _config = MutableStateFlow(loadConfig())
    val config: StateFlow<SoundboxConfig> = _config.asStateFlow()

    private val _shiftCutoff = MutableStateFlow(getInitialShiftCutoff())
    val shiftCutoff: StateFlow<Long> = _shiftCutoff.asStateFlow()

    private val _recentDiagnostics = MutableStateFlow<List<NotificationDiagnosticEntry>>(emptyList())
    val recentDiagnostics: StateFlow<List<NotificationDiagnosticEntry>> = _recentDiagnostics.asStateFlow()

    private val processedKeys = LinkedHashSet<String>()

    init {
        val savedKeys = prefs.getStringSet(KEY_PROCESSED_NOTIFICATIONS, emptySet()) ?: emptySet()
        processedKeys.addAll(savedKeys)

        // Purge any invalid transactions (e.g. Ualá $0.22 interest) and anonymize stored history
        repoScope.launch {
            try {
                paymentDao.purgeInvalidTransactions()
                val existing = paymentDao.getAllTransactionsSnapshot()
                for (tx in existing) {
                    val anonymizedRaw = WalletNotificationParser.anonymizeNotificationText(tx.rawNotificationText)
                    val pkgId = WalletNotificationParser.resolveDisplayPackageId(tx.sender ?: "", tx.wallet)
                    if (anonymizedRaw != tx.rawNotificationText || tx.sender != pkgId) {
                        paymentDao.insertTransaction(
                            tx.copy(
                                sender = pkgId,
                                rawNotificationText = anonymizedRaw
                            )
                        )
                    }
                }
            } catch (_: Exception) {
            }
        }
    }

    val shiftTransactions: Flow<List<PaymentTransaction>> = _shiftCutoff.flatMapLatest { cutoff ->
        paymentDao.getTransactionsForShift(cutoff)
    }

    val shiftTotal: Flow<Long> = _shiftCutoff.flatMapLatest { cutoff ->
        paymentDao.getTotalForShift(cutoff)
    }

    val shiftCount: Flow<Int> = _shiftCutoff.flatMapLatest { cutoff ->
        paymentDao.getCountForShift(cutoff)
    }

    fun setServiceActive(active: Boolean) {
        prefs.edit().putBoolean(KEY_SERVICE_ACTIVE, active).apply()
        _isServiceActive.value = active
    }

    fun updateConfig(newConfig: SoundboxConfig) {
        prefs.edit()
            .putBoolean(KEY_PLAY_CHIME, newConfig.playChime)
            .putBoolean(KEY_SPEAK_WALLET, newConfig.speakWallet)
            .putBoolean(KEY_SPEAK_SENDER, false)
            .putBoolean(KEY_SPEAK_PESOS_WORD, newConfig.speakPesosWord)
            .putBoolean(KEY_ENABLE_NEQUI, newConfig.enableNequi)
            .putBoolean(KEY_ENABLE_DAVIPLATA, newConfig.enableDaviplata)
            .putBoolean(KEY_ENABLE_BANCOLOMBIA, newConfig.enableBancolombia)
            .putBoolean(KEY_ENABLE_SMS, newConfig.enableSms)
            .putBoolean(KEY_STRICT_PKG_FILTER, newConfig.strictPackageFilter)
            .apply()
        _config.value = newConfig.copy(speakSender = false)
    }

    fun isWalletEnabled(wallet: String): Boolean {
        val cfg = _config.value
        return when (wallet) {
            "Nequi" -> cfg.enableNequi
            "Daviplata" -> cfg.enableDaviplata
            "Bancolombia" -> cfg.enableBancolombia
            else -> cfg.enableSms
        }
    }

    @Synchronized
    fun markNotificationIfNew(signature: String): Boolean {
        if (processedKeys.contains(signature)) {
            return false
        }
        processedKeys.add(signature)
        if (processedKeys.size > 100) {
            val first = processedKeys.iterator().next()
            processedKeys.remove(first)
        }
        prefs.edit().putStringSet(KEY_PROCESSED_NOTIFICATIONS, processedKeys.toSet()).apply()
        return true
    }

    fun logDiagnostic(
        packageName: String,
        title: String,
        body: String,
        status: String,
        isPaymentMatched: Boolean
    ) {
        val anonymizedBody = WalletNotificationParser.anonymizeNotificationText(body)
        val entry = NotificationDiagnosticEntry(
            timestamp = System.currentTimeMillis(),
            packageName = packageName,
            title = title,
            body = anonymizedBody,
            status = status,
            isPaymentMatched = isPaymentMatched
        )
        _recentDiagnostics.value = (listOf(entry) + _recentDiagnostics.value).take(20)
    }

    suspend fun recordPayment(
        wallet: String,
        amount: Long,
        rawNotificationText: String,
        sender: String? = null
    ): PaymentTransaction {
        val formatted = formatCop(amount)
        val anonymizedText = WalletNotificationParser.anonymizeNotificationText(rawNotificationText)
        val appPackageId = WalletNotificationParser.resolveDisplayPackageId(sender ?: "", wallet)
        val transaction = PaymentTransaction(
            wallet = wallet,
            amount = amount,
            formattedAmount = formatted,
            timestamp = System.currentTimeMillis(),
            sender = appPackageId,
            rawNotificationText = anonymizedText,
            shiftId = _shiftCutoff.value
        )
        val id = paymentDao.insertTransaction(transaction)
        return transaction.copy(id = id)
    }

    fun resetShift() {
        val now = System.currentTimeMillis()
        prefs.edit().putLong(KEY_SHIFT_CUTOFF, now).apply()
        _shiftCutoff.value = now
    }

    suspend fun clearAll() {
        paymentDao.clearAll()
        resetShift()
    }

    private fun loadConfig(): SoundboxConfig {
        return SoundboxConfig(
            playChime = prefs.getBoolean(KEY_PLAY_CHIME, true),
            speakWallet = prefs.getBoolean(KEY_SPEAK_WALLET, true),
            speakSender = false,
            speakPesosWord = prefs.getBoolean(KEY_SPEAK_PESOS_WORD, true),
            enableNequi = prefs.getBoolean(KEY_ENABLE_NEQUI, true),
            enableDaviplata = prefs.getBoolean(KEY_ENABLE_DAVIPLATA, true),
            enableBancolombia = prefs.getBoolean(KEY_ENABLE_BANCOLOMBIA, true),
            enableSms = prefs.getBoolean(KEY_ENABLE_SMS, true),
            strictPackageFilter = prefs.getBoolean(KEY_STRICT_PKG_FILTER, true)
        )
    }

    private fun getInitialShiftCutoff(): Long {
        val saved = prefs.getLong(KEY_SHIFT_CUTOFF, -1L)
        if (saved != -1L) return saved

        // Default to beginning of today (midnight)
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val midnight = cal.timeInMillis
        prefs.edit().putLong(KEY_SHIFT_CUTOFF, midnight).apply()
        return midnight
    }

    companion object {
        private const val PREFS_NAME = "cantapagos_prefs"
        private const val KEY_SERVICE_ACTIVE = "key_service_active"
        private const val KEY_SHIFT_CUTOFF = "key_shift_cutoff"
        private const val KEY_PROCESSED_NOTIFICATIONS = "key_processed_notifications"

        private const val KEY_PLAY_CHIME = "key_play_chime"
        private const val KEY_SPEAK_WALLET = "key_speak_wallet"
        private const val KEY_SPEAK_SENDER = "key_speak_sender"
        private const val KEY_SPEAK_PESOS_WORD = "key_speak_pesos_word"
        private const val KEY_ENABLE_NEQUI = "key_enable_nequi"
        private const val KEY_ENABLE_DAVIPLATA = "key_enable_daviplata"
        private const val KEY_ENABLE_BANCOLOMBIA = "key_enable_bancolombia"
        private const val KEY_ENABLE_SMS = "key_enable_sms"
        private const val KEY_STRICT_PKG_FILTER = "key_strict_pkg_filter"

        @Volatile
        private var INSTANCE: PaymentRepository? = null

        fun getInstance(context: Context): PaymentRepository {
            return INSTANCE ?: synchronized(this) {
                val db = AppDatabase.getDatabase(context)
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val instance = PaymentRepository(db.paymentDao(), prefs)
                INSTANCE = instance
                instance
            }
        }

        fun formatCop(amount: Long): String {
            val format = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-CO"))
            format.maximumFractionDigits = 0
            val formatted = format.format(amount)
            return formatted.replace("COP", "$").trim()
        }
    }
}
