package pl.monter.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import pl.monter.app.data.PlayGamesService
import pl.monter.app.ui.AppViewModel
import pl.monter.app.ui.MonterRoot
import pl.monter.app.ui.theme.MonterTheme

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        vm.attach(PlayGamesService(this))
        setContent {
            MonterTheme {
                MonterRoot(vm)
            }
        }
    }

    override fun onDestroy() {
        vm.detach()
        super.onDestroy()
    }
}
