package pl.monter.app.data

import android.app.Activity
import android.util.Log
import com.google.android.gms.games.PlayGames
import com.google.android.gms.games.SnapshotsClient
import com.google.android.gms.games.snapshot.SnapshotMetadataChange
import kotlinx.coroutines.tasks.await

/**
 * Integracja z Google Play Games Services v2:
 *  - logowanie kontem Google Play Gry (automatyczne przy starcie, ręczne przyciskiem),
 *  - zapis w chmurze przez Saved Games (Snapshots),
 *  - osiągnięcia i tabela wyników.
 *
 * Wszystkie wywołania są „miękkie" – brak konfiguracji w Play Console czy brak sieci
 * nie może zablokować gry; wtedy działa ona na zapisie lokalnym.
 */
class PlayGamesService(private val activity: Activity) {

    data class Player(val id: String, val name: String)

    private val snapshotName = "monter-progress"

    suspend fun isAuthenticated(): Boolean = runCatching {
        PlayGames.getGamesSignInClient(activity).isAuthenticated().await().isAuthenticated()
    }.onFailure { Log.w(TAG, "isAuthenticated", it) }.getOrDefault(false)

    suspend fun signIn(): Boolean = runCatching {
        PlayGames.getGamesSignInClient(activity).signIn().await().isAuthenticated()
    }.onFailure { Log.w(TAG, "signIn", it) }.getOrDefault(false)

    suspend fun currentPlayer(): Player? = runCatching {
        val p = PlayGames.getPlayersClient(activity).currentPlayer.await()
        Player(p.playerId, p.displayName)
    }.onFailure { Log.w(TAG, "player", it) }.getOrNull()

    /** Odczyt zapisu z chmury. Konflikty rozwiązuje polityka „najnowszy zapis" po stronie Google. */
    suspend fun loadCloud(): String? = runCatching {
        val client = PlayGames.getSnapshotsClient(activity)
        val result = client.open(snapshotName, true, SnapshotsClient.RESOLUTION_POLICY_MOST_RECENTLY_MODIFIED).await()
        val snapshot = result.data ?: return null
        val bytes = snapshot.snapshotContents.readFully()
        client.discardAndClose(snapshot)
        if (bytes.isEmpty()) null else bytes.decodeToString()
    }.onFailure { Log.w(TAG, "loadCloud", it) }.getOrNull()

    suspend fun saveCloud(json: String, description: String, progress: Long): Boolean = runCatching {
        val client = PlayGames.getSnapshotsClient(activity)
        val result = client.open(snapshotName, true, SnapshotsClient.RESOLUTION_POLICY_MOST_RECENTLY_MODIFIED).await()
        val snapshot = result.data ?: return false
        snapshot.snapshotContents.writeBytes(json.encodeToByteArray())
        val meta = SnapshotMetadataChange.Builder()
            .setDescription(description)
            .setProgressValue(progress)
            .build()
        client.commitAndClose(snapshot, meta).await()
        true
    }.onFailure { Log.w(TAG, "saveCloud", it) }.getOrDefault(false)

    fun unlockAchievement(achievementId: String) {
        val pgsId = resString("pgs_ach_$achievementId") ?: return
        runCatching { PlayGames.getAchievementsClient(activity).unlock(pgsId) }
    }

    fun submitScore(score: Long) {
        val id = resString("pgs_leaderboard_sparks") ?: return
        runCatching { PlayGames.getLeaderboardsClient(activity).submitScore(id, score) }
    }

    suspend fun showAchievements(): Boolean = runCatching {
        val intent = PlayGames.getAchievementsClient(activity).achievementsIntent.await()
        activity.startActivityForResult(intent, 9001)
        true
    }.getOrDefault(false)

    private fun resString(name: String): String? {
        @Suppress("DiscouragedApi")
        val id = activity.resources.getIdentifier(name, "string", activity.packageName)
        if (id == 0) return null
        return activity.getString(id).takeIf { it.isNotBlank() }
    }

    companion object {
        private const val TAG = "PlayGames"
    }
}
