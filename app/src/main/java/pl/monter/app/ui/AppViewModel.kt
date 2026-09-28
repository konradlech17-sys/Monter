package pl.monter.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pl.monter.app.data.LocalSaveStore
import pl.monter.app.data.PlayGamesService
import pl.monter.core.game.Achievement
import pl.monter.core.game.Achievements
import pl.monter.core.game.Attempt
import pl.monter.core.game.Difficulty
import pl.monter.core.game.PurchaseResult
import pl.monter.core.game.Reward
import pl.monter.core.game.SaveData
import pl.monter.core.game.Scoring
import pl.monter.core.game.Shop
import pl.monter.core.level.Level
import pl.monter.core.level.Levels

data class CloudState(
    val signedIn: Boolean = false,
    val playerName: String? = null,
    val status: String = "Łączenie z Google Play Gry…",
    val busy: Boolean = false,
)

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val store = LocalSaveStore(app)

    private val _save = MutableStateFlow(store.load())
    val save: StateFlow<SaveData> = _save

    private val _cloud = MutableStateFlow(CloudState())
    val cloud: StateFlow<CloudState> = _cloud

    /** Krótkie komunikaty (np. nowe osiągnięcia) wyświetlane jako „toast" w grze. */
    private val _toasts = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val toasts: SharedFlow<String> = _toasts

    private var pgs: PlayGamesService? = null
    private var autoSignInTried = false
    private var pushJob: Job? = null

    fun attach(service: PlayGamesService) {
        pgs = service
        if (autoSignInTried) return
        autoSignInTried = true
        viewModelScope.launch {
            if (service.isAuthenticated()) onSignedIn(service)
            else _cloud.update { it.copy(status = "Gra offline – zaloguj się, aby zapisywać postępy w chmurze") }
        }
    }

    fun detach() {
        pgs = null
    }

    fun signIn() {
        val service = pgs ?: return
        viewModelScope.launch {
            _cloud.update { it.copy(busy = true, status = "Logowanie…") }
            if (service.signIn()) onSignedIn(service)
            else _cloud.update { it.copy(busy = false, status = "Nie udało się zalogować. Sprawdź konto Google Play Gry.") }
        }
    }

    private suspend fun onSignedIn(service: PlayGamesService) {
        val player = service.currentPlayer()
        _cloud.update { it.copy(signedIn = true, playerName = player?.name, busy = true, status = "Synchronizacja…") }
        val remote = service.loadCloud()?.let { runCatching { SaveData.fromJson(it) }.getOrNull() }
        val merged = if (remote != null) SaveData.merge(_save.value, remote) else _save.value
        _save.value = merged
        store.save(merged)
        val ok = service.saveCloud(merged.toJson(), describe(merged), merged.totalEarned.toLong())
        merged.achievements.forEach { service.unlockAchievement(it) }
        _cloud.update { it.copy(busy = false, status = if (ok) "Zapis w chmurze aktualny ☁️" else "Chmura niedostępna – zapis lokalny") }
    }

    private fun describe(s: SaveData): String {
        val done = Levels.all.count { s.completed(it) }
        return "Ukończone poziomy: $done/${Levels.all.size}, iskry: ${s.sparks}"
    }

    private fun update(next: SaveData) {
        _save.value = next
        store.save(next)
        schedulePush()
    }

    /** Zapis w chmurze z opóźnieniem – kilka zmian pod rząd = jeden zapis. */
    private fun schedulePush() {
        val service = pgs ?: return
        if (!_cloud.value.signedIn) return
        pushJob?.cancel()
        pushJob = viewModelScope.launch {
            delay(1500)
            val s = _save.value
            val ok = service.saveCloud(s.toJson(), describe(s), s.totalEarned.toLong())
            _cloud.update { it.copy(status = if (ok) "Zapis w chmurze aktualny ☁️" else "Chmura niedostępna – zapis lokalny") }
        }
    }

    private fun announce(list: List<Achievement>) {
        for (a in list) {
            _toasts.tryEmit("${a.icon} Osiągnięcie: ${a.title}")
            pgs?.unlockAchievement(a.id)
        }
    }

    fun completeLevel(level: Level, difficulty: Difficulty, attempt: Attempt): Reward {
        val (next, reward) = Scoring.complete(_save.value, level, difficulty, attempt, System.currentTimeMillis())
        update(next)
        announce(reward.newAchievements)
        pgs?.submitScore(next.totalEarned.toLong())
        return reward
    }

    fun buy(id: String): PurchaseResult {
        val r = Shop.buy(_save.value, id, System.currentTimeMillis())
        if (r is PurchaseResult.Ok) {
            update(r.save)
            announce(r.newAchievements)
        }
        return r
    }

    fun setTheme(id: String) = update(_save.value.copy(activeTheme = id, updatedAt = System.currentTimeMillis()))

    fun setHelmet(id: String) = update(_save.value.copy(activeHelmet = id, updatedAt = System.currentTimeMillis()))

    fun recordShort() {
        val s = _save.value
        val next = s.copy(stats = s.stats.copy(shortsCaused = s.stats.shortsCaused + 1))
        val unlocked = Achievements.newlyUnlocked(next, null)
        update(next.copy(achievements = next.achievements + unlocked.map { it.id }))
        announce(unlocked)
    }

    fun recordCheck(wires: Int) {
        val s = _save.value
        update(s.copy(stats = s.stats.copy(checks = s.stats.checks + 1, wiresLaid = s.stats.wiresLaid + wires)))
    }

    /** Zużywa kupioną podpowiedź. */
    fun consumeHint(): Boolean {
        val s = _save.value
        if (s.hints <= 0) return false
        update(s.copy(hints = s.hints - 1, updatedAt = System.currentTimeMillis()))
        return true
    }

    fun showPlayAchievements() {
        val service = pgs ?: return
        viewModelScope.launch { service.showAchievements() }
    }
}
