package com.tosnos.cosa.binary.asm.value;

import com.microsoft.z3.Context;
import com.microsoft.z3.Z3Exception;
import com.tosnos.cosa.binary.LibraryModule;
import com.tosnos.cosa.binary.asm.Operand;
import com.tosnos.cosa.binary.function.Function;
import soot.Type;

/**
 * Created by kevin on 12/7/14.
 */
public abstract class AbstractValue extends Operand implements Cloneable {
    public static AbstractValue parse(LibraryModule libraryModule, String str) {
        int index = 0;
        boolean immediate = false;
        char ch = str.charAt(index);
        if (ch == '"') {
            return new StringValue(str.replace("\"", ""));
        }

        if (ch == '#') {
            ch = str.charAt(++index);
            immediate = true;
        }

        if (ch == '-' || ch == '+') {
            ch = str.charAt(++index);
        }

        if (ch == '=' || str.indexOf("-") > index || str.indexOf("+") > index) {
            return new ExprValue(libraryModule, str);
        }

        if (Character.isDigit(ch)) {
            if (immediate) {
                str = str.substring(1);
            }
            return new Immediate(str);
        }

        // get variable
        return libraryModule.findLocalVariable(str);
    }

    public AbstractValue getReplacement(Numeral numeral) {
        throw new RuntimeException("not implemented " + getClass() + " ");
    }

    public AbstractValue getNegativeReplacement() {
        throw new RuntimeException("not implemented " + getClass());
    }

    public boolean isSubroutineOrFunction() {
        return false;
    }

    public boolean isUnknown() {
        return false;
    }

    public Integer intValue() {
        throw new RuntimeException("not implemented " + getClass());
    }

    @Override
    public boolean isAbstractValue() {
        return true;
    }

    public boolean isIMemoryValue() {
        return false;
    }

    public boolean isFileValue() {
        return false;
    }

    public boolean isValueSet() {
        return false;
    }

    public static AbstractValue newValue(byte type) {
        switch (type) {
            case TYPE_BYTE:
            case TYPE_HALF:
            case TYPE_INT:
            case TYPE_FLOAT:
            case TYPE_DOUBLE:
            case TYPE_LONG:
                return Immediate.newValue(type);
            case TYPE_STRING:
                return new StringValue("");
      //      default:
        //        return ObjectValue.UNKNOWN_VOID;
        }
        return null;
    }

    public static AbstractValue newValue(Type type) {
        if (type == Function.INT) {
            return Immediate.newValue(TYPE_INT);
        } else if (type == Function.BYTE) {
            return Immediate.newValue(TYPE_BYTE);
        } else if (type == Function.SHORT) {
            return Immediate.newValue(TYPE_HALF);
        } else if (type == Function.FLOAT) {
            return Immediate.newValue(TYPE_FLOAT);
        } else if (type == Function.DOUBLE) {
            return Immediate.newValue(TYPE_DOUBLE);
        } else if (type == Function.LONG) {
            return Immediate.newValue(TYPE_LONG);
        } else if (type == Function.STRING) {
            return new StringValue("");
       // } else if (type == Function.VOID) {
       //     return ObjectValue.UNKNOWN_VOID;
        } else {
            return new SootValue(type);
        }
    }

    public Object clone()  {
        throw new RuntimeException("not implemented " + getClass());
    }

    public byte getType() {
        return TYPE_ADDRESS;
    }

    public AbstractValue getLower(Context ctx) throws Z3Exception {
        return getReplacement(getNumeral().getLower(ctx));
    }

    public AbstractValue getUpper(Context ctx)throws Z3Exception {
        return getReplacement(getNumeral().getUpper(ctx));
    }

    public AbstractValue DoublePrecision() {
        return this;
    }

    public AbstractValue SinglePrecision() {
        return this;
    }


    public AbstractValue add(AbstractValue value) {
        if(value.isUnknown()) {
            return this;//
//            throw new RuntimeException("values have an unknown value");
        }
        Numeral n = getNumeral().add(value.intValue());
        if(isImmediate()) {
            return value.getReplacement(n);
        } else {
            return getReplacement(n);
        }
    }

    public AbstractValue sub(AbstractValue value) {
        if(value.isUnknown()) {
            return this;
//            throw new RuntimeException("values have an unknown value");
        }
        Numeral n = new Numeral(intValue() - value.intValue());
        if(isImmediate()) {
            return value.getReplacement(n);
        } else {
            return getReplacement(n);
        }
    }

    public AbstractValue mul(AbstractValue value) {
        if(value.isUnknown()) {
            return this;
//            throw new RuntimeException("values have an unknown value");
        }
        Numeral n = new Numeral(intValue() * value.intValue());
        if(isImmediate()) {
            return value.getReplacement(n);
        } else {
            return getReplacement(n);
        }
    }

}
