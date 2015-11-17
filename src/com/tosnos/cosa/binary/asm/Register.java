package com.tosnos.cosa.binary.asm;

import com.microsoft.z3.Z3Exception;
import com.tosnos.cosa.binary.asm.value.AbstractValue;
import com.tosnos.cosa.binary.asm.value.PseudoValue;
import com.tosnos.cosa.binary.asm.value.memory.Address;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class Register extends Operand {
    public static final int NUM_OF_REGISTERS = 16;
    public static final int R0 = 0;
    public static final int R1 = 1;
    public static final int R2 = 2;
    public static final int R3 = 3;
    public static final int R4 = 4;
    public static final int R5 = 5;
    public static final int R6 = 6;
    public static final int R7 = 7;
    public static final int R8 = 8;
    public static final int R9 = 9;
    public static final int R10 = 10;
    public static final int R11 = 11;
    public static final int R12 = 12;
    public static final int R13 = 13;
    public static final int R14 = 14;
    public static final int R15 = 15;
    public static final int SL = R10;
    public static final int FP = R11;
    public static final int IP = R12;
    public static final int SP = R13;
    public static final int LR = R14;
    public static final int PC = R15;
    public static final Register r0 = new Register(R0);
    public static final Register r1 = new Register(R1);
    public static final int REG_PARAM_MAX = 4;
    private final static Logger logger = LoggerFactory.getLogger(Register.class);
    private static AbstractValue[] values = new AbstractValue[NUM_OF_REGISTERS];
    //    private static final String regex = "(?i)^-?(R|S|D|CR)[0-9]{1,2}(\\[[0-9]{1,2}\\])?|^-?(SL|FP|IP|SP|LR|PC|FPSID|FPSCR|FPEXE|APSR_[A-Z]+)";
    private static final String regex = "(?i)^-?(R|S|D|CR)[0-9]{1,2}(\\[[0-9]{1,2}\\])?|^-?(SP|LR|PC|FPSID|FPSCR|FPEXE|APSR_[A-Z]+)";
    protected int number;
    private Shift shift;
    private String name;
    private boolean negative = false;

    public static AbstractValue[] replaceValues(AbstractValue[] newValues) {
        AbstractValue[] oldValues = values;
        values = newValues;
        return oldValues;
    }


    public Register(int number) {
        this(number, false);
    }

    public Register(int number, boolean negative) {
        this.number = number;
        this.negative = negative;
    }

    public static void clearAll() {
        for(int i=0;i<NUM_OF_REGISTERS;i++) {
            clear(i);
        }

        VPFRegister.clearAll();
    }

    public static void clear(int number) {
        setValue(number, Instruction.empty);
    }

    public static AbstractValue getRegisterValue(int number) {
        return values[number];
    }

    public static Instruction getPC() {
        Instruction instruction = (Instruction) getRegisterValue(PC);
        setPC(null);
        return instruction;
    }

    public static void setPC(AbstractValue value) {
        setValue(PC, value);
    }

    public static Instruction getLR() {
        return (Instruction) getRegisterValue(LR);
    }

    public static void setLR(AbstractValue value) {
        setValue(LR, value);
    }

    public static Address getSP() {
        return (Address) getRegisterValue(SP);
    }

    public static void setSP(AbstractValue value) {
        setValue(SP, value);
    }

    public static AbstractValue setValue(int number, AbstractValue value) {
        AbstractValue old = values[number];
        values[number] = value;
        return old;
    }

    public static AbstractValue[] getValues() {
        return values;
    }

    public static String getName(int number) {
        switch (number) {
            case SP:
                return "SP";
            case LR:
                return "LR";
            case PC:
                return "PC";
            default:
                return "R" + number;
        }
    }

    public static String getStatus() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            sb.append(String.format("%s:%s ", Register.getName(i), values[i]));
        }
        return sb.toString();
    }

    public static Register parse(String str) {
        if (isValidRegisterName(str)) {
            int vector = -1; // for floating pointer register
            boolean negative = false;
            if (str.charAt(0) == '-') {
                negative = true;
                str = str.substring(1);
            }

            if (str.endsWith("]")) {
                int pos = str.lastIndexOf("[");
                if(pos!=str.length()-2) {
                    vector = Integer.parseInt(str.substring(pos + 1, str.length() - 1));
                    str = str.substring(0, pos);
                }
            }

            if (Character.isDigit(str.charAt(str.length() - 1))) { // for R0, S0....
                VPFRegister.TYPE type = VPFRegister.TYPE.SINGLE;
                int number;
                switch (str.charAt(0)) {
                    case 'R':
                        number = Integer.parseInt(str.substring(1));
                        return new Register(number, negative);
                    case 'D':
                        type = VPFRegister.TYPE.DOUBLE;
                    case 'S':
                        number = Integer.parseInt(str.substring(1));
                        return new VPFRegister(number, type, negative, vector);
                    case 'C':
                        number = Integer.parseInt(str.substring(2));
                        return new SpecialRegister(str, number, SpecialRegister.CR, negative);
                }
            }

            if ("SP".equals(str)) {
                return new Register(SP, negative);
            } else if ("LR".equals(str)) {
                return new Register(LR, negative);
            } else if ("PC".equals(str)) {
                return new Register(PC, negative);
            } else if ("FPSID".equals(str)) {
                return new SpecialRegister(str, -1, SpecialRegister.FPSID, negative);
            } else if ("FPSCR".equals(str)) {
                return new SpecialRegister(str, -1, SpecialRegister.FPSCR, negative);
            } else if ("FPEXE".equals(str)) {
                return new SpecialRegister(str, -1, SpecialRegister.FPEXC, negative);
            } else {
                return new SpecialRegister(str, -1, SpecialRegister.APSR, negative);
            }
        }
        throw new RuntimeException("\"" + str + "\" is not a register format");
    }

    public static AbstractValue[] dump() {
        return values.clone();
    }

    public static void restore(AbstractValue[] regValues) {
        System.arraycopy(regValues, 0, values, 0, regValues.length);
    }

    public static boolean isValidRegisterName(String str) {
//        R|S|D|CR)[0-9]{1,2}(\[[0-9]{1,2}\])?|^-?(SP|LR|PC|FPSID|FPSCR|FPEXE|APSR_
        if (str.charAt(0) == '-') {
            str = str.substring(1);
        }

        int length = str.length();
        if (length < 2) {
            return false;
        }

        char ch = str.charAt(0);
        if (length < 4 && (ch == 'R' || ch == 'S' || ch == 'Q' || ch =='D') && Character.isDigit(str.charAt(1))) {
            return true;
        }

        if (length < 8 && (ch == 'D') && Character.isDigit(str.charAt(1)) && str.charAt(str.length()-1)==']') {
            return true;
        }


        if (length == 2 && (str.equals("SP") || str.equals("LR") || str.equals("PC"))) {
            return true;
        }

        if (length > 2) {
            if (str.startsWith("CR") && Character.isDigit(str.charAt(2))) {
                return true;
            }
            if (str.startsWith("FPS") || str.startsWith("ASPR")) {
                return true;
            }
        }
        return false;
//        return str.matches(regex);
    }

    public void clear() {
        clear(number);
    }

    public void  setValue(AbstractValue value) {
//        AbstractValue old = values[number];
        values[number] = value;
//        return old;
    }

    public boolean hasShift() {
        return shift!=null;
    }

    public boolean isValid() {
        return ((0 <= number) && (number <= getNumberOfRegisters()));
    }

    public int getNumber() {
        return number;
    }

    @Override
    public boolean equals(Object x) {
        if (x == null) {
            return false;
        }

        if (!getClass().equals(x.getClass())) {
            return false;
        }

        Register reg = (Register) x;

        return (reg.number == this.number);
    }

    public int hashCode() {
        return number;
    }

    @Override
    public boolean isRegister() {
        return true;
    }

    public boolean isLinkRegister() {
        return number == LR;
    }

    public boolean isProgramCounter() {
        return number == PC;
    }

    public boolean isGeneralRegister() {
        return true;
    }


    public boolean isDoubleword() {
        return false;
    }

    public boolean isQuadword() {
        return false;
    }

    public boolean isSingleword() {
        return false;
    }

    public String toString() {
        if (name == null) {
            name = getName(number);
        }
        return name;
    }

    public void setShift(String opcodeStr, Operand operand) {
        this.shift = new Shift(opcodeStr, operand);
    }

    public Shift getShift() {
        return shift;
    }

    @Override
    public List<Register> getRegisterList() {
        List<Register> registers = new ArrayList<Register>();

        if (!isProgramCounter()) { // except PC
            registers.add(this);
            if (shift != null && shift.operand.isRegister()) {
                registers.add((Register) shift.operand);
            }
        }
        return registers;
    }

//    public AbstractValue getValue() {
//        return getValue((short)0);
//    }

    public AbstractValue getValue() {
        AbstractValue value = negative ? values[number].getNegativeReplacement() : values[number];
        if (value!=null && shift != null) {
            // if value is pseudo, it could be a number
            if(value.isPseudoValue()) {
                ((PseudoValue)value).setPossibleType(PseudoValue.PossibleType.INT);
            }
            try {
                return ShiftInstruction.shift(shift.getOpcode(), value, shift.shiftLength());
            } catch (Z3Exception e) {
                e.printStackTrace();
            }
        }
        return value;
    }

    public int getNumberOfRegisters() {
        return NUM_OF_REGISTERS;
    }

    public boolean isStackPointer() {
        return number == SP;
    }

    public boolean isFramePointer() {
        return number == FP;
    }

    public boolean isNegative() {
        return negative;
    }

    public class Shift {
        private Operand operand;
        private Opcode opcode;

        private Shift(String opcodeStr, Operand operand) {
            this.opcode = Opcode.valueOf(opcodeStr);
            this.operand = operand;
        }

        public Opcode getOpcode() {
            return opcode;
        }

        public Operand shiftLength() {
            return operand;
        }

        @Override
        public String toString() {
            return opcode + " " + operand;
        }
    }

}
