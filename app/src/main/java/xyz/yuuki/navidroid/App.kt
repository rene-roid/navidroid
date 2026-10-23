package xyz.yuuki.navidroid

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.disk.directory
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.SourceFetchResult
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.Options
import coil3.request.crossfade
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.withContext
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import okio.ByteString.Companion.toByteString
import java.io.ByteArrayOutputStream
import java.io.IOException

class App : Application(), SingletonImageLoader.Factory {
    companion object {
        lateinit var it: App
            private set
    }

    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    lateinit var prefs: Prefs
    lateinit var api: Api
    val offline by lazy { Offline(this) }

    override fun onCreate() {
        super.onCreate()
        it = this
        prefs = Prefs(this)
        api = Api(prefs, scope)
        if (prefs.loggedIn) api.reconnect()

        // Re-race the endpoints whenever the network changes (home Wi-Fi <-> mobile data).
        getSystemService(ConnectivityManager::class.java).registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                api.networkChanged(true)
                if (prefs.loggedIn) api.reconnect()
            }

            override fun onLost(network: Network) = api.networkChanged(false)
        })
    }

    /**
     * Not on Wi-Fi. Looks past the VPN, which counts as metered when Tailscale happens to ride on
     * mobile data, even while the phone is on Wi-Fi and the music comes over it.
     */
    @Suppress("DEPRECATION")
    val metered: Boolean
        get() = getSystemService(ConnectivityManager::class.java).run {
            allNetworks.none { n ->
                getNetworkCapabilities(n)?.run {
                    hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) && hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) &&
                        !hasTransport(NetworkCapabilities.TRANSPORT_VPN)
                } == true
            }
        }

    val streamBitrate get() = if (metered) prefs.streamBitrateMobile else prefs.streamBitrateWifi

    /** For covers: the API's client, but each cover is re-encoded as WebP before it is cached, about a third smaller than the server's JPEG. */
    val imageHttp by lazy { api.http.newBuilder().addInterceptor { webp(it.proceed(it.request())) }.build() }

    override fun newImageLoader(context: PlatformContext) = ImageLoader.Builder(context)
        .crossfade(150)
        // The disk cache is read before the network, so covers seen once also show offline.
        .diskCache { DiskCache.Builder().directory(cacheDir.resolve("images")).maxSizeBytes(512L * 1024 * 1024).build() }
        .components {
            add(CoverFetcher.Factory())
            add(OkHttpNetworkFetcherFactory(callFactory = { imageHttp }))
        }
        .build()
}

@Suppress("DEPRECATION")
private fun webp(r: Response): Response {
    if (!r.isSuccessful) return r
    val bytes = r.body!!.bytes()
    val keep = r.newBuilder().body(bytes.toResponseBody(r.body!!.contentType())).build()
    val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return keep
    val out = ByteArrayOutputStream()
    // Before Android 11 plain WEBP is lossy below quality 100.
    bmp.compress(if (Build.VERSION.SDK_INT >= 30) Bitmap.CompressFormat.WEBP_LOSSY else Bitmap.CompressFormat.WEBP, 80, out)
    bmp.recycle()
    if (out.size() >= bytes.size) return keep
    return r.newBuilder().removeHeader("Content-Length").header("Content-Type", "image/webp")
        .body(out.toByteArray().toResponseBody("image/webp".toMediaType())).build()
}

/** A cover by id and pixel size, for [CoverFetcher]. */
data class CoverArt(val id: String, val size: Int)

/**
 * Covers in the disk cache by content: each id points at the SHA-256 of its picture, so an album's
 * songs, which mostly share one cover under ids of their own, keep a single copy between them.
 */
private class CoverFetcher(private val art: CoverArt, private val cache: DiskCache) : Fetcher {
    override suspend fun fetch(): FetchResult = withContext(Dispatchers.IO) {
        val key = "${art.id}@${art.size}"
        val fs = cache.fileSystem
        // A pointer keeps the hash in its metadata and leaves its data empty.
        val hash = cache.openSnapshot("ref:$key")?.use { fs.read(it.metadata) { readUtf8() } }
        // Before, each cover sat under its own key.
        (hash?.let(cache::openSnapshot) ?: cache.openSnapshot(key))?.let { return@withContext disk(it) }
        val bytes = App.it.imageHttp.newCall(Request.Builder().url(App.it.api.coverUrl(art.id, art.size)).build()).execute().use { r ->
            if (!r.isSuccessful) throw IOException("HTTP ${r.code} for cover ${art.id}")
            r.body!!.bytes()
        }
        val sha = "sha:" + bytes.toByteString().sha256().hex()
        cache.openEditor("ref:$key")?.run { fs.write(metadata) { writeUtf8(sha) }; fs.write(data) {}; commit() }
        val snap = cache.openSnapshot(sha) ?: cache.openEditor(sha)?.run { fs.write(data) { write(bytes) }; commitAndOpenSnapshot() }
        snap?.let(::disk) ?: SourceFetchResult(ImageSource(Buffer().write(bytes), fs), null, DataSource.NETWORK)
    }

    private fun disk(s: DiskCache.Snapshot) = SourceFetchResult(ImageSource(s.data, cache.fileSystem, closeable = s), null, DataSource.DISK)

    class Factory : Fetcher.Factory<CoverArt> {
        override fun create(data: CoverArt, options: Options, imageLoader: ImageLoader) = imageLoader.diskCache?.let { CoverFetcher(data, it) }
    }
}
