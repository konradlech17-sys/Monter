package pl.monter.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import pl.monter.app.ui.screens.AchievementsScreen
import pl.monter.app.ui.screens.GameScreen
import pl.monter.app.ui.screens.HomeScreen
import pl.monter.app.ui.screens.LevelMapScreen
import pl.monter.app.ui.screens.ShopScreen
import pl.monter.app.ui.theme.Palette
import pl.monter.core.game.Difficulty
import pl.monter.core.level.Levels

sealed interface Screen {
    data object Home : Screen
    data object Map : Screen
    data object Shop : Screen
    data object Achievements : Screen
    data class Game(val levelId: String, val difficulty: Difficulty, val nonce: Long = System.nanoTime()) : Screen
}

@Composable
fun MonterRoot(vm: AppViewModel) {
    val save by vm.save.collectAsStateWithLifecycle()
    val cloud by vm.cloud.collectAsStateWithLifecycle()
    var stack by remember { mutableStateOf(listOf<Screen>(Screen.Home)) }
    var difficulty by remember { mutableStateOf(Difficulty.EASY) }
    var toast by remember { mutableStateOf<String?>(null) }

    fun push(s: Screen) { stack = stack + s }
    fun pop() { if (stack.size > 1) stack = stack.dropLast(1) }
    fun replaceTop(s: Screen) { stack = stack.dropLast(1) + s }

    BackHandler(enabled = stack.size > 1) { pop() }

    LaunchedEffect(Unit) {
        vm.toasts.collect { msg ->
            toast = msg
            delay(2600)
            toast = null
        }
    }

    Box(Modifier.fillMaxSize().background(Palette.Bg).safeDrawingPadding()) {
        AnimatedContent(
            targetState = stack.last(),
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "nav",
        ) { screen ->
            when (screen) {
                Screen.Home -> HomeScreen(
                    save = save, cloud = cloud,
                    onPlay = { push(Screen.Map) },
                    onShop = { push(Screen.Shop) },
                    onAchievements = { push(Screen.Achievements) },
                    onSignIn = vm::signIn,
                )
                Screen.Map -> LevelMapScreen(
                    save = save,
                    difficulty = difficulty,
                    onDifficulty = { difficulty = it },
                    onBack = ::pop,
                    onLevel = { push(Screen.Game(it.id, difficulty)) },
                    onBuyLevel = { vm.buy("level_${it.id}") },
                )
                Screen.Shop -> ShopScreen(save = save, vm = vm, onBack = ::pop)
                Screen.Achievements -> AchievementsScreen(save = save, signedIn = cloud.signedIn, onPlayGames = vm::showPlayAchievements, onBack = ::pop)
                is Screen.Game -> {
                    val level = Levels.byId(screen.levelId)!!
                    GameScreen(
                        level = level,
                        difficulty = screen.difficulty,
                        save = save,
                        vm = vm,
                        onExit = ::pop,
                        onNext = { next -> if (next != null) replaceTop(Screen.Game(next.id, screen.difficulty)) else pop() },
                        onRetry = { replaceTop(Screen.Game(level.id, screen.difficulty)) },
                    )
                }
            }
        }
        toast?.let {
            Box(
                Modifier.align(Alignment.TopCenter).padding(top = 12.dp).clip(RoundedCornerShape(50))
                    .background(Palette.SurfaceHi).padding(horizontal = 18.dp, vertical = 10.dp),
            ) { Text(it, color = Palette.Volt, fontWeight = FontWeight.Bold) }
        }
    }
}
