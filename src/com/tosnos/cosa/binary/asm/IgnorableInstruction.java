package com.tosnos.cosa.binary.asm;


import com.tosnos.cosa.binary.LibraryModule;

/**
 * Created by kevin on 8/4/14.
 */
public class IgnorableInstruction extends Instruction {

    public IgnorableInstruction(LibraryModule libraryModule, int address, boolean thumb, Opcode opcode, int modifier, byte condition, Operand operand) {
        super(libraryModule, address, thumb, opcode, modifier, condition, operand);
    }

    public IgnorableInstruction(LibraryModule libraryModule, int address, boolean thumb, String str) {
        super(libraryModule, address, thumb, null, 0, (byte) 0, null);
        this.str = str;
    }

    public String toString() {
        return str;
    }

    public boolean isIgnorable() {
        return true;
    }

    @Override
    public Operand[] getRequiredOperands() {
        return null;
    }

    @Override
    public Operand[] getAffectedOperands() {
        return null;
    }
}
