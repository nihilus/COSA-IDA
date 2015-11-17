package com.tosnos.cosa.binary.function;

import com.tosnos.cosa.binary.LibraryModule;

/**
 * ImportedSubroutine is a subroutine that is located in other modules
 */
public class ImportedSubroutine extends Subroutine {

    private SubroutineOrFunction sof;

    public ImportedSubroutine(LibraryModule module, String name, int address) {
        super(module, name, address);
    }

    public void setSubroutineOrFunction(SubroutineOrFunction sof) {
        this.sof = sof;
    }
    public SubroutineOrFunction getFunctionOrSubroutine() {
        return sof;
    }

    @Override
    public boolean isImported() {
        return true;
    }
}

