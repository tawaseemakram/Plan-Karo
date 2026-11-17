package com.example.reminder

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.example.service.TaskAlarmService

object AlarmSoundManager {
    private const val TAG = "AlarmSoundManager"
    const val MAX_ALARM_DURATION_MS = 60_000L

    private var mediaPlayer: MediaPlayer? = null
    private var ringtone: Ringtone? = null
    private var activeVibrator: Vibrator? = null
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private var activeAppContext: Context? = null

    @Volatile
    private var isAlarmActive: Boolean = false

    private val mainHandler = Handler(Looper.getMainLooper())
    private val autoStopRunnable = Runnable {
        Log.w(TAG, "Auto-stopping alarm after timeout to prevent background audio leak")
        stopAlarm(activeAppContext)
    }

    @Synchronized
    fun startAlarm(context: Context) {
        if (isAlarmActive) {
            mainHandler.removeCallbacks(autoStopRunnable)
            mainHandler.postDelayed(autoStopRunnable, MAX_ALARM_DURATION_MS)
            return
        }
        val appContext = context.applicationContext
        activeAppContext = appContext
        isAlarmActive = true

        try {
            requestAlarmAudioFocus(appContext)

            val customAlarmUri = ReminderManager.getAlarmSoundUri(appContext)
            val fallbackUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            val startedMediaPlayer = startLoopingMediaPlayer(appContext, customAlarmUri)
                || (fallbackUri != null && startLoopingMediaPlayer(appContext, fallbackUri))

            if (!startedMediaPlayer) {
                val r = RingtoneManager.getRingtone(appContext, customAlarmUri)
                    ?: RingtoneManager.getRingtone(appContext, fallbackUri)
                if (r != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        r.audioAttributes = AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        r.isLooping = true
                    }
                    ringtone = r
                    r.play()
                }
            }

            // Continuous repeating vibration until stopped or timed out
            vibrate(appContext)

            // Safety auto-stop timeout so an unacknowledged alarm cannot loop indefinitely
            mainHandler.removeCallbacks(autoStopRunnable)
            mainHandler.postDelayed(autoStopRunnable, MAX_ALARM_DURATION_MS)
        } catch (e: Exception) {
            Log.e(TAG, "Error playing alarm sound", e)
            stopAlarm(appContext)
        }
    }

    private fun startLoopingMediaPlayer(context: Context, uri: android.net.Uri): Boolean {
        return try {
            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(context, uri)
                isLooping = true
                setOnErrorListener { mp, _, _ ->
                    runCatching { mp.release() }
                    if (mediaPlayer === mp) {
                        mediaPlayer = null
                    }
                    true
                }
                prepare()
                start()
            }
            mediaPlayer = player
            true
        } catch (e: Exception) {
            Log.w(TAG, "MediaPlayer fallback for alarm URI $uri: ${e.message}")
            runCatching { mediaPlayer?.release() }
            mediaPlayer = null
            false
        }
    }

    private fun vibrate(context: Context) {
        try {
            val vibrator = resolveVibrator(context)
            activeVibrator = vibrator

            val pattern = longArrayOf(0, 600, 300, 600, 300, 800)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, 0)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Vibrate error", e)
        }
    }

    private fun resolveVibrator(context: Context): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    private fun requestAlarmAudioFocus(context: Context) {
        try {
            val am = (context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager) ?: return
            audioManager = am
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAcceptsDelayedFocusGain(false)
                    .setOnAudioFocusChangeListener { focusChange ->
                        if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
                            stopAlarm(activeAppContext)
                        }
                    }
                    .build()
                audioFocusRequest = focusRequest
                am.requestAudioFocus(focusRequest)
            } else {
                @Suppress("DEPRECATION")
                am.requestAudioFocus(
                    null,
                    AudioManager.STREAM_ALARM,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Unable to request alarm audio focus", e)
        }
    }

    private fun abandonAlarmAudioFocus(context: Context?) {
        try {
            val am = audioManager
                ?: (context?.applicationContext?.getSystemService(Context.AUDIO_SERVICE) as? AudioManager)
            if (am != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    audioFocusRequest?.let { am.abandonAudioFocusRequest(it) }
                } else {
                    @Suppress("DEPRECATION")
                    am.abandonAudioFocus(null)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Unable to abandon alarm audio focus", e)
        } finally {
            audioFocusRequest = null
            audioManager = null
        }
    }

    @Synchronized
    fun stopAlarm(context: Context? = null) {
        val targetContext = context?.applicationContext ?: activeAppContext
        stopLocalPlaybackOnly(targetContext)
        targetContext?.let { TaskAlarmService.stopAlarmService(it) }
    }

    @Synchronized
    fun stopLocalPlaybackOnly(context: Context? = null) {
        isAlarmActive = false
        mainHandler.removeCallbacks(autoStopRunnable)
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping alarm MediaPlayer", e)
        } finally {
            mediaPlayer = null
        }

        try {
            ringtone?.let {
                if (it.isPlaying) {
                    it.stop()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping alarm Ringtone", e)
        } finally {
            ringtone = null
        }

        try {
            val targetCtx = context?.applicationContext ?: activeAppContext
            val vibrator = activeVibrator ?: targetCtx?.let { resolveVibrator(it) }
            vibrator?.cancel()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping vibration", e)
        } finally {
            activeVibrator = null
        }

        abandonAlarmAudioFocus(context ?: activeAppContext)
        activeAppContext = null
    }

    fun isPlaying(): Boolean = isAlarmActive
}
