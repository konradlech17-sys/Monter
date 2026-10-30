"""Syntezator efektów dźwiękowych gry (bez zewnętrznych nagrań i licencji).

Uruchom: python3 tools/sfx_gen.py app/src/main/res/raw
Tworzy krótkie pliki WAV 22,05 kHz / 16 bit / mono.
"""
import math
import random
import struct
import sys
import wave

RATE = 22050
random.seed(7)


def env(n, attack=0.004, decay=8.0):
    a = max(1, int(attack * RATE))
    return [min(1.0, i / a) * math.exp(-decay * i / RATE) for i in range(n)]


def tone(freq, dur, decay=6.0, harmonics=(1.0,), attack=0.004):
    n = int(dur * RATE)
    e = env(n, attack, decay)
    return [e[i] * sum(h * math.sin(2 * math.pi * freq * (k + 1) * i / RATE) for k, h in enumerate(harmonics)) for i in range(n)]


def noise(dur, decay=30.0):
    n = int(dur * RATE)
    e = env(n, 0.001, decay)
    return [e[i] * random.uniform(-1, 1) for i in range(n)]


def mix(*tracks, offsets=None):
    offsets = offsets or [0.0] * len(tracks)
    n = max(int(o * RATE) + len(t) for t, o in zip(tracks, offsets))
    out = [0.0] * n
    for t, o in zip(tracks, offsets):
        s = int(o * RATE)
        for i, v in enumerate(t):
            out[s + i] += v
    return out


def gain(t, g):
    return [v * g for v in t]


def save(path, t, peak=0.85):
    m = max(1e-9, max(abs(v) for v in t))
    fade = int(0.004 * RATE)
    data = bytearray()
    for i, v in enumerate(t):
        f = min(1.0, (len(t) - i) / fade)
        data += struct.pack("<h", int(v / m * peak * f * 32767))
    with wave.open(path, "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(RATE)
        w.writeframes(bytes(data))


def crackle(dur):
    """Iskrzenie: nieregularne trzaski + przydźwięk 100 Hz."""
    n = int(dur * RATE)
    out = [0.0] * n
    i = 0
    while i < n:
        burst = noise(random.uniform(0.004, 0.02), decay=random.uniform(80, 200))
        amp = random.uniform(0.4, 1.0) * math.exp(-2.5 * i / RATE)
        for k, v in enumerate(burst):
            if i + k < n:
                out[i + k] += v * amp
        i += int(random.uniform(0.006, 0.03) * RATE)
    hum = tone(100, dur, decay=4.0, harmonics=(0.5, 0.3, 0.2))
    return mix(out, gain(hum, 0.35))


def main(out):
    effects = {
        # wpięcie przewodu w zacisk: krótkie „klik” śruby
        "sfx_wire": mix(noise(0.012, 90), gain(tone(2200, 0.05, 60), 0.6)),
        # usunięcie przewodu: dwa cięcia nożycami
        "sfx_snip": mix(noise(0.02, 120), noise(0.02, 120), offsets=[0, 0.07]),
        # łącznik / przycisk w trybie TEST
        "sfx_switch": mix(noise(0.01, 150), gain(tone(900, 0.06, 50, (1, 0.4)), 0.7), gain(tone(160, 0.08, 35), 0.6)),
        # zwarcie
        "sfx_short": crackle(0.55),
        # zadziałanie wyłącznika: mechaniczne „klak”
        "sfx_trip": mix(gain(noise(0.015, 100), 0.8), gain(tone(110, 0.16, 18, (1, 0.5)), 0.9), gain(tone(1400, 0.03, 90), 0.4), offsets=[0, 0, 0.005]),
        # przegrzanie / uszkodzenie odbiornika
        "sfx_buzz": tone(100, 0.35, 3.0, (0.6, 0.0, 0.4, 0.0, 0.25, 0.0, 0.15)),
        # dzwonek: ding-dong
        "sfx_bell": mix(tone(659.3, 1.0, 3.2, (1, 0.25, 0.12)), tone(523.3, 1.2, 2.8, (1, 0.25, 0.12)), offsets=[0, 0.42]),
        # poziom zaliczony: arpeggio C-E-G-C
        "sfx_success": mix(*[tone(f, 0.45, 7, (1, 0.3, 0.1)) for f in (523.3, 659.3, 784.0, 1046.5)], offsets=[0, 0.09, 0.18, 0.27]),
        # sprawdzenie z błędami
        "sfx_error": mix(tone(392, 0.2, 10, (1, 0.5, 0.3)), tone(311, 0.3, 8, (1, 0.5, 0.3)), offsets=[0, 0.16]),
        # dobra odpowiedź w nauce do SEP
        "sfx_correct": mix(tone(784, 0.18, 14, (1, 0.2)), tone(1175, 0.3, 10, (1, 0.2)), offsets=[0, 0.08]),
        # zła odpowiedź
        "sfx_wrong": tone(196, 0.28, 9, (1, 0.6, 0.4, 0.2)),
    }
    for name, t in effects.items():
        save(f"{out}/{name}.wav", t)
        print(name, f"{len(t) / RATE:.2f}s")


if __name__ == "__main__":
    main(sys.argv[1] if len(sys.argv) > 1 else ".")
