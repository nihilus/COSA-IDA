package com.tosnos.cosa.binary.asm;

import com.tosnos.cosa.binary.LibraryModule;
import com.tosnos.cosa.binary.asm.value.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

/**
 * Created by kevin on 12/7/14.
 */
public class InstructionDecoder implements Modifier {
    private static final int MAX_MNEMONIC_LENGTH = 8;
    private static final short CONDITION_LENGTH = 2;
    private static final Map<String, Opcode> opcodeNameMap = new HashMap<String, Opcode>();
    private static final Map<String, Byte> conditionNameMap = new HashMap<String, Byte>();

    private static final Logger logger = LoggerFactory.getLogger(InstructionDecoder.class);

    static {
        Opcode[] values = Opcode.values();
        for (int i = 2; i < values.length; i++) {
            opcodeNameMap.put(values[i].name(), values[i]);
        }

        for (byte i = 0; i < conditions.length; i++) {
            conditionNameMap.put(conditions[i], i);
        }
    }

    private static final int FLAG_OPERAND = 0;
    private static final int FLAG_OPERAND_SET = 1;
    private static final int FLAG_ADDESS_SET = 2;

    public static boolean isShift(String str) {
        return str.matches("(ASR|LSL|LSR|ROR|RRX).+");
    }

    public static Instruction decode(LibraryModule libraryModule, int address, boolean thumb, String str) throws ParseException {
        Opcode opcode = null; // null;
        Byte condition = CONDI_AL; // unconditional
        int modifier = TYPE_INT;

        String opcodeStr, operandStr = null;
        String[] tokens = str.split("\\s+", 2);
        opcodeStr = tokens[0];
        if (tokens.length == 2) {
            operandStr = tokens[1].split("\\s+;")[0];
        }

        String types[] = opcodeStr.split("\\.");
        StringBuilder mnemonic = new StringBuilder(types[0]);

        if(types.length>1) {
            if(types[1].equals("W")) {
                modifier |= MODIFIER_WIDTH;
            } else if(types[1].equals("8")) {
                modifier |= VLD_DATA_8;
            } else if(types[1].equals("16")) {
                modifier |= VLD_DATA_16;
            } else if(types[1].equals("32")) {
                modifier |= VLD_DATA_32;
            } else if(types[1].equals("64")) {
                modifier |= VLD_DATA_64;

            } else {
                switch(types[1].charAt(0)) {
                    case 'I':
                        modifier |=VFP_DATA_I;
                        break;
                    case 'F':
                        modifier |=VFP_DATA_F;
                        break;
                    case 'U':
                        modifier |=VFP_DATA_U;
                        break;
                    case 'S':
                        modifier |=VFP_DATA_S;
                        break;
                    case 'P':
                        modifier |=VFP_DATA_P;
                        break;
                    default:
                        throw new RuntimeException("Unknown type "+types[1] + " " + str);
                }

                String width = types[1].substring(1);
                if("64".equals(width)) {
                    modifier |=VFP_DATA_64;
                } else if("32".equals(width)) {
                    modifier |=VFP_DATA_32;
                } else if("16".equals(width)) {
                    modifier |=VFP_DATA_16;
                } else if("8".equals(width)) {
                    modifier |=VFP_DATA_8;
                } else {
                    throw new RuntimeException("Unknown type "+types[1]);
                }

                if(types.length>3) {
                    switch(types[2].charAt(0)) {
                        case 'I':
                            modifier |=VFP_DATA_FROM_I;
                            break;
                        case 'F':
                            modifier |=VFP_DATA_FROM_F;
                            break;
                        case 'U':
                            modifier |=VFP_DATA_FROM_U;
                            break;
                        case 'S':
                            modifier |=VFP_DATA_FROM_S;
                            break;
                        default:
                            throw new RuntimeException("Unknown type "+types[1]);
                    }
                    width = types[2].substring(1);
                    if("64".equals(width)) {
                        modifier |=VFP_DATA_FROM_64;
                    } else if("32".equals(width)) {
                        modifier |=VFP_DATA_FROM_32;
                    } else if("16".equals(width)) {
                        modifier |=VFP_DATA_FROM_16;
                    } else if("8".equals(width)) {
                        modifier |=VFP_DATA_FROM_8;
                    } else {
                        throw new RuntimeException("Unknown type "+types[1]);
                    }
                }
            }
        }

        int length = mnemonic.length();
        for (int i = (length > MAX_MNEMONIC_LENGTH ? MAX_MNEMONIC_LENGTH : length); i > 0; i--) {
            opcode = opcodeNameMap.get(mnemonic.substring(0, i));
            if (opcode != null) {
                if (opcode == Opcode.BL && length == 3) {
                    opcode = Opcode.B;
                    i = 1;
                } else if (opcode == Opcode.IT) {
                    mnemonic.delete(0, i);
                    mnemonic.append(operandStr);
                    operandStr = null;
                    length = mnemonic.length();
                    break;
                }
                mnemonic.delete(0, i);
                length -= i;
                break;
            }
        }

        if (opcode != null && length > 0) {
            if (length >= CONDITION_LENGTH) {
                condition = conditionNameMap.get(mnemonic.substring(0, CONDITION_LENGTH));
                if (condition != null) {
                    mnemonic.delete(0, CONDITION_LENGTH);
                    length -= CONDITION_LENGTH;
                } else {
                    condition = CONDI_AL;
                }
            }

            if (opcode == Opcode.IT) {
                // use Modifier.... to keep condition switch
                modifier = length;
                for (int i = 0; i > length; i++) {
                    if (mnemonic.charAt(i) == 'T') {
                        modifier |= (1 << (2 + i));
                    }
                }
                mnemonic.delete(0, length);
                length = 0;
            }

            if (length >= 2) {
                String substr = mnemonic.substring(0, 2);
                switch (opcode) {
                    case SMUL:
                    case SMLA:
                        if ("BB".equals(substr)) {
                        modifier |= MODIFIER_MB | MODIFIER_SB;
                        mnemonic.delete(0, 2);
                    } else if ("BT".equals(substr)) {
                        modifier |= MODIFIER_MB | MODIFIER_ST;
                        mnemonic.delete(0, 2);
                    } else if ("TT".equals(substr)) {
                        modifier |= MODIFIER_MT | MODIFIER_ST;
                        mnemonic.delete(0, 2);
                    } else if ("TB".equals(substr)) {
                        modifier |= MODIFIER_MT | MODIFIER_SB;
                        mnemonic.delete(0, 2);
                    } else if ("WT".equals(substr)) {
                        modifier |= MODIFIER_ST | MODIFIER_WIDE;
                        mnemonic.delete(0, 2);
                    } else if ("WB".equals(substr)) {
                        modifier |= MODIFIER_SB | MODIFIER_WIDE;
                        mnemonic.delete(0, 2);
                    }
                        break;
                    case STR:
                    case LDR:
                        if ("EX".equals(substr)) {
                            modifier |= MODIFIER_EXTENTION;
                            mnemonic.delete(0, 2);
                        }
                        break;
                    case STM:
                        if ("EA".equals(substr) || "IA".equals(substr)) { // default
                            mnemonic.delete(0, 2);
                        } else if ("FD".equals(substr) || "DB".equals(substr)) {
                            modifier |= MODIFIER_DECREMENT;
                            modifier |= MODIFIER_BEFORE;
                            mnemonic.delete(0, 2);
                        } else if ("FA".equals(substr) || "IB".equals(substr)) {
                            modifier |= MODIFIER_BEFORE;
                            mnemonic.delete(0, 2);
                        } else if ("ED".equals(substr) || "DA".equals(substr)) {
                            modifier |= MODIFIER_DECREMENT;
                            mnemonic.delete(0, 2);
                        }
                        break;
                    case LDM:
                        if ("FD".equals(substr) || "IA".equals(substr)) {
                            mnemonic.delete(0, 2); // default
                        } else if ("FA".equals(substr) || "DA".equals(substr)) {
                            modifier |= MODIFIER_DECREMENT;
                            mnemonic.delete(0, 2);
                        } else if ("ED".equals(substr) || "IB".equals(substr)) {
                            modifier |= MODIFIER_BEFORE;
                            mnemonic.delete(0, 2);
                        } else if ("EA".equals(substr) || "DB" .equals(substr)) {
                            modifier |= MODIFIER_DECREMENT;
                            modifier |= MODIFIER_BEFORE;
                        }
                        break;
                    default:
                        if ("16".equals(substr)) {
                            mnemonic.delete(0, 2);
                            modifier |= MODIFIER_16;
                        }
                }
            }

            length = mnemonic.length();
            int remainder = length;
            for (int i = 0; i < length; i++) {
                char c = mnemonic.charAt(i);
                switch (c) {
                    case 'L':
                        modifier &=~0xf;
                        modifier |= TYPE_LONG;
                        remainder--;
                        break;
                    case 'S':
                        modifier |= MODIFIER_UPDATE_FLAG;
                        remainder--;
                        break;
                    case 'W':
                        modifier |= MODIFIER_WIDE;
                        remainder--;
                        break;
                    case 'B':
                        modifier &=~0xf;
                        modifier |= TYPE_BYTE;
                        remainder--;
                        break;
                    case 'D':
                        modifier &=~0xf;
                        modifier |= TYPE_DOUBLE;
                        remainder--;
                        break;
                    case 'H':
                        modifier &=~0xf;
                        modifier |= TYPE_HALF;
                        remainder--;
                        break;
                    case 'T':
                        modifier |= MODIFIER_USER;
                        remainder--;
                        break;
                    case '8':
                        modifier |= MODIFIER_8;
                        remainder--;
                        break;
                    case 'X':
                        modifier |= MODIFIER_EXTENTION;
                        remainder--;
                        break;
                    case 'P':
                        modifier |= MODIFIER_26_BIT_PSR;
                        remainder--;
                        break;
                }
            }

            if (remainder != 0) {
                opcode = Opcode.IGNORABLE;
            }
        }


        if (opcode == null) {
            opcode = Opcode.IGNORABLE;
        }

        Operand operand = null;

        if (opcode != Opcode.IGNORABLE && opcode != Opcode.NOP && operandStr != null) {
//            operand = getOperand(module, operandStr.replace("\t", " "));
            operand = getOperand(libraryModule, operandStr);
        }


//        if (opcode == Opcode.IGNORABLE) {
//            logger.debug("{} {}", opcode, str);
//        }
//

        switch (opcode) {
            case ASR: // shift
            case LSR:
            case ROR:
            case RRX:
            case LSL:
                return new ShiftInstruction(libraryModule, address, thumb, opcode, modifier, condition, operand).addStr(str);

            case AND: // shift
            case ORR:
            case ORN:
            case EOR:
            case BIC:
                return new LogicInstruction(libraryModule, address, thumb, opcode, modifier, condition, operand).addStr(str);

            case NEG:
            case REV:
            case RBIT:
            case SSAT:
            case USAT:
            case SBFX:
            case UBFX:
            case UXT:
            case SXT:
//            case SXTH:
//            case UXTH:
            case UXTA:
            case SXTA:
//            case SXTAH:
//            case SXTAB:
            case BFI:
            case BFC:
            case CLZ:
                return new GeneralInstruction(libraryModule, address, thumb, opcode, modifier, condition, operand).addStr(str);

            case UDIV:
                return new DivideInstruction(libraryModule, address, thumb, opcode, modifier, condition, operand).addStr(str);
            case VDIV:
                return new VFPDivideInstruction(libraryModule, address, thumb, opcode, modifier, condition, operand).addStr(str);

            case MUL:
            case MLA:
            case MLS:
            case SMLA:
            case SMLS:
            case SMUL:
            case UMAAL:
            case UMLAL:
            case UMULL:
            case SMULL:
                return new MultiplyInstruction(libraryModule, address, thumb, opcode, modifier, condition, operand).addStr(str);


            case ADD:
                if (operand instanceof OperandSet) { // for switch branch with pc...
                    Operand o = ((OperandSet) operand).get(0);
                    if (o.isRegister() && ((Register) o).isProgramCounter()) {
                        return new BranchInstruction(libraryModule, address, thumb, opcode, modifier, condition, operand).addStr(str);
                    }
                }
            case SUB:
            case RSB:
                return new ArithmeticInstruction(libraryModule, address, thumb, opcode, modifier, condition, operand).addStr(str);

            case QADD:
            case QSUB:
            case QDADD:
            case QDSUB:
            case VSUB:
            case VADD:
                return new VFPArithmeticInstruction(libraryModule, address, thumb, opcode, modifier, condition, operand).addStr(str);

            case ADC:
            case SBC:
            case RSC:
                condition = CONDI_CS; // CS==1?
                modifier |= MODIFIER_COND_DO_NOT_SKIP_FOR_FALSE;
                // for carry condition
                return new ArithmeticInstruction(libraryModule, address, thumb, opcode, modifier, condition, operand).addStr(str);
 /*
            CMP             R1, #4  ; switch 5 cases
                    .text:000B0BD0                 ADDLS           PC, PC, R1,LSL#2 ; switch jump
                    .text:000B0BD4 ; ---------------------------------------------------------------------------
            .text:000B0BD4
                    .text:000B0BD4 loc_B0BD4                               ; CODE XREF: _Unwind_VRS_Pop+14j
                    .text:000B0BD4                 B               loc_B0F00 ; jumptable 000B0BD0 default case
                .text:000B0BD8 ; ---------------------------------------------------------------------------
   */

            case TB:
                return new TableSwitchBranchInstruction(libraryModule, address, thumb, opcode, modifier, condition, operand).addStr(str);
            case BL:
                if(operand.isVariable()) {
                    if(((LocalVariable)operand).getName().equals("__gnu_thumb1_case_uhi")) {
                        return new TableSwitchBranchInstruction(libraryModule, address, thumb, Opcode.TB, TYPE_HALF, condition, new OperandSet(new Operand[]{Register.parse("PC"), Register.parse("R0")})).addStr(str+ " -> tbh");
                    } else if(((LocalVariable)operand).getName().equals("__gnu_thumb1_case_uqi")) {
                        return new TableSwitchBranchInstruction(libraryModule, address, thumb, Opcode.TB, TYPE_BYTE, condition, new OperandSet(new Operand[]{Register.parse("PC"), Register.parse("R0")})).addStr(str+ " -> tbb");
                    }
                }
            case BLX:
                return new BranchInstruction(libraryModule, address, thumb, opcode, modifier, condition, operand).setCall().addStr(str);

            case CBNZ:
            case CBZ:
                return new CompareBranchInstruction(libraryModule, address, thumb, opcode, modifier, condition, operand).addStr(str);

            case B:
            case BX:
//                if (operand.isRegister() && ((Register) operand).isLinkRegister()) {
//                    return new ARMReturnInstruction(libraryModule, address, thumb, opcode, modifier, condition, operand).addStr(str);
//                }
                return new BranchInstruction(libraryModule, address, thumb, opcode, modifier, condition, operand).addStr(str);
            //  491500:   e1a0f00e    mov   pc, lr branch...
            case VCVT:
            case VMOV:
                return new VFPMoveInstruction(libraryModule, address, thumb, opcode, modifier, condition, operand).addStr(str);
            case ADR:
            case MOV:
//                if (operand instanceof OperandSet) { // for switch branch with pc...
//                    Operand o = ((OperandSet) operand).get(0);
//                    if (o.isRegister() && ((Register) o).isProgramCounter()) {
//                        return new BranchInstruction(libraryModule, address, thumb, opcode, modifier, condition, operand).addStr(str);
//                    }
//                }
            case MOVT:
            case MVN:
                return new MoveInstruction(libraryModule, address, thumb, opcode, modifier, condition, operand).addStr(str);

            case VPUSH:
            case VSTR:
            case PUSH:
            case STM:
            case STR:
                return new StoreInstruction(libraryModule, address, thumb, opcode, modifier, condition, operand).addStr(str);

            case VLDR:
                if(operand.isOperandSet()&&((OperandSet)operand).get(1).isExprValue() && ((ExprValue)((OperandSet)operand).get(1)).isAssignedValue()) {
                    return new VFPMoveInstruction(libraryModule, address, thumb, Opcode.VMOV, modifier, condition, operand).addStr(str);
                }
            case LDR:
                if(operand.isOperandSet()&&((OperandSet)operand).get(1).isExprValue() && ((ExprValue)((OperandSet)operand).get(1)).isAssignedValue()) {
                    return new MoveInstruction(libraryModule, address, thumb, Opcode.MOV, modifier, condition, operand).addStr(str);
                }
            case VPOP:
            case POP:
            case LDM:
                return new LoadInstruction(libraryModule, address, thumb, opcode, modifier, condition, operand).addStr(str);

            case CMP:
            case CMN:
            case TEQ: // shift
            case TST: // shift
                modifier |= MODIFIER_UPDATE_FLAG;
                return new CompareInstruction(libraryModule, address, thumb, opcode, modifier, condition, operand).addStr(str);
            case IT:
                return new IfThenInstruction(libraryModule, address, thumb, opcode, modifier, CONDI_AL, operand).addStr(str);
//            case mrrc:case mrrc2:case ldc:case stc:case ldc2:case stc2:case cdp:case cdp2:case mrc:case mrc2:case mcr:case mcr2:case mcrr:// coprocessor data operation
            case NOP:
//            case smc: // secure monitor call
//            case svc: // supervisor call
//            case hvc: // hypervisor call
            case PKH:
            case IGNORABLE:
            default:
                return new IgnorableInstruction(libraryModule, address, thumb, opcode, modifier, condition, operand).addStr(str);
        }
    }

    private static Operand getAddressSet(LibraryModule libraryModule, String str) throws ParseException {
        return getOperand(libraryModule, str, FLAG_ADDESS_SET);
    }

    private static Operand getOperandSet(LibraryModule libraryModule, String str) throws ParseException {
        return getOperand(libraryModule, str, FLAG_OPERAND_SET);
    }

    private static Operand getOperand(LibraryModule libraryModule, String str) throws ParseException {
        return getOperand(libraryModule, str, FLAG_OPERAND);
    }

    private static Operand getOperand(LibraryModule libraryModule, String str, int setFlag) throws ParseException { // if setFlag is true, this should return OperandSet
        Vector<Operand> operands = new Vector<Operand>();
        int length = str.length();
        for (int i = 0; i < length; i++) {
            char c = str.charAt(i);
            switch (c) {
                case '=': {
                    int start = i;
                    i++; // skip one chars
                    while (i < length) {
                        c = str.charAt(i++);
                        if (c == ')') {
                            break;
                        }
                    }
                    operands.add(AbstractValue.parse(libraryModule, str.substring(start, i)));
                    break;
                }
                case '[': {
                    int start = i;
                    int brCount = 1;
                    while (brCount > 0) {
                        c = str.charAt(++i);
                        if (c == '[') {
                            brCount++;
                        } else if (c == ']') {
                            brCount--;
                        }
                    }
                    operands.add(getAddressSet(libraryModule, str.substring(start + 1, i)).setIndirectAddress());
                    break;
                }
                case '{': {
                    int start = i;
                    int brCount = 1;
                    while (brCount > 0) {
                        c = str.charAt(++i);
                        if (c == '{') {
                            brCount++;
                        } else if (c == '}') {
                            brCount--;
                        }
                    }
                    operands.add(getOperandSet(libraryModule, str.substring(start + 1, i)));
                    break;
                }
                case '\t':
                case ' ':
                case ',':
                    break;
                case '!':
                    operands.lastElement().setPreIndexed();
                    break;
                default: {
                    int start = i;
                    i++; // skip one chars
                    while (i < length) {
                        c = str.charAt(i++);
                        if (c == ',' || c == '!') {
                            i--;
                            break;
                        }
                    }

                    String substr = str.substring(start, i).trim();

                    i--;
                    if (substr.charAt(0) == '#') { // || Character.isDigit(ch)) { // for immediate value
                        operands.add(AbstractValue.parse(libraryModule, substr));
                    } else if (substr.charAt(0) == '-' || Register.isValidRegisterName(substr)) { // || substr.contains(":")) {
                        operands.add(Register.parse(substr));
                    } else if (isShift(substr)) {
                        String[] splits = substr.split("\\s");
                        if (splits.length > 1) {
                            ((Register) operands.lastElement()).setShift(splits[0], getOperand(libraryModule, splits[1]));
                        } else {
                            splits = substr.split("#");
                            if (splits.length > 1) {
                                ((Register) operands.lastElement()).setShift(splits[0], new Immediate(splits[1]));
                            } else {
                                ((Register) operands.lastElement()).setShift(splits[0], null);
                            }
                        }
                    } else if (substr.contains("-")) {
                        String[] splits = substr.split("-");
                        Register beginReg = Register.parse(splits[0]);
                        Register endReg = Register.parse(splits[1]);
                        for (int r = beginReg.getNumber(); r <= endReg.getNumber(); r++) {
                            operands.add(new Register(r));
                        }
                    } else if (substr.startsWith("0x")) {
                        operands.add(new Immediate(substr));
                    } else if (substr.length() > 1 && Character.isDigit(substr.charAt(0))) {
                        // shift - e.g. add   ip, pc, #0, 12
                        if (operands.lastElement().isRegister()) {
                            ((Register) operands.lastElement()).setShift("ROR", new Immediate(substr));
                        } else {
                            Immediate lastElement = (Immediate) operands.lastElement();
                            operands.remove(lastElement);
                            lastElement = new Immediate(Integer.rotateRight(lastElement.intValue().intValue(), Integer.parseInt(substr)));
                            operands.add(lastElement);
                        }
                    } else {
                        operands.add(AbstractValue.parse(libraryModule, substr));
                    }
                }
            }
        }

        switch (setFlag) {
            case FLAG_OPERAND_SET:
                return new OperandSet(operands.toArray(new Operand[operands.size()]));
            case FLAG_ADDESS_SET:
                return new AddressSet(operands.toArray(new Operand[operands.size()]));
            default:
                if (operands.size() == 1) {
                    return operands.get(0);
                } else {
                    return new OperandSet(operands.toArray(new Operand[operands.size()]));
                }
        }
    }
}
