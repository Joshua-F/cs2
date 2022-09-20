package org.runestar.cs2

import org.runestar.cs2.bin.ScriptName
import org.runestar.cs2.bin.StackType
import org.runestar.cs2.bin.Type
import org.runestar.cs2.ir.PROTOTYPE_LOOKUP_TABLE
import org.runestar.cs2.ir.Prototype
import org.runestar.cs2.util.Loader
import org.runestar.cs2.util.orElse
import org.runestar.cs2.util.thisClass

private fun <T : Any> readLoader(fileName: String, keyMapper: (String) -> Int = { it.toInt() }, valueMapper: (String) -> T): Loader.Map<T> {
    val map = HashMap<Int, T>()
    thisClass.getResourceAsStream(fileName).bufferedReader().use {
        while (true) {
            val line = it.readLine() ?: break
            val tab = line.indexOf('\t')
            val id = keyMapper(line.substring(0, tab))
            val v = valueMapper(line.substring(tab + 1))
            check(map.put(id, v) == null)
        }
    }
    return Loader(map)
}

private fun readNames(fileName: String, keyMapper: (String) -> Int = { it.toInt() }): Loader.Map<String> = readLoader(fileName, keyMapper) { it }
private fun readPrototype(fileName: String): Loader.Map<Prototype> = readLoader(fileName) { PROTOTYPE_LOOKUP_TABLE[it] ?: Prototype(Type.of(it)) }
private fun readPrototypeArray(fileName: String): Loader.Map<Array<Prototype>> = readLoader(fileName) {
    it.split(",").map {
            split -> PROTOTYPE_LOOKUP_TABLE[split] ?: Prototype(Type.of(split))
    }.toTypedArray()
}

val DBTABLE_TYPES = readPrototypeArray("dbtable-types.tsv")
val PARAM_TYPES = readPrototype("param-types-override.tsv").orElse(readPrototype("param-types.tsv"))

val BOOLEAN_NAMES = readNames("boolean-names.tsv")
val FONTMETRICS_NAMES = readNames("fontmetrics-names.tsv")
val GRAPHIC_NAMES = readNames("graphic-names.tsv")
val INTERFACE_NAMES = readNames("interface-names.tsv")
val DBTABLE_NAMES = readNames("dbtable-names.tsv")
val DBCOLUMN_NAMES = readNames("dbcolumn-names.tsv") {
    val split = it.split(",");
    split[0].toInt() shl 12 or split[1].toInt() shl 4
}
val INV_NAMES = readNames("inv-names.tsv")
val LOC_NAMES = readNames("loc-names.tsv")
val MAPAREA_NAMES = readNames("maparea-names.tsv")
val MODEL_NAMES = readNames("model-names.tsv")
val NPC_NAMES = readNames("npc-names.tsv")
val OBJ_NAMES = readNames("obj-names.tsv")
val PARAM_NAMES = readNames("param-names.tsv")
val SEQ_NAMES = readNames("seq-names.tsv")
val STAT_NAMES = readNames("stat-names.tsv")
val STRUCT_NAMES = readNames("struct-names.tsv")
val SYNTH_NAMES = readNames("synth-names.tsv")
val LOCSHAPE_NAMES = readNames("locshape-names.tsv")

val CHATFILTER_NAMES = readNames("chatfilter-names.tsv")
val CHATTYPE_NAMES = readNames("chattype-names.tsv")
val CLIENTTYPE_NAMES = readNames("clienttype-names.tsv")
val IFTYPE_NAMES = readNames("iftype-names.tsv")
val KEY_NAMES = readNames("key-names.tsv")
val SETPOSH_NAMES = readNames("setposh-names.tsv")
val SETPOSV_NAMES = readNames("setposv-names.tsv")
val SETSIZE_NAMES = readNames("setsize-names.tsv")
val SETTEXTALIGNH_NAMES = readNames("settextalignh-names.tsv")
val SETTEXTALIGNV_NAMES = readNames("settextalignv-names.tsv")
val WINDOWMODE_NAMES = readNames("windowmode-names.tsv")
val PLATFORMTYPE_NAMES = readNames("platformtype-names.tsv")
val CLANTYPE_NAMES = readNames("clantype-names.tsv")
val MINIMENU_ENTRY_TYPE_NAMES = readNames("minimenu-entry-type-names.tsv")
val DEVICEOPTION_NAMES = readNames("deviceoption-names.tsv")
val GAMEOPTION_NAMES = readNames("gameoption-names.tsv")
val SETTING_NAMES = readNames("setting-names.tsv")
val VARP_NAMES = readNames("varp-names.tsv").orElse(readNames("varp-names-unconfirmed.tsv"))
val VARC_NAMES = readNames("varc-names.tsv").orElse(readNames("varc-names-unconfirmed.tsv"))
val VARBIT_NAMES = readNames("varbit-names.tsv").orElse(readNames("varbit-names-unconfirmed.tsv"))

// must be at the bottom since it relies on other type names
val SCRIPT_NAMES = readLoader("script-names.tsv") { ScriptName(it) }
val SCRIPT_ARGS = readLoader("script-arguments.tsv") { it.split(",").map { literal -> PROTOTYPE_LOOKUP_TABLE[literal] ?: Prototype(Type.of(literal)) } }