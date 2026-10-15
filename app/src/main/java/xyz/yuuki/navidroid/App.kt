package xyz.yuuki.navidroid

import android.app.Application
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
import kotlinx.coroutines.launch

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
        if (prefs.loggedIn) scope.launch { api.pickFastest() }

        // Re-race the endpoints whenever the network changes (home Wi-Fi <-> mobile data).
        getSystemService(ConnectivityManager::class.java).registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                api.networkChanged(true)
                if (prefs.loggedIn) scope.launch { api.pickFastest() }
            }

            override fun onLost(network: Network) = api.networkChanged(false)
        })
    }

    val metered: Boolean
        get() = getSystemService(ConnectivityManager::class.java).run {
            getNetworkCapabilities(activeNetwork)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) != true
        }

    val streamBitrate get() = if (metered) prefs.streamBitrateMobile else prefs.streamBitrateWifi

    override fun newImageLoader(context: PlatformContext) = ImageLoader.Builder(context)
        .crossfade(150)
        .diskCache { DiskCache.Builder().directory(cacheDir.resolve("images")).maxSizeBytes(512L * 1024 * 1024).build() }
        .components { add(OkHttpNetworkFetcherFactory(callFactory = { api.http })) }
        .build()
}
