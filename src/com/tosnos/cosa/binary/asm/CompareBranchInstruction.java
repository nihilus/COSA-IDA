package com.tosnos.cosa.binary.asm;
import com.microsoft.z3.Context;
import com.tosnos.cosa.binary.LibraryModule;
import com.tosnos.cosa.binary.asm.value.AbstractValue;
import com.tosnos.cosa.binary.asm.value.Immediate;
import com.tosnos.cosa.binary.asm.value.Numeral;

/**
 * Created by kevin on 8/3/14.
 */
public class CompareBranchInstruction extends BranchInstruction {
    public CompareBranchInstruction(LibraryModule libraryModule, int address, boolean thumb, Opcode opcode, int modifier, byte condition, Operand operand) {
        super(libraryModule, address, thumb, opcode, modifier, condition, operand);
    }

    @Override
    public Operand[] getRequiredOperands() {
        return new Operand[]{operands[0]};
    }

    @Override
    public Operand[] getAffectedOperands() {
        return new Operand[]{operands[0]};
    }

    public Register getSourceRegister() {
        return (Register) operands[0];
    }

    public Operand getBranchDestination() {
        return operands[1];
    }

    public boolean isCompareBranch() {
        return true;
    }
}
