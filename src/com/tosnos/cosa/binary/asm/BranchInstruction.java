package com.tosnos.cosa.binary.asm;

import com.tosnos.cosa.binary.LibraryModule;

import java.util.Arrays;


/**
 * Created by kevin on 7/4/14.
 */
public class BranchInstruction extends Instruction {
    private boolean indirect = false;
    private boolean lrRelative = false;
    private Operand[] affectedOperands;
    private boolean call = false;
    private Operand[] requiredOperands;

    public BranchInstruction(LibraryModule libraryModule, int address, boolean thumb, Opcode opcode, int modifier, byte condition, Operand operand) {
        super(libraryModule, address, thumb, opcode, modifier, condition, operand);
        Operand rd = operands[operands.length - 1];
        if (rd.isRegister() && (!((Register) rd).isProgramCounter() || opcode == Opcode.ADD || opcode == Opcode.MOV)) {
            indirect = true;
            lrRelative = ((Register) rd).isLinkRegister();
        }
    }

    public BranchInstruction setCall() {
        call = true;
        return this;
    }

    @Override
    public Operand[] getRequiredOperands() {
        if(!call) {
            return operands;
        } else {
            return requiredOperands;
        }
    }

    @Override
    public boolean isIndirect() {
        return indirect;
    }

    public Operand getBranchDestination() {
        if (opcode == Opcode.MOV) {
            return operands[1];
        }
        if (operands.length == 1) {
            return operands[0];
        } else {
            return new OperandSet(Arrays.copyOfRange(operands, 1, operands.length));
        }
    }

    public boolean isLRRelative() {
        return lrRelative;
    }

    public boolean isExchangeISA() {
        if (operands[0].isRegister()) {
            return false;
        }

        if (opcode == Opcode.BX || opcode == Opcode.BLX) {
//            if(operands[0].isRegister()) {
//                IAbstractValue v = operands[0].getAbstractValue();
//                if(v!=null && (v.intValue()&1)>0 ) {
//                    return true;
//                }
//                return false;
//            }
            return true;
        }

        return false;
    }

    public boolean isSwitchBranch() {
        return opcode == Opcode.ADD && isConditional();
    }

    @Override
    public boolean isBranch() {
        return true;
    }

    @Override
    public Operand[] getAffectedOperands() {
        return affectedOperands;
    }

    @Override
    public void setAffectedOperands(Operand[] operands) {
        this.affectedOperands = operands;
    }

    @Override
    public boolean isCall() {
        return call;
    }

    public void setRequiredOperands(Operand[] operands) {
        this.requiredOperands = operands;
    }
}
