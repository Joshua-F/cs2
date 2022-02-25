package org.runestar.cs2.bin

import org.runestar.cs2.ir.LOC
import org.runestar.cs2.ir.MAPELEMENT
import org.runestar.cs2.ir.NPC
import org.runestar.cs2.ir.Prototype

enum class Trigger(val id: Int, val subjectType: Prototype? = null) {

    opworldmapelement1(10, MAPELEMENT),
    opworldmapelement2(11, MAPELEMENT),
    opworldmapelement3(12, MAPELEMENT),
    opworldmapelement4(13, MAPELEMENT),
    opworldmapelement5(14, MAPELEMENT),
    worldmapelementmouseover(15, MAPELEMENT),
    worldmapelementmouseleave(16, MAPELEMENT),
    worldmapelementmouserepeat(17, MAPELEMENT),
    loadnpc(35, NPC),
    loadloc(37, LOC),
    trigger_47(47),
    trigger_48(48),
    trigger_49(49),
    proc(73),
    clientscript(76),
    ;

    companion object {

        private val VALUES = values().associateBy { it.id }

        fun of(id: Int): Trigger = VALUES.getValue(id)
    }
}