package com.tosnos.cosa.binary.asm.value.JNI;

import com.tosnos.cosa.binary.asm.value.*;
import com.tosnos.cosa.binary.asm.value.Immediate;
import com.tosnos.cosa.binary.asm.value.memory.Address;
import soot.*;

import java.io.IOException;

/**
 * Created by kevin on 12/28/14.
 */
public class JNIValue extends AbstractValue  {
    public static final Immediate JNI_FALSE = Immediate.ZERO;
    public static final Immediate JNI_TRUE = Immediate.ONE;
    public static final Immediate JNI_OK = Immediate.ZERO;
    public static final Immediate JNI_ERR = Immediate.MINUS_ONE;

    public static final int THIS = -1;
    public static final int CLASS = -2;
    public static final int RESULT = -3;
    public static final int ABSTRACT = -4;
    protected int paramIndex = ABSTRACT; // -1: JAbstract, -2: Result
    private final Type type;
    private AbstractValue value;

    public JNIValue(int paramIndex, Type type, AbstractValue value) {
        this.paramIndex = paramIndex;
        this.type = type;
        this.value = value;
    }

    public JNIValue(Type type, AbstractValue value) {
        this.type = type;
        this.value = value;
    }

    public JNIValue(int paramIndex, Type type) {
        this(paramIndex, type, AbstractValue.newValue(type));
    }

    @Override
    public AbstractValue getReplacement(Numeral offset) {
        JNIValue jniValue = new JNIValue(paramIndex, type, value.getReplacement(offset));
        return jniValue;
    }

    @Override
    public boolean isJNIValue() {
        return true;
    }

    public int getParamIndex() {
        return paramIndex;
    }

    public String toString() {
        switch (paramIndex) {
            case THIS:
                return "J:This:" + value;
            case CLASS:
                return "J:Class:" + value;
            case RESULT:
                return "J:Result:" + value;
            case ABSTRACT:
                return "J:Abstract:" + value;
            default:
                return "J:" + paramIndex + ":" + value;
        }
    }

    public Type getValueType() {
        return type;
    }

    public void setValue(AbstractValue value) {
        this.value = value;
    }

//    public void write(AbstractValue v) {
//        ((Address) value).write(v);
//    }

    public AbstractValue read() {
        try {
            if (value.isAddress()) {
                return ((Address) value).read();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return this;
    }

    @Override
    public AbstractValue getValue() {
        return value;
    }

    public Address getAddress() {
        if(value.isAssociatedValue()) {
            return ((AssociatedValue)value).getValueAddress();
        } else if (value.isAddress()) {
            return (Address) value;
        } else if (value.isVariable()) {
            return ((LocalVariable)value).getAddress();
        } else {
            return new StringValue("");
        }
    }

    @Override
    public boolean isValueContainer() {
        return true;
    }

//    public StringValue getStringValue() {
//        if(value.isUnknown()) {
//            return (StringValue)value;
//        }
//
//        if (value.isStringValue()) {
//            return (StringValue) value;
//        }
//
//        try {
//            Address address = getAddress();
//            if (address != null) {
//                return address.readText();
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        return null;
//    }

    @Override
    public Integer intValue() {
        return value.intValue();
    }

    @Override
    public Numeral getNumeral() {
        return value.getNumeral();
    }

    @Override
    public boolean isUnknown() {
        return value.isUnknown();
    }

    @Override
    public boolean isImmediate() {
        return value.isImmediate();
    }
//    @Override
//    public int sizeOf() {
//        return ((IUnknown)value).sizeOf();
//    }
//
//    @Override
//    public Number getLowerBound() {
//        if(value.isUnknown()) {
//            ((IUnknown)value).getLowerBound();
//        }
//        return null;
//    }
//
//    @Override
//    public Number getUpperBound() {
//        if(value.isUnknown()) {
//            ((IUnknown)value).getUpperBound();
//        }
//        return null;
//    }
    @Override
    public Object clone() {
        JNIValue cloned = new JNIValue(this.paramIndex,this.type, (AbstractValue)value.clone());
        return cloned;
    }
}
