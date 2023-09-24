package org.runestar.cs2.bin

import org.runestar.cs2.util.CP1252
import org.runestar.cs2.util.toByte

enum class Type(desc: Char = 0.toChar(), literal: String? = null) {

    AREA('R'),
    BOOLEAN('1'),
    CATEGORY('y'),
    CHAR('z'),
    CHATCAT('k'),
    CHATPHRASE('e'),
    COMPONENT('I'),
    CURSOR('@'),
    COORD('c'),
    DBROW('Ð'),
    ENUM('g'),
    FONTMETRICS('f'),
    GRAPHIC('d'),
    IDKIT('K'),
    INT('i'),
    INTERFACE('a'),
    INV('v'),
    STRINGVECTOR('¸'),
    LOC('l'),
    LOCSHAPE('H'),
    MAPAREA('`', "wma"),
    MAPELEMENT('µ'),
    MIDI('M'),
    MODEL('m'),
    NAMEDOBJ('O'),
    ENTITYOVERLAY('-'),
    NPC('n'),
    NPC_UID('u'),
    OBJ('o'),
    OVERLAYINTERFACE('L'),
    PARAM,
    PLAYER_UID('p'),
    SEQ('A'),
    SPOTANIM('t'),
    STAT('S'),
    STRING('s'),
    STRUCT('J'),
    SYNTH('P'),
    TOPLEVELINTERFACE('F'),
    TYPE,
    DBTABLE,
    DBCOLUMN,
    ;

    val desc = desc.toByte(CP1252)

    val stackType get() = if (this == STRING) StackType.STRING else StackType.INT

    val literal = literal ?: name.toLowerCase()

    override fun toString() = literal

    companion object {

        private val VALUES = values().filter { it.desc != 0.toByte() }.associateBy { it.desc }
        private val VALUES_BY_LITERAL = values().associateBy { it.literal }

        fun of(desc: Byte): Type = VALUES.getValue(desc)
        fun of(literal: String): Type = VALUES_BY_LITERAL.getValue(literal)

        fun union(types: Set<Type>): Type? {
            when (types.size) {
                0 -> return null
                1 -> return types.iterator().next()
                2 -> {
                    if (OBJ in types && NAMEDOBJ in types) return OBJ
                    if (FONTMETRICS in types && GRAPHIC in types) return FONTMETRICS
                }
            }
            error(types)
        }

        fun intersection(types: Set<Type>): Type? {
            when (types.size) {
                0 -> return null
                1 -> return types.iterator().next()
                2 -> {
                    if (OBJ in types && NAMEDOBJ in types) return NAMEDOBJ
                    if (FONTMETRICS in types && GRAPHIC in types) return GRAPHIC
                }
            }
            error(types)
        }
    }
}