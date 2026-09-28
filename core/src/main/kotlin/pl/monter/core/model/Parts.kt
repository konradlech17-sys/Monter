package pl.monter.core.model

/** Definicja zacisku w obrębie elementu (współrzędne względem lewego-górnego rogu elementu). */
data class TerminalDef(
    val id: String,
    val label: String,
    val x: Float,
    val y: Float,
    val role: Role,
    /** Ile przewodów można zacisnąć w zacisku (złączki WAGO i szyny: 1, aparaty: 2). */
    val maxWires: Int = 2,
)

enum class Category { SOURCE, LOAD, CONTROL, SWITCHGEAR, CONNECTOR, ACTIVE }

private fun t(id: String, label: String, x: Int, y: Int, role: Role, max: Int = 2) =
    TerminalDef(id, label, x.toFloat(), y.toFloat(), role, max)

private fun row(y: Int, max: Int, role: Role, xs: List<Int>): List<TerminalDef> =
    xs.mapIndexed { i, x -> t("${i + 1}", "", x, y, role, max) }

/**
 * Rodzaje elementów. Oznaczenia zacisków aparatów modułowych odpowiadają oznaczeniom
 * spotykanym na prawdziwych aparatach: 1-2, 3-4, 5-6 (bieguny fazowe), N-N'.
 */
enum class Kind(
    val title: String,
    val w: Float,
    val h: Float,
    val category: Category,
    val terminals: List<TerminalDef>,
    /** Liczba stanów sterowania (0 – element bez sterowania). */
    val states: Int = 0,
    val defaultState: Int = 0,
) {
    SUPPLY_1P(
        "Zasilanie 230 V", 150f, 80f, Category.SOURCE,
        listOf(t("L", "L", 30, 68, Role.L, 1), t("N", "N", 75, 68, Role.N, 1), t("PE", "PE", 120, 68, Role.PE, 1)),
    ),
    SUPPLY_3P(
        "Złącze 3×400/230 V", 230f, 80f, Category.SOURCE,
        listOf(
            t("L1", "L1", 30, 68, Role.L1, 1), t("L2", "L2", 70, 68, Role.L2, 1), t("L3", "L3", 110, 68, Role.L3, 1),
            t("N", "N", 160, 68, Role.N, 1), t("PE", "PE", 200, 68, Role.PE, 1),
        ),
    ),
    LAMP(
        "Oprawa oświetleniowa", 120f, 110f, Category.LOAD,
        listOf(t("L", "L", 25, 96, Role.SWITCHED), t("N", "N", 60, 96, Role.N), t("PE", "PE", 95, 96, Role.PE)),
    ),
    SOCKET(
        "Gniazdo 230 V", 110f, 110f, Category.LOAD,
        listOf(t("L", "L", 25, 96, Role.L), t("N", "N", 55, 96, Role.N), t("PE", "PE", 85, 96, Role.PE)),
    ),
    SOCKET_400(
        "Gniazdo 400 V (5p)", 150f, 120f, Category.LOAD,
        listOf(
            t("L1", "L1", 20, 106, Role.L1), t("L2", "L2", 47, 106, Role.L2), t("L3", "L3", 74, 106, Role.L3),
            t("N", "N", 101, 106, Role.N), t("PE", "PE", 128, 106, Role.PE),
        ),
    ),
    SWITCH_1(
        "Łącznik jednobiegunowy", 90f, 100f, Category.CONTROL,
        listOf(t("L", "L", 25, 88, Role.L), t("P", "↑", 65, 88, Role.SWITCHED)), states = 2,
    ),
    SWITCH_2(
        "Łącznik świecznikowy", 110f, 100f, Category.CONTROL,
        listOf(t("L", "L", 20, 88, Role.L), t("P1", "↑1", 55, 88, Role.SWITCHED), t("P2", "↑2", 90, 88, Role.SWITCHED)),
        states = 4,
    ),
    SWITCH_STAIR(
        "Łącznik schodowy", 100f, 100f, Category.CONTROL,
        listOf(t("C", "C", 20, 88, Role.L), t("P1", "↑1", 55, 88, Role.TRAVELER), t("P2", "↑2", 85, 88, Role.TRAVELER)),
        states = 2,
    ),
    SWITCH_CROSS(
        "Łącznik krzyżowy", 120f, 100f, Category.CONTROL,
        listOf(
            t("1", "1", 18, 88, Role.TRAVELER), t("2", "2", 45, 88, Role.TRAVELER),
            t("3", "3", 75, 88, Role.TRAVELER), t("4", "4", 102, 88, Role.TRAVELER),
        ),
        states = 2,
    ),
    BUTTON(
        "Przycisk dzwonkowy", 80f, 90f, Category.CONTROL,
        listOf(t("1", "1", 22, 78, Role.LV), t("2", "2", 58, 78, Role.LV)), states = 2,
    ),
    BELL(
        "Dzwonek 8 V", 110f, 100f, Category.LOAD,
        listOf(t("A", "~", 35, 88, Role.LV), t("B", "~", 75, 88, Role.LV)),
    ),
    TRANSFORMER(
        "Transformator 230/8 V", 110f, 120f, Category.ACTIVE,
        listOf(
            t("L", "L", 25, 12, Role.L), t("N", "N", 85, 12, Role.N),
            t("S1", "8V", 30, 108, Role.LV), t("S2", "8V", 80, 108, Role.LV),
        ),
    ),
    MOTION_SENSOR(
        "Czujnik ruchu", 110f, 100f, Category.ACTIVE,
        listOf(t("L", "L", 20, 88, Role.L), t("N", "N", 55, 88, Role.N), t("OUT", "L′", 90, 88, Role.SWITCHED)),
        states = 2,
    ),
    WAGO3("Złączka 3×", 64f, 40f, Category.CONNECTOR, row(22, 1, Role.ANY, listOf(14, 32, 50))),
    WAGO5("Złączka 5×", 100f, 40f, Category.CONNECTOR, row(22, 1, Role.ANY, listOf(14, 32, 50, 68, 86))),
    MAIN_SWITCH_2P(
        "Rozłącznik główny 2P", 80f, 120f, Category.SWITCHGEAR,
        listOf(
            t("1", "1", 20, 10, Role.L), t("N", "N", 60, 10, Role.N),
            t("2", "2", 20, 110, Role.L), t("N'", "N", 60, 110, Role.N),
        ),
        states = 2, defaultState = 1,
    ),
    MAIN_SWITCH_4P(
        "Rozłącznik główny 4P", 160f, 120f, Category.SWITCHGEAR, fourPole(), states = 2, defaultState = 1,
    ),
    RCD_2P(
        "Wyłącznik różnicowoprądowy 2P", 80f, 120f, Category.SWITCHGEAR,
        listOf(
            t("1", "1", 20, 10, Role.L), t("N", "N", 60, 10, Role.N),
            t("2", "2", 20, 110, Role.L), t("N'", "N", 60, 110, Role.N),
        ),
        states = 2, defaultState = 1,
    ),
    RCD_4P("Wyłącznik różnicowoprądowy 4P", 160f, 120f, Category.SWITCHGEAR, fourPole(), states = 2, defaultState = 1),
    MCB_1P(
        "Wyłącznik nadprądowy 1P", 40f, 120f, Category.SWITCHGEAR,
        listOf(t("1", "1", 20, 10, Role.L), t("2", "2", 20, 110, Role.L)),
        states = 2, defaultState = 1,
    ),
    MCB_3P(
        "Wyłącznik nadprądowy 3P", 120f, 120f, Category.SWITCHGEAR,
        listOf(
            t("1", "1", 20, 10, Role.L1), t("3", "3", 60, 10, Role.L2), t("5", "5", 100, 10, Role.L3),
            t("2", "2", 20, 110, Role.L1), t("4", "4", 60, 110, Role.L2), t("6", "6", 100, 110, Role.L3),
        ),
        states = 2, defaultState = 1,
    ),
    SPD_4P(
        "Ogranicznik przepięć T1+T2", 160f, 120f, Category.LOAD,
        listOf(
            t("L1", "L1", 20, 10, Role.L1), t("L2", "L2", 60, 10, Role.L2), t("L3", "L3", 100, 10, Role.L3),
            t("N", "N", 140, 10, Role.N), t("PE", "PE", 80, 110, Role.PE),
        ),
    ),
    N_BAR("Szyna N", 264f, 36f, Category.CONNECTOR, row(18, 1, Role.N, (0 until 8).map { 20 + it * 32 })),
    PE_BAR("Szyna PE", 264f, 36f, Category.CONNECTOR, row(18, 1, Role.PE, (0 until 8).map { 20 + it * 32 })),
    CIRCUIT_1P(
        "Obwód odbiorczy", 150f, 70f, Category.LOAD,
        listOf(t("L", "L", 30, 14, Role.L, 1), t("N", "N", 75, 14, Role.N, 1), t("PE", "PE", 120, 14, Role.PE, 1)),
    ),
    CIRCUIT_3P(
        "Obwód 3-fazowy", 220f, 70f, Category.LOAD,
        listOf(
            t("L1", "L1", 25, 14, Role.L1, 1), t("L2", "L2", 65, 14, Role.L2, 1), t("L3", "L3", 105, 14, Role.L3, 1),
            t("N", "N", 150, 14, Role.N, 1), t("PE", "PE", 195, 14, Role.PE, 1),
        ),
    );

    val isUserControl get() = category == Category.CONTROL || this == MOTION_SENSOR
    val isSwitchgear get() = category == Category.SWITCHGEAR
    val isRcd get() = this == RCD_2P || this == RCD_4P
    val isMcb get() = this == MCB_1P || this == MCB_3P
    val isMainSwitch get() = this == MAIN_SWITCH_2P || this == MAIN_SWITCH_4P
    val isSupply get() = this == SUPPLY_1P || this == SUPPLY_3P
    val isBar get() = this == N_BAR || this == PE_BAR
    val isConnector get() = category == Category.CONNECTOR

    fun terminal(id: String): TerminalDef = terminals.first { it.id == id }

    /** Pary zacisków (wejście → wyjście) biegunów aparatu modułowego. */
    val poles: List<Pair<String, String>>
        get() = when (this) {
            MAIN_SWITCH_2P, RCD_2P -> listOf("1" to "2", "N" to "N'")
            MAIN_SWITCH_4P, RCD_4P -> listOf("1" to "2", "3" to "4", "5" to "6", "N" to "N'")
            MCB_1P -> listOf("1" to "2")
            MCB_3P -> listOf("1" to "2", "3" to "4", "5" to "6")
            else -> emptyList()
        }
}

private fun fourPole() = listOf(
    TerminalDef("1", "1", 20f, 10f, Role.L1), TerminalDef("3", "3", 60f, 10f, Role.L2),
    TerminalDef("5", "5", 100f, 10f, Role.L3), TerminalDef("N", "N", 140f, 10f, Role.N),
    TerminalDef("2", "2", 20f, 110f, Role.L1), TerminalDef("4", "4", 60f, 110f, Role.L2),
    TerminalDef("6", "6", 100f, 110f, Role.L3), TerminalDef("N'", "N", 140f, 110f, Role.N),
)

/** Wymagania dla obwodu odbiorczego w rozdzielnicy. */
data class CircuitSpec(
    val name: String,
    val minA: Int,
    val maxA: Int,
    val needsRcd: Boolean,
    val rcdMaxmA: Int = 30,
)

/** Element ustawiony na planszy poziomu. */
data class Part(
    val id: String,
    val kind: Kind,
    val x: Float,
    val y: Float,
    val label: String = kind.title,
    val spec: DeviceSpec? = null,
    /** Jeśli niepuste – gracz musi wybrać wariant aparatu / odbiornika. */
    val options: List<DeviceSpec> = emptyList(),
    val circuit: CircuitSpec? = null,
) {
    fun effectiveSpec(choices: Map<String, String>): DeviceSpec? =
        if (options.isEmpty()) spec else options.firstOrNull { it.id == choices[id] }

    fun terminalRef(id: String) = TermRef(this.id, id)
}
