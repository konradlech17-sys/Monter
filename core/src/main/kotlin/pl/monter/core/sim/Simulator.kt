package pl.monter.core.sim

import pl.monter.core.model.Kind
import pl.monter.core.model.Part
import pl.monter.core.model.Potential
import pl.monter.core.model.TermRef
import pl.monter.core.model.Wire

enum class FaultKind(val title: String, val trips: Boolean) {
    SHORT_LN("Zwarcie L–N", true),
    SHORT_LL("Zwarcie międzyfazowe", true),
    EARTH_FAULT("Zwarcie doziemne L–PE", true),
    LV_SHORT("Zwarcie w obwodzie 8 V", true),
    N_PE_BRIDGE("Połączenie N z PE", false),
    SELV_MIXED("Połączenie obwodu SELV z siecią 230 V", false),
}

data class Fault(val kind: FaultKind, val at: TermRef, val potentials: Set<Potential>)

enum class LoadState { OFF, ON, DAMAGED }

enum class Note {
    /** Faza podana na zacisk N (np. zamienione L i N). */
    POLARITY_SWAPPED,
    NO_PE,
    /** Obudowa (zacisk PE) pod napięciem! */
    LIVE_ENCLOSURE,
    /** Prąd wraca przewodem PE zamiast N. */
    VIA_PE,
    PHASE_ORDER,
    NO_N,
}

data class LoadStatus(val state: LoadState, val notes: Set<Note> = emptySet(), val message: String? = null) {
    val on get() = state == LoadState.ON
}

/** Zadziałanie zabezpieczenia w trakcie symulacji. */
data class TripEvent(val part: String, val reason: String)

data class SimResult(
    val solved: Solved,
    val loads: Map<String, LoadStatus>,
    val faults: List<Fault>,
    val trips: List<TripEvent>,
    val tripped: Set<String>,
) {
    fun potentials(t: TermRef): Set<Potential> = solved.pots(t)
    fun wireLive(w: Wire): Boolean = solved.pots(w.a).any { it.isPhase || it.isLowVoltage }
}

/** Wynik „rozwiązania" połączeń: podział zacisków na sieci (węzły) i ich potencjały. */
class Solved(
    private val index: Map<TermRef, Int>,
    private val root: IntArray,
    private val netPots: Map<Int, Set<Potential>>,
    val activeParts: Set<String>,
) {
    fun net(t: TermRef): Int = root[index.getValue(t)]
    fun pots(t: TermRef): Set<Potential> = netPots[net(t)] ?: emptySet()
    fun nets(): Map<Int, Set<Potential>> = netPots
    fun connected(a: TermRef, b: TermRef) = net(a) == net(b)
}

private class UnionFind(n: Int) {
    val p = IntArray(n) { it }
    fun find(x: Int): Int {
        var r = x
        while (p[r] != r) r = p[r]
        var c = x
        while (p[c] != r) { val nx = p[c]; p[c] = r; c = nx }
        return r
    }
    fun union(a: Int, b: Int) { val ra = find(a); val rb = find(b); if (ra != rb) p[ra] = rb }
}

/**
 * Symulator obwodu. Model jest „logiczny": każdy węzeł niesie zbiór potencjałów (L1, L2, L3, N, PE, 8 V).
 * Na tej podstawie wykrywamy zwarcia, zadziałanie zabezpieczeń oraz stan odbiorników.
 */
class Simulator(
    val parts: List<Part>,
    val wires: List<Wire>,
    val choices: Map<String, String> = emptyMap(),
) {
    private val terms: List<TermRef> = parts.flatMap { p -> p.kind.terminals.map { TermRef(p.id, it.id) } }
    private val index: Map<TermRef, Int> = terms.withIndex().associate { it.value to it.index }
    private val byId = parts.associateBy { it.id }

    fun part(id: String) = byId.getValue(id)

    fun stateOf(p: Part, controls: Map<String, Int>) = controls[p.id] ?: p.kind.defaultState

    /**
     * @param open aparaty traktowane jako otwarte (zadziałały / zostały „wyjęte" podczas analizy)
     * @param cutPoles pojedyncze bieguny (id aparatu, zacisk wejściowy) traktowane jako rozwarte
     */
    fun solve(
        controls: Map<String, Int>,
        open: Set<String> = emptySet(),
        cutPoles: Set<Pair<String, String>> = emptySet(),
    ): Solved {
        var active = emptySet<String>()
        var result: Solved
        var guard = 0
        while (true) {
            val uf = UnionFind(terms.size)
            fun u(a: TermRef, b: TermRef) = uf.union(index.getValue(a), index.getValue(b))
            for (w in wires) u(w.a, w.b)
            for (p in parts) for ((x, y) in links(p, stateOf(p, controls), open, active, cutPoles)) {
                u(TermRef(p.id, x), TermRef(p.id, y))
            }
            val root = IntArray(terms.size) { uf.find(it) }
            val pots = HashMap<Int, MutableSet<Potential>>()
            fun src(t: TermRef, pot: Potential) = pots.getOrPut(root[index.getValue(t)]) { mutableSetOf() }.add(pot)
            for (p in parts) {
                when (p.kind) {
                    Kind.SUPPLY_1P -> {
                        if (p.id !in open) src(TermRef(p.id, "L"), Potential.L1)
                        src(TermRef(p.id, "N"), Potential.N); src(TermRef(p.id, "PE"), Potential.PE)
                    }
                    Kind.SUPPLY_3P -> {
                        if (p.id !in open) {
                            src(TermRef(p.id, "L1"), Potential.L1); src(TermRef(p.id, "L2"), Potential.L2)
                            src(TermRef(p.id, "L3"), Potential.L3)
                        }
                        src(TermRef(p.id, "N"), Potential.N); src(TermRef(p.id, "PE"), Potential.PE)
                    }
                    Kind.TRANSFORMER -> if (p.id in active) {
                        src(TermRef(p.id, "S1"), Potential.LV_A); src(TermRef(p.id, "S2"), Potential.LV_B)
                    }
                    else -> {}
                }
            }
            result = Solved(index, root, pots, active)
            val nowActive = parts.filter { it.kind.category == pl.monter.core.model.Category.ACTIVE && it.id !in open }
                .filter { mainsPowered(result, it) }.map { it.id }.toSet()
            if (nowActive == active || ++guard > 6) break
            active = nowActive
        }
        return result
    }

    /** Czy element aktywny (transformator, czujnik) ma napięcie 230 V między L i N. */
    private fun mainsPowered(s: Solved, p: Part): Boolean {
        val l = s.pots(TermRef(p.id, "L")); val n = s.pots(TermRef(p.id, "N"))
        if (p.kind == Kind.TRANSFORMER) {
            return (l.any { it.isPhase } && Potential.N in n) || (n.any { it.isPhase } && Potential.N in l)
        }
        return l.any { it.isPhase } && Potential.N in n
    }

    private fun links(
        p: Part, s: Int, open: Set<String>, active: Set<String>, cut: Set<Pair<String, String>>,
    ): List<Pair<String, String>> = when (p.kind) {
        Kind.SWITCH_1 -> if (s == 1) listOf("L" to "P") else emptyList()
        Kind.SWITCH_2 -> buildList {
            if (s and 1 != 0) add("L" to "P1")
            if (s and 2 != 0) add("L" to "P2")
        }
        Kind.SWITCH_STAIR -> listOf(if (s == 0) "C" to "P1" else "C" to "P2")
        Kind.SWITCH_CROSS -> if (s == 0) listOf("1" to "3", "2" to "4") else listOf("1" to "4", "2" to "3")
        Kind.BUTTON -> if (s == 1) listOf("1" to "2") else emptyList()
        Kind.MOTION_SENSOR -> if (s == 1 && p.id in active) listOf("L" to "OUT") else emptyList()
        Kind.WAGO3, Kind.WAGO5, Kind.N_BAR, Kind.PE_BAR -> p.kind.terminals.drop(1).map { p.kind.terminals[0].id to it.id }
        else -> if (p.kind.isSwitchgear && s == 1 && p.id !in open) {
            p.kind.poles.filter { (p.id to it.first) !in cut }
        } else emptyList()
    }

    // ------------------------------------------------------------------ zwarcia i zabezpieczenia

    fun faults(s: Solved): List<Fault> {
        val repr = HashMap<Int, TermRef>()
        for (t in terms) repr.putIfAbsent(s.net(t), t)
        val out = mutableListOf<Fault>()
        for ((net, pots) in s.nets()) {
            val at = repr[net] ?: continue
            val phases = pots.count { it.isPhase }
            val mains = phases > 0 || Potential.N in pots || Potential.PE in pots
            val lv = pots.count { it.isLowVoltage }
            val kind = when {
                phases >= 2 -> FaultKind.SHORT_LL
                phases == 1 && Potential.N in pots -> FaultKind.SHORT_LN
                phases == 1 && Potential.PE in pots -> FaultKind.EARTH_FAULT
                lv > 0 && mains -> FaultKind.SELV_MIXED
                lv == 2 -> FaultKind.LV_SHORT
                Potential.N in pots && Potential.PE in pots -> FaultKind.N_PE_BRIDGE
                else -> null
            }
            if (kind != null) out += Fault(kind, at, pots)
        }
        return out
    }

    private fun stillFaulty(s: Solved, f: Fault): Boolean =
        faults(s).any { it.kind == f.kind && s.net(it.at) == s.net(f.at) }

    private fun ratingOf(p: Part): Int = p.effectiveSpec(choices)?.ratingA ?: if (p.kind.isSupply) 25 else 16

    /** Wyznacza aparat, który zadziała przy danym zwarciu (najbliższy / najczulszy). */
    private fun trippingDevice(controls: Map<String, Int>, open: Set<String>, f: Fault): Part? {
        if (f.kind == FaultKind.LV_SHORT) {
            return parts.firstOrNull { it.kind == Kind.TRANSFORMER && it.id !in open }
        }
        val candidates = parts.filter {
            it.id !in open && stateOf(it, controls) == it.kind.defaultState &&
                (it.kind.isMcb || it.kind.isSupply || (it.kind.isRcd && f.kind == FaultKind.EARTH_FAULT))
        }
        val clearing = candidates.filter { !stillFaulty(solve(controls, open + it.id), f) }
        if (f.kind == FaultKind.EARTH_FAULT) {
            clearing.filter { it.kind.isRcd }.minByOrNull { it.effectiveSpec(choices)?.rcdmA ?: 30 }?.let { return it }
        }
        return clearing.filter { !it.kind.isRcd }.minWithOrNull(compareBy<Part>({ ratingOf(it) }, { if (it.kind.isSupply) 1 else 0 }))
    }

    /** Odbiorniki, przez które płynie prąd (do analizy wyłączników RCD). */
    private fun currentPaths(p: Part): Pair<List<String>, List<String>>? = when (p.kind) {
        Kind.LAMP, Kind.FAN, Kind.SOCKET, Kind.CIRCUIT_1P, Kind.MOTION_SENSOR, Kind.TRANSFORMER -> listOf("L", "N") to listOf("L", "N")
        Kind.SOCKET_400, Kind.CIRCUIT_3P -> listOf("L1", "L2", "L3", "N") to listOf("L1", "L2", "L3", "N")
        else -> null
    }

    /**
     * RCD porównuje prąd płynący biegunami fazowymi z prądem w biegunie N. Jeżeli odbiornik pobiera
     * prąd fazą przez RCD, a oddaje go z pominięciem RCD (lub odwrotnie) – RCD wyzwala.
     */
    private fun imbalancedRcd(controls: Map<String, Int>, open: Set<String>, s: Solved, loads: Map<String, LoadStatus>): Part? {
        val rcds = parts.filter { it.kind.isRcd && it.id !in open && stateOf(it, controls) == 1 }
        for (rcd in rcds) {
            val phasePoles = rcd.kind.poles.filter { it.first != "N" }.map { rcd.id to it.first }.toSet()
            val nPole = setOf(rcd.id to "N")
            val cutPhase = solve(controls, open, phasePoles)
            val cutN = solve(controls, open, nPole)
            for (load in parts) {
                val st = loads[load.id] ?: continue
                if (!st.on) continue
                val (terms, _) = currentPaths(load) ?: continue
                val refs = terms.map { TermRef(load.id, it) }
                val viaPhase = refs.any { t -> s.pots(t).any { it.isPhase } && cutPhase.pots(t).none { it.isPhase } }
                val viaN = refs.any { t -> Potential.N in s.pots(t) && Potential.N !in cutN.pots(t) }
                if (viaPhase != viaN) return rcd
            }
        }
        return null
    }

    fun simulate(controls: Map<String, Int>): SimResult {
        val open = mutableSetOf<String>()
        val trips = mutableListOf<TripEvent>()
        var s = solve(controls, open)
        var loads = evaluateLoads(s)
        repeat(12) {
            val f = faults(s).firstOrNull { it.kind.trips }
            if (f != null) {
                val dev = trippingDevice(controls, open, f) ?: return@repeat
                open += dev.id
                trips += TripEvent(dev.id, f.kind.title)
            } else {
                val rcd = imbalancedRcd(controls, open, s, loads) ?: return SimResult(s, loads, faults(s), trips, open)
                open += rcd.id
                trips += TripEvent(rcd.id, "Prąd różnicowy – przewód N/PE podłączony z pominięciem wyłącznika RCD")
            }
            s = solve(controls, open)
            loads = evaluateLoads(s)
        }
        return SimResult(s, loads, faults(s), trips, open)
    }

    // ------------------------------------------------------------------ odbiorniki

    fun evaluateLoads(s: Solved): Map<String, LoadStatus> =
        parts.mapNotNull { p -> evaluate(p, s)?.let { p.id to it } }.toMap()

    private fun evaluate(p: Part, s: Solved): LoadStatus? {
        fun pots(id: String) = s.pots(TermRef(p.id, id))
        return when (p.kind) {
            Kind.LAMP, Kind.SOCKET, Kind.CIRCUIT_1P -> singlePhase(pots("L"), pots("N"), pots("PE"))
            // Wentylator ma II klasę ochronności (podwójna izolacja) – nie ma zacisku PE.
            Kind.FAN -> singlePhase(pots("L"), pots("N"), setOf(Potential.PE))
            Kind.MOTOR -> {
                // Silnik trójfazowy nie potrzebuje przewodu N – obciąża fazy symetrycznie.
                val st = threePhase(listOf(pots("L1"), pots("L2"), pots("L3")), setOf(Potential.N), pots("PE"))
                st
            }
            Kind.SPD_2P -> {
                val l = pots("L"); val n = pots("N"); val pe = pots("PE")
                if (l.any { it.isPhase } && Potential.N in n && Potential.PE in pe) LoadStatus(LoadState.ON) else LoadStatus(LoadState.OFF)
            }
            Kind.SOCKET_400, Kind.CIRCUIT_3P -> threePhase(listOf(pots("L1"), pots("L2"), pots("L3")), pots("N"), pots("PE"))
            Kind.SPD_4P -> {
                val tp = threePhase(listOf(pots("L1"), pots("L2"), pots("L3")), pots("N"), pots("PE"))
                if (tp.on && tp.notes.none { it == Note.NO_PE || it == Note.NO_N }) LoadStatus(LoadState.ON) else LoadStatus(LoadState.OFF)
            }
            Kind.BELL -> {
                val a = pots("A"); val b = pots("B")
                when {
                    (a.any { it.isPhase } && (b.any { it.isPhase || it == Potential.N })) ||
                        (b.any { it.isPhase } && Potential.N in a) ->
                        LoadStatus(LoadState.DAMAGED, message = "Dzwonek 8 V podłączony do 230 V – spalony!")
                    (Potential.LV_A in a && Potential.LV_B in b) || (Potential.LV_B in a && Potential.LV_A in b) -> LoadStatus(LoadState.ON)
                    else -> LoadStatus(LoadState.OFF)
                }
            }
            Kind.TRANSFORMER, Kind.MOTION_SENSOR ->
                if (p.id in s.activeParts) LoadStatus(LoadState.ON) else LoadStatus(LoadState.OFF)
            else -> null
        }
    }

    private fun singlePhase(l: Set<Potential>, n: Set<Potential>, pe: Set<Potential>): LoadStatus {
        val notes = mutableSetOf<Note>()
        if (pe.any { it.isPhase }) notes += Note.LIVE_ENCLOSURE
        if (Potential.PE !in pe) notes += Note.NO_PE
        val pl = l.filter { it.isPhase }.toSet(); val pn = n.filter { it.isPhase }.toSet()
        if (l.any { it.isLowVoltage } || n.any { it.isLowVoltage }) return LoadStatus(LoadState.OFF, notes)
        return when {
            pl.isNotEmpty() && pn.isNotEmpty() && pl != pn ->
                LoadStatus(LoadState.DAMAGED, notes, "Odbiornik 230 V podłączony między dwie fazy (400 V) – spalony!")
            pl.isNotEmpty() && Potential.N in n -> LoadStatus(LoadState.ON, notes)
            pn.isNotEmpty() && Potential.N in l -> LoadStatus(LoadState.ON, notes + Note.POLARITY_SWAPPED)
            pl.isNotEmpty() && Potential.PE in n -> LoadStatus(LoadState.ON, notes + Note.VIA_PE)
            pn.isNotEmpty() && Potential.PE in l -> LoadStatus(LoadState.ON, notes + Note.VIA_PE + Note.POLARITY_SWAPPED)
            else -> LoadStatus(LoadState.OFF, notes)
        }
    }

    private fun threePhase(ls: List<Set<Potential>>, n: Set<Potential>, pe: Set<Potential>): LoadStatus {
        val notes = mutableSetOf<Note>()
        if (pe.any { it.isPhase }) notes += Note.LIVE_ENCLOSURE
        if (Potential.PE !in pe) notes += Note.NO_PE
        val phases = ls.map { set -> set.singleOrNull { it.isPhase } }
        if (phases.any { it == null } || phases.toSet().size != 3) return LoadStatus(LoadState.OFF, notes)
        if (n.any { it.isPhase }) return LoadStatus(LoadState.DAMAGED, notes, "Faza na zacisku N – odbiornik uszkodzony!")
        if (Potential.N !in n) notes += Note.NO_N
        val order = phases.map { it!!.ordinal }
        val clockwise = (order[1] - order[0] + 3) % 3 == 1 && (order[2] - order[1] + 3) % 3 == 1
        if (!clockwise) notes += Note.PHASE_ORDER
        return LoadStatus(LoadState.ON, notes)
    }
}
