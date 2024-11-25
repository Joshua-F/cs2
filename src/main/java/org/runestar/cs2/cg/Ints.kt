package org.runestar.cs2.cg

import org.runestar.cs2.BOOLEAN_NAMES
import org.runestar.cs2.CHATFILTER_NAMES
import org.runestar.cs2.CHATTYPE_NAMES
import org.runestar.cs2.CLANTYPE_NAMES
import org.runestar.cs2.CLIENTTYPE_NAMES
import org.runestar.cs2.DBCOLUMN_NAMES
import org.runestar.cs2.DBTABLE_NAMES
import org.runestar.cs2.DEVICEOPTION_NAMES
import org.runestar.cs2.FONTMETRICS_NAMES
import org.runestar.cs2.GAMEOPTION_NAMES
import org.runestar.cs2.GRAPHIC_NAMES
import org.runestar.cs2.IFTYPE_NAMES
import org.runestar.cs2.INTERFACE_NAMES
import org.runestar.cs2.INV_NAMES
import org.runestar.cs2.KEY_NAMES
import org.runestar.cs2.LOCSHAPE_NAMES
import org.runestar.cs2.LOC_NAMES
import org.runestar.cs2.MAPAREA_NAMES
import org.runestar.cs2.MINIMENU_ENTRY_TYPE_NAMES
import org.runestar.cs2.MODEL_NAMES
import org.runestar.cs2.NPC_NAMES
import org.runestar.cs2.OBJ_NAMES
import org.runestar.cs2.PARAM_NAMES
import org.runestar.cs2.PLATFORMTYPE_NAMES
import org.runestar.cs2.SEQ_NAMES
import org.runestar.cs2.SETPOSH_NAMES
import org.runestar.cs2.SETPOSV_NAMES
import org.runestar.cs2.SETSIZE_NAMES
import org.runestar.cs2.SETTEXTALIGNH_NAMES
import org.runestar.cs2.SETTEXTALIGNV_NAMES
import org.runestar.cs2.SETTING_NAMES
import org.runestar.cs2.STAT_NAMES
import org.runestar.cs2.STRUCT_NAMES
import org.runestar.cs2.SYNTH_NAMES
import org.runestar.cs2.util.Loader
import org.runestar.cs2.bin.Type
import org.runestar.cs2.WINDOWMODE_NAMES
import org.runestar.cs2.ir.*
import org.runestar.cs2.util.loadNotNull
import org.runestar.cs2.util.map
import org.runestar.cs2.util.mapIndexed
import org.runestar.cs2.util.orElse
import java.util.TreeMap

private val VALUE = Loader { it.toString() }

private val NULL = Loader { if (it == -1) "null" else null }

private fun Loader<String>.quote() = map {
    if (it.contains(" ") || it.contains(",")) '"' + it + '"' else it
}

private fun Loader<String>.prefix(prefix: String) = map { prefix + it }

private val COORDS = Loader {
    val plane = it ushr 28
    val x = (it ushr 14) and 0x3FFF
    val z = it and 0x3FFF
    "${plane}_${x / 64}_${z / 64}_${x and 0x3F}_${z and 0x3F}"
}

private val COLOUR_CONSTANTS = Loader(mapOf(
        0xFF0000 to "^red",
        0x00FF00 to "^green",
        0x0000FF to "^blue",
        0xFFFF00 to "^yellow",
        0xFF00FF to "^magenta",
        0x00FFFF to "^cyan",
        0xFFFFFF to "^white",
        0x000000 to "^black"
))

private val COLOURS = Loader {
    check((it shr 24) == 0)
    "0x%06x".format(it)
}

private val INT_CONSTANTS = Loader(mapOf(
        Int.MAX_VALUE to "^max_32bit_int",
        Int.MIN_VALUE to "^min_32bit_int"
))

private val INTERFACES = unique(INTERFACE, INTERFACE_NAMES)
private val TOPLEVELINTERFACES = unique(TOPLEVELINTERFACE, INTERFACE_NAMES)
private val OVERLAYINTERFACES = unique(OVERLAYINTERFACE, INTERFACE_NAMES)
private val DBTABLES = unique(DBTABLE, DBTABLE_NAMES)

private val COMPONENTS = Loader { INTERFACES.loadNotNull(it shr 16) + ':' + (it and 0xFFFF) }
private val DBCOLUMNS = Loader {
    val tableName = DBTABLES.loadNotNull(it shr 12)
    val columnName = DBCOLUMN_NAMES.load(it and 0xf.inv()) ?: (it shr 4 and 0xFF)
    "$tableName:$columnName"
}

private fun cst(prefix: String, loader: Loader<String>) = loader.prefix('^' + prefix + '_').orElse(NULL).orElse(VALUE)

private fun Loader<String>.idSuffix(separator: String = "_") = mapIndexed { id, n -> n + separator + id }

private fun unique(prototype: Prototype, loader: Loader<String>) = loader.orElse(unknown(prototype))

private fun uniqueExhaustive(loader: Loader<String>) = loader.orElse(NULL)

private fun unknown(prototype: Prototype, separator: String = "_") = NULL.orElse(Loader(prototype.identifier).idSuffix(separator))

private fun nonUnique(prototype: Prototype, loader: Loader<String>) = NULL.orElse(loader.orElse(Loader(prototype.identifier)).idSuffix())

private val PROTOTYPES = HashMap<Prototype, Loader<String>>().apply {
    this[INT] = INT_CONSTANTS.orElse(VALUE)
    this[COORD] = NULL.orElse(COORDS)
    this[COLOUR] = NULL.orElse(COLOUR_CONSTANTS).orElse(COLOURS)
    this[COMPONENT] = NULL.orElse(COMPONENTS)
    this[ENTITYOVERLAY] = NULL.orElse(VALUE)
    this[TYPE] = Loader { Type.of(it.toByte()).literal }
    this[BOOL] = BOOLEAN_NAMES.prefix("^").orElse(NULL)
    this[GRAPHIC] = NULL.orElse(GRAPHIC_NAMES.orElse(Loader(GRAPHIC.identifier).idSuffix()).quote())
    this[NPC_UID] = NULL.orElse(VALUE)
    this[PLAYER_UID] = NULL.orElse(VALUE)

    this[ENUM] = unknown(ENUM)
    this[CATEGORY] = unknown(CATEGORY)
    this[MAPELEMENT] = unknown(MAPELEMENT)

    this[CHAR] = NULL
    this[AREA] = NULL

    this[BOOLEAN] = uniqueExhaustive(BOOLEAN_NAMES)
    this[STAT] = uniqueExhaustive(STAT_NAMES)
    this[MAPAREA] = uniqueExhaustive(MAPAREA_NAMES)
    this[FONTMETRICS] = uniqueExhaustive(FONTMETRICS_NAMES)

    this[INV] = unique(INV, INV_NAMES)
    this[SYNTH] = unique(SYNTH, SYNTH_NAMES)
    this[PARAM] = unique(PARAM, PARAM_NAMES)
    this[INTERFACE] = INTERFACES
    this[TOPLEVELINTERFACE] = TOPLEVELINTERFACES
    this[OVERLAYINTERFACE] = OVERLAYINTERFACES

    this[OBJ] = nonUnique(OBJ, OBJ_NAMES)
    this[LOC] = nonUnique(LOC, LOC_NAMES)
    this[LOCSHAPE] = uniqueExhaustive(LOCSHAPE_NAMES)
    this[MODEL] = nonUnique(MODEL, MODEL_NAMES)
    this[STRUCT] = nonUnique(STRUCT, STRUCT_NAMES)
    this[NPC] = nonUnique(NPC, NPC_NAMES)
    this[SEQ] = nonUnique(SEQ, SEQ_NAMES)
    this[NAMEDOBJ] = getValue(OBJ)

    this[KEY] = cst(KEY.identifier, KEY_NAMES)
    this[IFTYPE] = cst(IFTYPE.identifier, IFTYPE_NAMES)
    this[SETSIZE] = cst(SETSIZE.identifier, SETSIZE_NAMES)
    this[SETPOSH] = cst("setpos", SETPOSH_NAMES)
    this[SETPOSV] = cst("setpos", SETPOSV_NAMES)
    this[SETTEXTALIGNH] = cst("settextalign", SETTEXTALIGNH_NAMES)
    this[SETTEXTALIGNV] = cst("settextalign", SETTEXTALIGNV_NAMES)
    this[CHATTYPE] = cst(CHATTYPE.identifier, CHATTYPE_NAMES)
    this[WINDOWMODE] = cst(WINDOWMODE.identifier, WINDOWMODE_NAMES)
    this[CLIENTTYPE] = cst(CLIENTTYPE.identifier, CLIENTTYPE_NAMES)
    this[CHATFILTER] = cst(CHATFILTER.identifier, CHATFILTER_NAMES)
    this[PLATFORMTYPE] = cst(PLATFORMTYPE.identifier, PLATFORMTYPE_NAMES)
    this[CLANTYPE] = cst(CLANTYPE.identifier, CLANTYPE_NAMES)
    this[CLANSLOT] = NULL.orElse(VALUE)
    this[MINIMENU_ENTRY_TYPE] = cst(MINIMENU_ENTRY_TYPE.identifier, MINIMENU_ENTRY_TYPE_NAMES)
    this[DEVICEOPTION] = cst(DEVICEOPTION.identifier, DEVICEOPTION_NAMES)
    this[GAMEOPTION] = cst(GAMEOPTION.identifier, GAMEOPTION_NAMES)
    this[SETTING] = cst(SETTING.identifier, SETTING_NAMES)

    this[DBROW] = unknown(DBROW)
    this[DBTABLE] = DBTABLES
    this[DBCOLUMN] = NULL.orElse(DBCOLUMNS)

    this[STRINGVECTOR] = unknown(STRINGVECTOR)
    this[VARP] = unknown(VARP)

    this[SCRIPT] = unknown(SCRIPT, separator = "")
}

val TYPE_SYMBOLS = mutableMapOf<Type, TreeMap<Int, String>>()
fun intConstantToString(n: Int, prototype: Prototype): String {
    val name = (PROTOTYPES[prototype] ?: PROTOTYPES.getValue(Prototype(prototype.type))).loadNotNull(n)

    // store the name for possible dumping later
    val names = TYPE_SYMBOLS.getOrPut(prototype.type) { TreeMap() }
    names[n] = name

    return name
}