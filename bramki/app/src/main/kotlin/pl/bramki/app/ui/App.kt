package pl.bramki.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import pl.bramki.app.data.Records
import pl.bramki.app.game.GameScreen
import pl.bramki.engine.Mode
import pl.bramki.engine.RunResult
import kotlin.random.Random

sealed class Screen {
    data object Home : Screen()
    data class Game(val mode: Mode, val seed: Long) : Screen()
    data class Result(val result: RunResult, val isRecord: Boolean) : Screen()
}

@Composable
fun BramkiApp(records: Records) {
    var screen by remember { mutableStateOf<Screen>(Screen.Home) }

    BackHandler(enabled = screen != Screen.Home) { screen = Screen.Home }

    when (val s = screen) {
        Screen.Home -> HomeScreen(
            records = records,
            onPlay = { mode -> screen = Screen.Game(mode, Random.nextLong()) },
        )
        is Screen.Game -> GameScreen(
            mode = s.mode,
            seed = s.seed,
            onFinished = { result ->
                val isRecord = records.record(result)
                screen = Screen.Result(result, isRecord)
            },
            onQuit = { screen = Screen.Home },
        )
        is Screen.Result -> ResultScreen(
            result = s.result,
            isRecord = s.isRecord,
            onRetry = { screen = Screen.Game(s.result.mode, Random.nextLong()) },
            onHome = { screen = Screen.Home },
        )
    }
}
