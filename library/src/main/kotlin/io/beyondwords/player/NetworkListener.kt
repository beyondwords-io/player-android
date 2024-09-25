package io.beyondwords.player

import android.net.ConnectivityManager.NetworkCallback
import android.net.Network
import androidx.annotation.RequiresApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@RequiresApi(24)
class NetworkListener(private val playerView: PlayerView) : NetworkCallback() {
    override fun onAvailable(network: Network) {
        super.onAvailable(network)
        CoroutineScope(Dispatchers.Main).launch {
            playerView.setPlaybackState("playing")
        }
    }

    override fun onLost(network: Network) {
        super.onLost(network)
        CoroutineScope(Dispatchers.Main).launch {
            playerView.setPlaybackState("paused")
        }
    }
}