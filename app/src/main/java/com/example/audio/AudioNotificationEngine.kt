package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.util.Log
import com.example.parser.ParsedPayment
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Locale
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

class AudioNotificationEngine(private val context: Context) : TextToSpeech.OnInitListener {

    private val tag = "AudioNotificationEngine"
    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    private var soundPool: SoundPool? = null
    private var chimeSoundId: Int = -1
    private var isSoundPoolReady = false

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val mainHandler = Handler(Looper.getMainLooper())

    private val audioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build()

    init {
        initSoundPool()
        initTts()
    }

    private fun initTts() {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val colombia = Locale.forLanguageTag("es-CO")
            val available = tts?.isLanguageAvailable(colombia) ?: TextToSpeech.LANG_NOT_SUPPORTED
            if (available >= TextToSpeech.LANG_AVAILABLE) {
                tts?.language = colombia
            } else {
                tts?.language = Locale.forLanguageTag("es")
            }
            tts?.setAudioAttributes(audioAttributes)
            tts?.setSpeechRate(0.95f) // Slightly relaxed pace for maximum counter clarity
            tts?.setPitch(1.0f)
            isTtsReady = true
            Log.d(tag, "TextToSpeech initialized in Spanish.")
        } else {
            Log.e(tag, "Failed to initialize TextToSpeech: status=$status")
        }
    }

    private fun initSoundPool() {
        soundPool = SoundPool.Builder()
            .setMaxStreams(2)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .build()

        soundPool?.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) {
                isSoundPoolReady = true
                Log.d(tag, "Chime sound loaded into SoundPool (id=$sampleId)")
            }
        }

        try {
            val chimeFile = generateCashChimeWav(context)
            chimeSoundId = soundPool?.load(chimeFile.absolutePath, 1) ?: -1
        } catch (e: Exception) {
            Log.e(tag, "Error generating or loading chime sound", e)
        }
    }

    /**
     * Announces a received payment out loud adhering to user-selected announcement items.
     */
    fun announcePayment(payment: ParsedPayment, config: com.example.data.repository.SoundboxConfig = com.example.data.repository.SoundboxConfig()) {
        mainHandler.post {
            ensureAudibleVolume()

            val speechText = buildAnnouncementPhrase(payment, config)

            // Visual feedback on screen
            try {
                android.widget.Toast.makeText(context, "🔔 $speechText", android.widget.Toast.LENGTH_LONG).show()
            } catch (_: Exception) {}

            if (config.playChime && soundPool != null && chimeSoundId != -1) {
                soundPool?.play(chimeSoundId, 1.0f, 1.0f, 1, 0, 1.0f)
                // Wait 300ms so the chime rings first
                mainHandler.postDelayed({
                    speakText(speechText)
                }, 300)
            } else {
                speakText(speechText)
            }
        }
    }

    private fun buildAnnouncementPhrase(payment: ParsedPayment, config: com.example.data.repository.SoundboxConfig): String {
        val amountPart = if (config.speakPesosWord) {
            payment.spokenAmount
        } else {
            payment.spokenAmount.replace(" pesos", "").replace(" de pesos", "").trim()
        }

        return if (config.speakWallet) {
            "¡${payment.wallet}! Recibiste $amountPart"
        } else {
            "Recibiste $amountPart"
        }
    }

    fun playTestChimeAndVoice(
        amount: Long = 10000L,
        wallet: String = "Nequi",
        config: com.example.data.repository.SoundboxConfig = com.example.data.repository.SoundboxConfig()
    ) {
        val spoken = com.example.parser.WalletNotificationParser.numberToSpanishWords(amount)
        val testPayment = ParsedPayment(
            wallet = wallet,
            amount = amount,
            spokenAmount = spoken,
            formattedAmount = "$ 10.000",
            sender = null,
            rawText = "Prueba de sonido",
            packageName = com.example.parser.WalletNotificationParser.resolveDisplayPackageId("", wallet)
        )
        announcePayment(testPayment, config)
    }

    private fun speakText(text: String) {
        if (!isTtsReady || tts == null) {
            Log.w(tag, "TTS is not ready yet. Retrying in 500ms...")
            mainHandler.postDelayed({ speakText(text) }, 500)
            return
        }

        val params = Bundle()
        params.putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC)
        params.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)

        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "PAYMENT_ANNOUNCEMENT_${System.currentTimeMillis()}")
    }

    private fun ensureAudibleVolume() {
        try {
            val maxMedia = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val currentMedia = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            val targetVolume = (maxMedia * 0.85).toInt().coerceAtLeast(1)
            if (currentMedia < targetVolume) {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVolume, 0)
            }
        } catch (e: Exception) {
            Log.w(tag, "Could not adjust media volume: ${e.message}")
        }
    }

    fun release() {
        mainHandler.removeCallbacksAndMessages(null)
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (e: Exception) {
            Log.e(tag, "Error shutting down TTS", e)
        }
        try {
            soundPool?.release()
        } catch (e: Exception) {
            Log.e(tag, "Error releasing SoundPool", e)
        }
    }

    companion object {
        /**
         * Synthesizes a high-fidelity two-tone cash register bell ("Clin-ck!") into an offline WAV file.
         * Harmonic frequencies: 2000Hz (strike) and 2800Hz (resonant silver bell ring) with decay.
         */
        fun generateCashChimeWav(context: Context): File {
            val file = File(context.cacheDir, "cash_register_chime.wav")
            if (file.exists() && file.length() > 1000) {
                return file
            }

            val sampleRate = 44100
            val durationSeconds = 0.38
            val totalSamples = (sampleRate * durationSeconds).toInt()
            val pcmData = ShortArray(totalSamples)

            val freq1 = 2050.0 // Main chime strike
            val freq2 = 2850.0 // High overtone bell
            val freq3 = 3800.0 // Shimmer sparkle

            for (i in 0 until totalSamples) {
                val t = i.toDouble() / sampleRate
                // Attack: instant rise, Exponential decay
                val envelope = exp(-t * 11.0)
                // Short click sound during the first 15ms
                val click = if (t < 0.015) ((Math.random() * 2 - 1) * exp(-t * 200.0) * 0.25) else 0.0

                val sampleVal = (
                        0.55 * sin(2.0 * PI * freq1 * t) +
                        0.35 * sin(2.0 * PI * freq2 * t) +
                        0.10 * sin(2.0 * PI * freq3 * t) +
                        click
                ) * envelope

                val clamped = (sampleVal * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                pcmData[i] = clamped.toShort()
            }

            // Write 16-bit Mono WAV
            FileOutputStream(file).use { fos ->
                val byteData = ByteBuffer.allocate(pcmData.size * 2).order(ByteOrder.LITTLE_ENDIAN)
                for (s in pcmData) {
                    byteData.putShort(s)
                }
                val audioBytes = byteData.array()
                val header = createWavHeader(audioBytes.size, sampleRate, 1, 16)
                fos.write(header)
                fos.write(audioBytes)
            }

            return file
        }

        private fun createWavHeader(pcmDataSize: Int, sampleRate: Int, channels: Int, bitsPerSample: Int): ByteArray {
            val totalDataLen = pcmDataSize + 36
            val byteRate = sampleRate * channels * (bitsPerSample / 8)
            val header = ByteArray(44)
            val bb = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)

            bb.put("RIFF".toByteArray())
            bb.putInt(totalDataLen)
            bb.put("WAVE".toByteArray())
            bb.put("fmt ".toByteArray())
            bb.putInt(16) // Subchunk1Size for PCM
            bb.putShort(1.toShort()) // AudioFormat (1 = PCM)
            bb.putShort(channels.toShort())
            bb.putInt(sampleRate)
            bb.putInt(byteRate)
            bb.putShort((channels * (bitsPerSample / 8)).toShort()) // BlockAlign
            bb.putShort(bitsPerSample.toShort())
            bb.put("data".toByteArray())
            bb.putInt(pcmDataSize)

            return header
        }

        @Volatile
        private var INSTANCE: AudioNotificationEngine? = null

        fun getInstance(context: Context): AudioNotificationEngine {
            return INSTANCE ?: synchronized(this) {
                val instance = AudioNotificationEngine(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
