package com.tosnos.cosa.binary.function;

import com.tosnos.cosa.binary.LibraryModule;
import com.tosnos.cosa.binary.asm.Instruction;
import com.tosnos.cosa.binary.asm.value.Numeral;
import com.tosnos.cosa.binary.cfg.CFG;


/**
 * Created by kevin on 12/5/14.
 */
public class Subroutine extends SubroutineOrFunction {
    protected final int address;
    protected LibraryModule module;
    private boolean systemSubroutine;
    private CFG cfg = null;


    public Subroutine(LibraryModule module, String name, int address) {
        super(name);
        this.module = module;
        this.address = address;
    }

    @Override
    public Subroutine getReplacement(Numeral value) {
        return new Subroutine(module, name, value.intValue());
    }

    public boolean isNativeMethod() {
        return false;
    }

    public boolean isLoaded() {
        return module.isLoaded();
    }

    public boolean isSystemSubroutine() {
        return systemSubroutine;
    }

    public void setSystemSubroutine() {
        systemSubroutine = true;
    }

    public boolean isSubroutine() {
        return true;
    }

    public LibraryModule getLibraryModule() {
        return module;
    }

    public Instruction getFirstInstruction() {
        return module.getInstruction(address);
    }

    public CFG getCFG() {
        if(cfg==null) {
            cfg = CFG.parse(this);
        }
        return cfg;
    }

    @Override
    public Numeral getNumeral() {
        return new Numeral(address);
    }
}
