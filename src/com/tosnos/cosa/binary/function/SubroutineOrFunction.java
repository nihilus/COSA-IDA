package com.tosnos.cosa.binary.function;

import com.tosnos.cosa.binary.asm.Modifier;
import com.tosnos.cosa.binary.asm.value.AbstractValue;

/**
 * Created by kevin on 7/7/15.
 */
public abstract class SubroutineOrFunction extends AbstractValue {
    protected final String name;
    protected SubroutineOrFunction(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public boolean isSubroutineOrFunction() {
        return true;
    }

    public boolean isFunction() {
        return false;
    }


    public boolean isSubroutine() {
        return false;
    }

    public boolean isImported() {
            return false;
    }
}
