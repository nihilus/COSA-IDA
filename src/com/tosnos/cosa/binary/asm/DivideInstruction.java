package com.tosnos.cosa.binary.asm;

import com.tosnos.cosa.binary.LibraryModule;

import java.util.Arrays;

/**
 * Created by kevin on 11/30/14.
 */
public class DivideInstruction extends Instruction {

    public DivideInstruction(LibraryModule libraryModule, int address, boolean thumb, Opcode opcode, int modifier, byte condition, Operand operand) {
        super(libraryModule, address, thumb, opcode, modifier, condition, operand);
    }

    @Override
    public Operand[] getRequiredOperands() {
        return new Operand[]{getDivideDestination()};
    }

    @Override
    public Operand[] getAffectedOperands() {
        return getDivideSources();
    }

    @Override
    public boolean isDivide() {
        return true;
    }


    public Operand[] getDivideSources() {
        if (operands.length > 2) {
            return Arrays.copyOfRange(operands, 1, operands.length);
        }
        return operands;
    }

    public Register getDivideDestination() {
        return (Register)operands[0];
    }
}