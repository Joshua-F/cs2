package org.runestar.cs2.ir

import org.runestar.cs2.bin.ClientScriptDesc
import org.runestar.cs2.bin.StackType
import org.runestar.cs2.bin.Type
import org.runestar.cs2.bin.*
import org.runestar.cs2.ir.COORD as _COORD
import org.runestar.cs2.ir.CLIENTTYPE as _CLIENTTYPE
import org.runestar.cs2.ir.ENUM as _ENUM
import org.runestar.cs2.ir.MES as _MES
import org.runestar.cs2.ir.PLATFORMTYPE as _PLATFORMTYPE
import org.runestar.cs2.ir.STAT as _STAT
import org.runestar.cs2.ir.NPC_UID as _NPC_UID
import org.runestar.cs2.ir.PLAYER_UID as _PLAYER_UID
import org.runestar.cs2.bin.string
import org.runestar.cs2.util.Loader
import org.runestar.cs2.util.loadNotNull

interface Command {

    val id: Int

    fun translate(state: InterpreterState): Instruction

    companion object {

        val COMMANDS: List<Command> get() = ArrayList<Command>().apply {
            add(Switch)
            add(Branch)
            add(Enum)
            add(Proc)
            add(Return)
            add(JoinString)
            add(DefineArray)
            add(PushArrayInt)
            add(PopArrayInt)
            addAll(BranchCompare.values().asList())
            addAll(Discard.values().asList())
            addAll(Assign.values().asList())
            addAll(Basic.values().asList())
            addAll(ClientScript.values().asList())
            addAll(Param.values().asList())
        }

        fun loader(commands: Iterable<Command>): Loader<Command> = Loader(commands.associateBy { it.id })

        val LOADER: Loader<Command> = loader(COMMANDS)
    }

    object Switch : Command {

        override val id = SWITCH

        override fun translate(state: InterpreterState): Instruction {
            return Instruction.Switch(state.pop(StackType.INT), state.switch.mapValues { Instruction.Label(it.value + 1 + state.pc) })
        }
    }

    object Branch : Command {

        override val id = BRANCH

        override fun translate(state: InterpreterState): Instruction {
            return Instruction.Goto(Instruction.Label(state.pc + state.operand.int + 1))
        }
    }

    object Proc : Command {

        override val id = GOSUB_WITH_PARAMS

        override fun translate(state: InterpreterState): Instruction {
            val invokeId = state.operand.int
            state.callGraph.call(state.scriptId, invokeId, Trigger.proc)
            val invoked = state.scripts.loadNotNull(invokeId)
            val args = Expression(state.pop(invoked.intArgumentCount + invoked.stringArgumentCount))
            assign(state.typings.of(args), state.typings.args(invokeId, args.stackTypes))
            val proc = Expression.Proc(invoked.returnTypes, invokeId, args)
            val defs = Expression(state.push(invoked.returnTypes))
            assign(state.typings.of(proc), state.typings.of(defs))
            return Instruction.Assignment(defs, proc)
        }
    }

    object Return : Command {

        override val id = RETURN

        override fun translate(state: InterpreterState): Instruction {
            val e = Expression(state.popAll())
            assign(state.typings.of(e), state.typings.returns(state.scriptId, e.stackTypes))
            return Instruction.Return(e)
        }
    }

    object Enum : Command {

        override val id = ENUM

        override fun translate(state: InterpreterState): Instruction {
            val key = state.pop(StackType.INT)
            val enumId = state.pop(StackType.INT)
            assign(state.typings.of(enumId), state.typings.of(_ENUM))
            val valueType = Type.of((checkNotNull(state.peekValue()).int).toByte())
            val valueTypeVar = state.pop(StackType.INT)
            assign(state.typings.of(valueTypeVar), state.typings.of(TYPE))
            val keyType = Type.of((checkNotNull(state.peekValue()).int).toByte())
            assign(state.typings.of(key), state.typings.of(Prototype(keyType)))
            val keyTypeVar = state.pop(StackType.INT)
            assign(state.typings.of(keyTypeVar), state.typings.of(TYPE))
            val args = Expression(keyTypeVar, valueTypeVar, enumId, key)
            val value = state.push(valueType.stackType)
            val operation = Expression.Operation(listOf(valueType.stackType), id, args)
            val operationTyping = state.typings.of(operation).single()
            operationTyping.freeze(valueType)
            assign(operationTyping, state.typings.of(value))
            return Instruction.Assignment(value, operation)
        }
    }

    enum class BranchCompare : Command {

        BRANCH_NOT,
        BRANCH_EQUALS,
        BRANCH_LESS_THAN,
        BRANCH_GREATER_THAN,
        BRANCH_LESS_THAN_OR_EQUALS,
        BRANCH_GREATER_THAN_OR_EQUALS;

        override val id = opcodes.getValue(name)

        override fun translate(state: InterpreterState): Instruction {
            val right = state.pop(StackType.INT)
            val left = state.pop(StackType.INT)
            compare(state.typings.of(left), state.typings.of(right))
            val expr = Expression.Operation(emptyList(), id, Expression(left, right))
            return Instruction.Branch(expr, Instruction.Label(state.pc + state.operand.int + 1))
        }
    }

    object DefineArray : Command {

        override val id = DEFINE_ARRAY

        override fun translate(state: InterpreterState): Instruction {
            val length = state.pop(StackType.INT)
            val array = Element.Access(Variable.array(state.scriptId, state.operand.int shr 16))
            val arrayType = Type.of(state.operand.int.toByte())
            assign(state.typings.of(length), state.typings.of(LENGTH))
            state.typings.of(array).freeze(arrayType)
            return Instruction.Assignment(Expression.Operation(emptyList(), id, Expression(array, length)))
        }
    }

    object PushArrayInt : Command {

        override val id = PUSH_ARRAY_INT

        override fun translate(state: InterpreterState): Instruction {
            val arrayIndex = state.pop(StackType.INT)
            val array = Element.Access(Variable.array(state.scriptId, state.operand.int))
            val operation = Expression.Operation(listOf(StackType.INT), id, Expression(array, arrayIndex))
            val def = state.push(StackType.INT)
            assign(state.typings.of(arrayIndex), state.typings.of(INDEX))
            assign(state.typings.of(array), state.typings.of(operation).single())
            assign(state.typings.of(operation).single(), state.typings.of(def))
            return Instruction.Assignment(def, operation)
        }
    }

    object PopArrayInt : Command {

        override val id = POP_ARRAY_INT

        override fun translate(state: InterpreterState): Instruction {
            val value = state.pop(StackType.INT)
            val arrayIndex = state.pop(StackType.INT)
            val array = Element.Access(Variable.array(state.scriptId, state.operand.int))
            assign(state.typings.of(arrayIndex), state.typings.of(INDEX))
            assign(state.typings.of(value), state.typings.of(array))
            return Instruction.Assignment(Expression.Operation(emptyList(), id, Expression(array, arrayIndex, value)))
        }
    }

    enum class Discard(val stackType: StackType) : Command {

        POP_INT_DISCARD(StackType.INT),
        POP_STRING_DISCARD(StackType.STRING),
        ;

        override val id = opcodes.getValue(name)

        override fun translate(state: InterpreterState): Instruction {
            return Instruction.Assignment(state.pop(stackType))
        }
    }

    enum class Assign : Command {

        PUSH_CONSTANT_INT,
        PUSH_CONSTANT_STRING,
        PUSH_VAR,
        POP_VAR,
        PUSH_VARBIT,
        POP_VARBIT,
        PUSH_INT_LOCAL,
        POP_INT_LOCAL,
        PUSH_STRING_LOCAL,
        POP_STRING_LOCAL,
        PUSH_VARC_INT,
        POP_VARC_INT,
        PUSH_VARC_STRING,
        POP_VARC_STRING,
        PUSH_VARCLANSETTING,
        PUSH_VARCLAN,
        ;

        override val id = opcodes.getValue(name)

        override fun translate(state: InterpreterState): Instruction {
            val a = when (this) {
                PUSH_CONSTANT_INT -> Instruction.Assignment(state.push(StackType.INT, state.operand), Element.Constant(state.operand))
                PUSH_CONSTANT_STRING -> Instruction.Assignment(state.push(StackType.STRING, state.operand), Element.Constant(state.operand))
                PUSH_VAR -> Instruction.Assignment(state.push(StackType.INT), Element.Access(Variable.varp(state.operand.int)))
                POP_VAR -> Instruction.Assignment(Element.Access(Variable.varp(state.operand.int)), state.pop(StackType.INT))
                PUSH_VARBIT -> Instruction.Assignment(state.push(StackType.INT), Element.Access(Variable.varbit(state.operand.int)))
                POP_VARBIT -> Instruction.Assignment(Element.Access(Variable.varbit(state.operand.int)), state.pop(StackType.INT))
                PUSH_INT_LOCAL -> Instruction.Assignment(state.push(StackType.INT), Element.Access(Variable.int(state.scriptId, state.operand.int)))
                POP_INT_LOCAL -> Instruction.Assignment(Element.Access(Variable.int(state.scriptId, state.operand.int)), state.pop(StackType.INT))
                PUSH_STRING_LOCAL -> Instruction.Assignment(state.push(StackType.STRING), Element.Access(Variable.string(state.scriptId, state.operand.int)))
                POP_STRING_LOCAL -> Instruction.Assignment(Element.Access(Variable.string(state.scriptId, state.operand.int)), state.pop(StackType.STRING))
                PUSH_VARC_INT -> Instruction.Assignment(state.push(StackType.INT), Element.Access(Variable.varcint(state.operand.int)))
                POP_VARC_INT -> Instruction.Assignment(Element.Access(Variable.varcint(state.operand.int)), state.pop(StackType.INT))
                PUSH_VARC_STRING -> Instruction.Assignment(state.push(StackType.STRING), Element.Access(Variable.varcstring(state.operand.int)))
                POP_VARC_STRING -> Instruction.Assignment(Element.Access(Variable.varcstring(state.operand.int)), state.pop(StackType.STRING))
                PUSH_VARCLANSETTING -> Instruction.Assignment(state.push(StackType.INT), Element.Access(Variable.varclansetting(state.operand.int)))
                PUSH_VARCLAN -> Instruction.Assignment(state.push(StackType.INT), Element.Access(Variable.varclan(state.operand.int)))
            }
            assign(state.typings.of(a.expression), state.typings.of(a.definitions))
            return a
        }
    }

    enum class Basic(
            val args: List<Prototype>,
            val defs: List<Prototype>,
            val o: Boolean = false,
    ) : Command {
        CC_CREATE(listOf(COMPONENT, IFTYPE, COMSUBID), listOf(), true),
        CC_DELETE(listOf(), listOf(), true),
        CC_DELETEALL(listOf(COMPONENT), listOf()),
        ENTITYOVERLAY_CC_CREATE(listOf(ENTITYOVERLAY, IFTYPE, COMSUBID), listOf()),
        ENTITYOVERLAY_CC_DELETEALL(listOf(ENTITYOVERLAY), listOf()),
        CC_FIND(listOf(COMPONENT, COMSUBID), listOf(BOOL), true),
        IF_FIND(listOf(COMPONENT), listOf(BOOLEAN), true),
        ENTITYOVERLAY_IF_FIND(listOf(ENTITYOVERLAY), listOf(BOOL)),
        ENTITYOVERLAY_CC_FIND(listOf(ENTITYOVERLAY, COMSUBID), listOf(BOOL)),

        CC_SETPOSITION(listOf(X, Y, SETPOSH, SETPOSV), listOf(), true),
        CC_SETSIZE(listOf(WIDTH, HEIGHT, SETSIZE, SETSIZE), listOf(), true),
        CC_SETHIDE(listOf(BOOLEAN), listOf(), true),
        CC_SETNOCLICKTHROUGH(listOf(BOOLEAN), listOf(), true),
        CC_SETNOSCROLLTHROUGH(listOf(BOOLEAN), listOf(), true),

        CC_SETSCROLLPOS(listOf(X, Y), listOf(), true),
        CC_SETCOLOUR(listOf(COLOUR), listOf(), true),
        CC_SETFILL(listOf(BOOLEAN), listOf(), true),
        CC_SETTRANS(listOf(TRANS), listOf(), true),
        CC_SETLINEWID(listOf(INT), listOf(), true),
        CC_SETGRAPHIC(listOf(GRAPHIC), listOf(), true),
        CC_SET2DANGLE(listOf(ANGLE), listOf(), true),
        CC_SETTILING(listOf(BOOLEAN), listOf(), true),
        CC_SETMODEL(listOf(MODEL), listOf(), true),
        CC_SETMODELANGLE(listOf(INT, INT, INT, INT, INT, INT), listOf(), true),
        CC_SETMODELANIM(listOf(SEQ), listOf(), true),
        CC_SETMODELORTHOG(listOf(BOOLEAN), listOf(), true),
        CC_SETTEXT(listOf(TEXT), listOf(), true),
        CC_SETTEXTFONT(listOf(FONTMETRICS), listOf(), true),
        CC_SETTEXTALIGN(listOf(SETTEXTALIGNH, SETTEXTALIGNV, INT), listOf(), true),
        CC_SETTEXTSHADOW(listOf(BOOLEAN), listOf(), true),
        CC_SETOUTLINE(listOf(INT), listOf(), true),
        CC_SETGRAPHICSHADOW(listOf(COLOUR), listOf(), true),
        CC_SETVFLIP(listOf(BOOLEAN), listOf(), true),
        CC_SETHFLIP(listOf(BOOLEAN), listOf(), true),
        CC_SETSCROLLSIZE(listOf(WIDTH, HEIGHT), listOf(), true),
        CC_RESUME_PAUSEBUTTON(listOf(), listOf(), true),
        CC_SETCLICKMASK(listOf(GRAPHIC), listOf(), true),
        CC_SETGRADIENTCOLOUR(listOf(COLOUR), listOf(), true),
        CC_SETGRADIENTTRANS(listOf(INT), listOf(), true),
        CC_SETGRADIENT(listOf(INT), listOf(), true),
        CC_SETLINEDIRECTION(listOf(BOOLEAN), listOf(), true),
        CC_SETMODELTRANSPARENT(listOf(BOOLEAN), listOf(), true),
        CC_SETARC(listOf(INT, INT), listOf(), true),
        CC_SETHTTPSPRITE(listOf(STRING), listOf(), true),
        CC_SETCRM_URL(listOf(STRING), listOf(), true),
        CC_SETCRM_TEXTFONT(listOf(INT, FONTMETRICS), listOf(), true),
        CC_SETCRM_SERVERTARGETS(listOf(STRING, INT), listOf(), true),

        CC_SETOBJECT(listOf(OBJ, NUM), listOf(), true),
        CC_SETNPCHEAD(listOf(NPC), listOf(), true),
        CC_SETPLAYERHEAD_SELF(listOf(), listOf(), true),
        CC_SETOBJECT_NONUM(listOf(OBJ, NUM), listOf(), true),
        CC_SETOBJECT_ALWAYSNUM(listOf(OBJ, NUM), listOf(), true),

        CC_SETOP(listOf(OPINDEX, OP), listOf(), true),
        CC_SETDRAGGABLE(listOf(COMPONENT, INT), listOf(), true),
        CC_SETDRAGRENDERBEHAVIOUR(listOf(INT), listOf(), true),
        CC_SETDRAGDEADZONE(listOf(INT), listOf(), true),
        CC_SETDRAGDEADTIME(listOf(INT), listOf(), true),
        CC_SETOPBASE(listOf(OPBASE), listOf(), true),
        CC_SETTARGETVERB(listOf(STRING), listOf(), true),
        CC_CLEAROPS(listOf(), listOf(), true),
        CC_SETOPPRIORITY(listOf(BOOLEAN), listOf(), true),
        _1309(listOf(INT), listOf(), true),
        CC_SETOPKEY(listOf(INT, INT, INT, INT, INT, INT, INT, INT, INT, INT, INT), listOf(), true),
        CC_SETOPTKEY(listOf(INT, INT), listOf(), true),
        CC_SETOPKEYRATE(listOf(INT, INT, INT), listOf(), true),
        CC_SETOPTKEYRATE(listOf(INT, INT), listOf(), true),
        CC_SETOPKEYIGNOREHELD(listOf(INT), listOf(), true),
        CC_SETOPTKEYIGNOREHELD(listOf(), listOf(), true),

        CC_GETX(listOf(), listOf(X), true),
        CC_GETY(listOf(), listOf(Y), true),
        CC_GETWIDTH(listOf(), listOf(WIDTH), true),
        CC_GETHEIGHT(listOf(), listOf(HEIGHT), true),
        CC_GETHIDE(listOf(), listOf(BOOLEAN), true),
        CC_GETLAYER(listOf(), listOf(LAYER), true),

        CC_GETSCROLLX(listOf(), listOf(X), true),
        CC_GETSCROLLY(listOf(), listOf(Y), true),
        CC_GETTEXT(listOf(), listOf(TEXT), true),
        CC_GETSCROLLWIDTH(listOf(), listOf(WIDTH), true),
        CC_GETSCROLLHEIGHT(listOf(), listOf(HEIGHT), true),
        CC_GETMODELZOOM(listOf(), listOf(INT), true),
        CC_GETMODELANGLE_X(listOf(), listOf(INT), true),
        CC_GETMODELANGLE_Z(listOf(), listOf(INT), true),
        CC_GETMODELANGLE_Y(listOf(), listOf(INT), true),
        CC_GETTRANS(listOf(), listOf(TRANS), true),
        CC_GETGRADIENTTRANS(listOf(), listOf(INT), true),
        CC_GETCOLOUR(listOf(), listOf(COLOUR), true),
        CC_GETGRADIENTCOLOUR(listOf(), listOf(COLOUR), true),
        CC_GETGRADIENT(listOf(), listOf(INT), true),
        CC_GETMODELTRANSPARENT(listOf(), listOf(BOOLEAN), true),
        CC_GETARCSTART(listOf(), listOf(INT)),
        CC_GETARCEND(listOf(), listOf(INT)),

        CC_GETINVOBJECT(listOf(), listOf(OBJ), true),
        CC_GETINVCOUNT(listOf(), listOf(COUNT), true),
        CC_GETID(listOf(), listOf(COMSUBID), true),
        CC_GETCRM_HASRESPONSE(listOf(), listOf(BOOLEAN), true),
        CC_GETCRM_INT(listOf(STRING), listOf(INT), true),
        CC_GETCRM_STRING(listOf(STRING), listOf(STRING), true),

        CC_GETTARGETMASK(listOf(), listOf(INT), true),
        CC_GETOP(listOf(INT), listOf(OP), true),
        CC_GETOPBASE(listOf(), listOf(OPBASE), true),

        CC_CALLONRESIZE(listOf(BOOLEAN), listOf()),
        CC_TRIGGEROP(listOf(OPINDEX), listOf(), true),

        IF_SETPOSITION(listOf(X, Y, SETPOSH, SETPOSV, COMPONENT), listOf()),
        IF_SETSIZE(listOf(WIDTH, HEIGHT, SETSIZE, SETSIZE, COMPONENT), listOf()),
        IF_SETHIDE(listOf(BOOLEAN, COMPONENT), listOf()),
        IF_SETNOCLICKTHROUGH(listOf(BOOLEAN, COMPONENT), listOf()),
        IF_SETNOSCROLLTHROUGH(listOf(BOOLEAN, COMPONENT), listOf()),

        IF_SETSCROLLPOS(listOf(X, Y, COMPONENT), listOf()),
        IF_SETCOLOUR(listOf(COLOUR, COMPONENT), listOf()),
        IF_SETFILL(listOf(BOOLEAN, COMPONENT), listOf()),
        IF_SETTRANS(listOf(TRANS, COMPONENT), listOf()),
        IF_SETLINEWID(listOf(INT, COMPONENT), listOf()),
        IF_SETGRAPHIC(listOf(GRAPHIC, COMPONENT), listOf()),
        IF_SET2DANGLE(listOf(ANGLE, COMPONENT), listOf()),
        IF_SETTILING(listOf(BOOLEAN, COMPONENT), listOf()),
        IF_SETMODEL(listOf(MODEL, COMPONENT), listOf()),
        IF_SETMODELANGLE(listOf(INT, INT, INT, INT, INT, INT, COMPONENT), listOf()),
        IF_SETMODELANIM(listOf(SEQ, COMPONENT), listOf()),
        IF_SETMODELORTHOG(listOf(BOOLEAN, COMPONENT), listOf()),
        IF_SETTEXT(listOf(TEXT, COMPONENT), listOf()),
        IF_SETTEXTFONT(listOf(FONTMETRICS, COMPONENT), listOf()),
        IF_SETTEXTALIGN(listOf(SETTEXTALIGNH, SETTEXTALIGNV, INT, COMPONENT), listOf()),
        IF_SETTEXTSHADOW(listOf(BOOLEAN, COMPONENT), listOf()),
        IF_SETOUTLINE(listOf(INT, COMPONENT), listOf()),
        IF_SETGRAPHICSHADOW(listOf(COLOUR, COMPONENT), listOf()),
        IF_SETVFLIP(listOf(BOOLEAN, COMPONENT), listOf()),
        IF_SETHFLIP(listOf(BOOLEAN, COMPONENT), listOf()),
        IF_SETSCROLLSIZE(listOf(WIDTH, HEIGHT, COMPONENT), listOf()),
        IF_RESUME_PAUSEBUTTON(listOf(COMPONENT), listOf()),
        IF_SETCLICKMASK(listOf(GRAPHIC, COMPONENT), listOf()),
        IF_SETGRADIENTCOLOUR(listOf(COLOUR, COMPONENT), listOf()),
        IF_SETGRADIENTTRANS(listOf(INT, COMPONENT), listOf()),
        IF_SETGRADIENT(listOf(INT, COMPONENT), listOf()),
        IF_SETLINEDIRECTION(listOf(BOOLEAN, COMPONENT), listOf()),
        IF_SETMODELTRANSPARENT(listOf(BOOLEAN, COMPONENT), listOf()),
        IF_SETARC(listOf(INT, INT, COMPONENT), listOf()),
        IF_SETHTTPSPRITE(listOf(STRING, COMPONENT), listOf()),
        IF_SETCRM_URL(listOf(STRING, COMPONENT), listOf()),
        IF_SETCRM_TEXTFONT(listOf(INT, FONTMETRICS, COMPONENT), listOf()),
        IF_SETCRM_SERVERTARGETS(listOf(STRING, INT, COMPONENT), listOf()),

        IF_SETOBJECT(listOf(OBJ, NUM, COMPONENT), listOf()),
        IF_SETNPCHEAD(listOf(NPC, COMPONENT), listOf()),
        IF_SETPLAYERHEAD_SELF(listOf(COMPONENT), listOf()),
        IF_SETOBJECT_NONUM(listOf(OBJ, NUM, COMPONENT), listOf()),
        IF_SETOBJECT_ALWAYSNUM(listOf(OBJ, NUM, COMPONENT), listOf()),

        IF_SETOP(listOf(OPINDEX, OP, COMPONENT), listOf()),
        IF_SETDRAGGABLE(listOf(COMPONENT, INT, COMPONENT), listOf()),
        IF_SETDRAGRENDERBEHAVIOUR(listOf(INT, COMPONENT), listOf()),
        IF_SETDRAGDEADZONE(listOf(INT, COMPONENT), listOf()),
        IF_SETDRAGDEADTIME(listOf(INT, COMPONENT), listOf()),
        IF_SETOPBASE(listOf(OPBASE, COMPONENT), listOf()),
        IF_SETTARGETVERB(listOf(STRING, COMPONENT), listOf()),
        IF_CLEAROPS(listOf(COMPONENT), listOf()),
        IF_SETOPPRIORITY(listOf(BOOLEAN, COMPONENT), listOf()),
        _2309(listOf(INT, COMPONENT), listOf()),
        IF_SETOPKEY(listOf(OPINDEX, KEY, FLAGS, COMPONENT), listOf()),
        IF_SETOPTKEY(listOf(INT, INT, COMPONENT), listOf()),
        IF_SETOPKEYRATE(listOf(INT, INT, INT, COMPONENT), listOf()),
        IF_SETOPTKEYRATE(listOf(INT, INT, COMPONENT), listOf()),
        IF_SETOPKEYIGNOREHELD(listOf(INT, COMPONENT), listOf()),
        IF_SETOPTKEYIGNOREHELD(listOf(COMPONENT), listOf()),

        IF_GETX(listOf(COMPONENT), listOf(X)),
        IF_GETY(listOf(COMPONENT), listOf(Y)),
        IF_GETWIDTH(listOf(COMPONENT), listOf(WIDTH)),
        IF_GETHEIGHT(listOf(COMPONENT), listOf(HEIGHT)),
        IF_GETHIDE(listOf(COMPONENT), listOf(BOOLEAN)),
        IF_GETLAYER(listOf(COMPONENT), listOf(LAYER)),

        IF_GETSCROLLX(listOf(COMPONENT), listOf(X)),
        IF_GETSCROLLY(listOf(COMPONENT), listOf(Y)),
        IF_GETTEXT(listOf(COMPONENT), listOf(TEXT)),
        IF_GETSCROLLWIDTH(listOf(COMPONENT), listOf(WIDTH)),
        IF_GETSCROLLHEIGHT(listOf(COMPONENT), listOf(HEIGHT)),
        IF_GETMODELZOOM(listOf(COMPONENT), listOf(INT)),
        IF_GETMODELANGLE_X(listOf(COMPONENT), listOf(INT)),
        IF_GETMODELANGLE_Z(listOf(COMPONENT), listOf(INT)),
        IF_GETMODELANGLE_Y(listOf(COMPONENT), listOf(INT)),
        IF_GETTRANS(listOf(COMPONENT), listOf(TRANS)),
        IF_GETGRADIENTRANS(listOf(COMPONENT), listOf(INT)),
        IF_GETCOLOUR(listOf(COMPONENT), listOf(COLOUR)),
        IF_GETGRADIENTCOLOUR(listOf(COMPONENT), listOf(COLOUR)),
        IF_GETGRADIENT(listOf(COMPONENT), listOf(INT)),
        IF_GETMODELTRANSPARENT(listOf(COMPONENT), listOf(BOOLEAN)),
        IF_GETARCSTART(listOf(COMPONENT), listOf(INT)),
        IF_GETARCEND(listOf(COMPONENT), listOf(INT)),

        IF_GETINVOBJECT(listOf(COMPONENT), listOf(OBJ)),
        IF_GETINVCOUNT(listOf(COMPONENT), listOf(COUNT)),
        IF_HASSUB(listOf(COMPONENT), listOf(BOOLEAN)),
        IF_GETTOP(listOf(), listOf(TOPLEVELINTERFACE)),
        IF_GETCRM_HASRESPONSE(listOf(COMPONENT), listOf(BOOLEAN)),
        IF_GETCRM_INT(listOf(STRING, COMPONENT), listOf(INT)),
        IF_GETCRM_STRING(listOf(STRING, COMPONENT), listOf(STRING)),

        IF_GETTARGETMASK(listOf(COMPONENT), listOf(INT)),
        IF_GETOP(listOf(INT, COMPONENT), listOf(OP)),
        IF_GETOPBASE(listOf(COMPONENT), listOf(OPBASE)),

        IF_CALLONRESIZE(listOf(COMPONENT), listOf(), true),
        IF_TRIGGEROP(listOf(COMPONENT, COMSUBID, OPINDEX), listOf()),

        MES(listOf(_MES), listOf()),
        ANIM(listOf(SEQ, INT), listOf()),
        IF_CLOSE(listOf(), listOf()),
        RESUME_COUNTDIALOG(listOf(STRING), listOf()),
        RESUME_NAMEDIALOG(listOf(STRING), listOf()),
        RESUME_STRINGDIALOG(listOf(STRING), listOf()),
        OPPLAYER(listOf(INT, STRING), listOf()),
        IF_DRAGPICKUP(listOf(COMPONENT, INT, INT), listOf()),
        CC_DRAGPICKUP(listOf(INT, INT), listOf(), true),
        SETMOUSECAM(listOf(BOOLEAN), listOf()),
        GETREMOVEROOFS(listOf(), listOf(BOOLEAN)),
        SETREMOVEROOFS(listOf(BOOLEAN), listOf()),
        OPENURL(listOf(URL, BOOLEAN), listOf()),
        RESUME_OBJDIALOG(listOf(OBJ), listOf()),
        BUG_REPORT(listOf(INT, STRING, STRING), listOf()),
        SETSHIFTCLICKDROP(listOf(BOOLEAN), listOf()),
        SETSHOWMOUSEOVERTEXT(listOf(BOOLEAN), listOf()),
        RENDERSELF(listOf(BOOLEAN), listOf()),
        SETDRAWPLAYERNAMES_FRIENDS(listOf(BOOLEAN), listOf()),
        SETDRAWPLAYERNAMES_CLANMATES(listOf(BOOLEAN), listOf()),
        SETDRAWPLAYERNAMES_OTHERS(listOf(BOOLEAN), listOf()),
        SETDRAWPLAYERNAMES_SELF(listOf(BOOLEAN), listOf()),
        RESETDRAWPLAYERNAMES(listOf(), listOf()),
        SETSHOWCROSS(listOf(BOOLEAN), listOf()),
        SETSHOWLOADINGMESSAGES(listOf(BOOLEAN), listOf()),
        SETTAPTODROP(listOf(BOOLEAN), listOf()),
        GETTAPTODROP(listOf(), listOf(BOOLEAN)),
        SETOCULUSSPEED(listOf(INT, INT), listOf()),
        FORMATCROSS(listOf(GRAPHIC, BOOLEAN), listOf()),
        SETDRAWTARGETVERB(listOf(BOOLEAN), listOf()),
        GETCANVASSIZE(listOf(), listOf(INT, INT)),
        MOBILE_SETFPS(listOf(INT), listOf()),
        MOBILE_OPENSTORE(listOf(), listOf()),
        MOBILE_OPENSTORECATEGORY(listOf(INT, BOOLEAN), listOf()),
        SETKEYINPUTMODE_COMPONENT(listOf(COMPONENT), listOf()),
        SETKEYINPUTMODE_INTERFACE(listOf(INTERFACE), listOf()),
        SETKEYINPUTMODE_ALL(listOf(), listOf()),
        SETKEYINPUTMODE_NONE(listOf(), listOf()),
        SETKEYINPUTMODE_ACTIVECOMPONENT(listOf(), listOf(), true),
        SETHIDEUSERNAME(listOf(BOOLEAN), listOf()),
        GETHIDEUSERNAME(listOf(), listOf(BOOLEAN)),
        SETREMEMBERUSERNAME(listOf(BOOLEAN), listOf()),
        GETREMEMBERUSERNAME(listOf(), listOf(BOOLEAN)),
        SHOW_IOS_REVIEW(listOf(), listOf()),
        SETMUTETITLESCREEN(listOf(BOOLEAN), listOf()),
        GETMUTETITLESCREEN(listOf(), listOf(BOOLEAN)),
        SETTERMSANDPRIVACY(listOf(BOOLEAN), listOf()),
        GETTERMSANDPRIVACY(listOf(), listOf(BOOLEAN)),
        ELIGIBLE_FOR_TRIAL_PURCHASE(listOf(), listOf(BOOLEAN)),
        ELIGIBLE_FOR_INTRODUCTORY_PRICE(listOf(), listOf(BOOLEAN)),
        GET_MEMBERSHIP_ELIGIBILITY_LOADING_STATUS(listOf(), listOf(INT)),
        GET_CLIENT_LOAD_PERCENT(listOf(), listOf(INT)),
        GET_CACHE_LOAD_PERCENT(listOf(), listOf(INT)),
        PURCHASE_SHOP_ITEM(listOf(STRING), listOf()),
        REQUEST_SHOP_DATA(listOf(), listOf()),
        OPEN_SHOP(listOf(INT, INT), listOf()),
        GET_PURCHASE_SHOP_ITEM_STATUS(listOf(), listOf(INT)),
        GET_REQUEST_SHOP_DATA_STATUS(listOf(), listOf(INT)),
        GET_SHOP_CATEGORY_COUNT(listOf(), listOf(INT)),
        GET_SHOP_CATEGORY_ID(listOf(INT), listOf(INT)),
        GET_SHOP_CATEGORY_INDEX_BY_ID(listOf(INT), listOf(INT)),
        GET_SHOP_CATEGORY_INDEX_BY_NAME(listOf(INT), listOf(INT)),
        GET_SHOP_CATEGORY_DESCRIPTION(listOf(INT), listOf(INT)),
        GET_SHOP_CATEGORY_PRODUCT_COUNT(listOf(INT), listOf(INT)),
        IS_SHOP_PRODUCT_AVAILABLE(listOf(INT, INT), listOf(BOOLEAN)),
        IS_SHOP_PRODUCT_RECOMMENDED(listOf(INT, INT), listOf(BOOLEAN)),
        GET_SHOP_PRODUCT_DETAILS(listOf(INT, INT), listOf(STRING, STRING, STRING, STRING, STRING, STRING, STRING, STRING, STRING)),
        OPEN_NOTIFICATION_SETTINGS(listOf(), listOf()),
        SEND_NOTIFICATION(listOf(STRING, STRING, INT, INT), listOf(INT)),
        SEND_GROUPED_NOTIFICATION(listOf(STRING, STRING, INT, INT, STRING, INT), listOf(INT)),
        CANCEL_NOTIFICATION(listOf(INT), listOf(INT)),
        IS_NOTIFICATION_SCHEDULED(listOf(INT), listOf(INT)),
        _3174(listOf(INT), listOf()),
        ARE_NOTIFICATIONS_ENABLED(listOf(), listOf(INT)),
        _3176(listOf(), listOf()),
        INIT_MOBILE_ANALYTICS_SERVICE(listOf(), listOf()),
        SEND_MOBILE_ANALYTICS_EVENT(listOf(STRING), listOf()),
        INIT_MOBILE_ATTRIBUTION_SERVICE(listOf(), listOf()),
        SEND_MOBILE_ATTRIBUTION_EVENT(listOf(STRING), listOf()),
        SETBRIGHTNESS(listOf(INT), listOf()),
        GETBRIGHTNESS(listOf(), listOf(INT)),
        SETANTIDRAG(listOf(BOOLEAN), listOf()),
        _3184(listOf(INT), listOf()),

        SOUND_SYNTH(listOf(SYNTH, INT, INT), listOf()),
        SOUND_SONG(listOf(INT), listOf()),
        SOUND_JINGLE(listOf(INT, INT), listOf()),
        SETVOLUMEMUSIC(listOf(INT), listOf()),
        GETVOLUMEMUSIC(listOf(), listOf(INT)),
        SETVOLUMESOUNDS(listOf(INT), listOf()),
        GETVOLUMESOUNDS(listOf(), listOf(INT)),
        SETVOLUMEAREASOUNDS(listOf(INT), listOf()),
        GETVOLUMEAREASOUNDS(listOf(), listOf(INT)),
        SETCLIENTOPTION(listOf(INT, INT), listOf()),
        GETCLIENTOPTION(listOf(INT), listOf(INT)),
        SETDEVICEOPTION(listOf(DEVICEOPTION, INT), listOf()),
        SETGAMEOPTION(listOf(GAMEOPTION, INT), listOf()),
        GETDEVICEOPTION(listOf(DEVICEOPTION), listOf(INT)),
        GETGAMEOPTION(listOf(GAMEOPTION), listOf(INT)),

        CLIENTCLOCK(listOf(), listOf(CLOCK)),
        INV_GETOBJ(listOf(INV, SLOT), listOf(OBJ)),
        INV_GETNUM(listOf(INV, SLOT), listOf(NUM)),
        INV_TOTAL(listOf(INV, OBJ), listOf(TOTAL)),
        INV_SIZE(listOf(INV), listOf(SIZE)),
        STAT(listOf(_STAT), listOf(LVL)),
        STAT_BASE(listOf(_STAT), listOf(LVL)),
        STAT_XP(listOf(_STAT), listOf(XP)),
        COORD(listOf(), listOf(_COORD)),
        COORDX(listOf(_COORD), listOf(X)),
        COORDY(listOf(_COORD), listOf(Y)),
        COORDZ(listOf(_COORD), listOf(Z)),
        MAP_MEMBERS(listOf(), listOf(BOOL)),
        INVOTHER_GETOBJ(listOf(INV, SLOT), listOf(OBJ)),
        INVOTHER_GETNUM(listOf(INV, SLOT), listOf(NUM)),
        INVOTHER_TOTAL(listOf(INV, OBJ), listOf(TOTAL)),
        STAFFMODLEVEL(listOf(), listOf(INT)),
        REBOOTTIMER(listOf(), listOf(INT)),
        MAP_WORLD(listOf(), listOf(WORLD)),
        RUNENERGY_VISIBLE(listOf(), listOf(INT)),
        RUNWEIGHT_VISIBLE(listOf(), listOf(INT)),
        PLAYERMOD(listOf(), listOf(BOOLEAN)),
        WORLDFLAGS(listOf(), listOf(FLAGS)),
        MOVECOORD(listOf(_COORD, X, Y, Z), listOf(_COORD)),
        MOUSE_GETX(listOf(), listOf(X)),
        MOUSE_GETY(listOf(), listOf(Y)),
        GETTIMETOLOGOUT(listOf(), listOf(INT)),
        TOGGLELOGOUTNOTIFIER(listOf(), listOf()),
        GET_DESTINATION_COORD(listOf(), listOf(_COORD)),

        ENUM_STRING(listOf(_ENUM, INT), listOf(STRING)),
        ENUM_GETOUTPUTCOUNT(listOf(_ENUM), listOf(COUNT)),

        KEYHELD(listOf(KEY), listOf(BOOLEAN)),

        FRIEND_COUNT(listOf(), listOf(COUNT)),
        FRIEND_GETNAME(listOf(INDEX), listOf(USERNAME, USERNAME)),
        FRIEND_GETWORLD(listOf(INDEX), listOf(WORLD)),
        FRIEND_GETRANK(listOf(INDEX), listOf(RANK)),
        FRIEND_SETRANK(listOf(USERNAME, RANK), listOf()),
        FRIEND_ADD(listOf(USERNAME), listOf()),
        FRIEND_DEL(listOf(USERNAME), listOf()),
        IGNORE_ADD(listOf(USERNAME), listOf()),
        IGNORE_DEL(listOf(USERNAME), listOf()),
        FRIEND_TEST(listOf(USERNAME), listOf(BOOLEAN)),
        CLAN_GETCHATDISPLAYNAME(listOf(), listOf(STRING)),
        CLAN_GETCHATCOUNT(listOf(), listOf(COUNT)),
        CLAN_GETCHATUSERNAME(listOf(INDEX), listOf(USERNAME)),
        CLAN_GETCHATUSERWORLD(listOf(INDEX), listOf(WORLD)),
        CLAN_GETCHATUSERRANK(listOf(INDEX), listOf(RANK)),
        CLAN_GETCHATMINKICK(listOf(), listOf(RANK)),
        CLAN_KICKUSER(listOf(USERNAME), listOf()),
        CLAN_GETCHATRANK(listOf(), listOf(RANK)),
        CLAN_JOINCHAT(listOf(USERNAME), listOf()),
        CLAN_LEAVECHAT(listOf(), listOf()),
        IGNORE_COUNT(listOf(), listOf(COUNT)),
        IGNORE_GETNAME(listOf(INDEX), listOf(USERNAME, USERNAME)),
        IGNORE_TEST(listOf(USERNAME), listOf(BOOLEAN)),
        CLAN_ISSELF(listOf(INDEX), listOf(BOOLEAN)),
        CLAN_GETCHATOWNERNAME(listOf(), listOf(USERNAME)),
        CLAN_ISFRIEND(listOf(INDEX), listOf(BOOLEAN)),
        CLAN_ISIGNORE(listOf(INDEX), listOf(BOOLEAN)),
        FRIENDLIST_SORT_RESET(listOf(), listOf()),
        FRIENDLIST_SORT_LEGACY(listOf(BOOLEAN), listOf()),
        FRIENDLIST_SORT_NAME(listOf(BOOLEAN), listOf()),
        FRIENDLIST_SORT_WORLD(listOf(BOOLEAN), listOf()),
        FRIENDLIST_SORT_HOPTIME(listOf(BOOLEAN), listOf()),
        FRIENDLIST_SORT_ONLINE_STATUS(listOf(BOOLEAN), listOf()),
        FRIENDLIST_SORT_ONLINE_NAME(listOf(BOOLEAN), listOf()),
        FRIENDLIST_SORT_ONLINE_HOPTIME(listOf(BOOLEAN), listOf()),
        FRIENDLIST_SORT_ONLINE_WORLD(listOf(BOOLEAN), listOf()),
        FRIENDLIST_SORT_LOCAL_NAME(listOf(BOOLEAN), listOf()),
        FRIENDLIST_SORT_LOCAL_WORLD(listOf(BOOLEAN), listOf()),
        FRIENDLIST_SORT_APPLY(listOf(), listOf()),
        IGNORELIST_SORT_RESET(listOf(), listOf()),
        IGNORELIST_SORT_LEGACY(listOf(BOOLEAN), listOf()),
        IGNORELIST_SORT_NAME(listOf(BOOLEAN), listOf()),
        IGNORELIST_SORT_APPLY(listOf(), listOf()),
        CLAN_SORT_RESET(listOf(), listOf()),
        CLAN_SORT_LEGACY(listOf(BOOLEAN), listOf()),
        CLAN_SORT_NAME(listOf(BOOLEAN), listOf()),
        CLAN_SORT_WORLD(listOf(BOOLEAN), listOf()),
        CLAN_SORT_HOPTIME(listOf(BOOLEAN), listOf()),
        CLAN_SORT_ONLINE_STATUS(listOf(BOOLEAN), listOf()),
        CLAN_SORT_ONLINE_NAME(listOf(BOOLEAN), listOf()),
        CLAN_SORT_ONLINE_HOPTIME(listOf(BOOLEAN), listOf()),
        CLAN_SORT_ONLINE_WORLD(listOf(BOOLEAN), listOf()),
        CLAN_SORT_LOCAL_NAME(listOf(BOOLEAN), listOf()),
        CLAN_SORT_LOCAL_WORLD(listOf(BOOLEAN), listOf()),
        CLAN_SORT_APPLY(listOf(), listOf()),
        FRIENDLIST_SORT_RANK(listOf(BOOLEAN), listOf()),
        CLAN_SORT_RANK(listOf(BOOLEAN), listOf()),

        STEAM_SET_UNLOCKED(listOf(STRING, INT, INT), listOf(INT)),
        STEAM_SET_STAT_PROGRESS(listOf(STRING, INT, INT), listOf(INT)),
        _3702(listOf(), listOf(INT)),

        ACTIVECLANSETTINGS_FIND_LISTENED(listOf(), listOf(BOOLEAN)),
        ACTIVECLANSETTINGS_FIND_AFFINED(listOf(CLANTYPE), listOf(BOOLEAN)),
        ACTIVECLANSETTINGS_GETCLANNAME(listOf(), listOf(STRING)),
        ACTIVECLANSETTINGS_GETALLOWUNAFFINED(listOf(), listOf(BOOLEAN)),
        ACTIVECLANSETTINGS_GETRANKTALK(listOf(), listOf(INT)),
        ACTIVECLANSETTINGS_GETRANKKICK(listOf(), listOf(INT)),
        ACTIVECLANSETTINGS_GETRANKLOOTSHARE(listOf(), listOf(INT)),
        ACTIVECLANSETTINGS_GETCOINSHARE(listOf(), listOf(INT)),
        ACTIVECLANSETTINGS_GETAFFINEDCOUNT(listOf(), listOf(INT)),
        ACTIVECLANSETTINGS_GETAFFINEDDISPLAYNAME(listOf(CLANSLOT), listOf(STRING)),
        ACTIVECLANSETTINGS_GETAFFINEDRANK(listOf(CLANSLOT), listOf(INT)),
        ACTIVECLANSETTINGS_GETBANNEDCOUNT(listOf(), listOf(INT)),
        ACTIVECLANSETTINGS_GETBANNEDDISPLAYNAME(listOf(CLANSLOT), listOf(STRING)),
        ACTIVECLANSETTINGS_GETAFFINEDEXTRAINFO(listOf(CLANSLOT, INT, INT), listOf(INT)),
        ACTIVECLANSETTINGS_GETCURRENTOWNER_SLOT(listOf(), listOf(CLANSLOT)),
        ACTIVECLANSETTINGS_GETREPLACEMENTOWNER_SLOT(listOf(), listOf(CLANSLOT)),
        ACTIVECLANSETTINGS_GETAFFINEDSLOT(listOf(STRING), listOf(CLANSLOT)),
        ACTIVECLANSETTINGS_GETSORTEDAFFINEDSLOT(listOf(INT), listOf(CLANSLOT)),
        AFFINEDCLANSETTINGS_ADDBANNED_FROMCHANNEL(listOf(CLANSLOT, INT), listOf()),
        ACTIVECLANSETTINGS_GETAFFINEDJOINRUNEDAY(listOf(CLANSLOT), listOf(INT)),
        AFFINEDCLANSETTINGS_SETMUTED_FROMCHANNEL(listOf(CLANSLOT, BOOLEAN, INT), listOf()),
        ACTIVECLANSETTINGS_GETAFFINEDMUTED(listOf(CLANSLOT), listOf(BOOLEAN)),

        ACTIVECLANCHANNEL_FIND_LISTENED(listOf(), listOf(BOOLEAN)),
        ACTIVECLANCHANNEL_FIND_AFFINED(listOf(CLANTYPE), listOf(BOOLEAN)),
        ACTIVECLANCHANNEL_GETCLANNAME(listOf(), listOf(STRING)),
        ACTIVECLANCHANNEL_GETRANKKICK(listOf(), listOf(INT)),
        ACTIVECLANCHANNEL_GETRANKTALK(listOf(), listOf(INT)),
        ACTIVECLANCHANNEL_GETUSERCOUNT(listOf(), listOf(INT)),
        ACTIVECLANCHANNEL_GETUSERDISPLAYNAME(listOf(CLANSLOT), listOf(STRING)),
        ACTIVECLANCHANNEL_GETUSERRANK(listOf(CLANSLOT), listOf(INT)),
        ACTIVECLANCHANNEL_GETUSERWORLD(listOf(CLANSLOT), listOf(INT)),
        ACTIVECLANCHANNEL_KICKUSER(listOf(CLANSLOT), listOf()),
        ACTIVECLANCHANNEL_GETUSERSLOT(listOf(STRING), listOf(CLANSLOT)),
        ACTIVECLANCHANNEL_GETSORTEDUSERSLOT(listOf(INT), listOf(CLANSLOT)),

        CLANPROFILE_FIND(listOf(), listOf(BOOLEAN)),

        STOCKMARKET_GETOFFERTYPE(listOf(INT), listOf(INT)),
        STOCKMARKET_GETOFFERITEM(listOf(INT), listOf(OBJ)),
        STOCKMARKET_GETOFFERPRICE(listOf(INT), listOf(INT)),
        STOCKMARKET_GETOFFERCOUNT(listOf(INT), listOf(INT)),
        STOCKMARKET_GETOFFERCOMPLETEDCOUNT(listOf(INT), listOf(INT)),
        STOCKMARKET_GETOFFERCOMPLETEDGOLD(listOf(INT), listOf(INT)),
        STOCKMARKET_ISOFFEREMPTY(listOf(INT), listOf(BOOLEAN)),
        STOCKMARKET_ISOFFERSTABLE(listOf(INT), listOf(BOOLEAN)),
        STOCKMARKET_ISOFFERFINISHED(listOf(INT), listOf(BOOLEAN)),
        STOCKMARKET_ISOFFERADDING(listOf(INT), listOf(BOOLEAN)),

        TRADINGPOST_SORTBY_NAME(listOf(BOOLEAN), listOf()),
        TRADINGPOST_SORTBY_PRICE(listOf(BOOLEAN), listOf()),
        TRADINGPOST_SORTFILTERBY_WORLD(listOf(BOOLEAN, BOOLEAN), listOf()),
        TRADINGPOST_SORTBY_AGE(listOf(BOOLEAN), listOf()),
        TRADINGPOST_SORTBY_COUNT(listOf(BOOLEAN), listOf()),
        TRADINGPOST_GETTOTALOFFERS(listOf(), listOf(INT)),
        TRADINGPOST_GETOFFERWORLD(listOf(INT), listOf(INT)),
        TRADINGPOST_GETOFFERNAME(listOf(INT), listOf(STRING)),
        TRADINGPOST_GETOFFERPREVIOUSNAME(listOf(INT), listOf(STRING)),
        TRADINGPOST_GETOFFERAGE(listOf(INT), listOf(STRING)),
        TRADINGPOST_GETOFFERCOUNT(listOf(INT), listOf(INT)),
        TRADINGPOST_GETOFFERPRICE(listOf(INT), listOf(INT)),
        TRADINGPOST_GETOFFERITEM(listOf(INT), listOf(INT)),

        ADD(listOf(INT, INT), listOf(INT)),
        SUB(listOf(INT, INT), listOf(INT)),
        MULTIPLY(listOf(INT, INT), listOf(INT)),
        DIVIDE(listOf(INT, INT), listOf(INT)),
        RANDOM(listOf(INT), listOf(INT)),
        RANDOMINC(listOf(INT), listOf(INT)),
        INTERPOLATE(listOf(INT, INT, INT, INT, INT), listOf(INT)),
        ADDPERCENT(listOf(INT, INT), listOf(INT)),
        SETBIT(listOf(FLAGS, INDEX), listOf(FLAGS)),
        CLEARBIT(listOf(FLAGS, INDEX), listOf(FLAGS)),
        TESTBIT(listOf(FLAGS, INDEX), listOf(BOOL)),
        MODULO(listOf(INT, INT), listOf(INT)),
        POW(listOf(INT, INT), listOf(INT)),
        INVPOW(listOf(INT, INT), listOf(INT)),
        AND(listOf(INT, INT), listOf(INT)),
        OR(listOf(INT, INT), listOf(INT)),
        SCALE(listOf(INT, INT, INT), listOf(INT)),
        BITCOUNT(listOf(INT), listOf(INT)),
        TOGGLEBIT(listOf(INT, INT), listOf(INT)),
        SETBIT_RANGE(listOf(INT, INT, INT), listOf(INT)),
        CLEARBIT_RANGE(listOf(INT, INT, INT), listOf(INT)),
        GETBIT_RANGE(listOf(INT, INT, INT), listOf(INT)),
        SETBIT_RANGE_TOINT(listOf(INT, INT, INT, INT), listOf(INT)),
        SIN_DEG(listOf(INT), listOf(INT)),
        COS_DEG(listOf(INT), listOf(INT)),
        ATAN2_DEG(listOf(INT, INT), listOf(INT)),
        ABS(listOf(INT), listOf(INT)),
        APPEND_NUM(listOf(STRING, INT), listOf(STRING)),
        APPEND(listOf(STRING, STRING), listOf(STRING)),
        APPEND_SIGNNUM(listOf(STRING, INT), listOf(STRING)),
        LOWERCASE(listOf(STRING), listOf(STRING)),
        FROMDATE(listOf(INT), listOf(STRING)),
        TEXT_GENDER(listOf(STRING, STRING), listOf(STRING)),
        TOSTRING(listOf(INT), listOf(STRING)),
        COMPARE(listOf(STRING, STRING), listOf(INT)),
        PARAHEIGHT(listOf(STRING, WIDTH, FONTMETRICS), listOf(HEIGHT)),
        PARAWIDTH(listOf(STRING, WIDTH, FONTMETRICS), listOf(WIDTH)),
        TEXT_SWITCH(listOf(INT, STRING, STRING), listOf(STRING)),
        ESCAPE(listOf(STRING), listOf(STRING)),
        APPEND_CHAR(listOf(STRING, CHAR), listOf(STRING)),
        CHAR_ISPRINTABLE(listOf(CHAR), listOf(BOOLEAN)),
        CHAR_ISALPHANUMERIC(listOf(CHAR), listOf(BOOLEAN)),
        CHAR_ISALPHA(listOf(CHAR), listOf(BOOLEAN)),
        CHAR_ISNUMERIC(listOf(CHAR), listOf(BOOLEAN)),
        STRING_LENGTH(listOf(STRING), listOf(LENGTH)),
        SUBSTRING(listOf(STRING, INDEX, INDEX), listOf(STRING)),
        REMOVETAGS(listOf(STRING), listOf(STRING)),
        STRING_INDEXOF_CHAR(listOf(STRING, CHAR), listOf(INDEX)),
        STRING_INDEXOF_STRING(listOf(STRING, STRING, INDEX), listOf(INDEX)),
        UPPERCASE(listOf(STRING), listOf(STRING)),

        OC_NAME(listOf(OBJ), listOf(STRING)),
        OC_OP(listOf(OBJ, OPINDEX), listOf(OP)),
        OC_IOP(listOf(OBJ, OPINDEX), listOf(OP)),
        OC_COST(listOf(OBJ), listOf(INT)),
        OC_STACKABLE(listOf(OBJ), listOf(BOOLEAN)),
        OC_CERT(listOf(OBJ), listOf(OBJ)),
        OC_UNCERT(listOf(OBJ), listOf(OBJ)),
        OC_MEMBERS(listOf(OBJ), listOf(BOOL)),
        OC_PLACEHOLDER(listOf(OBJ), listOf(OBJ)),
        OC_UNPLACEHOLDER(listOf(OBJ), listOf(OBJ)),
        OC_FIND(listOf(STRING, BOOLEAN), listOf(INT)),
        OC_FINDNEXT(listOf(), listOf(OBJ)),
        OC_FINDRESTART(listOf(), listOf()),
        OC_SHIFTCLICKOP(listOf(OBJ), listOf(INT)),

        CHAT_GETFILTER_PUBLIC(listOf(), listOf(CHATFILTER)),
        CHAT_SETFILTER(listOf(CHATFILTER, CHATFILTER, CHATFILTER), listOf()),
        CHAT_SENDABUSEREPORT(listOf(STRING, INT, INT), listOf()),
        CHAT_GETHISTORY_BYTYPEANDLINE_OLD(listOf(CHATTYPE, INT), listOf(MESUID, CLOCK, USERNAME, STRING, _MES, INT)),
        CHAT_GETHISTORY_BYUID_OLD(listOf(MESUID), listOf(CHATTYPE, CLOCK, USERNAME, STRING, _MES, INT)),
        CHAT_GETFILTER_PRIVATE(listOf(), listOf(CHATFILTER)),
        CHAT_SENDPUBLIC(listOf(_MES, INT), listOf()),
        CHAT_SENDPRIVATE(listOf(USERNAME, _MES), listOf()),
        CHAT_SENDCLAN(listOf(_MES, INT, CLANTYPE), listOf()),
        CHAT_PLAYERNAME(listOf(), listOf(USERNAME)),
        CHAT_GETFILTER_TRADE(listOf(), listOf(CHATFILTER)),
        CHAT_GETHISTORYLENGTH(listOf(CHATTYPE), listOf(LENGTH)),
        CHAT_GETNEXTUID(listOf(MESUID), listOf(MESUID)),
        CHAT_GETPREVUID(listOf(MESUID), listOf(MESUID)),
        DOCHEAT(listOf(STRING), listOf()),
        CHAT_SETMESSAGEFILTER(listOf(STRING), listOf()),
        CHAT_GETMESSAGEFILTER(listOf(), listOf(STRING)),
        WRITECONSOLE(listOf(STRING), listOf()),
        CHAT_SETTIMESTAMPS(listOf(INT), listOf()),
        CHAT_GETTIMESTAMPS(listOf(), listOf(INT)),
        CHAT_GETHISTORY_BYTYPEANDLINE(listOf(CHATTYPE, INT), listOf(MESUID, CLOCK, USERNAME, STRING, _MES, INT, STRING, INT)),
        CHAT_GETHISTORY_BYUID(listOf(MESUID), listOf(CHATTYPE, CLOCK, USERNAME, STRING, _MES, INT, STRING, INT)),

        GETWINDOWMODE(listOf(), listOf(WINDOWMODE)),
        SETWINDOWMODE(listOf(WINDOWMODE), listOf()),
        GETDEFAULTWINDOWMODE(listOf(), listOf(WINDOWMODE)),
        SETDEFAULTWINDOWMODE(listOf(WINDOWMODE), listOf()),
        SETMINASPECTRATIO(listOf(INT), listOf()),
        SETWINDOWSIZE(listOf(INT, INT), listOf()),
        SETWINDOWALWAYSONTOP(listOf(BOOLEAN), listOf()),
        
        _5350(listOf(INT, STRING, STRING), listOf()),
        _5351(listOf(STRING), listOf()),

        CAM_FORCEANGLE(listOf(INT, INT), listOf()),
        CAM_GETANGLE_XA(listOf(), listOf(INT)),
        CAM_GETANGLE_YA(listOf(), listOf(INT)),
        CAM_SETFOLLOWHEIGHT(listOf(INT), listOf()),
        CAM_GETFOLLOWHEIGHT(listOf(), listOf(INT)),

        LOGOUT(listOf(), listOf()),
        FEDERATED_LOGIN(listOf(STRING, STRING), listOf()),
        GET_STORE_STATUS(listOf(), listOf(INT)),
        OPEN_STORE(listOf(STRING, STRING), listOf()),

        VIEWPORT_SETFOV(listOf(INT, INT), listOf()),
        VIEWPORT_SETZOOM(listOf(INT, INT), listOf()),
        VIEWPORT_CLAMPFOV(listOf(INT, INT, INT, INT), listOf()),
        VIEWPORT_GETEFFECTIVESIZE(listOf(), listOf(WIDTH, HEIGHT)),
        VIEWPORT_GETZOOM(listOf(), listOf(INT, INT)),
        VIEWPORT_GETFOV(listOf(), listOf(INT, INT)),
        SETUIZOOM(listOf(INT), listOf()),
        RESETUIZOOM(listOf(), listOf()),
        VIEWPORT_GETX(listOf(), listOf(INT)),
        VIEWPORT_GETY(listOf(), listOf(INT)),
        VIEWPORT_GETWIDTH(listOf(), listOf(INT)),
        VIEWPORT_GETHEIGHT(listOf(), listOf(INT)),
        _6230(listOf(INT), listOf()),

        WORLDLIST_FETCH(listOf(), listOf(BOOLEAN)),
        WORLDLIST_START(listOf(), listOf(WORLD, FLAGS, STRING, INT, COUNT, STRING)),
        WORLDLIST_NEXT(listOf(), listOf(WORLD, FLAGS, STRING, INT, COUNT, STRING)),
        WORLDLIST_SPECIFIC(listOf(WORLD), listOf(WORLD, FLAGS, STRING, INT, COUNT, STRING)),
        WORLDLIST_SORT(listOf(INT, BOOLEAN, INT, BOOLEAN), listOf()),
        WORLDLIST_GETBYINDEX(listOf(INT), listOf(INT, INT, STRING, INT, INT, STRING)),
        SETFOLLOWEROPSLOWPRIORITY(listOf(BOOLEAN), listOf()),

        ON_MOBILE(listOf(), listOf(BOOLEAN)),
        CLIENTTYPE(listOf(), listOf(_CLIENTTYPE)),
        MOBILE_KEYBOARDSHOW(listOf(), listOf()),
        MOBILE_KEYBOARDHIDE(listOf(), listOf()),
        MOBILE_KEYBOARDSHOWSTRING(listOf(STRING, INT), listOf()),
        MOBILE_KEYBOARDSHOWINTEGER(listOf(STRING, INT), listOf()),
        MOBILE_BATTERYLEVEL(listOf(), listOf(INT)),
        MOBILE_BATTERYCHARGING(listOf(), listOf(BOOLEAN)),
        MOBILE_WIFIAVAILABLE(listOf(), listOf(BOOLEAN)),
        PLATFORMTYPE(listOf(), listOf(_PLATFORMTYPE)),

        WORLDMAP_JUMPTOPLAYER(listOf(), listOf()),
        WORLDMAP_GETMAPNAME(listOf(MAPAREA), listOf(STRING)),
        WORLDMAP_SETMAP(listOf(MAPAREA), listOf()),
        WORLDMAP_GETZOOM(listOf(), listOf(INT)),
        WORLDMAP_SETZOOM(listOf(INT), listOf()),
        WORLDMAP_ISLOADED(listOf(), listOf(BOOLEAN)),
        WORLDMAP_JUMPTODISPLAYCOORD(listOf(_COORD), listOf()),
        WORLDMAP_JUMPTODISPLAYCOORD_INSTANT(listOf(_COORD), listOf()),
        WORLDMAP_JUMPTOSOURCECOORD(listOf(_COORD), listOf()),
        WORLDMAP_JUMPTOSOURCECOORD_INSTANT(listOf(_COORD), listOf()),
        WORLDMAP_GETDISPLAYPOSITION(listOf(), listOf(INT, INT)),
        WORLDMAP_GETCONFIGORIGIN(listOf(MAPAREA), listOf(INT)),
        WORLDMAP_GETCONFIGSIZE(listOf(MAPAREA), listOf(INT, INT)),
        WORLDMAP_GETCONFIGBOUNDS(listOf(MAPAREA), listOf(INT, INT, INT, INT)),
        WORLDMAP_GETCONFIGZOOM(listOf(MAPAREA), listOf(INT)),
        WORLDMAP_GETSOURCEPOSITION(listOf(), listOf(INT, INT)),
        WORLDMAP_GETCURRENTMAP(listOf(), listOf(MAPAREA)),
        WORLDMAP_GETDISPLAYCOORD(listOf(_COORD), listOf(INT, INT)),
        WORLDMAP_GETSOURCECOORD(listOf(_COORD), listOf(INT, INT)),
        WORLDMAP_SETMAP_COORD(listOf(INT, _COORD), listOf()),
        WORLDMAP_SETMAP_COORD_OVERRIDE(listOf(INT, _COORD), listOf()),
        WORLDMAP_COORDINMAP(listOf(MAPAREA, _COORD), listOf(BOOLEAN)),
        WORLDMAP_GETSIZE(listOf(), listOf(INT, INT)),
        WORLDMAP_GETMAP(listOf(_COORD), listOf(MAPAREA)),
        WORLDMAP_SETFLASHLOOPS(listOf(INT), listOf()),
        WORLDMAP_SETFLASHLOOPS_DEFAULT(listOf(), listOf()),
        WORLDMAP_SETFLASHTICS(listOf(INT), listOf()),
        WORLDMAP_SETFLASHTICS_DEFAULT(listOf(), listOf()),
        WORLDMAP_PERPETUALFLASH(listOf(BOOLEAN), listOf()),
        WORLDMAP_FLASHELEMENT(listOf(MAPELEMENT), listOf()),
        WORLDMAP_FLASHELEMENTCATEGORY(listOf(CATEGORY), listOf()),
        WORLDMAP_STOPCURRENTFLASHES(listOf(), listOf()),
        WORLDMAP_DISABLEELEMENTS(listOf(BOOLEAN), listOf()),
        WORLDMAP_DISABLEELEMENT(listOf(INT, BOOLEAN), listOf()),
        WORLDMAP_DISABLEELEMENTCATEGORY(listOf(CATEGORY, BOOLEAN), listOf()),
        WORLDMAP_GETDISABLEELEMENTS(listOf(), listOf(BOOLEAN)),
        WORLDMAP_GETDISABLEELEMENT(listOf(INT), listOf(BOOLEAN)),
        WORLDMAP_GETDISABLEELEMENTCATEGORY(listOf(CATEGORY), listOf(BOOLEAN)),
        WORLDMAP_FINDNEARESTELEMENT(listOf(INT, _COORD), listOf(INT)),
        WORLDMAP_LISTELEMENT_START(listOf(), listOf(MAPELEMENT, _COORD)),
        WORLDMAP_LISTELEMENT_NEXT(listOf(), listOf(MAPELEMENT, _COORD)),
        MEC_TEXT(listOf(MAPELEMENT), listOf(TEXT)),
        MEC_TEXTSIZE(listOf(MAPELEMENT), listOf(INT)),
        MEC_CATEGORY(listOf(MAPELEMENT), listOf(CATEGORY)),
        MEC_SPRITE(listOf(MAPELEMENT), listOf(INT)),
        MAPELEMENT_TYPE(listOf(), listOf(MAPELEMENT)),
        MAPELEMENT_DISPLAYCOORD(listOf(), listOf(_COORD)),
        MAPELEMENT_SOURCECOORD(listOf(), listOf(_COORD)),

        SHIFTOP_NPC_SET(listOf(INT, STRING, INT), listOf()),
        SHIFTOP_NPC_DEL(listOf(INT), listOf()),
        SHIFTOP_LOC_SET(listOf(INT, STRING, INT), listOf()),
        SHIFTOP_LOC_DEL(listOf(INT), listOf()),
        SHIFTOP_OBJ_SET(listOf(INT, STRING, INT), listOf()),
        SHIFTOP_OBJ_DEL(listOf(INT), listOf()),
        SHIFTOP_PLAYER_SET(listOf(INT, STRING, INT), listOf()),
        SHIFTOP_PLAYER_DEL(listOf(INT), listOf()),
        SHIFTOP_TILE_SET(listOf(INT, STRING, INT), listOf()),
        SHIFTOP_TILE_DEL(listOf(INT), listOf()),

        NPC_NAME(listOf(), listOf(STRING)),
        NPC_UID(listOf(), listOf(_NPC_UID)),
        _6752(listOf(), listOf(INT)),
        NPC_TYPE(listOf(), listOf(NPC)),
        NC_NAME(listOf(NPC), listOf(STRING)),
        NPC_FIND(listOf(_NPC_UID), listOf(BOOLEAN)),

        LOC_NAME(listOf(), listOf(STRING)),
        LOC_COORD(listOf(), listOf(_COORD)),
        LOC_TYPE(listOf(), listOf(LOC)),
        LOC_FIND(listOf(_COORD, LOC), listOf(BOOLEAN)),

        OBJ_NAME(listOf(), listOf(STRING)),
        OBJ_COORD(listOf(), listOf(_COORD)),
        OBJ_TYPE(listOf(), listOf(OBJ)),
        OBJ_COUNT(listOf(), listOf(INT)),

        PLAYER_NAME(listOf(), listOf(STRING)),
        WAYPOINT_COUNT(listOf(), listOf(INT)),
        WAYPOINT_SPECIFIC(listOf(INT), listOf(_COORD)),
        UID(listOf(), listOf(_PLAYER_UID)),
        SELF_PLAYER_UID(listOf(), listOf(_PLAYER_UID)),

        HOVERED_COORD(listOf(), listOf(_COORD)),
        
        HIGHLIGHT_NPC_SETUP(listOf(INT, COLOUR, INT, INT, INT), listOf()),
        HIGHLIGHT_NPC_ON(listOf(_NPC_UID, INT, INT), listOf()),
        HIGHLIGHT_NPC_OFF(listOf(_NPC_UID, INT, INT), listOf()),
        HIGHLIGHT_NPC_GET(listOf(_NPC_UID, INT, INT), listOf(BOOLEAN)),
        HIGHLIGHT_NPC_CLEAR(listOf(INT), listOf()),
        HIGHLIGHT_NPCTYPE_SETUP(listOf(INT, COLOUR, INT, INT, INT), listOf()),
        HIGHLIGHT_NPCTYPE_ON(listOf(NPC, INT), listOf()),
        HIGHLIGHT_NPCTYPE_OFF(listOf(NPC, INT), listOf()),
        HIGHLIGHT_NPCTYPE_GET(listOf(NPC, INT), listOf(BOOLEAN)),
        HIGHLIGHT_NPCTYPE_CLEAR(listOf(INT), listOf()),
        HIGHLIGHT_LOC_SETUP(listOf(INT, COLOUR, INT, INT, INT), listOf()),
        HIGHLIGHT_LOC_ON(listOf(LOC, _COORD, INT, INT), listOf()),
        HIGHLIGHT_LOC_OFF(listOf(LOC, _COORD, INT, INT), listOf()),
        HIGHLIGHT_LOC_GET(listOf(LOC, _COORD, INT, INT), listOf(BOOLEAN)),
        HIGHLIGHT_LOC_CLEAR(listOf(INT), listOf()),
        HIGHLIGHT_LOCTYPE_SETUP(listOf(INT, COLOUR, INT, INT, INT), listOf()),
        HIGHLIGHT_LOCTYPE_ON(listOf(LOC, INT), listOf()),
        HIGHLIGHT_LOCTYPE_OFF(listOf(LOC, INT), listOf()),
        HIGHLIGHT_LOCTYPE_GET(listOf(LOC, INT), listOf(BOOLEAN)),
        HIGHLIGHT_LOCTYPE_CLEAR(listOf(INT), listOf()),
        HIGHLIGHT_OBJ_SETUP(listOf(INT, COLOUR, INT, INT, INT), listOf()),
        HIGHLIGHT_OBJ_ON(listOf(OBJ, _COORD, INT, INT), listOf()),
        HIGHLIGHT_OBJ_OFF(listOf(OBJ, _COORD, INT, INT), listOf()),
        HIGHLIGHT_OBJ_GET(listOf(OBJ, _COORD, INT, INT), listOf(BOOLEAN)),
        HIGHLIGHT_OBJ_CLEAR(listOf(INT), listOf()),
        HIGHLIGHT_OBJTYPE_SETUP(listOf(INT, COLOUR, INT, INT, INT), listOf()),
        HIGHLIGHT_OBJTYPE_ON(listOf(OBJ, INT), listOf()),
        HIGHLIGHT_OBJTYPE_OFF(listOf(OBJ, INT), listOf()),
        HIGHLIGHT_OBJTYPE_GET(listOf(OBJ, INT), listOf(BOOLEAN)),
        HIGHLIGHT_OBJTYPE_CLEAR(listOf(INT), listOf()),
        HIGHLIGHT_PLAYER_SETUP(listOf(INT, COLOUR, INT, INT, INT), listOf()),
        HIGHLIGHT_PLAYER_ON(listOf(STRING, INT), listOf()),
        HIGHLIGHT_PLAYER_OFF(listOf(STRING, INT), listOf()),
        HIGHLIGHT_PLAYER_GET(listOf(STRING, INT), listOf(INT)),
        HIGHLIGHT_PLAYER_CLEAR(listOf(INT), listOf()),
        HIGHLIGHT_TILE_SETUP(listOf(INT, COLOUR, INT, INT, INT), listOf()),
        HIGHLIGHT_TILE_ON(listOf(_COORD, INT, INT), listOf()),
        HIGHLIGHT_TILE_OFF(listOf(_COORD, INT, INT), listOf()),
        HIGHLIGHT_TILE_GET(listOf(_COORD, INT, INT), listOf(BOOLEAN)),
        HIGHLIGHT_TILE_CLEAR(listOf(INT), listOf()),

        GET_ACTIVE_MINIMENU_ENTRY_TYPE(listOf(), listOf(MINIMENU_ENTRY_TYPE)),
        GET_ACTIVE_MINIMENU_ENTRY(listOf(), listOf(STRING, STRING)),
        NPC_FIND_ACTIVE_MINIMENU_ENTRY(listOf(), listOf(BOOLEAN)),
        LOC_FIND_ACTIVE_MINIMENU_ENTRY(listOf(), listOf(BOOLEAN)),
        OBJ_FIND_ACTIVE_MINIMENU_ENTRY(listOf(), listOf(BOOLEAN)),
        PLAYER_FIND_ACTIVE_MINIMENU_ENTRY(listOf(), listOf(BOOLEAN)),
        GET_ACTIVE_MINIMENU_ENTRY_COORD(listOf(), listOf(INT)),
        _7107(listOf(), listOf(INT)),
        IS_MINIMENU_OPEN(listOf(), listOf(BOOLEAN)),
        TARGETMODE_ACTIVE(listOf(), listOf(BOOLEAN)),
        GET_MINIMENU_LENGTH(listOf(), listOf(INT)),
        _7120(listOf(INT), listOf(INT)),
        _7121(listOf(INT, INT), listOf(INT)),
        _7122(listOf(INT, INT), listOf(INT)),

        ENTITYOVERLAY_CREATE_NPC(listOf(INT, INT, INT, INT, INT), listOf(ENTITYOVERLAY)),
        ENTITYOVERLAY_CREATE_LOC(listOf(INT, INT, INT, INT, INT), listOf(ENTITYOVERLAY)),
        ENTITYOVERLAY_CREATE_OBJ(listOf(INT, INT, INT, INT, INT), listOf(ENTITYOVERLAY)),
        ENTITYOVERLAY_CREATE_PLAYER(listOf(INT, INT, INT, INT, INT), listOf(ENTITYOVERLAY)),
        ENTITYOVERLAY_CREATE_COORD(listOf(_COORD, INT, INT, INT, INT, INT), listOf(ENTITYOVERLAY)),
        _7205(listOf(), listOf(INT)),
        _7206(listOf(), listOf(INT)),
        _7207(listOf(), listOf(INT)),
        _7208(listOf(), listOf(INT)),
        _7209(listOf(), listOf(INT)),
        ENTITYOVERLAY_DELETE_NPC(listOf(INT), listOf()),
        ENTITYOVERLAY_DELETE_LOC(listOf(INT), listOf()),
        ENTITYOVERLAY_DELETE_OBJ(listOf(INT), listOf()),
        ENTITYOVERLAY_DELETE_PLAYER(listOf(INT), listOf()),
        ENTITYOVERLAY_DELETE_COORD(listOf(_COORD, INT), listOf()),
        SETMINIMAPLOCK(listOf(BOOLEAN), listOf()),
        SET_MINIMAP_ZOOM(listOf(INT), listOf()),

        _7451(listOf(INT), listOf(INT)),
        _7453(listOf(INT), listOf(BOOLEAN)),
        _7454(listOf(INT), listOf(BOOLEAN)),
        _7455(listOf(INT), listOf(BOOLEAN)),
        _7456(listOf(INT), listOf(BOOLEAN)),
        _7460(listOf(), listOf(INT)),
        ;

        override val id = opcodes.getValue(name)

        private val defStackTypes = defs.map { it.stackType }

        override fun translate(state: InterpreterState): Instruction {
            val dot = o && state.operand.boolean
            val opArgs = Expression(state.pop(args.size))
            assign(state.typings.of(opArgs), state.typings.of(args))
            val operation = Expression.Operation(defStackTypes, id, opArgs, dot)
            val operationTyping = state.typings.of(operation)
            for (i in defs.indices) {
                operationTyping[i].freeze(defs[i])
            }
            val opDefs = Expression(state.push(defStackTypes))
            assign(state.typings.of(operation), state.typings.of(opDefs))
            return Instruction.Assignment(opDefs, operation)
        }
    }

    object JoinString : Command {

        override val id = JOIN_STRING

        override fun translate(state: InterpreterState): Instruction {
            val args = state.pop(state.operand.int)
            val operation = Expression.Operation(listOf(StackType.STRING), id, Expression(args))
            val def = state.push(StackType.STRING)
            return Instruction.Assignment(def, operation)
        }
    }

    enum class ClientScript : Command {

        CC_SETONCLICK,
        CC_SETONHOLD,
        CC_SETONRELEASE,
        CC_SETONMOUSEOVER,
        CC_SETONMOUSELEAVE,
        CC_SETONDRAG,
        CC_SETONTARGETLEAVE,
        CC_SETONVARTRANSMIT,
        CC_SETONTIMER,
        CC_SETONOP,
        CC_SETONDRAGCOMPLETE,
        CC_SETONCLICKREPEAT,
        CC_SETONMOUSEREPEAT,
        CC_SETONINVTRANSMIT,
        CC_SETONSTATTRANSMIT,
        CC_SETONTARGETENTER,
        CC_SETONSCROLLWHEEL,
        CC_SETONCHATTRANSMIT,
        CC_SETONKEY,
        CC_SETONFRIENDTRANSMIT,
        CC_SETONCLANTRANSMIT,
        CC_SETONMISCTRANSMIT,
        CC_SETONDIALOGABORT,
        CC_SETONSUBCHANGE,
        CC_SETONSTOCKTRANSMIT,
        CC_SETONTRADINGPOSTTRANSMIT,
        CC_SETONRESIZE,
        CC_SETONCLANSETTINGSTRANSMIT,
        CC_SETONCLANCHANNELTRANSMIT,
        CC_SETONKEYDOWN,
        CC_SETONKEYUP,
        CC_SETONCRMVIEWLOAD,
        IF_SETONCLICK,
        IF_SETONHOLD,
        IF_SETONRELEASE,
        IF_SETONMOUSEOVER,
        IF_SETONMOUSELEAVE,
        IF_SETONDRAG,
        IF_SETONTARGETLEAVE,
        IF_SETONVARTRANSMIT,
        IF_SETONTIMER,
        IF_SETONOP,
        IF_SETONDRAGCOMPLETE,
        IF_SETONCLICKREPEAT,
        IF_SETONMOUSEREPEAT,
        IF_SETONINVTRANSMIT,
        IF_SETONSTATTRANSMIT,
        IF_SETONTARGETENTER,
        IF_SETONSCROLLWHEEL,
        IF_SETONCHATTRANSMIT,
        IF_SETONKEY,
        IF_SETONFRIENDTRANSMIT,
        IF_SETONCLANTRANSMIT,
        IF_SETONMISCTRANSMIT,
        IF_SETONDIALOGABORT,
        IF_SETONSUBCHANGE,
        IF_SETONSTOCKTRANSMIT,
        IF_SETONTRADINGPOSTTRANSMIT,
        IF_SETONRESIZE,
        IF_SETONCLANSETTINGSTRANSMIT,
        IF_SETONCLANCHANNELTRANSMIT,
        IF_SETONKEYDOWN,
        IF_SETONKEYUP,
        IF_SETONCRMVIEWLOAD
        ;

        override val id = opcodes.getValue(name)

        override fun translate(state: InterpreterState): Instruction {
            var component: Element? = null
            var dot = false
            if (id >= 2000) {
                val c = state.pop(StackType.INT)
                assign(state.typings.of(c), state.typings.of(COMPONENT))
                component = c
            } else {
                dot = state.operand.boolean
            }
            val desc = ClientScriptDesc(state.popValue().string)
            val triggers = ArrayList<Element>()
            if (desc.triggers) {
                val triggerCount = state.popValue().int
                repeat(triggerCount) {
                    val e = when (this) {
                        IF_SETONSTATTRANSMIT, CC_SETONSTATTRANSMIT -> {
                            state.pop(StackType.INT).also { assign(state.typings.of(it), state.typings.of(_STAT)) }
                        }
                        IF_SETONINVTRANSMIT, CC_SETONINVTRANSMIT -> {
                            state.pop(StackType.INT).also { assign(state.typings.of(it), state.typings.of(INV)) }
                        }
                        IF_SETONVARTRANSMIT, CC_SETONVARTRANSMIT -> {
                            Element.Pointer(Variable.varp(state.popValue().int))
                        }
                        else -> error(this)
                    }
                    triggers.add(e)
                }
                triggers.reverse()
            }
            val args = ArrayList<Element>(desc.argumentTypes.size)
            for (t in desc.argumentTypes.asReversed()) {
                val ep = state.peekValue()?.let { EventProperty.of(it) }
                val p = state.pop(t.stackType)
                args.add(ep ?: p)
            }
            val scriptId = state.popValue().int
            state.callGraph.call(state.scriptId, scriptId, Trigger.clientscript)
            args.reverse()
            val argsExpr = Expression(args)
            val argsTyping = state.typings.args(scriptId, args.map { it.stackType })
            for (i in desc.argumentTypes.indices) {
                argsTyping[i].freeze(desc.argumentTypes[i])
            }
            assign(state.typings.of(argsExpr), argsTyping)
            return Instruction.Assignment(Expression.ClientScript(id, scriptId, argsExpr, Expression(triggers), dot, component))
        }
    }

    enum class Param(val prototype: Prototype) : Command {

        NC_PARAM(NPC),
        LC_PARAM(LOC),
        OC_PARAM(OBJ),
        STRUCT_PARAM(STRUCT),
        ;

        override val id = opcodes.getValue(name)

        override fun translate(state: InterpreterState): Instruction {
            val paramId = checkNotNull(state.peekValue()).int
            val paramPrototype = state.paramTypes.loadNotNull(paramId)
            val paramType = paramPrototype.type
            val param = state.pop(StackType.INT)
            assign(state.typings.of(param), state.typings.of(PARAM))
            val recv = state.pop(StackType.INT)
            assign(state.typings.of(recv), state.typings.of((prototype)))
            val operation = Expression.Operation(listOf(paramType.stackType), id, Expression(recv, param))
            val operationTyping = state.typings.of(operation).single()
            operationTyping.freeze(paramType)
            val def = state.push(paramType.stackType)
            assign(operationTyping, state.typings.of(paramPrototype))
            return Instruction.Assignment(def, operation)
        }
    }
}