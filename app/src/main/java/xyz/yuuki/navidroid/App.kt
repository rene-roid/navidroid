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
import coil3.disk.directory
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import java.io.ByteArrayOutputStream

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
        .components { add(OkHttpNetworkFetcherFactory(callFactory = { imageHttp })) }
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
