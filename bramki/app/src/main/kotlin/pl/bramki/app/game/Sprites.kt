package pl.bramki.app.game

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import pl.bramki.app.R
import pl.bramki.engine.Mode

/** Grafiki wygenerowane przez Gemini (tools/gen_art.py): postać na każdy szczebel i tło na każdy tryb. */
object Sprites {
    private val characters: Map<String, IntArray> = mapOf(
        "epoki" to intArrayOf(R.drawable.char_epoki_0, R.drawable.char_epoki_1, R.drawable.char_epoki_2, R.drawable.char_epoki_3, R.drawable.char_epoki_4, R.drawable.char_epoki_5, R.drawable.char_epoki_6, R.drawable.char_epoki_7),
        "ewolucja" to intArrayOf(R.drawable.char_ewolucja_0, R.drawable.char_ewolucja_1, R.drawable.char_ewolucja_2, R.drawable.char_ewolucja_3, R.drawable.char_ewolucja_4, R.drawable.char_ewolucja_5, R.drawable.char_ewolucja_6, R.drawable.char_ewolucja_7),
        "fortuna" to intArrayOf(R.drawable.char_fortuna_0, R.drawable.char_fortuna_1, R.drawable.char_fortuna_2, R.drawable.char_fortuna_3, R.drawable.char_fortuna_4, R.drawable.char_fortuna_5, R.drawable.char_fortuna_6, R.drawable.char_fortuna_7),
        "armia" to intArrayOf(R.drawable.char_armia_0, R.drawable.char_armia_1, R.drawable.char_armia_2, R.drawable.char_armia_3, R.drawable.char_armia_4, R.drawable.char_armia_5, R.drawable.char_armia_6, R.drawable.char_armia_7),
        "kosmos" to intArrayOf(R.drawable.char_kosmos_0, R.drawable.char_kosmos_1, R.drawable.char_kosmos_2, R.drawable.char_kosmos_3, R.drawable.char_kosmos_4, R.drawable.char_kosmos_5, R.drawable.char_kosmos_6, R.drawable.char_kosmos_7),
    )

    private val backgrounds: Map<String, Int> = mapOf(
        "epoki" to R.drawable.bg_epoki,
        "ewolucja" to R.drawable.bg_ewolucja,
        "fortuna" to R.drawable.bg_fortuna,
        "armia" to R.drawable.bg_armia,
        "kosmos" to R.drawable.bg_kosmos,
    )

    fun character(modeId: String, tier: Int): Int = characters.getValue(modeId)[tier.coerceIn(0, 7)]
    fun background(modeId: String): Int = backgrounds.getValue(modeId)
}

/** Zdekodowane bitmapy jednego trybu – ładowane raz na przebieg, rysowane na Canvas. */
class ModeArt(context: Context, mode: Mode) {
    val characters: List<ImageBitmap> = mode.tiers.indices.map { i ->
        BitmapFactory.decodeResource(context.resources, Sprites.character(mode.id, i)).asImageBitmap()
    }
    val background: ImageBitmap = BitmapFactory.decodeResource(context.resources, Sprites.background(mode.id)).asImageBitmap()
}
