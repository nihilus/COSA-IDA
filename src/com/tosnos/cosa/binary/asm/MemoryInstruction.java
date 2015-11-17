package com.tosnos.cosa.binary.asm;

import com.tosnos.cosa.binary.LibraryModule;

/**
 * Created by kevin on 7/12/14.
 */
public abstract class MemoryInstruction extends Instruction {
    public MemoryInstruction(LibraryModule libraryModule, int address, boolean thumb, Opcode opcode, int modifier, byte condition, Operand operand) {
        super(libraryModule, address, thumb, opcode, modifier, condition, operand);
    }

    public boolean isBefore() {
        return (modifier & MODIFIER_BEFORE) > 0;
    }

    public boolean isDecerement() {
        return (modifier & MODIFIER_DECREMENT) > 0;
    }

    public int getDataType() {
        return 0;
    }
}