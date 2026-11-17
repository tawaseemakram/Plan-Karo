package com.example.audio

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.File
import java.io.IOException

class AudioNoteRecorder {
    private var mediaRecorder: MediaRecorder? = null
    private var currentOutputFile: File? = null
    private var recordingStartTime: Long = 0L

    var isRecording: Boolean = false
        private set

    fun startRecording(context: Context): File? {
        if (isRecording) {
            cancelRecording()
        }

        return try {
            val audioDir = File(context.filesDir, "audio_notes").apply {
                if (!exists()) mkdirs()
            }
            val outputFile = File(audioDir, "audio_${System.currentTimeMillis()}.m4a")

            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(outputFile.absolutePath)
                prepare()
                start()
            }

            mediaRecorder = recorder
            currentOutputFile = outputFile
            recordingStartTime = System.currentTimeMillis()
            isRecording = true
            outputFile
        } catch (e: Exception) {
            Log.e("AudioNoteRecorder", "Failed to start recording", e)
            cleanup()
            null
        }
    }

    fun stopRecording(): Pair<File?, Int> {
        if (!isRecording) return Pair(null, 0)

        val durationSeconds = ((System.currentTimeMillis() - recordingStartTime) / 1000).toInt().coerceAtLeast(1)
        val file = currentOutputFile

        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            Log.e("AudioNoteRecorder", "Error stopping recorder", e)
        } finally {
            mediaRecorder = null
            isRecording = false
            currentOutputFile = null
        }

        return Pair(file, durationSeconds)
    }

    fun cancelRecording() {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            Log.w("AudioNoteRecorder", "Error canceling recording", e)
        } finally {
            mediaRecorder = null
            isRecording = false
            currentOutputFile?.let { file ->
                if (file.exists()) {
                    file.delete()
                }
            }
            currentOutputFile = null
        }
    }

    fun getMaxAmplitude(): Int {
        return try {
            if (isRecording) mediaRecorder?.maxAmplitude ?: 0 else 0
        } catch (_: Exception) {
            0
        }
    }

    private fun cleanup() {
        try {
            mediaRecorder?.release()
        } catch (_: Exception) {}
        mediaRecorder = null
        isRecording = false
        currentOutputFile?.let {
            if (it.exists()) it.delete()
        }
        currentOutputFile = null
    }
}

class AudioNotePlayer {
    private var mediaPlayer: MediaPlayer? = null
    private var currentPlayingUri: String? = null
    private var onCompletionCallback: (() -> Unit)? = null

    val isPlaying: Boolean
        get() = try {
            mediaPlayer?.isPlaying == true
        } catch (_: Exception) {
            false
        }

    val currentPosition: Int
        get() = try {
            mediaPlayer?.currentPosition ?: 0
        } catch (_: Exception) {
            0
        }

    val duration: Int
        get() = try {
            mediaPlayer?.duration ?: 0
        } catch (_: Exception) {
            0
        }

    fun play(filePathOrUri: String, onCompletion: () -> Unit): Boolean {
        stop()

        return try {
            val player = MediaPlayer().apply {
                setDataSource(filePathOrUri)
                prepare()
                setOnCompletionListener {
                    onCompletionCallback?.invoke()
                    this@AudioNotePlayer.stop()
                }
                setOnErrorListener { _, what, extra ->
                    Log.e("AudioNotePlayer", "MediaPlayer error: what=$what extra=$extra")
                    this@AudioNotePlayer.stop()
                    true
                }
                start()
            }
            mediaPlayer = player
            currentPlayingUri = filePathOrUri
            onCompletionCallback = onCompletion
            true
        } catch (e: Exception) {
            Log.e("AudioNotePlayer", "Failed to play audio: $filePathOrUri", e)
            stop()
            false
        }
    }

    fun pause() {
        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.pause()
            }
        } catch (e: Exception) {
            Log.e("AudioNotePlayer", "Failed to pause", e)
        }
    }

    fun resume() {
        try {
            mediaPlayer?.start()
        } catch (e: Exception) {
            Log.e("AudioNotePlayer", "Failed to resume", e)
        }
    }

    fun seekTo(positionMs: Int) {
        try {
            mediaPlayer?.seekTo(positionMs)
        } catch (e: Exception) {
            Log.e("AudioNotePlayer", "Failed to seek", e)
        }
    }

    fun stop() {
        try {
            mediaPlayer?.apply {
                if (isPlaying) {
                    stop()
                }
                reset()
                release()
            }
        } catch (e: Exception) {
            Log.e("AudioNotePlayer", "Failed to stop", e)
        } finally {
            mediaPlayer = null
            currentPlayingUri = null
            onCompletionCallback = null
        }
    }

    fun release() {
        stop()
    }
}

object AudioUtils {
    fun formatDuration(seconds: Int): String {
        val safeSec = seconds.coerceAtLeast(0)
        val mins = safeSec / 60
        val secs = safeSec % 60
        return "%d:%02d".format(mins, secs)
    }

    fun formatDurationMillis(millis: Int): String {
        return formatDuration(millis / 1000)
    }

    fun deleteOwnedAudioFile(context: Context, audioUri: String?): Boolean {
        if (audioUri.isNullOrBlank()) return false
        return try {
            val path = if (audioUri.startsWith("file://")) {
                android.net.Uri.parse(audioUri).path
            } else {
                audioUri
            } ?: return false
            val target = File(path)
            val audioDir = File(context.applicationContext.filesDir, "audio_notes")
            val inOwnedDir = target.canonicalPath.startsWith(audioDir.canonicalPath + File.separator)
            inOwnedDir && target.exists() && target.delete()
        } catch (e: Exception) {
            Log.w("AudioUtils", "Failed deleting owned audio file: $audioUri", e)
            false
        }
    }
}
