package pl.monter.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pl.monter.app.ui.AppViewModel
import pl.monter.app.ui.board.BoardTheme
import pl.monter.app.ui.components.GameButton
import pl.monter.app.ui.components.Header
import pl.monter.app.ui.components.Helmet
import pl.monter.app.ui.components.SparkBadge
import pl.monter.app.ui.components.helmetColor
import pl.monter.app.ui.theme.Palette
import pl.monter.core.game.PurchaseResult
import pl.monter.core.game.SaveData
import pl.monter.core.game.Shop
import pl.monter.core.game.ShopCategory
import pl.monter.core.game.ShopItem

@Composable
fun ShopScreen(save: SaveData, vm: AppViewModel, onBack: () -> Unit) {
    var message by remember { mutableStateOf<String?>("Wydawaj iskry zdobyte za ukończone zadania. Za gwiazdki dostajesz więcej!") }
    Column(Modifier.fillMaxSize()) {
        Header("Hurtownia elektryczna", onBack) {
            Text("💡 ${save.hints}", color = Palette.Text, modifier = Modifier.padding(end = 12.dp))
            SparkBadge(save.sparks)
        }
        message?.let { Text(it, color = Palette.TextDim, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 16.dp)) }
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            items(ShopCategory.entries) { cat ->
                Column {
                    Text(cat.title, color = Palette.Volt, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(Shop.items.filter { it.category == cat }) { item ->
                            ShopCard(item, save) {
                                message = when {
                                    !item.consumable && item.id in save.owned && cat == ShopCategory.THEME -> { vm.setTheme(item.id); "Ustawiono: ${item.name}" }
                                    !item.consumable && item.id in save.owned && cat == ShopCategory.HELMET -> { vm.setHelmet(item.id); "Założono: ${item.name}" }
                                    !item.consumable && item.id in save.owned -> "Już posiadasz: ${item.name}"
                                    else -> when (vm.buy(item.id)) {
                                        is PurchaseResult.Ok -> {
                                            if (cat == ShopCategory.THEME) vm.setTheme(item.id)
                                            if (cat == ShopCategory.HELMET) vm.setHelmet(item.id)
                                            "Kupiono: ${item.name} 🎉"
                                        }
                                        PurchaseResult.NotEnough -> "Brakuje ${item.price - save.sparks} iskier."
                                        PurchaseResult.AlreadyOwned -> "Już posiadasz: ${item.name}"
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ShopCard(item: ShopItem, save: SaveData, onClick: () -> Unit) {
    val owned = !item.consumable && item.id in save.owned
    val active = item.id == save.activeTheme || item.id == save.activeHelmet
    Column(
        Modifier.width(200.dp).clip(RoundedCornerShape(16.dp)).background(Palette.Surface)
            .border(2.dp, if (active) Palette.Volt else Color.Transparent, RoundedCornerShape(16.dp))
            .padding(12.dp),
    ) {
        Box(Modifier.fillMaxWidth().height(56.dp), contentAlignment = Alignment.Center) {
            when (item.category) {
                ShopCategory.HELMET -> Helmet(helmetColor(item.id), Modifier.size(56.dp))
                ShopCategory.THEME -> {
                    val th = BoardTheme.of(item.id)
                    Box(Modifier.fillMaxWidth().height(50.dp).clip(RoundedCornerShape(10.dp)).background(th.background))
                }
                ShopCategory.CONSUMABLE -> Text(if (item.amount > 1) "💡×${item.amount}" else "💡", fontSize = 30.sp)
                ShopCategory.TOOL -> Text(
                    when (item.id) { "tool_tester" -> "🪛"; "tool_meter" -> "📟"; else -> "📷" }, fontSize = 30.sp,
                )
                ShopCategory.LEVEL -> Text("🎁", fontSize = 30.sp)
            }
        }
        Text(item.name, color = Palette.Text, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text(item.description, color = Palette.TextDim, fontSize = 11.sp, minLines = 3, maxLines = 3)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            val label = when {
                active -> "W użyciu"
                owned && (item.category == ShopCategory.THEME || item.category == ShopCategory.HELMET) -> "Użyj"
                owned -> "Posiadane ✓"
                item.price == 0 -> "Za darmo"
                else -> "⚡ ${item.price}"
            }
            GameButton(label, onClick, small = true, enabled = !active && !(owned && item.category !in setOf(ShopCategory.THEME, ShopCategory.HELMET)), color = if (save.sparks >= item.price || owned) Palette.Volt else Palette.SurfaceHi)
        }
    }
}
