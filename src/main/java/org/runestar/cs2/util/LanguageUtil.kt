package org.runestar.cs2.util

private val QUOTED_IDENTIFIER = Regex("[^a-zA-Z0-9_:]")

fun String.asConstant() = "^$this"
fun String.asIdentifier() = if (contains(QUOTED_IDENTIFIER)) "\"$this\"" else this