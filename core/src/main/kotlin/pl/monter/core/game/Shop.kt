package pl.monter.core.game

import pl.monter.core.level.Levels

enum class ShopCategory(val title: String) { CONSUMABLE("Podpowiedzi"), TOOL("Narzędzia"), LEVEL("Poziomy bonusowe"), THEME("Wygląd tablicy"), HELMET("Kaski") }

data class ShopItem(
    val id: String,
    val name: String,
    val description: String,
    val price: Int,
    val category: ShopCategory,
    /** Dla przedmiotów zużywalnych – ile sztuk dostajemy. */
    val amount: Int = 1,
) {
    val consumable get() = category == ShopCategory.CONSUMABLE
}

sealed interface PurchaseResult {
    data class Ok(val save: SaveData, val newAchievements: List<Achievement>) : PurchaseResult
    data object NotEnough : PurchaseResult
    data object AlreadyOwned : PurchaseResult
}

object Shop {
    val items = listOf(
        ShopItem("hint1", "Podpowiedź", "Wskazuje jeden błąd na planszy, zanim klikniesz „Sprawdź\".", 50, ShopCategory.CONSUMABLE),
        ShopItem("hint5", "Pakiet 5 podpowiedzi", "Taniej w pakiecie!", 200, ShopCategory.CONSUMABLE, amount = 5),
        ShopItem("tool_tester", "Wskaźnik napięcia", "W trybie TEST pokazuje, na których zaciskach jest faza.", 150, ShopCategory.TOOL),
        ShopItem("tool_meter", "Miernik uniwersalny", "W trybie TEST pokazuje potencjał każdego zacisku: L1/L2/L3/N/PE.", 450, ShopCategory.TOOL),
        ShopItem("tool_camera", "Kamera termowizyjna", "Podświetla przewody zbyt cienkie dla zabezpieczenia – jak prawdziwa termowizja wykrywa grzejące się połączenia.", 600, ShopCategory.TOOL),
        ShopItem("theme_classic", "Klasyczna płyta", "Jasna płyta montażowa.", 0, ShopCategory.THEME),
        ShopItem("theme_wood", "Drewniana tablica", "Jak w warsztacie szkolnym.", 300, ShopCategory.THEME),
        ShopItem("theme_blueprint", "Schemat techniczny", "Niebieski papier kreślarski.", 500, ShopCategory.THEME),
        ShopItem("theme_neon", "Neon nocny", "Ciemna tablica, świecące przewody.", 800, ShopCategory.THEME),
        ShopItem("helmet_yellow", "Żółty kask", "Standard na budowie.", 0, ShopCategory.HELMET),
        ShopItem("helmet_blue", "Niebieski kask", "Kask elektryka.", 150, ShopCategory.HELMET),
        ShopItem("helmet_red", "Czerwony kask", "Kierownik robót!", 250, ShopCategory.HELMET),
        ShopItem("helmet_gold", "Złoty kask", "Dla prawdziwych mistrzów.", 2000, ShopCategory.HELMET),
    ) + Levels.all.filter { it.unlockCost != null }.map {
        ShopItem("level_${it.id}", it.title, it.subtitle, it.unlockCost!!, ShopCategory.LEVEL)
    }

    fun item(id: String) = items.first { it.id == id }

    fun buy(save: SaveData, id: String, now: Long): PurchaseResult {
        val item = item(id)
        if (!item.consumable && id in save.owned) return PurchaseResult.AlreadyOwned
        if (save.sparks < item.price) return PurchaseResult.NotEnough
        var next = save.copy(
            sparks = save.sparks - item.price,
            hints = if (item.consumable) save.hints + item.amount else save.hints,
            owned = if (item.consumable) save.owned else save.owned + id,
            stats = save.stats.copy(purchases = save.stats.purchases + 1),
            updatedAt = now,
        )
        val unlocked = Achievements.newlyUnlocked(next, null)
        next = next.copy(achievements = next.achievements + unlocked.map { it.id })
        return PurchaseResult.Ok(next, unlocked)
    }
}
