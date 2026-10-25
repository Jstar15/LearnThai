package com.example.learnthai.audio

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import java.io.File
import java.io.IOException

class AudioRecorderManager(private val context: Context) {

    private var recorder: MediaRecorder? = null
    private var player: MediaPlayer? = null
    private val audioFile: File by lazy {
        File(context.cacheDir, "user_speech_attempt.aac")
    }

    var isRecording: Boolean = false
        private set

    var isPlaying: Boolean = false
        private set

    fun startRecording(): Boolean {
        stopPlaying()
        stopRecording()

        return try {
            recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.AAC_ADTS)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(audioFile.absolutePath)
                prepare()
                start()
            }
            isRecording = true
            true
        } catch (e: Exception) {
            e.printStackTrace()
            isRecording = false
            false
        }
    }

    fun stopRecording(): Boolean {
        if (!isRecording) return false
        return try {
            recorder?.stop()
            recorder?.release()
            recorder = null
            isRecording = false
            true
        } catch (e: Exception) {
            e.printStackTrace()
            recorder = null
            isRecording = false
            false
        }
    }

    fun hasRecording(): Boolean {
        return audioFile.exists() && audioFile.length() > 0
    }

    fun playRecording(onComplete: () -> Unit = {}) {
        stopPlaying()
        if (!hasRecording()) return

        try {
            player = MediaPlayer().apply {
                setDataSource(audioFile.absolutePath)
                prepare()
                setOnCompletionListener {
                    this@AudioRecorderManager.isPlaying = false
                    onComplete()
                }
                start()
            }
            isPlaying = true
        } catch (e: IOException) {
            e.printStackTrace()
            isPlaying = false
        }
    }

    fun stopPlaying() {
        if (isPlaying) {
            try {
                player?.stop()
                player?.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            player = null
            isPlaying = false
        }
    }

    fun release() {
        stopRecording()
        stopPlaying()
    }
}
