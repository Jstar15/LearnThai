package com.example.learnthai.audio

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Minimal 16-bit PCM WAV (RIFF) read/write helpers. No external dependencies.
 */
object WavFile {

    data class Pcm(val samples: FloatArray, val sampleRate: Int)

    /** Writes mono 16-bit PCM samples as a WAV file. */
    fun writePcm16(file: File, samples: ShortArray, sampleRate: Int) {
        val channels = 1
        val dataSize = samples.size * 2
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
        header.put("RIFF".toByteArray(Charsets.US_ASCII))
        header.putInt(36 + dataSize)
        header.put("WAVE".toByteArray(Charsets.US_ASCII))
        header.put("fmt ".toByteArray(Charsets.US_ASCII))
        header.putInt(16)                                  // fmt chunk size (PCM)
        header.putShort(1)                                 // audio format = PCM
        header.putShort(channels.toShort())
        header.putInt(sampleRate)
        header.putInt(sampleRate * channels * 2)           // byte rate
        header.putShort((channels * 2).toShort())          // block align
        header.putShort(16)                                // bits per sample
        header.put("data".toByteArray(Charsets.US_ASCII))
        header.putInt(dataSize)

        FileOutputStream(file).use { out ->
            out.write(header.array())
            val body = ByteBuffer.allocate(dataSize).order(ByteOrder.LITTLE_ENDIAN)
            for (s in samples) body.putShort(s)
            out.write(body.array())
        }
    }

    /** Reads a WAV file and returns normalized mono float samples plus the sample rate. */
    fun readPcm16(file: File): Pcm? {
        return try {
            FileInputStream(file).use { input ->
                if (String(readFully(input, 4), Charsets.US_ASCII) != "RIFF") return null
                readLeInt(input) // RIFF chunk size (unused)
                if (String(readFully(input, 4), Charsets.US_ASCII) != "WAVE") return null

                var sampleRate = 0
                var channels = 1
                var dataBytes: ByteArray? = null

                while (true) {
                    val idBytes = readFully(input, 4)
                    if (idBytes.size < 4) break
                    val id = String(idBytes, Charsets.US_ASCII)
                    val chunkSize = readLeInt(input)
                    when (id) {
                        "fmt " -> {
                            val fmt = readFully(input, chunkSize)
                            val bb = ByteBuffer.wrap(fmt).order(ByteOrder.LITTLE_ENDIAN)
                            val audioFormat = bb.getShort(0).toInt()
                            if (audioFormat != 1) return null          // only PCM
                            channels = bb.getShort(2).toInt().coerceAtLeast(1)
                            sampleRate = bb.getInt(4)
                        }
                        "data" -> {
                            dataBytes = readFully(input, chunkSize)
                            break
                        }
                        else -> skipFully(input, chunkSize)
                    }
                }

                val data = dataBytes ?: return null
                if (sampleRate <= 0 || data.size < 2) return null

                val totalShorts = data.size / 2
                val bb = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
                val all = FloatArray(totalShorts) { bb.short.toFloat() / 32768f }

                val samples = if (channels <= 1) {
                    all
                } else {
                    val monoCount = (totalShorts + channels - 1) / channels
                    FloatArray(monoCount) { idx -> all[idx * channels] }
                }
                Pcm(samples, sampleRate)
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun readLeInt(input: InputStream): Int {
        val b = readFully(input, 4)
        return (b[0].toInt() and 0xFF) or
            ((b[1].toInt() and 0xFF) shl 8) or
            ((b[2].toInt() and 0xFF) shl 16) or
            ((b[3].toInt() and 0xFF) shl 24)
    }

    private fun readFully(input: InputStream, n: Int): ByteArray {
        val buf = ByteArray(n)
        var off = 0
        while (off < n) {
            val r = input.read(buf, off, n - off)
            if (r < 0) break
            off += r
        }
        return if (off == n) buf else buf.copyOf(off)
    }

    private fun skipFully(input: InputStream, n: Int) {
        var remaining = n
        val buf = ByteArray(4096)
        while (remaining > 0) {
            val r = input.read(buf, 0, minOf(remaining, buf.size))
            if (r < 0) break
            remaining -= r
        }
    }
}
