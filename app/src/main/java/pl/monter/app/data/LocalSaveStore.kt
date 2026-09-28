package pl.monter.app.data

import android.content.Context
import android.util.Log
import pl.monter.core.game.SaveData
import java.io.File

/** Zapis lokalny – plik JSON w katalogu prywatnym aplikacji (objęty też kopią zapasową Androida). */
class LocalSaveStore(context: Context) {
    private val file = File(context.filesDir, "save.json")

    fun load(): SaveData = runCatching {
        if (file.exists()) SaveData.fromJson(file.readText()) else SaveData()
    }.onFailure { Log.w("LocalSave", "load", it) }.getOrDefault(SaveData())

    fun save(data: SaveData) {
        runCatching {
            val tmp = File(file.parentFile, "save.json.tmp")
            tmp.writeText(data.toJson())
            // Zapis atomowy – nie stracimy postępów, jeśli telefon wyłączy się w trakcie.
            if (!tmp.renameTo(file)) {
                file.writeText(data.toJson())
                tmp.delete()
            }
        }.onFailure { Log.w("LocalSave", "save", it) }
    }
}
