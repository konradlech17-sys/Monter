package pl.bramki.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import pl.bramki.app.data.Records
import pl.bramki.app.ui.BramkiApp
import pl.bramki.app.ui.BramkiTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val records = Records(applicationContext)
        setContent {
            BramkiTheme {
                BramkiApp(records)
            }
        }
    }
}
