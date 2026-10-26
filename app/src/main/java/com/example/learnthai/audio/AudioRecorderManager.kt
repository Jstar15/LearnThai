package com.example.learnthai.audio

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Process
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Records the user's voice as raw 16-bit PCM (via [AudioRecord]) for pitch analysis,
 * stores it as a WAV file for playback, and exposes the samples.
 *
 * IMPORTANT: this must NOT run at the same time as SpeechRecognizer (they contend over the mic),
 * so the Speech Lab uses it in a separate phase after transcription has finished.
 */
class AudioRecorderManager(private val context: Context) {

    companion object {
        const val SAMPLE_RATE = 44100
    }

    private var audioRecord: AudioRecord? = null
    private var recordingThread: Thread? = null
    private var player: MediaPlayer? = null
    private val stopRequested = AtomicBoolean(false)
    private val recordedSamples = ArrayList<Short>()

    private val wavFile: File by lazy { File(context.cacheDir, "user_speech_attempt.wav") }

    @Volatile
    private var pcmShorts: ShortArray? = null

    var isRecording: Boolean = false
        private set

    var isPlaying: Boolean = false
        private set

    fun startRecording(): Boolean {
        stopPlaying()
        stopRecording()
        pcmShorts = null
        synchronized(recordedSamples) { recordedSamples.clear() }

        return try {
            val minBuf = AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val bufferSize = if (minBuf > 0) minBuf * 2 else 8192

            val record = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            )
            if (record.state != AudioRecord.STATE_INITIALIZED) {
                record.release()
                return false
            }

            audioRecord = record
            stopRequested.set(false)
            record.startRecording()

            recordingThread = Thread({
                Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)
                val buffer = ShortArray(bufferSize)
                while (!stopRequested.get()) {
                    val n = record.read(buffer, 0, buffer.size)
                    if (n > 0) {
                        synchronized(recordedSamples) {
                            for (i in 0 until n) recordedSamples.add(buffer[i])
                        }
                    }
                }
            }, "PcmRecorder").apply { start() }

            isRecording = true
            true
        } catch (e: Exception) {
            e.printStackTrace()
            isRecording = false
            false
        }
    }

    fun stopRecording(): Boolean {
        stopRequested.set(true)
        recordingThread?.join(1000)
        recordingThread = null

        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        audioRecord = null

        val wasRecording = isRecording
        isRecording = false
        if (!wasRecording) return false

        val shorts = synchronized(recordedSamples) {
            ShortArray(recordedSamples.size) { recordedSamples[it] }
        }
        if (shorts.isNotEmpty()) {
            pcmShorts = shorts
            try {
                WavFile.writePcm16(wavFile, shorts, SAMPLE_RATE)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return true
    }

    /** Returns the most recent recording as normalized floats in [-1, 1], or null. */
    fun getPcmSamples(): FloatArray? {
        val shorts = pcmShorts ?: return null
        if (shorts.isEmpty()) return null
        return FloatArray(shorts.size) { shorts[it] / 32768f }
    }

    /** Returns the raw 16-bit little-endian PCM bytes of the last recording. */
    fun getPcmBytes(): ByteArray? {
        val shorts = pcmShorts ?: return null
        if (shorts.isEmpty()) return null
        val bytes = ByteArray(shorts.size * 2)
        val bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        for (s in shorts) bb.putShort(s)
        return bytes
    }

    fun getRecordingDurationMs(): Float {
        val shorts = pcmShorts ?: return 0f
        return shorts.size * 1000f / SAMPLE_RATE
    }

    fun hasRecording(): Boolean = wavFile.exists() && wavFile.length() > 0

    fun playRecording(onComplete: () -> Unit = {}) {
        stopPlaying()
        if (!hasRecording()) return
        try {
            player = MediaPlayer().apply {
                setDataSource(wavFile.absolutePath)
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
