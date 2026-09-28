package pl.monter.app

import android.app.Application
import com.google.android.gms.games.PlayGamesSdk

class MonterApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Play Games Services v2 wymaga inicjalizacji przed pierwszym użyciem klientów.
        runCatching { PlayGamesSdk.initialize(this) }
    }
}
