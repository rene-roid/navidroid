package xyz.yuuki.navidroid

import androidx.media3.common.C
import androidx.media3.exoplayer.audio.TeeAudioProcessor
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.exp
import kotlin.math.sqrt

/**
 * How loud the music is and when the bass hits, for the aurora. ExoPlayer decodes ahead of the speaker, so every
 * 20 ms chunk is stamped with when it will be heard: chunks queue up back to back after the last one, or start
 * now if the queue has drained (start, seek, resume).
 */
object Levels : TeeAudioProcessor.AudioBufferSink {
    private const val N = 256
    private const val CHUNK_NS = 20_000_000L
    private val stamps = LongArray(N)
    private val levels = FloatArray(N)
    private val kicks = FloatArray(N)
    private var count = 0

    /** Off when the aurora doesn't react to music, to save the work. */
    @Volatile var listening = true

    // Analysis state, touched only by the playback thread.
    private var rate = 44_100
    private var channels = 2
    private var encoding = C.ENCODING_INVALID
    private var lowA = 0.02f
    private var low1 = 0f
    private var low2 = 0f
    private var sum = 0f
    private var bass = 0f
    private var frames = 0
    private var peak = 0.05f
    private var bassAvg = 0f
    private var next = 0L

    override fun flush(sampleRateHz: Int, channelCount: Int, encoding: Int) {
        rate = sampleRateHz
        channels = channelCount
        this.encoding = encoding
        // Two one-pole low-passes at 150 Hz keep the kick and the bass line.
        lowA = 1 - exp(-2 * Math.PI.toFloat() * 150f / sampleRateHz)
        frames = 0
        sum = 0f
        bass = 0f
        next = 0L
        synchronized(this) { count = 0 }
    }

    override fun handleBuffer(buffer: ByteBuffer) {
        if (!listening) return
        val b = buffer.order(ByteOrder.nativeOrder())
        val now = System.nanoTime()
        when (encoding) {
            C.ENCODING_PCM_16BIT -> while (b.remaining() >= 2 * channels) {
                var x = 0f
                repeat(channels) { x += b.short / 32768f }
                feed(x / channels, now)
            }
            C.ENCODING_PCM_FLOAT -> while (b.remaining() >= 4 * channels) {
                var x = 0f
                repeat(channels) { x += b.float }
                feed(x / channels, now)
            }
        }
    }

    internal fun feed(x: Float, now: Long) {
        low1 += lowA * (x - low1)
        low2 += lowA * (low1 - low2)
        sum += x * x
        bass += low2 * low2
        if (++frames < rate / 50) return
        val rms = sqrt(sum / frames)
        val e = bass / frames
        frames = 0
        sum = 0f
        bass = 0f
        // Loudness against the recent peak, so quiet masters move the sky as much as loud ones.
        peak = maxOf(rms, peak * 0.997f, 0.02f)
        val level = (rms / peak).coerceIn(0f, 1f)
        // A hit is bass energy well above its average over the last ~200 ms.
        val kick = if (bassAvg > 0f && sqrt(e) > peak * 0.15f) ((e / bassAvg - 1.4f) / 1.6f).coerceIn(0f, 1f) else 0f
        bassAvg = bassAvg * 0.9f + e * 0.1f
        val stamp = maxOf(now, next)
        next = stamp + CHUNK_NS
        synchronized(this) {
            val i = count++ % N
            stamps[i] = stamp
            levels[i] = level
            kicks[i] = kick
        }
    }

    /** Loudness and bass hit (both 0..1) of what's heard at [now]; zero when nothing is playing. */
    fun at(now: Long, out: FloatArray) = synchronized(this) {
        out[0] = 0f
        out[1] = 0f
        for (k in 1..minOf(count, N)) {
            val i = (count - k) % N
            if (stamps[i] > now) continue
            if (now - stamps[i] < 3 * CHUNK_NS) {
                out[0] = levels[i]
                out[1] = kicks[i]
            }
            break
        }
    }
}
