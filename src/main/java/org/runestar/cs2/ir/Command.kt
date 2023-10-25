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
            add(EnumHasOutput)
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

    object EnumHasOutput : Command {

        override val id = ENUM_HASOUTPUT

        override fun translate(state: InterpreterState): Instruction {
            val key = state.pop(StackType.INT)
            val enumId = state.pop(StackType.INT)
            assign(state.typings.of(enumId), state.typings.of(_ENUM))
            val keyType = Type.of((checkNotNull(state.peekValue()).int).toByte())
            assign(state.typings.of(key), state.typings.of(Prototype(keyType)))
            val keyTypeVar = state.pop(StackType.INT)
            assign(state.typings.of(keyTypeVar), state.typings.of(TYPE))
            val args = Expression(keyTypeVar, enumId, key)
            val value = state.push(StackType.INT)
            val operation = Expression.Operation(listOf(StackType.INT), id, args)
            val operationTyping = state.typings.of(operation).single()
            operationTyping.freeze(BOOLEAN)
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
        CC_DELETEALL(listOf(COMPONENT), listOf(), true),
        CC_FIND(listOf(COMPONENT, COMSUBID), listOf(BOOL), true),
        IF_FIND(listOf(COMPONENT), listOf(BOOLEAN), true),

        BASEIDKIT(listOf(INT, IDKIT), listOf()),
        BASECOLOUR(listOf(INT, INT), listOf()),
        SETGENDER(listOf(INT), listOf()),

        CC_SETPOSITION(listOf(X, Y, SETPOSH, SETPOSV), listOf(), true),
        CC_SETSIZE(listOf(WIDTH, HEIGHT, SETSIZE, SETSIZE), listOf(), true),
        CC_SETHIDE(listOf(BOOLEAN), listOf(), true),
        CC_SETASPECT(listOf(WIDTH, HEIGHT), listOf(), true),
        CC_SETNOCLICKTHROUGH(listOf(BOOLEAN), listOf(), true),

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
        _1121(listOf(INT, INT), listOf(), true),
        CC_SETALPHA(listOf(BOOLEAN), listOf(), true),
        CC_SETMODELZOOM(listOf(INT), listOf(), true),
        CC_SETLINEDIRECTION(listOf(BOOLEAN), listOf(), true),

        CC_SETOBJECT(listOf(OBJ, NUM), listOf(), true),
        CC_SETNPCHEAD(listOf(NPC), listOf(), true),
        CC_SETPLAYERHEAD_SELF(listOf(), listOf(), true),
        CC_SETNPCMODEL(listOf(NPC), listOf(), true),
        CC_SETPLAYERMODEL(listOf(), listOf(), true),
        CC_SETOBJECT_NONUM(listOf(OBJ, NUM), listOf(), true),
        CC_SETOBJECT_WEARCOL(listOf(OBJ, NUM), listOf(), true),
        CC_SETOBJECT_WEARCOL_NONUM(listOf(OBJ, NUM), listOf(), true),
        CC_SETPLAYERMODEL_SELF(listOf(), listOf(), true),
        CC_SETOBJECT_ALWAYSNUM(listOf(OBJ, NUM), listOf(), true),
        CC_SETOBJECT_WEARCOL_ALWAYSNUM(listOf(OBJ, NUM), listOf(), true),

        CC_SETOP(listOf(OPINDEX, OP), listOf(), true),
        CC_SETDRAGGABLE(listOf(COMPONENT, INT), listOf(), true),
        CC_SETDRAGGABLEBEHAVIOR(listOf(INT), listOf(), true),
        CC_SETDRAGDEADZONE(listOf(INT), listOf(), true),
        CC_SETDRAGDEADTIME(listOf(INT), listOf(), true),
        CC_SETOPBASE(listOf(OPBASE), listOf(), true),
        CC_SETTARGETVERB(listOf(STRING), listOf(), true),
        CC_CLEAROPS(listOf(), listOf(), true),
        CC_SETTARGETCURSORS(listOf(CURSOR, CURSOR), listOf(), true),
        CC_SETOPCURSOR(listOf(INT, CURSOR), listOf(), true),
        CC_SETPAUSETEXT(listOf(STRING), listOf(), true),
        CC_SETTARGETOPCURSOR(listOf(CURSOR), listOf(), true),

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
        CC_GETMODELXOF(listOf(), listOf(INT), true),
        CC_GETMODELYOF(listOf(), listOf(INT), true),
        CC_GETGRAPHIC(listOf(), listOf(GRAPHIC), true),

        CC_GETINVOBJECT(listOf(), listOf(OBJ), true),
        CC_GETINVCOUNT(listOf(), listOf(COUNT), true),
        CC_GETID(listOf(), listOf(COMSUBID), true),

        CC_GETTARGETMASK(listOf(), listOf(INT), true),
        CC_GETOP(listOf(INT), listOf(OP), true),
        CC_GETOPBASE(listOf(), listOf(OPBASE), true),

        IF_SETPOSITION(listOf(X, Y, SETPOSH, SETPOSV, COMPONENT), listOf()),
        IF_SETSIZE(listOf(WIDTH, HEIGHT, SETSIZE, SETSIZE, COMPONENT), listOf()),
        IF_SETHIDE(listOf(BOOLEAN, COMPONENT), listOf()),
        IF_SETASPECT(listOf(WIDTH, HEIGHT, COMPONENT), listOf()),
        IF_SETNOCLICKTHROUGH(listOf(BOOLEAN, COMPONENT), listOf()),

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
        _2121(listOf(INT, INT, COMPONENT), listOf()),
        IF_SETALPHA(listOf(BOOLEAN, COMPONENT), listOf()),
        IF_SETMODELZOOM(listOf(INT, COMPONENT), listOf()),

        IF_SETOBJECT(listOf(OBJ, NUM, COMPONENT), listOf()),
        IF_SETNPCHEAD(listOf(NPC, COMPONENT), listOf()),
        IF_SETPLAYERHEAD_SELF(listOf(COMPONENT), listOf()),
        IF_SETNPCMODEL(listOf(NPC, COMPONENT), listOf()),
        IF_SETPLAYERMODEL(listOf(COMPONENT), listOf()),
        IF_SETOBJECT_NONUM(listOf(OBJ, NUM, COMPONENT), listOf()),
        IF_SETOBJECT_WEARCOL(listOf(OBJ, NUM, COMPONENT), listOf()),
        IF_SETOBJECT_WEARCOL_NONUM(listOf(OBJ, NUM, COMPONENT), listOf()),
        IF_SETPLAYERMODEL_SELF(listOf(COMPONENT), listOf()),
        IF_SETOBJECT_ALWAYSNUM(listOf(OBJ, NUM, COMPONENT), listOf()),
        IF_SETOBJECT_WEARCOL_ALWAYSNUM(listOf(OBJ, NUM, COMPONENT), listOf()),

        IF_SETOP(listOf(OPINDEX, OP, COMPONENT), listOf()),
        IF_SETDRAGGABLE(listOf(COMPONENT, INT, COMPONENT), listOf()),
        IF_SETDRAGGABLEBEHAVIOR(listOf(INT, COMPONENT), listOf()),
        IF_SETDRAGDEADZONE(listOf(INT, COMPONENT), listOf()),
        IF_SETDRAGDEADTIME(listOf(INT, COMPONENT), listOf()),
        IF_SETOPBASE(listOf(OPBASE, COMPONENT), listOf()),
        IF_SETTARGETVERB(listOf(STRING, COMPONENT), listOf()),
        IF_CLEAROPS(listOf(COMPONENT), listOf()),
        IF_SETTARGETCURSORS(listOf(CURSOR, CURSOR, COMPONENT), listOf()),
        IF_SETOPCURSOR(listOf(INT, CURSOR, COMPONENT), listOf()),
        IF_SETPAUSETEXT(listOf(STRING, COMPONENT), listOf()),
        IF_SETTARGETOPCURSOR(listOf(CURSOR, COMPONENT), listOf()),

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
        IF_GETMODELXOF(listOf(COMPONENT), listOf(INT)),
        IF_GETMODELYOF(listOf(COMPONENT), listOf(INT)),
        IF_GETGRAPHIC(listOf(COMPONENT), listOf(GRAPHIC)),

        IF_GETINVOBJECT(listOf(COMPONENT), listOf(OBJ)),
        IF_GETINVCOUNT(listOf(COMPONENT), listOf(COUNT)),
        IF_HASSUB(listOf(COMPONENT), listOf(BOOLEAN)),
        IF_GETNEXTSUBID(listOf(COMPONENT), listOf(COMSUBID)),
        IF_HASSUBMODAL(listOf(COMPONENT, INTERFACE), listOf(BOOLEAN)),
        IF_HASSUBOVERLAY(listOf(COMPONENT, OVERLAYINTERFACE), listOf(BOOLEAN)),

        IF_GETTARGETMASK(listOf(COMPONENT), listOf(INT)),
        IF_GETOP(listOf(INT, COMPONENT), listOf(OP)),
        IF_GETOPBASE(listOf(COMPONENT), listOf(OPBASE)),

        MES(listOf(_MES), listOf()),
        ANIM(listOf(SEQ, INT), listOf()),
        MES_TYPED(listOf(CHATTYPE, STRING), listOf()),
        IF_CLOSE(listOf(), listOf()),
        RESUME_COUNTDIALOG(listOf(STRING), listOf()),
        RESUME_NAMEDIALOG(listOf(STRING), listOf()),
        RESUME_STRINGDIALOG(listOf(STRING), listOf()),
        OPPLAYER(listOf(INT, STRING), listOf()),
        IF_DRAGPICKUP(listOf(COMPONENT, INT, INT), listOf()),
        CC_DRAGPICKUP(listOf(INT, INT), listOf(), true),
        RESUME_OBJDIALOG(listOf(OBJ), listOf()),
        IF_OPENSUBCLIENT(listOf(COMPONENT, CLIENTINTERFACE), listOf()),
        IF_CLOSESUBCLIENT(listOf(COMPONENT), listOf()),

        SOUND_SYNTH(listOf(SYNTH, INT, INT), listOf()),
        SOUND_SONG(listOf(INT), listOf()),
        SOUND_JINGLE(listOf(INT, INT), listOf()),

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
        PLAYERMOD(listOf(), listOf(BOOL)),
        PLAYERMODLEVEL(listOf(), listOf(INT)),
        PLAYERMEMBER(listOf(), listOf(BOOLEAN)),
        COMLEVEL_ACTIVE(listOf(), listOf(INT)),
        GENDER(listOf(), listOf(INT)),
        _3328(listOf(), listOf(BOOLEAN)),
        MAP_QUICKCHAT(listOf(), listOf(BOOLEAN)),
        INV_FREESPACE(listOf(INV), listOf(TOTAL)),
        INV_TOTALPARAM(listOf(INV, PARAM), listOf(TOTAL)),
        INV_TOTALPARAM_STACK(listOf(INV, PARAM), listOf(TOTAL)),
        _3333(listOf(), listOf(INT)),
        MAP_LANG(listOf(), listOf(INT)),
        MOVECOORD(listOf(_COORD, X, Y, Z), listOf(_COORD)),
        AFFILIATE(listOf(), listOf(INT)),
        PROFILE_CPU(listOf(), listOf(INT)),

        ENUM_STRING(listOf(_ENUM, INT), listOf(STRING)),
        ENUM_HASOUTPUT_STRING(listOf(_ENUM, STRING), listOf(BOOLEAN)),
        ENUM_GETOUTPUTCOUNT(listOf(_ENUM), listOf(COUNT)),

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
        FRIEND_GETWORLDNAME(listOf(INDEX), listOf(STRING)),
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
        CLAN_GETCHATUSERWORLDNAME(listOf(INDEX), listOf(STRING)),
        FRIEND_PLATFORM(listOf(INDEX), listOf(BOOLEAN)),
        FRIEND_GETSLOTFROMNAME(listOf(USERNAME), listOf(INDEX)),
        PLAYERCOUNTRY(listOf(), listOf(INT)),
        IGNORE_ADD_TEMP(listOf(USERNAME), listOf(INT)),
        IGNORE_IS_TEMP(listOf(INT), listOf(BOOLEAN)),
        CLAN_GETCHATUSERNAME_UNFILTERED(listOf(INT), listOf(STRING)),
        IGNORE_GETNAME_UNFILTERED(listOf(INT), listOf(STRING)),

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

        ADD(listOf(INT, INT), listOf(INT)),
        SUB(listOf(INT, INT), listOf(INT)),
        MULTIPLY(listOf(INT, INT), listOf(INT)),
        DIV(listOf(INT, INT), listOf(INT)),
        RANDOM(listOf(INT), listOf(INT)),
        RANDOMINC(listOf(INT), listOf(INT)),
        INTERPOLATE(listOf(INT, INT, INT, INT, INT), listOf(INT)),
        ADDPERCENT(listOf(INT, INT), listOf(INT)),
        SETBIT(listOf(FLAGS, INDEX), listOf(FLAGS)),
        CLEARBIT(listOf(FLAGS, INDEX), listOf(FLAGS)),
        TESTBIT(listOf(FLAGS, INDEX), listOf(BOOL)),
        MOD(listOf(INT, INT), listOf(INT)),
        POW(listOf(INT, INT), listOf(INT)),
        INVPOW(listOf(INT, INT), listOf(INT)),
        AND(listOf(INT, INT), listOf(INT)),
        OR(listOf(INT, INT), listOf(INT)),
        MIN(listOf(INT, INT), listOf(INT)),
        MAX(listOf(INT, INT), listOf(INT)),
        SCALE(listOf(INT, INT, INT), listOf(INT)),

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
        TEXT_SWITCH(listOf(BOOLEAN, STRING, STRING), listOf(STRING)),
        ESCAPE(listOf(STRING), listOf(STRING)),
        APPEND_CHAR(listOf(STRING, CHAR), listOf(STRING)),
        CHAR_ISPRINTABLE(listOf(CHAR), listOf(BOOLEAN)),
        CHAR_ISALPHANUMERIC(listOf(CHAR), listOf(BOOLEAN)),
        CHAR_ISALPHA(listOf(CHAR), listOf(BOOLEAN)),
        CHAR_ISNUMERIC(listOf(CHAR), listOf(BOOLEAN)),
        STRING_LENGTH(listOf(STRING), listOf(LENGTH)),
        SUBSTRING(listOf(STRING, INDEX, INDEX), listOf(STRING)),
        REMOVETAGS(listOf(STRING), listOf(STRING)),
        STRING_INDEXOF_CHAR(listOf(STRING, CHAR, INDEX), listOf(INDEX)),
        STRING_INDEXOF_STRING(listOf(STRING, STRING, INDEX), listOf(INDEX)),
        CHAR_TOLOWERCASE(listOf(CHAR), listOf(CHAR)),
        CHAR_TOUPPERCASE(listOf(CHAR), listOf(CHAR)),
        TOSTRING_LOCALISED(listOf(INT, BOOLEAN), listOf(STRING)),
        STRINGWIDTH(listOf(STRING, FONTMETRICS), listOf(WIDTH)),

        OC_NAME(listOf(OBJ), listOf(STRING)),
        OC_OP(listOf(OBJ, OPINDEX), listOf(OP)),
        OC_IOP(listOf(OBJ, OPINDEX), listOf(OP)),
        OC_COST(listOf(OBJ), listOf(INT)),
        OC_STACKABLE(listOf(OBJ), listOf(BOOLEAN)),
        OC_CERT(listOf(OBJ), listOf(OBJ)),
        OC_UNCERT(listOf(OBJ), listOf(OBJ)),
        OC_MEMBERS(listOf(OBJ), listOf(BOOL)),
        OC_ICURSOR(listOf(OBJ, INT), listOf(CURSOR)),
        OC_FIND(listOf(STRING, BOOLEAN), listOf(INT)),
        OC_FINDNEXT(listOf(), listOf(OBJ)),
        OC_FINDRESET(listOf(), listOf()),

        BAS_GETANIM_READY(listOf(BAS), listOf(SEQ)),

        CHAT_GETFILTER_PUBLIC(listOf(), listOf(CHATFILTER)),
        CHAT_SETFILTER(listOf(CHATFILTER, CHATFILTER, CHATFILTER), listOf()),
        CHAT_SENDABUSEREPORT(listOf(STRING, INT, INT, STRING), listOf()),
        CHAT_GETHISTORYMESSAGE(listOf(INT), listOf(_MES)),
        CHAT_GETHISTORYTYPE(listOf(INT), listOf(CHATTYPE)),
        CHAT_GETFILTER_PRIVATE(listOf(), listOf(CHATFILTER)),
        CHAT_SENDPUBLIC(listOf(_MES), listOf()),
        CHAT_SENDPRIVATE(listOf(USERNAME, _MES), listOf()),
        CHAT_GETHISTORYNAME(listOf(INT), listOf(USERNAME)),
        CHAT_GETHISTORYCLAN(listOf(INT), listOf(STRING)),
        CHAT_GETHISTORYPHRASE(listOf(INT), listOf(CHATPHRASE)),
        CHAT_PLAYERNAME_UNFILTERED(listOf(), listOf(USERNAME)),
        CHAT_GETFILTER_TRADE(listOf(), listOf(CHATFILTER)),
        CHAT_GETHISTORYLENGTH(listOf(), listOf(LENGTH)),
        _5018(listOf(INT), listOf(CHATTYPE)),
        _5019(listOf(INT), listOf(STRING)),
        CHAT_PLAYERNAME(listOf(), listOf(USERNAME)),

        CHATCAT_GETDESC(listOf(CHATCAT), listOf(STRING)),
        CHATCAT_GETSUBCATCOUNT(listOf(CHATCAT), listOf(INT)),
        CHATCAT_GETSUBCAT(listOf(CHATCAT, INT), listOf(CHATCAT)),
        CHATCAT_GETPHRASECOUNT(listOf(CHATCAT), listOf(COUNT)),
        CHATCAT_GETPHRASE(listOf(CHATCAT, INT), listOf(CHATPHRASE)),
        CHATPHRASE_GETTEXT(listOf(CHATPHRASE), listOf(_MES)),
        CHATPHRASE_GETAUTORESPONSECOUNT(listOf(CHATPHRASE), listOf(INT)),
        CHATPHRASE_GETAUTORESPONSE(listOf(CHATPHRASE, INT), listOf(CHATPHRASE)),
        ACTIVECHATPHRASE_PREPARE(listOf(CHATPHRASE), listOf()),
        ACTIVECHATPHRASE_SENDPUBLIC(listOf(), listOf()),
        ACTIVECHATPHRASE_SENDPRIVATE(listOf(USERNAME), listOf()),
        ACTIVECHATPHRASE_SENDCLAN(listOf(), listOf()),
        CHATCAT_GETSUBCATSHORTCUT(listOf(CHATCAT, INT), listOf(CHAR)),
        CHATCAT_GETPHRASESHORTCUT(listOf(CHATCAT, INT), listOf(CHAR)),
        CHATCAT_FINDSUBCATBYSHORTCUT(listOf(CHATCAT, CHAR), listOf(CHATCAT)),
        CHATCAT_FINDPHRASEBYSHORTCUT(listOf(CHATCAT, CHAR), listOf(CHATPHRASE)),
        CHATPHRASE_GETDYNAMICCOMMANDCOUNT(listOf(CHATPHRASE), listOf(COUNT)),
        CHATPHRASE_GETDYNAMICCOMMAND(listOf(CHATPHRASE, INT), listOf(INT)),
        ACTIVECHATPHRASE_SETDYNAMICINT(listOf(INT, INT), listOf()),
        ACTIVECHATPHRASE_SETDYNAMICOBJ(listOf(INT, OBJ), listOf()),
        CHATPHRASE_GETDYNAMICCOMMANDPARAM_ENUM(listOf(CHATPHRASE, INT, INT), listOf(_ENUM)),
        CHATPHRASE_FIND(listOf(STRING, BOOLEAN), listOf(COUNT)),
        CHATPHRASE_FINDNEXT(listOf(), listOf(CHATPHRASE)),
        CHATPHRASE_FINDRESTART(listOf(), listOf()),

        KEYHELD_ALT(listOf(), listOf(BOOLEAN)),
        KEYHELD_CTRL(listOf(), listOf(BOOLEAN)),
        KEYHELD_SHIFT(listOf(), listOf(BOOLEAN)),

        WORLDMAP_SETZOOM(listOf(INT), listOf()),
        WORLDMAP_GETZOOM(listOf(), listOf(INT)),
        _5205(listOf(MAPAREA), listOf()),
        WORLDMAP_GETMAP(listOf(_COORD), listOf(MAPAREA)),
        WORLDMAP_GETMAPNAME(listOf(MAPAREA), listOf(STRING)),
        WORLDMAP_GETSIZE(listOf(), listOf(INT, INT)),
        WORLDMAP_GETDISPLAYPOSITION(listOf(), listOf(INT, INT)),
        _5210(listOf(MAPAREA), listOf(INT, INT)),
        WORLDMAP_LISTELEMENT_START(listOf(), listOf(MAPELEMENT, _COORD)),
        WORLDMAP_LISTELEMENT_NEXT(listOf(), listOf(MAPELEMENT, _COORD)),
        WORLDMAP_JUMPTOSOURCECOORD(listOf(_COORD), listOf()),
        WORLDMAP_COORDINMAP(listOf(_COORD, MAPAREA), listOf(BOOLEAN)),
        WORLDMAP_GETCONFIGZOOM(listOf(MAPAREA), listOf(INT)),
        WORLDMAP_ISLOADED(listOf(), listOf(BOOLEAN)),
        WORLDMAP_JUMPTODISPLAYCOORD(listOf(_COORD), listOf()),
        WORLDMAP_GETSOURCEPOSITION(listOf(), listOf(INT, INT)),
        WORLDMAP_SETMAP_COORD(listOf(MAPAREA, _COORD), listOf()),
        WORLDMAP_GETDISPLAYCOORD(listOf(_COORD), listOf(INT, INT)),
        WORLDMAP_GETSOURCECOORD(listOf(_COORD), listOf(INT, INT)),
        WORLDMAP_FLASHELEMENT(listOf(INT), listOf()),
        WORLDMAP_SETMAP_COORD_OVERRIDE(listOf(MAPAREA, _COORD), listOf()),
        WORLDMAP_DISABLEELEMENTS(listOf(BOOLEAN), listOf()),
        WORLDMAP_GETDISABLEELEMENTS(listOf(), listOf(BOOLEAN)),
        WORLDMAP_FLASHELEMENTCATEGORY(listOf(CATEGORY), listOf()),
        WORLDMAP_DISABLEELEMENTCATEGORY(listOf(CATEGORY, BOOLEAN), listOf()),
        WORLDMAP_GETDISABLEELEMENTCATEGORY(listOf(), listOf(BOOLEAN)),
        WORLDMAP_DISABLEELEMENT(listOf(INT, BOOLEAN), listOf()),
        WORLDMAP_GETDISABLEELEMENT(listOf(), listOf(BOOLEAN)),
        WORLDMAP_GETCURRENTMAP(listOf(), listOf(MAPAREA)),

        FULLSCREEN_ENTER(listOf(WIDTH, HEIGHT), listOf(BOOLEAN)),
        FULLSCREEN_EXIT(listOf(), listOf()),
        FULLSCREEN_MODECOUNT(listOf(), listOf(COUNT)),
        FULLSCREEN_GETMODE(listOf(WINDOWMODE), listOf(WIDTH, HEIGHT)),
        FULLSCREEN_LASTMODE(listOf(), listOf(WINDOWMODE)),
        GETWINDOWMODE(listOf(), listOf(WINDOWMODE)),
        SETWINDOWMODE(listOf(WINDOWMODE), listOf()),
        GETDEFAULTWINDOWMODE(listOf(), listOf(WINDOWMODE)),
        SETDEFAULTWINDOWMODE(listOf(WINDOWMODE), listOf()),

        OPENURL(listOf(STRING, STRING, BOOLEAN), listOf()),
        _5401(listOf(INT, INT), listOf()),
        SPLINE_NEW(listOf(INT, INT), listOf()),
        SPLINE_ADDPOINT(listOf(INT, INT, _COORD, INT, _COORD, INT, INT), listOf()),
        SPLINE_LENGTH(listOf(INT), listOf(INT)),
        QUIT(listOf(), listOf()),
        LASTLOGIN(listOf(), listOf(STRING)),
        _5420(listOf(), listOf(BOOLEAN)),
        OPENURL_NOLOGIN(listOf(STRING, BOOLEAN), listOf()),
        _5422(listOf(INT, STRING, STRING), listOf()),
        WRITECONSOLE(listOf(STRING), listOf()),
        FORMATMINIMENU(listOf(COLOUR, TRANS, COLOUR, TRANS, GRAPHIC, GRAPHIC, GRAPHIC, GRAPHIC, GRAPHIC, COLOUR, COLOUR), listOf()),
        DEFAULTMINIMENU(listOf(), listOf()),
        SETDEFAULTCURSORS(listOf(CURSOR, CURSOR), listOf()),
        SETHARDCODEDOPCURSORS(listOf(CURSOR, CURSOR), listOf()),
        MINIMENUOPEN(listOf(COMPONENT, INT), listOf(BOOLEAN)),
        DOCHEAT(listOf(STRING), listOf()),

        CAM_MOVETO(listOf(_COORD, INT, INT, INT), listOf()),
        CAM_LOOKAT(listOf(_COORD, INT, INT, INT), listOf()),
        CAM_MOVEALONG(listOf(INT, INT, INT, INT, INT, INT), listOf()),
        CAM_RESET(listOf(), listOf()),
        CAM_FORCEANGLE(listOf(INT, INT), listOf()),
        CAM_GETANGLE_XA(listOf(), listOf(INT)),
        CAM_GETANGLE_YA(listOf(), listOf(INT)),
        CAM_INC_X(listOf(), listOf()),
        CAM_DEC_X(listOf(), listOf()),
        CAM_INC_Y(listOf(), listOf()),
        CAM_DEC_Y(listOf(), listOf()),
        CAM_FOLLOWCOORD(listOf(_COORD), listOf()),
        CAM_SMOOTHRESET(listOf(), listOf()),

        LOGIN_REQUEST(listOf(STRING, STRING, INT), listOf()),
        LOGIN_CONTINUE(listOf(), listOf()),
        LOGIN_RESETREPLY(listOf(), listOf()),
        _5603(listOf(INT, INT, INT, INT), listOf()),
        _5604(listOf(STRING), listOf()),
        _5605(listOf(STRING, STRING, INT, INT, INT, INT, STRING, INT, INT, INT), listOf()),
        _5606(listOf(), listOf()),
        LOGIN_REPLY(listOf(), listOf(INT)),
        LOGIN_HOPTIME(listOf(), listOf(INT)),
        CREATE_REPLY(listOf(), listOf(INT)),
        _5610(listOf(), listOf(STRING, STRING, STRING, STRING, STRING)),
        LOGIN_DISALLOWRESULT(listOf(), listOf(INT)),

        DETAIL_BRIGHTNESS(listOf(INT), listOf()),
        _6002(listOf(BOOLEAN), listOf()),
        DETAIL_REMOVEROOFS_OPTION(listOf(BOOLEAN), listOf()),
        DETAIL_GROUNDDECOR_ON(listOf(BOOLEAN), listOf()),
        _6006(listOf(BOOLEAN), listOf()),
        DETAIL_IDLEANIMS_MANY(listOf(BOOLEAN), listOf()),
        DETAIL_FLICKERING_ON(listOf(BOOLEAN), listOf()),
        _6009(listOf(BOOLEAN), listOf()),
        _6010(listOf(BOOLEAN), listOf()),
        _6011(listOf(INT), listOf()),
        DETAIL_LIGHTDETAIL_HIGH(listOf(BOOLEAN), listOf()),
        DETAIL_WATERDETAIL_HIGH(listOf(BOOLEAN), listOf()),
        DETAIL_FOG_ON(listOf(BOOLEAN), listOf()),
        DETAIL_ANTIALIASING(listOf(INT), listOf()),
        DETAIL_STEREO(listOf(BOOLEAN), listOf()),
        DETAIL_SOUNDVOL(listOf(INT), listOf()),
        DETAIL_MUSICVOL(listOf(INT), listOf()),
        DETAIL_BGSOUNDVOL(listOf(INT), listOf()),
        DETAIL_REMOVEROOFS_OPTION_OVERRIDE(listOf(BOOLEAN), listOf()),
        DETAIL_PARTICLES(listOf(INT), listOf(BOOLEAN)),
        DETAIL_ANTIALIASING_DEFAULT(listOf(INT), listOf()),
        _6025(listOf(INT), listOf()),
        DETAIL_BLOOM(listOf(BOOLEAN), listOf()),
        DETAIL_CUSTOMCURSORS(listOf(BOOLEAN), listOf()),
        DETAIL_IDLEANIMS(listOf(INT), listOf()),
        DETAIL_GROUNDBLENDING(listOf(BOOLEAN), listOf()),
        DETAIL_TOOLKIT(listOf(INT), listOf()),
        DETAIL_TOOLKIT_DEFAULT(listOf(INT), listOf()),
        DETAIL_CPUUSAGE(listOf(INT), listOf()),

        DETAILGET_BRIGHTNESS(listOf(), listOf(INT)),
        _6102(listOf(), listOf(BOOLEAN)),
        DETAILGET_REMOVEROOFS_OPTION(listOf(), listOf(BOOLEAN)),
        DETAILGET_GROUNDDECOR_ON(listOf(), listOf(BOOLEAN)),
        _6106(listOf(), listOf(BOOLEAN)),
        DETAILGET_IDLEANIMS_MANY(listOf(), listOf(BOOLEAN)),
        DETAILGET_FLICKERING_ON(listOf(), listOf(BOOLEAN)),
        _6109(listOf(), listOf(BOOLEAN)),
        _6110(listOf(), listOf(BOOLEAN)),
        _6111(listOf(), listOf(INT)),
        DETAILGET_LIGHTDETAIL_HIGH(listOf(), listOf(BOOLEAN)),
        DETAILGET_WATERDETAIL_HIGH(listOf(), listOf(BOOLEAN)),
        DETAILGET_FOG_ON(listOf(), listOf(BOOLEAN)),
        DETAILGET_ANTIALIASING(listOf(), listOf(INT)),
        DETAILGET_STEREO(listOf(), listOf(BOOLEAN)),
        DETAILGET_SOUNDVOL(listOf(), listOf(INT)),
        DETAILGET_MUSICVOL(listOf(), listOf(INT)),
        DETAILGET_BGSOUNDVOL(listOf(), listOf(INT)),
        _6121(listOf(), listOf(BOOLEAN)),
        DETAILGET_PARTICLES(listOf(), listOf(INT)),
        DETAILGET_ANTIALIASING_DEFAULT(listOf(), listOf(INT)),
        DETAILGET_BUILDAREA(listOf(), listOf(INT)),
        _6126(listOf(), listOf(BOOLEAN)),
        DETAILGET_BLOOM(listOf(), listOf(INT)),
        DETAILGET_CUSTOMCURSORS(listOf(), listOf(BOOLEAN)),
        DETAILGET_IDLEANIMS(listOf(), listOf(INT)),
        DETAILGET_GROUNDBLENDING(listOf(), listOf(BOOLEAN)),
        DETAILGET_TOOLKIT(listOf(), listOf(INT)),
        DETAILGET_TOOLKIT_DEFAULT(listOf(), listOf(INT)),
        _6133(listOf(), listOf(BOOLEAN)),
        DETAILGET_CPUUSAGE(listOf(), listOf(INT)),

        VIEWPORT_SETFOV(listOf(INT, INT), listOf()),
        VIEWPORT_SETZOOM(listOf(INT, INT), listOf()),
        VIEWPORT_CLAMPFOV(listOf(INT, INT, INT, INT), listOf()),
        VIEWPORT_GETEFFECTIVESIZE(listOf(), listOf(WIDTH, HEIGHT)),
        VIEWPORT_GETZOOM(listOf(), listOf(INT, INT)),
        VIEWPORT_GETFOV(listOf(), listOf(INT, INT)),

        DATE_MINUTES(listOf(), listOf(INT)),
        DATE_RUNEDAY(listOf(), listOf(INT)),
        DATE_RUNEDAY_FROMDATE(listOf(INT, INT, INT), listOf(INT)),
        DATE_YEAR(listOf(), listOf(INT)),
        DATE_ISLEAPYEAR(listOf(INT), listOf(BOOLEAN)),

        VIDEO_ADVERT_PLAY(listOf(), listOf(BOOLEAN)),
        VIDEO_ADVERT_HAS_FINISHED(listOf(), listOf(BOOLEAN)),

        WORLDLIST_FETCH(listOf(), listOf(BOOLEAN)),
        WORLDLIST_START(listOf(), listOf(WORLD, FLAGS, STRING, INT, STRING, COUNT, INT)),
        WORLDLIST_NEXT(listOf(), listOf(WORLD, FLAGS, STRING, INT, STRING, COUNT, INT)),
        WORLDLIST_SWITCH(listOf(WORLD), listOf(BOOLEAN)),
        _6504(listOf(WORLD), listOf()),
        _6505(listOf(), listOf(WORLD)),
        WORLDLIST_SPECIFIC(listOf(WORLD), listOf(FLAGS, STRING, INT, STRING, COUNT, INT)),
        WORLDLIST_SORT(listOf(INT, BOOLEAN, INT, BOOLEAN), listOf()),
        WORLDLIST_AUTOWORLD(listOf(), listOf()),
        WORLDLIST_PINGWORLDS(listOf(BOOLEAN), listOf()),

        MEC_TEXT(listOf(MAPELEMENT), listOf(STRING)),
        MEC_SPRITE(listOf(MAPELEMENT), listOf(GRAPHIC)),
        MEC_TEXTSIZE(listOf(MAPELEMENT), listOf(INT)),
        MEC_CATEGORY(listOf(MAPELEMENT), listOf(CATEGORY)),
        ;

        override val id = opcodes.getValue(name)

        private val defStackTypes = defs.map { it.stackType }

        override fun translate(state: InterpreterState): Instruction {
            require(o || !state.operand.boolean) { "non-zero operand: $this" }
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
        CC_SETONCAMFINISHED,
        CC_SETONRESIZE,
        CC_SETONVARCTRANSMIT,
        CC_SETONVARCSTRTRANSMIT,
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
        IF_SETONCAMFINISHED,
        IF_SETONRESIZE,
        IF_SETONVARCTRANSMIT,
        IF_SETONVARCSTRTRANSMIT,
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
                            val variable = Variable.varp(state.popValue().int)
                            state.typings.of(variable)
                            Element.Pointer(variable)
                        }
                        IF_SETONVARCTRANSMIT, CC_SETONVARCTRANSMIT -> {
                            val variable = Variable.varcint(state.popValue().int)
                            state.typings.of(variable)
                            Element.Pointer(variable)
                        }
                        IF_SETONVARCSTRTRANSMIT, CC_SETONVARCSTRTRANSMIT -> {
                            val variable = Variable.varcstring(state.popValue().int)
                            state.typings.of(variable)
                            Element.Pointer(variable)
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
        MEC_PARAM(MAPELEMENT),
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