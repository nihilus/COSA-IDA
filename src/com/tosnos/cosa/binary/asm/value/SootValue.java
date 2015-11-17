package com.tosnos.cosa.binary.asm.value;

import com.tosnos.cosa.binary.asm.Modifier;

/**
 * Created by kevin on 2/7/15.
 */
public class SootValue extends AbstractValue {
    Object value;

    public SootValue(Object value) {
        this.value = value;
    }

    public boolean isSootValue() {
        return true;
    }

    public Object getSootValue() {
        return value;
    }

    public String toString() {
        return value.toString();
    }

    public byte getValueType() {
        return Modifier.TYPE_INT;
    }

    @Override
    public Integer intValue() {
        return value.hashCode();
    }

    @Override
    public Numeral getNumeral() {
        return new Numeral(intValue());
    }

    @Override
    public AbstractValue getReplacement(Numeral numeral) {
        return this;
    }
}
