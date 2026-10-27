package com.example.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioNotificationEngine
import com.example.data.model.PaymentTransaction
import com.example.data.repository.PaymentRepository
import com.example.parser.ParsedPayment
import com.example.parser.WalletNotificationParser
import com.example.service.SoundboxListenerService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class WalletBreakdown(
    val wallet: String,
    val count: Int,
    val total: Long
)

class SoundboxViewModel(
    private val repository: PaymentRepository,
    private val audioEngine: AudioNotificationEngine
) : ViewModel() {

    val isServiceActive: StateFlow<Boolean> = repository.isServiceActive

    private val _isPermissionGranted = MutableStateFlow(false)
    val isPermissionGranted: StateFlow<Boolean> = _isPermissionGranted.asStateFlow()

    val shiftTransactions: StateFlow<List<PaymentTransaction>> = repository.shiftTransactions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val shiftTotal: StateFlow<Long> = repository.shiftTotal
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0L
        )

    val shiftCount: StateFlow<Int> = repository.shiftCount
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    val walletBreakdown: StateFlow<List<WalletBreakdown>> = shiftTransactions.map { list ->
        val groups = list.groupBy { it.wallet }
        listOf("Nequi", "Daviplata", "Bancolombia", "SMS Bancario").mapNotNull { walletName ->
            val items = groups[walletName] ?: emptyList()
            if (items.isNotEmpty()) {
                WalletBreakdown(
                    wallet = walletName,
                    count = items.size,
                    total = items.sumOf { it.amount }
                )
            } else null
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val config = repository.config
    val recentDiagnostics = repository.recentDiagnostics

    fun checkPermission(context: Context) {
        val granted = SoundboxListenerService.isNotificationAccessGranted(context)
        _isPermissionGranted.value = granted
        if (granted) {
            SoundboxListenerService.ensureServiceActiveAndScan(context)
        }
    }

    fun setServiceActive(active: Boolean) {
        repository.setServiceActive(active)
    }

    fun updateConfig(newConfig: com.example.data.repository.SoundboxConfig) {
        repository.updateConfig(newConfig)
    }

    fun playTestChime(amount: Long = 10000L, wallet: String = "Nequi") {
        audioEngine.playTestChimeAndVoice(amount, wallet, repository.config.value)
    }

    fun simulatePayment(wallet: String, amount: Long, sender: String? = null) {
        viewModelScope.launch {
            val spoken = WalletNotificationParser.numberToSpanishWords(amount)
            val formatted = PaymentRepository.formatCop(amount)
            val parsed = ParsedPayment(
                wallet = wallet,
                amount = amount,
                spokenAmount = spoken,
                formattedAmount = formatted,
                sender = sender,
                rawText = "Simulación: Recibiste $formatted en $wallet"
            )

            // 1. Record in DB
            repository.recordPayment(
                wallet = parsed.wallet,
                amount = parsed.amount,
                rawNotificationText = parsed.rawText,
                sender = parsed.sender
            )

            // 2. Announce with user preferences
            audioEngine.announcePayment(parsed, repository.config.value)
        }
    }

    fun closeShift() {
        repository.resetShift()
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAll()
        }
    }

    class Factory(
        private val repository: PaymentRepository,
        private val audioEngine: AudioNotificationEngine
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SoundboxViewModel(repository, audioEngine) as T
        }
    }
}
