package pl.monter.core.exam

import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.random.Random

/** Zadania obliczeniowe generowane losowo – za każdym razem inne liczby. */
object Generators {

    private const val U0 = 230.0

    private fun fmt(v: Double, digits: Int = 2): String {
        val p = Math.pow(10.0, digits.toDouble())
        val r = (v * p).roundToInt() / p
        return r.toString().replace('.', ',').let { if (it.endsWith(",0")) it.dropLast(2) else it }
    }

    /** Krotność prądu In zapewniająca natychmiastowe zadziałanie (górna granica wyzwalacza elektromagnetycznego). */
    private fun multiplier(curve: Char) = when (curve) { 'B' -> 5; 'C' -> 10; else -> 20 }

    private fun options(correct: String, wrong: List<String>): Pair<List<String>, Int> {
        val all = (listOf(correct) + wrong.filter { it != correct }.distinct()).take(4)
        return all to 0
    }

    /** Największa dopuszczalna impedancja pętli zwarcia dla wyłącznika nadprądowego. */
    fun zsMax(r: Random): Question {
        val curve = listOf('B', 'B', 'C', 'C', 'D').random(r)
        val inA = listOf(6, 10, 13, 16, 20, 25, 32).random(r)
        val ia = multiplier(curve) * inA
        val zs = U0 / ia
        val (answers, c) = options(
            "${fmt(zs)} Ω",
            listOf("${fmt(U0 / inA)} Ω", "${fmt(U0 / (ia / 2.0))} Ω", "${fmt(zs * 2 / 3)} Ω", "${fmt(400.0 / ia)} Ω"),
        )
        return Question(
            "gen.zsmax", SepCategory.MEASURE,
            "Jaka jest największa dopuszczalna impedancja pętli zwarcia Zs dla obwodu zabezpieczonego wyłącznikiem $curve$inA (układ TN, U0 = 230 V)?",
            answers, c,
            "Ia = ${multiplier(curve)} · In = ${multiplier(curve)} · $inA A = $ia A (dla charakterystyki $curve bierzemy górną granicę wyzwalacza elektromagnetycznego). " +
                "Zs ≤ U0 / Ia = 230 V / $ia A = ${fmt(zs)} Ω. W praktyce pomiarowej wynik zmierzony porównuje się często z 2/3 tej wartości " +
                "(${fmt(zs * 2 / 3)} Ω), bo podczas zwarcia przewody się nagrzewają i ich rezystancja rośnie.",
            "PN-HD 60364-4-41", generated = true,
        )
    }

    /** Ocena zmierzonej impedancji pętli zwarcia. */
    fun zsCheck(r: Random): Question {
        val curve = listOf('B', 'C').random(r)
        val inA = listOf(10, 16, 20, 25).random(r)
        val ia = multiplier(curve) * inA
        val limit = U0 / ia
        val ok = r.nextBoolean()
        val measured = if (ok) limit * (0.25 + r.nextDouble() * 0.6) else limit * (1.1 + r.nextDouble() * 0.8)
        val yes = "Tak – ${fmt(measured)} Ω ≤ ${fmt(limit)} Ω"
        val no = "Nie – ${fmt(measured)} Ω > ${fmt(limit)} Ω"
        val answers = listOf(if (ok) yes else no, if (ok) no else yes, "Nie da się ocenić bez pomiaru rezystancji izolacji", "Tak – impedancja pętli nie zależy od zabezpieczenia")
        return Question(
            "gen.zscheck", SepCategory.MEASURE,
            "Obwód gniazd zabezpieczono wyłącznikiem $curve$inA. Zmierzona impedancja pętli zwarcia Zs = ${fmt(measured)} Ω. Czy warunek samoczynnego wyłączenia jest spełniony (U0 = 230 V)?",
            answers, 0,
            "Ia = ${multiplier(curve)} · $inA A = $ia A, Zs dop = 230 / $ia = ${fmt(limit)} Ω. " +
                if (ok) "Zmierzona wartość jest mniejsza – prąd zwarcia będzie wystarczający do szybkiego wyłączenia."
                else "Zmierzona wartość jest za duża – prąd zwarcia będzie za mały, wyłącznik może nie zadziałać w 0,4 s. Rozwiązania: większy przekrój, krótszy obwód, mniejsze In lub RCD.",
            "PN-HD 60364-4-41", generated = true,
        )
    }

    fun iaValue(r: Random): Question {
        val curve = listOf('B', 'C', 'D').random(r)
        val inA = listOf(6, 10, 16, 20, 25, 32).random(r)
        val ia = multiplier(curve) * inA
        val (answers, c) = options("$ia A", listOf("${inA * 3} A", "${inA * 2} A", "${(inA * 1.45).roundToInt()} A", "${ia * 2} A"))
        return Question(
            "gen.ia", SepCategory.INSTALL,
            "Jaki prąd Ia przyjmuje się do sprawdzenia samoczynnego wyłączenia dla wyłącznika $curve$inA?",
            answers, c,
            "Charakterystyka B: 3–5 · In, C: 5–10 · In, D: 10–20 · In. Do obliczeń bierze się górną granicę, żeby mieć pewność zadziałania: ${multiplier(curve)} · $inA A = $ia A.",
            "PN-EN 60898-1", generated = true,
        )
    }

    fun insulation(r: Random): Question {
        val selv = r.nextInt(4) == 0
        val min = if (selv) 0.5 else 1.0
        val v = if (selv) 250 else 500
        val ok = r.nextBoolean()
        val measured = if (ok) min * (1.5 + r.nextDouble() * 300) else min * (0.1 + r.nextDouble() * 0.8)
        val circuit = if (selv) "obwodu SELV 12 V" else listOf("obwodu gniazd 230 V", "obwodu oświetlenia 230 V", "obwodu siłowego 400 V").random(r)
        val yes = "Tak – wymagane ≥ ${fmt(min, 1)} MΩ"
        val no = "Nie – wymagane ≥ ${fmt(min, 1)} MΩ"
        return Question(
            "gen.riso", SepCategory.MEASURE,
            "Rezystancja izolacji $circuit zmierzona napięciem $v V DC wynosi ${fmt(measured, if (measured < 10) 2 else 0)} MΩ. Czy wynik jest pozytywny?",
            listOf(if (ok) yes else no, if (ok) no else yes, "Tak – każda wartość powyżej 0 jest dobra", "Nie – wymagane jest co najmniej 100 MΩ"), 0,
            if (selv) "SELV/PELV: napięcie probiercze 250 V DC, minimum 0,5 MΩ." else "Obwody do 500 V: napięcie probiercze 500 V DC, minimum 1 MΩ. Nowa instalacja ma zwykle setki MΩ – niski wynik to sygnał wilgoci lub uszkodzenia.",
            "PN-HD 60364-6", generated = true,
        )
    }

    fun rcdTime(r: Random): Question {
        val five = r.nextBoolean()
        val limit = if (five) 40 else 300
        val ok = r.nextBoolean()
        val t = if (ok) (limit * (0.2 + r.nextDouble() * 0.7)).roundToInt() else (limit * (1.1 + r.nextDouble() * 0.8)).roundToInt()
        val test = if (five) "5 · IΔn (150 mA)" else "IΔn (30 mA)"
        val yes = "Tak – dopuszczalne ≤ $limit ms"
        val no = "Nie – dopuszczalne ≤ $limit ms"
        return Question(
            "gen.rcd", SepCategory.MEASURE,
            "Wyłącznik RCD 30 mA ogólnego typu przy prądzie probierczym $test zadziałał po $t ms. Czy wynik jest prawidłowy?",
            listOf(if (ok) yes else no, if (ok) no else yes, "Tak – dopuszczalne jest do 5 s", "Nie – RCD musi zadziałać w czasie poniżej 1 ms"), 0,
            "RCD bezzwłoczny: ≤ 300 ms przy IΔn, ≤ 150 ms przy 2 · IΔn, ≤ 40 ms przy 5 · IΔn.",
            "PN-EN 61008-1", generated = true,
        )
    }

    fun current1p(r: Random): Question {
        val p = listOf(1.0, 1.5, 2.0, 2.3, 3.0, 3.5).random(r)
        val i = p * 1000 / U0
        val (answers, c) = options("ok. ${fmt(i, 1)} A", listOf("ok. ${fmt(i * sqrt(3.0), 1)} A", "ok. ${fmt(p * 1000 / 400, 1)} A", "ok. ${fmt(i * 2, 1)} A", "ok. ${fmt(i / 2, 1)} A"))
        return Question(
            "gen.i1p", SepCategory.INSTALL,
            "Jaki prąd pobiera grzejnik o mocy ${fmt(p, 1)} kW zasilany napięciem 230 V (cos φ = 1)?",
            answers, c,
            "I = P / U = ${fmt(p * 1000, 0)} W / 230 V ≈ ${fmt(i, 1)} A. Przydatna reguła: każdy amper przy 230 V to ok. 230 W.",
            generated = true,
        )
    }

    fun current3p(r: Random): Question {
        val p = listOf(4.0, 5.5, 7.5, 11.0, 15.0).random(r)
        val cos = listOf(0.8, 0.85, 0.9).random(r)
        val i = p * 1000 / (sqrt(3.0) * 400 * cos)
        val (answers, c) = options("ok. ${fmt(i, 1)} A", listOf("ok. ${fmt(p * 1000 / (400 * cos), 1)} A", "ok. ${fmt(p * 1000 / (3 * 400 * cos), 1)} A", "ok. ${fmt(p * 1000 / U0, 1)} A"))
        return Question(
            "gen.i3p", SepCategory.INSTALL,
            "Odbiornik trójfazowy pobiera z sieci moc czynną ${fmt(p, 1)} kW przy napięciu 400 V i cos φ = ${fmt(cos, 2)}. Jaki prąd płynie w każdej fazie?",
            answers, c,
            "I = P / (√3 · U · cos φ) = ${fmt(p * 1000, 0)} / (1,732 · 400 · ${fmt(cos, 2)}) ≈ ${fmt(i, 1)} A.",
            generated = true,
        )
    }

    fun peSize(r: Random): Question {
        val s = listOf(1.5, 2.5, 4.0, 6.0, 10.0, 16.0, 25.0, 35.0, 50.0, 70.0, 95.0).random(r)
        val pe = when {
            s <= 16 -> s
            s <= 35 -> 16.0
            else -> s / 2
        }
        val wrongs = listOf(s, s / 2, 16.0, 10.0, 25.0, s * 2).filter { it != pe }.distinct().take(3)
        return Question(
            "gen.pe", SepCategory.INSTALL,
            "Jaki minimalny przekrój powinien mieć przewód ochronny PE (ten sam materiał), jeśli przewód fazowy ma ${fmt(s, 1)} mm²?",
            listOf("${fmt(pe, 1)} mm²") + wrongs.map { "${fmt(it, 1)} mm²" }, 0,
            "S ≤ 16 mm² → PE = S; 16 < S ≤ 35 mm² → PE = 16 mm²; S > 35 mm² → PE = S/2. Tu: ${fmt(pe, 1)} mm².",
            "PN-HD 60364-5-54", generated = true,
        )
    }

    fun ipCode(r: Random): Question {
        val first = mapOf(2 to "ciała stałe > 12,5 mm (palec)", 4 to "ciała stałe > 1 mm (drut)", 5 to "ochrona przed pyłem", 6 to "pyłoszczelne")
        val second = mapOf(0 to "brak ochrony przed wodą", 4 to "bryzgi wody z dowolnego kierunku", 5 to "struga wody", 7 to "chwilowe zanurzenie")
        val f = first.keys.random(r); val s = second.keys.random(r)
        val correct = "${first.getValue(f)}; ${second.getValue(s)}"
        val wrong = listOf(
            "${first.getValue(first.keys.filter { it != f }.random(r))}; ${second.getValue(s)}",
            "${first.getValue(f)}; ${second.getValue(second.keys.filter { it != s }.random(r))}",
            "klasa ochronności $f, napięcie ${s}0 V",
        )
        return Question(
            "gen.ip", SepCategory.INSTALL,
            "Co oznacza stopień ochrony IP$f$s?",
            listOf(correct) + wrong, 0,
            "Pierwsza cyfra – ochrona przed ciałami stałymi (0–6), druga – przed wodą (0–8). Kod IP nie mówi nic o klasie ochronności ani napięciu.",
            "PN-EN 60529", generated = true,
        )
    }

    val all: List<(Random) -> Question> = listOf(::zsMax, ::zsCheck, ::iaValue, ::insulation, ::rcdTime, ::current1p, ::current3p, ::peSize, ::ipCode)

    /** Identyfikatory generatorów (do śledzenia postępów). */
    val ids = listOf("gen.zsmax", "gen.zscheck", "gen.ia", "gen.riso", "gen.rcd", "gen.i1p", "gen.i3p", "gen.pe", "gen.ip")
}
