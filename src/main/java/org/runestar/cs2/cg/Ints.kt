package org.runestar.cs2.cg

import org.runestar.cs2.*
import org.runestar.cs2.bin.Type
import org.runestar.cs2.ir.*
import org.runestar.cs2.util.*
import java.util.TreeMap

private val VALUE = IntLoader { it.toString() }

private val NULL = IntLoader { if (it == -1) "null" else null }

private fun IntLoader<String>.quote() = map { '"' + it + '"' }

private fun IntLoader<String>.prefix(prefix: String) = map { prefix + it }

private val COORDS = IntLoader {
    val plane = it ushr 28
    val x = (it ushr 14) and 0x3FFF
    val z = it and 0x3FFF
    "${plane}_${x / 64}_${z / 64}_${x and 0x3F}_${z and 0x3F}"
}

private val COLOUR_CONSTANTS = loader(mapOf(
        0xFF0000 to "^red",
        0x00FF00 to "^green",
        0x0000FF to "^blue",
        0xFFFF00 to "^yellow",
        0xFF00FF to "^magenta",
        0x00FFFF to "^cyan",
        0xFFFFFF to "^white",
        0x000000 to "^black"
))

private val COLOURS = IntLoader {
    check((it shr 24) == 0)
    "0x%06x".format(it)
}

private val INT_CONSTANTS = loader(mapOf(
        Int.MAX_VALUE to "^max_32bit_int",
        Int.MIN_VALUE to "^min_32bit_int"
))

private val INTERFACES = unique(INTERFACE, INTERFACE_NAMES)
private val TOPLEVELINTERFACES = unique(TOPLEVELINTERFACE, INTERFACE_NAMES)
private val OVERLAYINTERFACES = unique(OVERLAYINTERFACE, INTERFACE_NAMES)
private val DBTABLES = unique(DBTABLE, DBTABLE_NAMES)

private val COMPONENTS = IntLoader { INTERFACES.loadNotNull(it shr 16) + ':' + (it and 0xFFFF) }
private val DBCOLUMNS = IntLoader {
    val tableName = DBTABLES.loadNotNull(it shr 12)
    val columnName = DBCOLUMN_NAMES.load(it and 0xf.inv()) ?: (it shr 4 and 0xFF)
    "$tableName:$columnName"
}

private fun cst(prefix: String, loader: IntLoader<String>) = loader.prefix('^' + prefix + '_').orElse(NULL).orElse(VALUE)

private fun IntLoader<String>.idSuffix() = mapIndexed { id, n -> n + '_' + id }

private fun unique(prototype: Prototype, loader: IntLoader<String>) = loader.orElse(unknown(prototype))

private fun uniqueExhaustive(loader: IntLoader<String>) = loader.orElse(NULL)

private fun unknown(prototype: Prototype) = NULL.orElse(intLoader(prototype.identifier).idSuffix())

private fun nonUnique(prototype: Prototype, loader: IntLoader<String>) = NULL.orElse(loader.orElse(intLoader(prototype.identifier)).idSuffix())

private val PROTOTYPES = HashMap<Prototype, IntLoader<String>>().apply {
    this[INT] = INT_CONSTANTS.orElse(VALUE)
    this[COORD] = NULL.orElse(COORDS)
    this[COLOUR] = NULL.orElse(COLOUR_CONSTANTS).orElse(COLOURS)
    this[COMPONENT] = NULL.orElse(COMPONENTS)
    this[ENTITYOVERLAY] = NULL.orElse(VALUE)
    this[TYPE] = IntLoader { Type.of(it.toByte()).literal }
    this[BOOL] = BOOLEAN_NAMES.prefix("^").orElse(NULL)
    val graphicNameLoader = GRAPHIC_NAMES.orElse(intLoader(GRAPHIC.identifier).idSuffix())
    this[GRAPHIC] = NULL.orElse {
        val graphicName = graphicNameLoader.loadNotNull(it)
        val constantName = GRAPHIC_CONSTANT_NAMES.load(graphicName)
        if (constantName != null) {
            constantName.asConstant()
        } else {
            graphicName.asIdentifier()
        }
    }
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
}

val TYPE_SYMBOLS = mutableMapOf<Type, TreeMap<Int, String>>()
fun intConstantToString(n: Int, prototype: Prototype): String {
    val name = (PROTOTYPES[prototype] ?: PROTOTYPES.getValue(Prototype(prototype.type))).loadNotNull(n)

    // store the name for possible dumping later
    val names = TYPE_SYMBOLS.getOrPut(prototype.type) { TreeMap() }
    names[n] = name

    return name
}