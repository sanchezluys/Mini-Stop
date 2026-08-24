package com.example.audio

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

class SoundEffects(private val context: Context) {
    private var toneGenerator: ToneGenerator? = null

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 85)
        } catch (_: Exception) {}
    }

    private fun getVibrator(): Vibrator? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                manager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        } catch (_: Exception) {
            null
        }
    }

    fun playTick() {
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 50)
            vibrate(30)
        } catch (_: Exception) {}
    }

    fun playRouletteSpin() {
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_PROMPT, 40)
            vibrate(20)
        } catch (_: Exception) {}
    }

    fun playRoundStart() {
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 250)
            vibrate(100)
        } catch (_: Exception) {}
    }

    fun playStopBuzzer() {
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_CDMA_ALERT_AUTOREDIAL_LITE, 600)
            vibrateLong()
        } catch (_: Exception) {}
    }

    fun playScoreSuccess() {
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 180)
            vibrate(40)
        } catch (_: Exception) {}
    }

    private fun vibrate(millis: Long) {
        try {
            val vibrator = getVibrator() ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(millis, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(millis)
            }
        } catch (_: Exception) {}
    }

    private fun vibrateLong() {
        try {
            val vibrator = getVibrator() ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val pattern = longArrayOf(0, 150, 80, 250)
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 150, 80, 250), -1)
            }
        } catch (_: Exception) {}
    }

    fun release() {
        try {
            toneGenerator?.release()
        } catch (_: Exception) {}
        toneGenerator = null
    }
}
