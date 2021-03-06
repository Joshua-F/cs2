package org.runestar.cs2.cg

import org.runestar.cs2.Opcodes
import org.runestar.cs2.TopType
import org.runestar.cs2.Type
import org.runestar.cs2.bin.NameLoader
import org.runestar.cs2.cfa.Construct
import org.runestar.cs2.cfa.reconstruct
import org.runestar.cs2.ir.Expr
import org.runestar.cs2.ir.Func
import org.runestar.cs2.ir.Insn
import org.runestar.cs2.names
import org.runestar.cs2.util.trim
import java.lang.Appendable
import java.lang.IllegalStateException

internal class Generator(
        val fontNameLoader: NameLoader,
        val graphicNameLoader: NameLoader,
        val scriptNameLoader: NameLoader
) {

    internal fun write(
            appendable: Appendable,
            func: Func
    ) {
        val writer = LineWriter(appendable)
        val root = reconstruct(func)
        val scriptName = scriptNameLoader.load(func.id)
        if (scriptName == null) {
            writer.append("script").append(func.id.toString())
        } else {
            writer.append(scriptName)
        }
        writer.append('(')
        func.args.joinTo(writer) { "${it.type.literal} \$${it.name}" }
        writer.append(")(")
        func.returns.joinTo(writer) { it.literal }
        writer.append(") {")
        writer.indents++
        writeConstruct(writer, root)
        writer.indents--
        writer.nextLine().append('}')
        writer.indents--
        writer.nextLine()
    }

    private fun writeConstruct(writer: LineWriter, construct: Construct) {
        when (construct) {
            is Construct.Seq -> writeSeq(writer, construct)
            is Construct.If -> writeIf(writer, construct)
            is Construct.While -> writeWhile(writer, construct)
            is Construct.Switch -> writeSwitch(writer, construct)
        }
    }

    private fun writeSeq(writer: LineWriter, construct: Construct.Seq) {
        for (insn in construct.insns) {
            writer.nextLine()
            writeInsn(writer, insn)
        }
        construct.next?.let { writeConstruct(writer, it) }
    }

    private fun writeIf(writer: LineWriter, construct: Construct.If) {
        writer.nextLine()
        val if0 = construct.branches.first()
        writer.append("if (")
        writeExpr(writer, if0.condition)
        writer.append(") {")
        writer.indents++
        writeConstruct(writer, if0.construct)
        writer.indents--
        writer.nextLine()
        writer.append('}')
        for (ifn in construct.branches.drop(1)) {
            writer.append(" else if (")
            writeExpr(writer, ifn.condition)
            writer.append(") {")
            writer.indents++
            writeConstruct(writer, ifn.construct)
            writer.indents--
            writer.nextLine()
            writer.append('}')
        }
        val elze = construct.elze
        if (elze != null) {
            writer.append(" else {")
            writer.indents++
            writeConstruct(writer, elze)
            writer.indents--
            writer.nextLine()
            writer.append('}')
        }
        construct.next?.let { writeConstruct(writer, it) }
    }

    private fun writeWhile(writer: LineWriter, construct: Construct.While) {
        writer.nextLine()
        writer.append("while (")
        writeExpr(writer, construct.condition)
        writer.append(") {")
        writer.indents++
        writeConstruct(writer, construct.inside)
        writer.indents--
        writer.nextLine()
        writer.append('}')
        construct.next?.let { writeConstruct(writer, it) }
    }

    private fun writeSwitch(writer: LineWriter, construct: Construct.Switch) {
        writer.nextLine()
        writer.append("switch (")
        writeExpr(writer, construct.expr)
        writer.append(") {")
        for ((ns, con) in construct.map) {
            writer.indents++
            writer.nextLine()
            val itr = ns.iterator()
            writeConstantInt(writer, itr.next(), construct.expr.type)
            while (itr.hasNext()) {
                writer.append(", ")
                writeConstantInt(writer, itr.next(), construct.expr.type)
            }
            writer.append(" : {")
            writer.indents++
            writeConstruct(writer, con)
            writer.indents--
            writer.nextLine()
            writer.append('}')
            writer.indents--
        }
        val elze = construct.elze
        if (elze != null) {
            writer.indents++
            writer.nextLine()
            writer.append("else : {")
            writer.indents++
            writeConstruct(writer, elze)
            writer.indents--
            writer.nextLine()
            writer.append('}')
            writer.indents--
        }
        writer.nextLine()
        writer.append('}')
        construct.next?.let { writeConstruct(writer, it) }
    }

    private fun writeInsn(writer: LineWriter, insn: Insn) {
        when (insn) {
            is Insn.Assignment -> writeAssignment(writer, insn)
            is Insn.Return -> writeReturn(writer, insn)
        }
    }

    private fun writeAssignment(writer: LineWriter, insn: Insn.Assignment) {
        val defs = insn.definitions.iterator()
        if (defs.hasNext()) {
            writeExpr(writer, defs.next())
        }
        while (defs.hasNext()) {
            writer.append(", ")
            writeExpr(writer, defs.next())
        }
        if (insn.definitions.isNotEmpty()) {
            writer.append(" = ")
        }
        writeExpr(writer, insn.expr)
    }

    private fun writeReturn(writer: LineWriter, insn: Insn.Return) {
        writeOperation(writer, insn.expr as Expr.Operation)
    }

    private fun writeExpr(writer: LineWriter, expr: Expr) {
        when (expr) {
            is Expr.Var -> writeVar(writer, expr)
            is Expr.Cst -> writeConst(writer, expr)
            is Expr.Operation -> writeOperation(writer, expr)
        }
    }

    private fun writeVar(writer: LineWriter, expr: Expr.Var) {
        writer.append('$').append(expr.name)
    }

    private fun writeConst(writer: LineWriter, expr: Expr.Cst) {
        when (expr.type.topType) {
            TopType.INT -> writeConstantInt(writer, expr.cst as Int, expr.type)
            TopType.STRING -> writeConstantString(writer, expr.cst as String)
        }
    }

    private fun writeConstantInt(writer: LineWriter, n: Int, type: Type) {
        when (type) {
            Type.TYPE -> writer.append(Type.of(n).literal)
            Type.COMPONENT -> {
                when (n) {
                    -1 -> writer.append("-1")
                    -2147483645 -> writer.append("?selected")
                    -2147483642 -> writer.append("?drag_target")
                    else -> writer.append("${n ushr 16}").append(':').append("${n and 0xFFFF}")
                }
            }
            Type.BOOLEAN -> when (n) {
                0 -> writer.append("false")
                1 -> writer.append("true")
                -1 -> writer.append("-1")
                else -> error(n)
            }
            Type.COORDGRID -> when (n) {
                -1 -> writer.append("-1")
                else -> writer.append("${n ushr 28}").append(':').append("${(n ushr 14) and 0x3FFF}").append(':').append("${n and 0x3FFF}")
            }
            Type.FONTMETRICS -> writeNamedInt(writer, fontNameLoader, n)
            Type.GRAPHIC -> writeNamedInt(writer, graphicNameLoader, n)
            Type.COLOUR -> {
                if (n == 0) {
                    writer.append('0')
                } else {
                    writer.append("0x").append(n.toString(16).toUpperCase().padStart(6, '0'))
                }
            }
            Type.INT -> {
                when (n) {
                    Int.MAX_VALUE -> writer.append("^int_max")
                    Int.MIN_VALUE -> writer.append("^int_min")
                    -2147483647 -> writer.append("?mouse_x")
                    -2147483646 -> writer.append("?mouse_y")
                    -2147483644 -> writer.append("?op_index")
                    -2147483643 -> writer.append("?selected_id")
                    -2147483641 -> writer.append("?drag_target_id")
                    -2147483640 -> writer.append("?key_typed")
                    else -> writer.append(n.toString())
                }
            }
            Type.CHAR -> {
                when (n) {
                    -2147483639 -> writer.append("?key_pressed")
                    -1 -> writer.append("-1")
                    else -> error(n)
                }
            }
            else -> writer.append(n.toString())
        }
    }

    private fun writeNamedInt(writer: LineWriter, nameLoader: NameLoader, n: Int) {
        val name = nameLoader.load(n)
        if (name == null) {
            writer.append(n.toString())
        } else {
            writer.append("#\"").append(name).append('"')
        }
    }

    private fun writeConstantString(writer: LineWriter, s: String) {
        when (s) {
            "event_opbase" -> writer.append("?op_base")
            else -> writer.append('"').append(s).append('"')
        }
    }

    private fun writeOperation(writer: LineWriter, expr: Expr.Operation) {
        val op = expr.id
        if (op == Opcodes.INVOKE) {
            writeInvoke(writer, expr)
            return
        } else if (
                (op >= Opcodes.CC_SETONCLICK && op <= Opcodes.CC_SETONRESIZE) ||
                (op >= Opcodes.IF_SETONCLICK && op <= Opcodes.IF_SETONRESIZE)
        ) {
            writeAddHook(writer, expr)
            return
        }
        if (op == Opcodes.BRANCH_EQUALS) {
            val right = expr.arguments[1]
            if (right is Expr.Cst && right.type == Type.BOOLEAN) {
                when (right.cst) {
                    1 -> {}
                    0 -> writer.append('!')
                    else -> throw IllegalStateException()
                }
                writeExpr(writer, expr.arguments[0])
                return
            }
        }
        val infixSym = INFIX_MAP[expr.id]
        if (infixSym != null) {
            val a = expr.arguments[0]
            if (a is Expr.Operation && a.id in INFIX_MAP) {
                writer.append('(')
                writeExpr(writer, a)
                writer.append(')')
            } else {
                writeExpr(writer, a)
            }

            writer.append(' ').append(infixSym).append(' ')

            val b = expr.arguments[1]
            if (b is Expr.Operation && b.id in INFIX_MAP) {
                writer.append('(')
                writeExpr(writer, b)
                writer.append(')')
            } else {
                writeExpr(writer, b)
            }

        } else {
            writer.append(names.getValue(op))
            writer.append('(')
            val args = expr.arguments.iterator()
            if (args.hasNext()) {
                writeExpr(writer, args.next())
            }
            while (args.hasNext()) {
                writer.append(", ")
                writeExpr(writer, args.next())
            }
            writer.append(')')
        }
    }

    private fun writeAddHook(writer: LineWriter, operation: Expr.Operation) {
        writer.append(names[operation.id]).append('(')

        val args = operation.arguments.toMutableList()
        val component = args.removeAt(args.lastIndex)
        writeExpr(writer, component)

        val invokeId = (args[0] as Expr.Cst).cst as Int
        if (invokeId == -1) {
            writer.append(", -1)")
            return
        }

        val scriptName = scriptNameLoader.load(invokeId)
        val triggerCount = (args.removeAt(args.lastIndex) as Expr.Cst).cst as Int
        val triggers = args.takeLast(triggerCount)
        repeat(triggerCount) { args.removeAt(args.lastIndex) }

        writer.append(", &")
        if (scriptName == null) {
            writer.append("script").append(invokeId.toString())
        } else {
            writer.append(scriptName.trim("[clientscript,", ']'))
        }
        writer.append('(')
        val scriptArgs = args.iterator()
        scriptArgs.next()
        if (scriptArgs.hasNext()) {
            writeExpr(writer, scriptArgs.next())
        }
        while (scriptArgs.hasNext()) {
            writer.append(", ")
            writeExpr(writer, scriptArgs.next())
        }
        writer.append(')')

        for (trigger in triggers) {
            writer.append(", ")
            writeExpr(writer, trigger)
        }

        writer.append(')')
    }

    private fun writeInvoke(writer: LineWriter, invoke: Expr.Operation) {
        writer.append('~')
        val invokeId = (invoke.arguments.first() as Expr.Cst).cst as Int
        val scriptName = scriptNameLoader.load(invokeId)
        if (scriptName == null) {
            writer.append("script").append(invokeId.toString())
        } else {
            writer.append(scriptName.trim("[proc,", ']'))
        }
        writer.append('(')
        val args = invoke.arguments.iterator()
        args.next()
        if (args.hasNext()) {
            writeExpr(writer, args.next())
        }
        while (args.hasNext()) {
            writer.append(", ")
            writeExpr(writer, args.next())
        }
        writer.append(')')
    }

    private val INFIX_MAP = mapOf(
            Opcodes.ADD to "+",
            Opcodes.SUB to "-",
            Opcodes.MULTIPLY to "*",
            Opcodes.DIV to "/",
            Opcodes.MOD to "%",
            Opcodes.AND to "&",
            Opcodes.OR to "|",
            Opcodes.APPEND to "+",
            Opcodes.BRANCH_EQUALS to "==",
            Opcodes.BRANCH_GREATER_THAN to ">",
            Opcodes.BRANCH_GREATER_THAN_OR_EQUALS to ">=",
            Opcodes.BRANCH_LESS_THAN to "<",
            Opcodes.BRANCH_LESS_THAN_OR_EQUALS to "<=",
            Opcodes.BRANCH_NOT to "!=",
            Opcodes.SS_OR to "||",
            Opcodes.SS_AND to "&&"
    )
}