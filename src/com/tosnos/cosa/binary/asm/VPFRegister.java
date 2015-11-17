package com.tosnos.cosa.binary.asm;

import com.microsoft.z3.Context;
import com.microsoft.z3.Z3Exception;
import com.tosnos.cosa.binary.NativeLibraryHandler;
import com.tosnos.cosa.binary.asm.value.AbstractValue;
import com.tosnos.cosa.binary.asm.value.Immediate;
import com.tosnos.cosa.binary.asm.value.Numeral;

/*
S<2n> maps to the least significant half of D<n>.
S<2n+1> maps to the most significant half of D<n>.
D<2n> maps to the least significant half of Q<n>.
D<2n+1> maps to the most significant half of Q<n>.
*/
/**
 * Created by kevin on 7/7/14.
 */
public class VPFRegister extends Register {
    public enum TYPE { SINGLE, DOUBLE, QUAD }
    static final int NUM_OF_QUADWORD_REGISTER = 16;
    static final int NUM_OF_SINGLEWORD_REGISTERS = 64;
    static final int NUM_OF_DOUBLEWORD_REGISTERS = 32;
    private final TYPE precision; // false = f32, true = f64
    private final int vector;
    private String name;
    private final static AbstractValue[] values = new AbstractValue[NUM_OF_SINGLEWORD_REGISTERS];

    VPFRegister(int number, TYPE precision, boolean negative) {
        this(number, precision, negative, -1);
    }

    VPFRegister(int number, TYPE precision, boolean negative, int vector) {
        super(number, negative);
        this.precision = precision;
        this.vector = vector;
        if (name == null) {
            switch(precision) {
                case QUAD:
                    name = "q" + number;
                    break;
                case DOUBLE:
                    name = "d" + number;
                    break;
                default:
                    name = "s" + number;
            }
        }
    }

    VPFRegister(int number, TYPE precision) {
        this(number, precision, false);
    }

    public static AbstractValue[] dump() {
        return values.clone();
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public int getNumberOfRegisters() {
        switch(precision) {
            case QUAD:
                return NUM_OF_QUADWORD_REGISTER;
            case DOUBLE:
                return NUM_OF_DOUBLEWORD_REGISTERS;
            default:
                return NUM_OF_SINGLEWORD_REGISTERS;
        }
    }

    @Override
    public boolean isStackPointer() {
        return false;
    }

    @Override
    public boolean isLinkRegister() {
        return false;
    }

    @Override
    public boolean isProgramCounter() {
        return false;
    }

    @Override
    public boolean isDoubleword() {
        return precision==TYPE.DOUBLE;
    }

    @Override
    public boolean isQuadword() {
        return precision==TYPE.QUAD;
    }

    @Override
    public boolean isSingleword() {
        return precision==TYPE.SINGLE;
    }

    @Override
    public boolean isGeneralRegister() {
        return false;
    }

    @Override
    public boolean equals(Object x) {
        if (x == null) {
            return false;
        }

        if (!getClass().equals(x.getClass())) {
            return false;
        }

        VPFRegister reg = (VPFRegister) x;

        return (reg.number == this.number && reg.precision == this.precision);
    }

    @Override
    public void setValue(AbstractValue value) {
        if(precision==TYPE.DOUBLE) {
            Context ctx = NativeLibraryHandler.getInstance().getContext();
            try {
                value = value.DoublePrecision();
                values[number*2] = value.getReplacement(value.getNumeral().getLower(ctx));
                values[number*2+1] = value.getReplacement(value.getNumeral().getUpper(ctx));
            } catch (Z3Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            values[number] = value.SinglePrecision();
        }
    }
    @Override
    public AbstractValue getValue() {
        if(precision==TYPE.DOUBLE) {
            return values[number*2].getReplacement(new Numeral(values[number*2].getNumeral(), values[number*2+1].getNumeral(), TYPE_DOUBLE));
        } else {
            return values[number];
        }
    }

    public static void clearAll() {
        for(int i=0;i<NUM_OF_SINGLEWORD_REGISTERS;i++) {
            clear(i);
        }
    }

    public static void clear(int number) {
        setValue(number, Immediate.EMPTY);
    }

    public static AbstractValue setValue(int number, AbstractValue value) {
        AbstractValue old = values[number];
        values[number] = value;
        return old;
    }

    public static void restore(AbstractValue[] regValues) {
        System.arraycopy(regValues, 0, values, 0, regValues.length);
    }
}
