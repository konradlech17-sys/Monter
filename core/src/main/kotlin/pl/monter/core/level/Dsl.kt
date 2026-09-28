package pl.monter.core.level

import pl.monter.core.model.CircuitSpec
import pl.monter.core.model.CrossSection
import pl.monter.core.model.DeviceSpec
import pl.monter.core.model.Kind
import pl.monter.core.model.Part
import pl.monter.core.model.TermRef
import pl.monter.core.model.Wire
import pl.monter.core.model.WireColor

// Krótkie aliasy kolorów i przekrojów używane w definicjach poziomów.
internal val BR = WireColor.BROWN
internal val BK = WireColor.BLACK
internal val GR = WireColor.GREY
internal val BL = WireColor.BLUE
internal val GY = WireColor.GREEN_YELLOW
internal val WH = WireColor.WHITE
internal val S05 = CrossSection.S0_5
internal val S15 = CrossSection.S1_5
internal val S25 = CrossSection.S2_5
internal val S4 = CrossSection.S4
internal val S6 = CrossSection.S6
internal val S10 = CrossSection.S10
internal val S16 = CrossSection.S16

internal fun ref(s: String): TermRef {
    val (p, t) = s.split(".", limit = 2)
    return TermRef(p, t)
}

internal fun supply(ratingA: Int, label: String = "$ratingA A") = DeviceSpec("fuse$ratingA", label, ratingA = ratingA)
internal fun mcb(a: Int, curve: Char = 'B') = DeviceSpec("$curve$a", "$curve$a", ratingA = a, curve = curve)
internal fun rcd(a: Int, ma: Int, type: String) =
    DeviceSpec("rcd${a}_${ma}_$type", "$a A / $ma mA typ $type", ratingA = a, rcdmA = ma, rcdType = type)
internal fun mainSwitch(a: Int) = DeviceSpec("fr$a", "$a A", ratingA = a)

/** Budowniczy poziomu – czytelny zapis planszy, przewodów fabrycznych i rozwiązania. */
class LevelBuilder(private val id: String, private val chapter: Int, private val number: Int, private val title: String) {
    var subtitle = ""
    var funFact = ""
    var safety = true
    var threePhase = false
    var balance = false
    var points = 100
    var unlock: Int? = null
    var width = 1000f
    var height = 600f
    var minCs: CrossSection = CrossSection.S1_5
    var requires: String? = null
    private val theory = mutableListOf<String>()
    private val hints = mutableListOf<String>()
    private val parts = mutableListOf<Part>()
    private val pre = mutableListOf<Wire>()
    private val sol = mutableListOf<SolWire>()
    private val choices = mutableMapOf<String, String>()
    private val goals = mutableListOf<Goal>()
    private val rules = mutableListOf<ChoiceRule>()

    fun theory(vararg t: String) { theory += t }
    fun hint(vararg t: String) { hints += t }
    fun goal(vararg g: Goal) { goals += g }
    fun rule(r: ChoiceRule) { rules += r }

    fun part(
        id: String, kind: Kind, x: Int, y: Int, label: String = kind.title,
        spec: DeviceSpec? = null, options: List<DeviceSpec> = emptyList(), circuit: CircuitSpec? = null,
    ) { parts += Part(id, kind, x.toFloat(), y.toFloat(), label, spec, options, circuit) }

    /** Przewód fabryczny (już ułożony, nieusuwalny). */
    fun pre(a: String, b: String, c: WireColor, cs: CrossSection) {
        pre += Wire(-(pre.size + 1), ref(a), ref(b), c, cs, fixed = true)
    }

    /** Przewód wzorcowego rozwiązania. */
    fun w(a: String, b: String, c: WireColor, cs: CrossSection) { sol += SolWire(ref(a), ref(b), c, cs) }

    fun choose(part: String, option: String) { choices[part] = option }

    fun build() = Level(
        id = id, chapter = chapter, number = number, title = title, subtitle = subtitle,
        theory = theory.toList(), funFact = funFact, parts = parts.toList(), prewired = pre.toList(),
        goals = goals.toList(), choiceRules = rules.toList(), safetyProcedure = safety, minCs = minCs,
        threePhase = threePhase, balancePhases = balance, basePoints = points, unlockCost = unlock,
        hints = hints.toList(), width = width, height = height,
        solution = Solution(sol.toList(), choices.toMap()), requires = requires,
    )
}

internal fun level(id: String, chapter: Int, number: Int, title: String, block: LevelBuilder.() -> Unit): Level =
    LevelBuilder(id, chapter, number, title).apply(block).build()

/** Zmiany wprowadzane w poprawnym układzie, by zbudować poziom „znajdź usterkę". */
class FaultBuilder(wires: List<SolWire>) {
    val wires = wires.toMutableList()
    private fun idx(a: String, b: String) = wires.indexOfFirst { it.same(ref(a), ref(b)) }.also {
        require(it >= 0) { "Brak przewodu $a – $b" }
    }
    fun remove(a: String, b: String) { wires.removeAt(idx(a, b)) }
    fun add(a: String, b: String, c: WireColor, cs: CrossSection) { wires += SolWire(ref(a), ref(b), c, cs) }
    fun replace(a: String, b: String, na: String, nb: String, c: WireColor? = null) {
        val i = idx(a, b); val old = wires[i]
        wires[i] = SolWire(ref(na), ref(nb), c ?: old.color, old.cs)
    }
    fun cs(a: String, b: String, cs: CrossSection) { val i = idx(a, b); wires[i] = wires[i].copy(cs = cs) }
}

/**
 * Poziom serwisowy: gotowa instalacja z ukrytym błędem. Przewody „po poprzednim elektryku" można
 * przecinać i zmieniać; wzorcowe rozwiązanie jest takie samo jak w poziomie bazowym.
 */
internal fun fault(
    base: Level, id: String, number: Int, title: String, story: String, why: String, hint: String,
    mutate: FaultBuilder.() -> Unit,
): Level {
    val broken = FaultBuilder(base.solution.wires).apply(mutate).wires
    return base.copy(
        id = id, chapter = 6, number = number, title = title,
        subtitle = "Zgłoszenie: ${base.title.lowercase()}",
        theory = listOf("📞 $story", "Twoje zadanie: znajdź i usuń usterkę. Przewody po poprzednim wykonawcy możesz przecinać ✂️ i zmieniać.", why),
        prewired = base.prewired + broken.mapIndexed { i, w -> Wire(-(500 + i), w.a, w.b, w.color, w.cs, fixed = false) },
        story = story, basePoints = base.basePoints + 60, unlockCost = null, requires = base.id,
        hints = listOf(hint),
    )
}
