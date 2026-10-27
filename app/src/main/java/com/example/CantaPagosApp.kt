package com.example

import android.app.Application
import android.util.Log
import com.example.audio.AudioNotificationEngine
import com.example.data.db.AppDatabase
import com.example.data.repository.PaymentRepository

class CantaPagosApp : Application() {

    lateinit var repository: PaymentRepository
        private set

    lateinit var audioEngine: AudioNotificationEngine
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        AppDatabase.getDatabase(this)
        repository = PaymentRepository.getInstance(this)
        audioEngine = AudioNotificationEngine.getInstance(this)

        Log.d("CantaPagosApp", "CantaPagos application initialized offline.")
    }

    companion object {
        lateinit var instance: CantaPagosApp
            private set
    }
}
