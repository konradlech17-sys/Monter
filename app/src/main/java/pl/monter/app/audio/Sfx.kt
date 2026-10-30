package pl.monter.app.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import pl.monter.app.R

/** Efekty dźwiękowe gry. Pliki w res/raw generuje tools/sfx_gen.py. */
enum class Sound(val res: Int) {
    WIRE(R.raw.sfx_wire),
    SNIP(R.raw.sfx_snip),
    SWITCH(R.raw.sfx_switch),
    SHORT(R.raw.sfx_short),
    TRIP(R.raw.sfx_trip),
    BUZZ(R.raw.sfx_buzz),
    BELL(R.raw.sfx_bell),
    SUCCESS(R.raw.sfx_success),
    ERROR(R.raw.sfx_error),
    CORRECT(R.raw.sfx_correct),
    WRONG(R.raw.sfx_wrong),
}

/**
 * SoundPool trzyma krótkie dźwięki zdekodowane w pamięci, więc odtwarza je bez opóźnień –
 * w przeciwieństwie do MediaPlayera, który nadaje się do muzyki, a nie do „kliknięć".
 */
class Sfx(context: Context) {
    private val pool = SoundPool.Builder()
        .setMaxStreams(6)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()
    private val ids = Sound.entries.associateWith { pool.load(context, it.res, 1) }

    fun play(sound: Sound, volume: Float = 0.8f) {
        ids[sound]?.let { pool.play(it, volume, volume, 1, 0, 1f) }
    }

    fun release() = pool.release()
}
