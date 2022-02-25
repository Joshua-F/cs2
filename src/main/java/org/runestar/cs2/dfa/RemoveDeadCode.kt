package org.runestar.cs2.dfa

import org.runestar.cs2.ir.Element
import org.runestar.cs2.ir.Function
import org.runestar.cs2.ir.FunctionSet
import org.runestar.cs2.ir.Instruction
import org.runestar.cs2.ir.Variable

object RemoveDeadCode : Phase.Individual() {

    override fun transform(f: Function, fs: FunctionSet) {
        var insn: Instruction? = f.instructions.first
        while (insn != null) {
            if (canTransform(f, insn)) {
                insn = f.instructions.next(insn)
                while (insn != null && insn !is Instruction.Label) {
                    if (insn is Instruction.Assignment) {
                        val def = insn.definitions as Element.Access
                        val v = def.variable as Variable.Stack
                        fs.typings.remove(v)
                        val c = insn.expression as Element.Constant
                        fs.typings.remove(c)
                    }
                    val next = f.instructions.next(insn)
                    f.instructions.remove(insn)
                    insn = next
                }
            } else {
                insn = f.instructions.next(insn)
            }
        }
    }

    private fun canTransform(f: Function, ret: Instruction): Boolean {
        if (ret !is Instruction.Return) {
            return false
        }
        val chain = f.instructions
        if (chain.indexOf(ret) == 0) {
            // A return instruction could never exist at index 0 unless it is a valid return.
            return false
        }
        var insn = chain.next(ret)
        while (insn != null && insn !is Instruction.Label) {
            if (insn is Instruction.Assignment) {
                val def = insn.definitions
                if (def !is Element.Access || def.variable !is Variable.Stack || insn.expression !is Element.Constant) {
                    return false
                }
            }
            insn = f.instructions.next(insn)
        }
        return true
    }
}