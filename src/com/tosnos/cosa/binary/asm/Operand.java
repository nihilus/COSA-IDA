package com.tosnos.cosa.binary.asm;


import com.tosnos.cosa.binary.asm.value.AbstractValue;
import com.tosnos.cosa.binary.asm.value.Numeral;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by kevin on 12/8/14.
 */
public abstract class Operand implements Modifier {
    protected boolean preIndexed = false;
    private boolean indirectAddress = false;

    public Operand setIndirectAddress() {
        indirectAddress = true;
        return this;
    }

    public List<Operand> getElements() {
        List list = new ArrayList<Operand>();
        list.add(this);
        return list;
    }

    public boolean isIndirectAddress() {
        return indirectAddress;
    }

    public boolean isRegister() {
        return false;
    }

    public boolean isVariable() {
        return false;
    }

    public Operand setPreIndexed() {
        this.preIndexed = true;
        return this;
    }

    public boolean isAbstractValue() {
        return false;
    }

    public boolean isPreIndexed() {
        return preIndexed;
    }

    public List<Register> getRegisterList() {
        return Collections.emptyList();
    }

    public boolean isOperandSet() {
        return false;
    }

    public boolean isAddressSet() {
        return false;
    }

    public boolean isExprValue() {
        return false;
    }

    public AbstractValue getValue() {
        if(isValueContainer()) {
            return getValue();
        }
        return (AbstractValue)this;
    }

    public boolean isValueContainer() { return false;}

    public boolean isImmediate() {
        return false;
    }

    public boolean isAllocatedMemory() {
        return false;
    }

    public boolean isInstruction() {
        return false;
    }

    public boolean isStringValue() {
        return false;
    }

    public boolean isJNIValue() {
        return false;
    }

    public boolean isPthread_t() {
        return false;
    }

    public boolean isAddress() {
        return false;
    }

     public boolean isPseudoValue() {
        return false;
    }

    public boolean isAssociatedValue() {
        return false;
    }

    public Numeral getNumeral() {
        if(!isAbstractValue()) {
            return getValue().getNumeral();
        }
        throw new RuntimeException("not implemented " +getClass().getName());
    }
 }
