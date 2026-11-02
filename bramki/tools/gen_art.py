#!/usr/bin/env python3
"""
Generuje grafiki postaci i teł przez Gemini (API generativelanguage.googleapis.com),
kluczuje zielone tło na przezroczystość i zapisuje WebP do app/src/main/res/drawable-nodpi/.

Użycie:  GEMINI_API_KEY=... python3 tools/gen_art.py [--only epoki_3,bg_kosmos] [--skip-existing]
Surowe obrazy lądują w art/raw/ (żeby dało się poprawić kluczowanie bez ponownego generowania).
"""
import base64, json, os, sys, urllib.request, concurrent.futures, io
from PIL import Image
import numpy as np

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RAW = os.path.join(ROOT, "art", "raw")
OUT = os.path.join(ROOT, "app", "src", "main", "res", "drawable-nodpi")
MODEL = os.environ.get("GEMINI_IMAGE_MODEL", "gemini-3.1-flash-image")
KEY = os.environ.get("GEMINI_API_KEY", "")

STYLE = ("Style: bold simple shapes, thick dark outlines with a white sticker border, flat vivid colors, "
         "cute mobile-game sticker look, friendly expression. The subject is centered, full body, facing the viewer, "
         "and fills most of the frame. Background: solid flat pure green color #00FF00, absolutely nothing else, "
         "no shadow, no ground, no text, no logo.")

CHARACTERS = {
    "epoki": [
        "a caveman wearing a fur tunic, holding a wooden club",
        "a bronze age warrior with a bronze helmet, round bronze shield and spear",
        "an ancient Greek philosopher in a white toga and laurel wreath, holding a scroll",
        "a medieval knight in shining plate armor with a sword and a shield with a cross",
        "a Renaissance painter with a beret, holding a palette and brush, puffy sleeves",
        "an industrial era engineer in a top hat and goggles, holding a big wrench, soot on cheeks",
        "a modern young person in a hoodie and sneakers looking at a smartphone, wireless earbuds",
        "a futuristic astronaut in a sleek white and blue spacesuit with a glowing visor and small jetpack",
    ],
    "ewolucja": [
        "a cute single cell microbe, round blob with cilia, a nucleus visible inside, big eyes",
        "a cute cartoon fish with big fins and scales",
        "a cute cartoon frog standing on two legs",
        "a cute cartoon lizard standing upright with a long tail",
        "a cute cartoon mouse standing upright with big round ears",
        "a cute cartoon monkey standing upright holding a banana",
        "a human hiker in casual outdoor clothes with a backpack, waving",
        "a friendly cyborg human with one glowing robotic eye and a metallic arm, futuristic suit",
    ],
    "fortuna": [
        "a poor person in patched clothes with empty pockets turned inside out, shrugging",
        "a person hugging a big pink piggy bank",
        "a person holding an open wallet with a few banknotes, smiling",
        "a person in a smart shirt holding a golden credit card and a phone",
        "a person standing next to a small steel safe full of gold coins",
        "a wealthy person in a suit holding keys, with a tiny mansion model under the arm",
        "a yacht captain in a white captain's hat and navy blazer holding a ship's wheel",
        "a billionaire in a golden spacesuit standing next to a small rocket",
    ],
    "armia": [
        "a lone warrior with a wooden shield and a short sword, leather armor",
        "a small group of three warriors standing together with shields",
        "a tight squad of hoplites with overlapping round shields and spears, phalanx formation",
        "an officer holding a tall banner flag, with soldiers behind him",
        "a medieval longbow archer drawing a bow, with a quiver of arrows",
        "a Roman centurion with a red crest helmet holding a golden eagle standard",
        "a Mongol horse archer riding a galloping horse, fur hat, bow",
        "an emperor with a golden crown and a red cape holding a scepter, imperial armor",
    ],
    "kosmos": [
        "a tiny sparkling speck of cosmic dust with a cute face, glittering",
        "a lumpy rocky asteroid with craters and a cute face",
        "a grey cratered moon with a cute sleepy face",
        "an Earth-like blue and green planet with clouds and a cute happy face",
        "a striped gas giant planet with wide rings and a cute face",
        "a bright yellow sun star with flames and a cute cheerful face",
        "a huge red supergiant star with swirling surface and a cute proud face",
        "a black hole with a glowing orange accretion disk and a cute mysterious face",
    ],
}

BG_STYLE = ("Wide stylized cartoon landscape background for a mobile game, flat vivid colors, soft shapes, "
            "horizon line in the lower third, lots of sky, no characters, no people, no text, no logo.")
BACKGROUNDS = {
    "epoki": "a timeline landscape: prehistoric cliffs on the left fading into a futuristic city skyline on the right, big sun and clouds",
    "ewolucja": "a lush prehistoric jungle shore with a calm turquoise ocean and volcanoes in the distance",
    "fortuna": "a golden sunset city skyline with skyscrapers and sparkling lights, warm tones",
    "armia": "a vast battlefield plain with distant castle walls, war banners and a dramatic cloudy sky",
    "kosmos": "deep space with a colorful purple and blue nebula, stars and a distant ringed planet",
}


def generate(prompt: str, aspect: str) -> bytes:
    body = {
        "contents": [{"parts": [{"text": prompt}]}],
        "generationConfig": {"responseModalities": ["IMAGE"], "imageConfig": {"aspectRatio": aspect}},
    }
    url = f"https://generativelanguage.googleapis.com/v1beta/models/{MODEL}:generateContent"
    req = urllib.request.Request(url, data=json.dumps(body).encode(), headers={"Content-Type": "application/json"})
    if KEY:
        req.add_header("x-goog-api-key", KEY)
    for attempt in range(4):
        try:
            with urllib.request.urlopen(req, timeout=180) as r:
                d = json.load(r)
            for p in d["candidates"][0]["content"]["parts"]:
                if "inlineData" in p:
                    return base64.b64decode(p["inlineData"]["data"])
            raise RuntimeError("brak obrazu w odpowiedzi: " + json.dumps(d)[:300])
        except Exception as e:  # noqa: BLE001
            if attempt == 3:
                raise
            print("  ponawiam:", e, file=sys.stderr)


def chroma_key(img: Image.Image) -> Image.Image:
    """Zielone tło (#00FF00, po JPEG lekko rozmyte) → alfa 0, z miękką krawędzią.

    Kluczowane są tylko zielone piksele POŁĄCZONE z brzegiem obrazu (zalewanie od krawędzi).
    Dzięki temu zielona żaba czy jaszczurka zostają nietknięte – od tła oddziela je biała obwódka naklejki.
    """
    a = np.asarray(img.convert("RGB")).astype(np.int32)  # int32: w int16 mnożenie przez 255 się przepełnia
    r, g, b = a[..., 0], a[..., 1], a[..., 2]
    greenness = g - np.maximum(r, b)          # jak bardzo G dominuje nad R i B
    greenish = greenness > 45
    # Tło = zielone obszary dotykające brzegu ORAZ zamknięte „dziury" (między nogami, pod koniem…),
    # w których jest czysta zieleń tła (> 180). Zielona żaba ma zieloność ≤ 150, więc zostaje.
    seeds = np.zeros_like(greenish)
    seeds[0, :] = greenish[0, :]
    seeds[-1, :] = greenish[-1, :]
    seeds[:, 0] = greenish[:, 0]
    seeds[:, -1] = greenish[:, -1]
    seeds |= greenness > 180
    background = flood(greenish, seeds)
    # Rozszerzamy obszar tła o 2 px, żeby objąć wygładzoną krawędź obwódki.
    background = dilate(background, 2)
    soft = np.clip((110 - greenness) * 255 / 70, 0, 255)  # >110 → 0, <40 → 255
    alpha = np.where(background, soft, 255).astype(np.uint8)
    rgb = a.copy()
    edge = background & (alpha > 0) & (alpha < 255)
    rgb[edge, 1] = np.minimum(rgb[edge, 1], np.maximum(rgb[edge, 0], rgb[edge, 2]) + 10)  # bez zielonej poświaty
    return Image.fromarray(np.dstack([rgb.astype(np.uint8), alpha]), "RGBA")


def flood(mask: np.ndarray, seeds: np.ndarray) -> np.ndarray:
    """Składowe spójne maski zawierające jakiekolwiek ziarno (BFS po 4 sąsiadach, czysty NumPy)."""
    visited = np.zeros_like(mask, dtype=bool)
    frontier = seeds & mask
    while frontier.any():
        visited |= frontier
        grown = np.zeros_like(mask, dtype=bool)
        grown[1:, :] |= frontier[:-1, :]
        grown[:-1, :] |= frontier[1:, :]
        grown[:, 1:] |= frontier[:, :-1]
        grown[:, :-1] |= frontier[:, 1:]
        frontier = grown & mask & ~visited
    return visited


def dilate(mask: np.ndarray, px: int) -> np.ndarray:
    out = mask.copy()
    for _ in range(px):
        grown = out.copy()
        grown[1:, :] |= out[:-1, :]
        grown[:-1, :] |= out[1:, :]
        grown[:, 1:] |= out[:, :-1]
        grown[:, :-1] |= out[:, 1:]
        out = grown
    return out


def trim_and_fit(img: Image.Image, size: int) -> Image.Image:
    bbox = img.getchannel("A").point(lambda v: 255 if v > 8 else 0).getbbox()
    if bbox:
        img = img.crop(bbox)
    img.thumbnail((size, size), Image.LANCZOS)
    canvas = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    canvas.paste(img, ((size - img.width) // 2, size - img.height))  # stopy przy dolnej krawędzi
    return canvas


def do_character(mode: str, idx: int, desc: str, skip_existing: bool):
    name = f"char_{mode}_{idx}"
    raw = os.path.join(RAW, name + ".jpg")
    out = os.path.join(OUT, name + ".webp")
    if skip_existing and os.path.exists(out):
        return name + " (pominięto)"
    if not os.path.exists(raw):
        data = generate(f"A cartoon game character sprite: {desc}. {STYLE}", "3:4")
        Image.open(io.BytesIO(data)).convert("RGB").save(raw, quality=90)
    img = trim_and_fit(chroma_key(Image.open(raw)), 512)
    img.save(out, "WEBP", quality=88, method=6)
    return f"{name}: {os.path.getsize(out) // 1024} KB"


def do_background(mode: str, desc: str, skip_existing: bool):
    name = f"bg_{mode}"
    raw = os.path.join(RAW, name + ".jpg")
    out = os.path.join(OUT, name + ".webp")
    if skip_existing and os.path.exists(out):
        return name + " (pominięto)"
    if not os.path.exists(raw):
        data = generate(f"{BG_STYLE} Scene: {desc}.", "16:9")
        Image.open(io.BytesIO(data)).convert("RGB").save(raw, quality=90)
    img = Image.open(raw).convert("RGB")
    img.thumbnail((1280, 720), Image.LANCZOS)
    img.save(out, "WEBP", quality=80, method=6)
    return f"{name}: {os.path.getsize(out) // 1024} KB"


def main():
    only = None
    skip = "--skip-existing" in sys.argv
    for a in sys.argv[1:]:
        if a.startswith("--only"):
            only = set(a.split("=", 1)[1].split(","))
    os.makedirs(RAW, exist_ok=True)
    os.makedirs(OUT, exist_ok=True)
    jobs = []
    for mode, descs in CHARACTERS.items():
        for i, d in enumerate(descs):
            if only is None or f"{mode}_{i}" in only:
                jobs.append((do_character, (mode, i, d, skip)))
    for mode, d in BACKGROUNDS.items():
        if only is None or f"bg_{mode}" in only:
            jobs.append((do_background, (mode, d, skip)))
    with concurrent.futures.ThreadPoolExecutor(max_workers=4) as ex:
        futs = [ex.submit(f, *args) for f, args in jobs]
        for f in concurrent.futures.as_completed(futs):
            try:
                print(f.result(), flush=True)
            except Exception as e:  # noqa: BLE001
                print("BŁĄD:", e, flush=True)


if __name__ == "__main__":
    main()
