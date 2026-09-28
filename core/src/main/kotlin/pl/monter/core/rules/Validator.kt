package pl.monter.core.rules

import pl.monter.core.game.Difficulty
import pl.monter.core.level.Controls
import pl.monter.core.level.Goal
import pl.monter.core.level.Level
import pl.monter.core.model.CrossSection
import pl.monter.core.model.Kind
import pl.monter.core.model.Part
import pl.monter.core.model.Potential
import pl.monter.core.model.TermRef
import pl.monter.core.model.Wire
import pl.monter.core.model.WireColor
import pl.monter.core.sim.FaultKind
import pl.monter.core.sim.LoadState
import pl.monter.core.sim.Note
import pl.monter.core.sim.SimResult
import pl.monter.core.sim.Simulator

enum class Severity { ERROR, WARNING }

data class Issue(
    val severity: Severity,
    val code: String,
    val title: String,
    /** Wyjaśnienie „dlaczego" – najważniejsza część z punktu widzenia nauki. */
    val detail: String,
    val wires: Set<Int> = emptySet(),
    val parts: Set<String> = emptySet(),
)

data class Report(val issues: List<Issue>, val scenariosChecked: Int) {
    val errors get() = issues.filter { it.severity == Severity.ERROR }
    val warnings get() = issues.filter { it.severity == Severity.WARNING }
    val passed get() = errors.isEmpty()
}

/**
 * Sprawdza zbudowany układ: najpierw zasady (kolory, przekroje, zabezpieczenia),
 * potem symulację wszystkich kombinacji łączników.
 */
class Validator(private val level: Level, wires: List<Wire>, private val choices: Map<String, String>, private val difficulty: Difficulty) {

    /** Pełna lista przewodów na planszy (fabryczne + gracza). */
    private val allWires = wires
    private val sim = Simulator(level.parts, allWires, choices)
    private val issues = mutableListOf<Issue>()

    /** Ostrzeżenie w trybie „Mistrz" staje się błędem. */
    private fun practice(code: String, title: String, detail: String, wires: Set<Int> = emptySet(), parts: Set<String> = emptySet()) {
        issues += Issue(if (difficulty.strict) Severity.ERROR else Severity.WARNING, code, title, detail, wires, parts)
    }

    private fun error(code: String, title: String, detail: String, wires: Set<Int> = emptySet(), parts: Set<String> = emptySet()) {
        issues += Issue(Severity.ERROR, code, title, detail, wires, parts)
    }

    private val controlParts = level.parts.filter { it.kind.isUserControl }

    /** Wszystkie kombinacje stanów łączników (maks. 256). */
    private val scenarios: List<Map<String, Int>> = run {
        var acc = listOf(emptyMap<String, Int>())
        for (p in controlParts) acc = acc.flatMap { m -> (0 until p.kind.states).map { m + (p.id to it) } }
        acc.take(256)
    }

    fun validate(): Report {
        checkChoices()
        checkTerminalLoad()
        val results = scenarios.map { it to sim.simulate(it) }
        checkFaults(results)
        if (issues.none { it.code.startsWith("fault") }) {
            checkColors(results)
            checkSwitchesInPhase(results)
            checkBellButton(results)
            checkLoadNotes(results)
            checkCrossSections(results)
            checkProtection(results)
            checkPhaseBalance(results)
            checkGoals(results)
        }
        checkDangling()
        return Report(issues.distinctBy { Triple(it.code, it.wires, it.parts) }, scenarios.size)
    }

    // ------------------------------------------------------------------ wybory wariantów

    private fun checkChoices() {
        for (p in level.parts.filter { it.options.isNotEmpty() }) {
            if (choices[p.id] == null) error("choice.missing", "Nie wybrano: ${p.label}", "Dotknij elementu i wybierz odpowiedni wariant.", parts = setOf(p.id))
        }
        for (rule in level.choiceRules) {
            val chosen = choices[rule.part] ?: continue
            if (chosen !in rule.valid) {
                error("choice.wrong", "Niewłaściwy wybór: ${level.part(rule.part).label}", rule.explain[chosen] ?: "Ten wariant nie pasuje do tego miejsca.", parts = setOf(rule.part))
            }
        }
    }

    // ------------------------------------------------------------------ zaciski

    private fun checkTerminalLoad() {
        val counts = allWires.flatMap { listOf(it.a, it.b) }.groupingBy { it }.eachCount()
        for ((t, n) in counts) {
            val def = level.partOrNull(t.part)?.kind?.terminals?.firstOrNull { it.id == t.terminal } ?: continue
            if (n > def.maxWires) {
                error(
                    "terminal.overload", "Za dużo przewodów w zacisku ${level.part(t.part).label} ${def.label}",
                    "Ten zacisk przyjmuje maksymalnie ${def.maxWires} przew. Użyj złączki (np. WAGO) lub szyny.",
                    wires = allWires.filter { it.touches(t) }.map { it.id }.toSet(), parts = setOf(t.part),
                )
            }
        }
        allWires.groupBy { setOf(it.a, it.b) }.values.filter { it.size > 1 }.forEach { dup ->
            practice("wire.duplicate", "Zdublowany przewód", "Dwa przewody łączą te same zaciski – to zbędne.", dup.map { it.id }.toSet())
        }
        allWires.filter { it.a.part == it.b.part && sameInternalNet(it) }.forEach {
            practice("wire.loop", "Zbędny mostek", "Ten przewód łączy zaciski, które już są połączone wewnątrz elementu.", setOf(it.id))
        }
    }

    private fun sameInternalNet(w: Wire): Boolean {
        val kind = level.part(w.a.part).kind
        return kind.isConnector
    }

    // ------------------------------------------------------------------ zwarcia

    private fun describe(c: Map<String, Int>): String {
        if (c.isEmpty()) return ""
        return " (stan łączników: " + c.entries.joinToString(", ") { (id, s) ->
            val p = level.part(id)
            val st = when (p.kind) {
                Kind.SWITCH_STAIR, Kind.SWITCH_CROSS -> "poz. ${s + 1}"
                Kind.SWITCH_2 -> listOf(if (s and 1 != 0) "1:wł" else "1:wył", if (s and 2 != 0) "2:wł" else "2:wył").joinToString("/")
                Kind.MOTION_SENSOR -> if (s == 1) "ruch" else "brak ruchu"
                Kind.BUTTON -> if (s == 1) "wciśnięty" else "puszczony"
                else -> if (s == 1) "wł." else "wył."
            }
            "${p.label} $st"
        } + ")"
    }

    private fun checkFaults(results: List<Pair<Map<String, Int>, SimResult>>) {
        for ((c, r) in results) {
            for (t in r.trips) {
                val p = level.part(t.part)
                val why = when {
                    t.reason.startsWith("Zwarcie L–N") -> "Przewód fazowy styka się bezpośrednio z neutralnym. Prąd zwarciowy jest ogromny – dlatego zadziałało zabezpieczenie."
                    t.reason.startsWith("Zwarcie między") -> "Połączono ze sobą dwie różne fazy (400 V)."
                    t.reason.startsWith("Zwarcie doziemne") -> "Faza połączona z przewodem ochronnym PE. W prawdziwej instalacji obudowy urządzeń znalazłyby się pod napięciem."
                    t.reason.startsWith("Zwarcie w obwodzie 8") -> "Zwarte zaciski wtórne transformatora dzwonkowego."
                    else -> t.reason
                }
                error("fault.trip", "${t.reason}: zadziałał ${p.label}${describe(c)}", why, parts = setOf(t.part))
            }
            for (f in r.faults) {
                when (f.kind) {
                    FaultKind.N_PE_BRIDGE -> error(
                        "fault.npe", "Połączono przewód N z PE",
                        "W układzie TN-S przewody N i PE są rozdzielone na całej długości. Połączenie ich za punktem rozdziału PEN powoduje, że prąd roboczy płynie przewodem ochronnym, a wyłącznik RCD nie może działać poprawnie.",
                        wires = wiresInNet(r, f.at),
                    )
                    FaultKind.SELV_MIXED -> error(
                        "fault.selv", "Obwód bardzo niskiego napięcia (SELV) połączony z siecią 230 V",
                        "Obwód SELV (np. dzwonek 8 V) musi być galwanicznie oddzielony od sieci – na tym polega jego bezpieczeństwo.",
                        wires = wiresInNet(r, f.at),
                    )
                    else -> {}
                }
            }
            for ((id, st) in r.loads) if (st.state == LoadState.DAMAGED) {
                error("fault.damaged", "${level.part(id).label}: uszkodzenie${describe(c)}", st.message ?: "Odbiornik otrzymał niewłaściwe napięcie.", parts = setOf(id))
            }
        }
    }

    private fun wiresInNet(r: SimResult, t: TermRef): Set<Int> =
        allWires.filter { r.solved.net(it.a) == r.solved.net(t) }.map { it.id }.toSet()

    // ------------------------------------------------------------------ kolory

    /** Suma potencjałów sieci danego przewodu we wszystkich scenariuszach. */
    private fun wireRoles(results: List<Pair<Map<String, Int>, SimResult>>): Map<Int, Set<Potential>> =
        allWires.associate { w -> w.id to results.flatMap { it.second.potentials(w.a) }.toSet() }

    private fun checkColors(results: List<Pair<Map<String, Int>, SimResult>>) {
        val roles = wireRoles(results)
        for (w in allWires) {
            val pots = roles.getValue(w.id)
            val phases = pots.filter { it.isPhase }.toSet()
            when {
                Potential.PE in pots && w.color != WireColor.GREEN_YELLOW -> error(
                    "color.pe", "Przewód ochronny PE musi być zielono-żółty",
                    "Wg PN-EN 60445 izolacja zielono-żółta jest zarezerwowana dla przewodu ochronnego. Każdy elektryk musi móc rozpoznać PE bez mierzenia.",
                    setOf(w.id),
                )
                Potential.PE !in pots && w.color == WireColor.GREEN_YELLOW -> error(
                    "color.gy", "Zielono-żółty użyty nie jako PE",
                    "Przewodu zielono-żółtego NIE WOLNO używać do innych celów niż ochronny – ktoś mógłby dotknąć „bezpiecznego\" przewodu pod napięciem.",
                    setOf(w.id),
                )
                Potential.N in pots && phases.isEmpty() && w.color != WireColor.BLUE -> practice(
                    "color.n", "Przewód neutralny N powinien być niebieski",
                    "Niebieski oznacza przewód neutralny (PN-EN 60445).", setOf(w.id),
                )
                phases.isNotEmpty() && Potential.N !in pots && !w.color.isPhaseColor -> error(
                    "color.l", "Przewód fazowy w kolorze ${w.color.pl}",
                    "Przewody fazowe mają izolację brązową, czarną lub szarą. Niebieski przewód pod napięciem to pułapka dla następnego elektryka.",
                    setOf(w.id),
                )
                level.threePhase && phases.size == 1 && pots.none { it == Potential.N } -> {
                    val expected = when (phases.first()) {
                        Potential.L1 -> WireColor.BROWN
                        Potential.L2 -> WireColor.BLACK
                        else -> WireColor.GREY
                    }
                    if (w.color != expected && difficulty != Difficulty.EASY) practice(
                        "color.phase", "Faza ${phases.first().label} – przyjęty kolor to ${expected.pl}",
                        "Zgodnie z PN-EN 60445 zaleca się: L1 – brązowy, L2 – czarny, L3 – szary. Ułatwia to późniejsze pomiary i rozbudowę.",
                        setOf(w.id),
                    )
                }
            }
        }
    }

    // ------------------------------------------------------------------ łączniki w fazie

    private fun checkSwitchesInPhase(results: List<Pair<Map<String, Int>, SimResult>>) {
        for (p in level.parts.filter { it.kind.isUserControl && it.kind != Kind.BUTTON }) {
            val contacts = p.kind.terminals.filter { !(p.kind == Kind.MOTION_SENSOR && it.id == "N") }
            val pots = contacts.flatMap { t -> results.flatMap { it.second.potentials(TermRef(p.id, t.id)) } }.toSet()
            if (Potential.N in pots && pots.none { it.isPhase }) {
                error(
                    "switch.neutral", "${p.label} przerywa przewód neutralny",
                    "Łącznik musi przerywać przewód fazowy L. Gdy wyłącza N, oprawka żarówki pozostaje pod napięciem nawet po zgaszeniu światła – wymiana żarówki grozi porażeniem!",
                    parts = setOf(p.id),
                )
            }
        }
    }

    private fun checkBellButton(results: List<Pair<Map<String, Int>, SimResult>>) {
        for (p in level.parts.filter { it.kind == Kind.BUTTON }) {
            val pots = p.kind.terminals.flatMap { t -> results.flatMap { it.second.potentials(TermRef(p.id, t.id)) } }.toSet()
            if (pots.any { it.isPhase || it == Potential.N }) {
                error(
                    "button.mains", "Przycisk dzwonkowy w obwodzie 230 V",
                    "Przycisk przy drzwiach wejściowych jest narażony na wilgoć i dotyk – umieszcza się go w obwodzie bardzo niskiego napięcia (SELV) po stronie wtórnej transformatora.",
                    parts = setOf(p.id),
                )
            }
        }
    }

    // ------------------------------------------------------------------ uwagi odbiorników

    private fun checkLoadNotes(results: List<Pair<Map<String, Int>, SimResult>>) {
        for ((c, r) in results) for ((id, st) in r.loads) {
            val p = level.part(id)
            if (st.state != LoadState.ON) continue
            if (Note.LIVE_ENCLOSURE in st.notes) error("load.live", "${p.label}: obudowa pod napięciem!", "Faza dotarła do zacisku ochronnego PE. Dotknięcie obudowy grozi porażeniem.", parts = setOf(id))
            if (Note.VIA_PE in st.notes) error("load.viape", "${p.label}: prąd wraca przewodem PE", "Przewód ochronny nie może być przewodem roboczym. Podłącz zacisk N do przewodu neutralnego.", parts = setOf(id))
            if (Note.NO_PE in st.notes && !p.kind.isSpd) error(
                "load.nope", "${p.label}: brak przewodu ochronnego PE",
                "Odbiorniki I klasy ochronności (metalowa obudowa, gniazda ze stykiem ochronnym) muszą mieć podłączony PE. To on odprowadza prąd przy uszkodzeniu izolacji, a zabezpieczenie wyłącza zasilanie.",
                parts = setOf(id),
            )
            if (Note.NO_N in st.notes) error("load.non", "${p.label}: brak przewodu N", "Gniazdo 5-biegunowe wymaga przewodu neutralnego.", parts = setOf(id))
            if (Note.POLARITY_SWAPPED in st.notes) {
                val detail = if (p.kind == Kind.LAMP) {
                    "Faza powinna trafić na zacisk L – w oprawce E27 jest on połączony ze stykiem środkowym, a gwint (którego łatwo dotknąć) z N."
                } else {
                    "Przyjęło się podłączać fazę do lewego styku gniazda (patrząc od przodu). Zachowaj zgodność oznaczeń L/N."
                }
                practice("load.polarity", "${p.label}: zamienione L i N", detail, parts = setOf(id))
            }
            if (Note.PHASE_ORDER in st.notes && p.kind == Kind.MOTOR) error(
                "load.rotation", "${p.label} kręci się w złą stronę!",
                "Zamiana dwóch dowolnych faz odwraca kierunek wirowania pola magnetycznego, a więc i silnika. Pompa nie tłoczy wody, wentylator ssie zamiast wydmuchiwać. Podłącz L1→U1, L2→V1, L3→W1.",
                parts = setOf(id),
            ) else if (Note.PHASE_ORDER in st.notes) practice(
                "load.rotation", "${p.label}: odwrócona kolejność faz${describe(c)}",
                "Kolejność L1-L2-L3 wyznacza kierunek wirowania silnika. Zamiana dwóch faz sprawi, że np. piła lub pompa będzie kręcić się w złą stronę.",
                parts = setOf(id),
            )
        }
    }

    // ------------------------------------------------------------------ przekroje

    /** Najniższy prąd zabezpieczenia chroniącego przewód fazowy (wyłącznik nadprądowy lub wkładka przedlicznikowa). */
    private fun protectingRating(controls: Map<String, Int>, w: Wire, base: SimResult): Int? {
        val phase = base.potentials(w.a).filter { it.isPhase }
        if (phase.isEmpty()) return null
        val devices = level.parts.filter { it.kind.isMcb || it.kind.isSupply }
        return devices.filter { d ->
            val s = sim.solve(controls, open = setOf(d.id))
            s.pots(w.a).none { it.isPhase }
        }.mapNotNull { it.effectiveSpec(choices)?.ratingA ?: if (it.kind.isSupply) 25 else null }.minOrNull()
    }

    private fun checkCrossSections(results: List<Pair<Map<String, Int>, SimResult>>) {
        val roles = wireRoles(results)
        val requiredPhase = HashMap<Int, CrossSection>()
        for (w in allWires) {
            val pots = roles.getValue(w.id)
            val selv = pots.isNotEmpty() && pots.all { it.isLowVoltage }
            if (!selv && w.cs < level.minCs) {
                error(
                    "cs.min", "Za cienki przewód (${w.cs.label})",
                    "W instalacjach 230/400 V minimalny przekrój żyły miedzianej to ${level.minCs.label} (PN-HD 60364-5-52). 0,5 mm² nadaje się tylko do obwodów SELV (np. dzwonkowych).",
                    setOf(w.id),
                )
                continue
            }
            if (pots.none { it.isPhase }) continue
            val rating = results.mapNotNull { (c, r) -> protectingRating(c, w, r) }.maxOrNull() ?: continue
            val need = CrossSection.minimumFor(rating)
            requiredPhase[w.id] = need
            if (w.cs < need) {
                error(
                    "cs.phase", "Przewód ${w.cs.label} za cienki dla zabezpieczenia ${rating} A",
                    "Zabezpieczenie musi chronić przewód przed przegrzaniem: jego prąd nie może przekraczać obciążalności przewodu. Dla ${rating} A potrzeba co najmniej ${need.label} Cu.",
                    setOf(w.id),
                )
            }
        }
        // N i PE – nie cieńsze niż przewód fazowy tego samego obwodu.
        for (w in allWires) {
            val pots = roles.getValue(w.id)
            val isN = Potential.N in pots && pots.none { it.isPhase }
            val isPe = Potential.PE in pots && pots.none { it.isPhase }
            if (!isN && !isPe) continue
            val endpoints = listOf(w.a.part, w.b.part).map { level.part(it) }
                .filter { !it.kind.isBar && !it.kind.isSupply && !it.kind.isConnector }
            val need = endpoints.mapNotNull { p ->
                allWires.filter { it.a.part == p.id || it.b.part == p.id }.mapNotNull { requiredPhase[it.id] }.maxOrNull()
            }.minOrNull() ?: continue
            if (w.cs < need) practice(
                "cs.npe", "Przewód ${if (isN) "N" else "PE"} cieńszy od fazowego",
                "W obwodach jednofazowych przewód N ma taki sam przekrój jak fazowy, a PE (do 16 mm²) – nie mniejszy niż fazowy (PN-HD 60364-5-54). Tu potrzeba ${need.label}.",
                setOf(w.id),
            )
        }
    }

    // ------------------------------------------------------------------ zabezpieczenia

    private fun phaseTerms(p: Part): List<String> = when (p.kind) {
        Kind.CIRCUIT_3P, Kind.SOCKET_400, Kind.SPD_4P, Kind.MOTOR -> listOf("L1", "L2", "L3")
        else -> listOf("L")
    }

    private fun checkProtection(results: List<Pair<Map<String, Int>, SimResult>>) {
        val (c, r) = results.first()
        val mains = level.parts.filter { it.kind.isMainSwitch }
        val mcbs = level.parts.filter { it.kind.isMcb }
        val rcds = level.parts.filter { it.kind.isRcd }
        fun protects(dev: Part, load: Part): Boolean {
            val s = sim.solve(c, open = setOf(dev.id))
            return phaseTerms(load).any { t -> r.potentials(TermRef(load.id, t)).any { it.isPhase } && s.pots(TermRef(load.id, t)).none { it.isPhase } }
        }
        val loads = level.parts.filter { it.kind == Kind.CIRCUIT_1P || it.kind == Kind.CIRCUIT_3P || it.kind == Kind.SOCKET_400 || it.kind.isSpd || (it.circuit != null) }
        for (load in loads) {
            if (r.loads[load.id]?.on != true) continue
            for (m in mains) if (!protects(m, load)) error(
                "prot.main", "${load.label} zasilany z pominięciem rozłącznika głównego",
                "Rozłącznik główny musi odłączać całą instalację – inaczej wyłączenie go nie zapewni bezpieczeństwa podczas prac.",
                parts = setOf(load.id),
            )
            if (load.kind.isSpd) {
                val behindRcd = rcds.filter { protects(it, load) }
                if (behindRcd.isNotEmpty()) error(
                    "prot.spd", "Ogranicznik przepięć za wyłącznikiem RCD",
                    "Ogranicznik przepięć T1+T2 montuje się PRZED wyłącznikiem różnicowoprądowym. Prąd udarowy odprowadzany do PE wyzwalałby RCD (i mógł go uszkodzić).",
                    parts = setOf(load.id),
                )
                continue
            }
            val spec = load.circuit
            val protecting = mcbs.filter { protects(it, load) }
            if (spec != null) {
                val mcb = protecting.minByOrNull { it.effectiveSpec(choices)?.ratingA ?: 99 }
                if (mcb == null) {
                    error("prot.mcb", "${load.label}: brak wyłącznika nadprądowego", "Każdy obwód odbiorczy musi mieć własne zabezpieczenie nadprądowe – przeciążenie jednego obwodu nie może wyłączyć całego domu.", parts = setOf(load.id))
                } else {
                    val a = mcb.effectiveSpec(choices)?.ratingA ?: 0
                    if (a > spec.maxA) error(
                        "prot.rating", "${load.label}: zabezpieczenie ${a} A za duże",
                        "Dla obwodu „${spec.name}\" zabezpieczenie nie może przekraczać ${spec.maxA} A – przewody obwodu przegrzałyby się zanim wyłącznik zadziała.",
                        parts = setOf(load.id, mcb.id),
                    )
                    if (a < spec.minA) practice(
                        "prot.rating.low", "${load.label}: zabezpieczenie ${a} A za małe",
                        "Wyłącznik ${a} A będzie wyzwalał przy normalnej pracy odbiorników tego obwodu (min. ${spec.minA} A).",
                        parts = setOf(load.id, mcb.id),
                    )
                    val phases = phaseTerms(load)
                    if (phases.size == 3) {
                        val common = protecting.filter { dev -> phases.all { t -> !sim.solve(c, open = setOf(dev.id)).pots(TermRef(load.id, t)).any { it.isPhase } } }
                        if (common.none { it.kind == Kind.MCB_3P }) error(
                            "prot.3p", "${load.label}: wymagany wyłącznik 3-biegunowy",
                            "Odbiornik trójfazowy zabezpiecza się wyłącznikiem 3P – przy przeciążeniu odłącza on wszystkie fazy jednocześnie. Trzy osobne wyłączniki 1P mogłyby zostawić silnik na dwóch fazach.",
                            parts = setOf(load.id),
                        )
                    }
                }
                if (spec.needsRcd) {
                    val rcd = rcds.filter { protects(it, load) }.minByOrNull { it.effectiveSpec(choices)?.rcdmA ?: 999 }
                    val ma = rcd?.effectiveSpec(choices)?.rcdmA
                    if (rcd == null || ma == null || ma > spec.rcdMaxmA) error(
                        "prot.rcd", "${load.label}: wymagana ochrona RCD ${spec.rcdMaxmA} mA",
                        "Gniazda wtyczkowe ogólnego przeznaczenia oraz obwody łazienkowe muszą być chronione wyłącznikiem różnicowoprądowym o IΔn ≤ 30 mA (PN-HD 60364-4-41). Wyłącza on zasilanie zanim prąd rażenia zatrzyma serce.",
                        parts = setOfNotNull(load.id, rcd?.id),
                    )
                    else if (rcd.effectiveSpec(choices)?.rcdType == "AC") practice(
                        "prot.rcd.type", "${load.label}: RCD typu AC",
                        "Współczesne odbiorniki (pralki, ładowarki, falowniki) wytwarzają prądy upływu pulsujące. Zalecany jest RCD typu A – typ AC może ich „nie zauważyć\".",
                        parts = setOf(load.id, rcd.id),
                    )
                }
            }
        }
        // RCD nie ma zabezpieczenia nadprądowego – jego prąd znamionowy nie może być mniejszy od zabezpieczenia przed nim.
        for (rcd in rcds) {
            val inA = rcd.effectiveSpec(choices)?.ratingA ?: continue
            val before = level.parts.filter { it.kind.isSupply }.mapNotNull { it.effectiveSpec(choices)?.ratingA ?: 25 }.minOrNull() ?: continue
            if (inA < before) error(
                "prot.rcd.in", "${rcd.label}: prąd znamionowy ${inA} A mniejszy od zabezpieczenia ${before} A",
                "Wyłącznik RCD nie chroni sam siebie przed przeciążeniem. Jego prąd znamionowy musi być co najmniej równy zabezpieczeniu przedlicznikowemu.",
                parts = setOf(rcd.id),
            )
        }
    }

    private fun checkPhaseBalance(results: List<Pair<Map<String, Int>, SimResult>>) {
        if (!level.balancePhases) return
        val r = results.first().second
        val counts = mutableMapOf(Potential.L1 to 0, Potential.L2 to 0, Potential.L3 to 0)
        for (p in level.parts.filter { it.kind == Kind.CIRCUIT_1P }) {
            val ph = r.potentials(TermRef(p.id, "L")).singleOrNull { it.isPhase } ?: continue
            counts[ph] = counts.getValue(ph) + 1
        }
        if ((counts.values.maxOrNull() ?: 0) - (counts.values.minOrNull() ?: 0) > 1) practice(
            "balance", "Nierównomierne obciążenie faz (L1:${counts[Potential.L1]}, L2:${counts[Potential.L2]}, L3:${counts[Potential.L3]})",
            "Obwody jednofazowe rozdziela się równomiernie na L1, L2, L3. Przeciążona faza grzeje przewody zasilające, a w przewodzie N płynie większy prąd wyrównawczy.",
        )
    }

    // ------------------------------------------------------------------ cele

    private fun checkGoals(results: List<Pair<Map<String, Int>, SimResult>>) {
        for (g in level.goals) {
            val fails = evalGoal(g, results)
            fails.firstOrNull()?.let { issues += it }
        }
    }

    private fun evalGoal(g: Goal, results: List<Pair<Map<String, Int>, SimResult>>): List<Issue> {
        fun on(r: SimResult, id: String) = r.loads[id]?.on == true
        val out = mutableListOf<Issue>()
        when (g) {
            is Goal.Powered -> results.firstOrNull { !on(it.second, g.part) }?.let { (c, _) ->
                out += Issue(Severity.ERROR, "goal.powered", "${level.part(g.part).label} nie jest zasilany${describe(c)}", "Sprawdź, czy do odbiornika dochodzi faza L, przewód N i ochronny PE.", parts = setOf(g.part))
            }
            is Goal.Follows -> results.firstOrNull { (c, r) -> on(r, g.part) != g.expected(Controls(c)) }?.let { (c, r) ->
                val shouldBe = g.expected(Controls(c))
                out += Issue(
                    Severity.ERROR, "goal.follows",
                    "${level.part(g.part).label} ${if (shouldBe) "powinien działać, a nie działa" else "działa, choć nie powinien"}${describe(c)}",
                    "Oczekiwane działanie: ${g.describe}.", parts = setOf(g.part),
                )
            }
            is Goal.Toggles -> {
                val byState = results.associate { (c, r) -> c to on(r, g.part) }
                if (byState.values.none { it }) {
                    out += Issue(Severity.ERROR, "goal.toggles", "${level.part(g.part).label} nie świeci w żadnej pozycji łączników", "Sprawdź doprowadzenie fazy do łącznika i przewody korespondencyjne.", parts = setOf(g.part))
                } else {
                    loop@ for ((c, lit) in byState) for (ctl in g.controls) {
                        val p = level.part(ctl)
                        val flipped = c + (ctl to (((c[ctl] ?: 0) + 1) % p.kind.states))
                        if (byState[flipped] == lit) {
                            out += Issue(
                                Severity.ERROR, "goal.toggles",
                                "Przełączenie „${p.label}\" nie zmienia stanu światła${describe(c)}",
                                "Każdy łącznik w układzie schodowym/krzyżowym musi móc zapalić i zgasić światło niezależnie od pozostałych. Sprawdź przewody korespondencyjne (↑1, ↑2).",
                                parts = setOf(g.part, ctl),
                            )
                            break@loop
                        }
                    }
                }
            }
            is Goal.OneOf -> {
                val attempts = g.options.map { opt -> opt.flatMap { evalGoal(it, results) } }
                if (attempts.none { it.isEmpty() }) out += attempts.minBy { it.size }
            }
        }
        return out
    }

    // ------------------------------------------------------------------ przewody „wiszące"

    private fun checkDangling() {
        val counts = allWires.flatMap { listOf(it.a, it.b) }.groupingBy { it }.eachCount()
        for (w in allWires) {
            for (end in listOf(w.a, w.b)) {
                val kind = level.part(end.part).kind
                if (kind.isConnector && (counts[end] ?: 0) == 1) {
                    val siblings = kind.terminals.map { TermRef(end.part, it.id) }.count { (counts[it] ?: 0) > 0 }
                    if (siblings < 2) practice("wire.dangling", "Złączka z jednym przewodem", "Przewód wprowadzony do złączki, z której nic nie wychodzi – jest zbędny.", setOf(w.id), setOf(end.part))
                }
            }
        }
    }
}
