package com.tosnos.cosa.binary.asm;


import com.microsoft.z3.Context;
import com.microsoft.z3.Z3Exception;
import com.tosnos.cosa.binary.LibraryModule;

import java.util.Arrays;

/**
 * Created by kevin on 7/24/14.
 */
public class CompareInstruction extends SecondShiftInstruction {

    public CompareInstruction(LibraryModule libraryModule, int address, boolean thumb, Opcode opcode, int modifier, byte condition, Operand operand) {
        super(libraryModule, address, thumb, opcode, modifier, condition, operand);
    }

    @Override
    public Operand[] getRequiredOperands() {
        return operands;
    }

    @Override
    public Operand[] getAffectedOperands() {
        return operands;
    }

    public Operand getFirst() {
        return operands[0];
    }

    public Operand getSecond() {
        return operands[1];
    }

    @Override
    public boolean isCompare() {
        return true;
    }

    public void compute(Context ctx, NZCVExpr[] nzcvExprs) throws Z3Exception {
        switch (opcode) {
            case CMP:
                ArithmeticInstruction.sub(ctx, operands[0], operands[1], nzcvExprs, MODIFIER_UPDATE_FLAG);
                break;
            case CMN:
                ArithmeticInstruction.add(ctx, operands[0], operands[1], nzcvExprs, MODIFIER_UPDATE_FLAG);
                break;
            case TST:
                LogicInstruction.compute(ctx, Opcode.AND, operands[0], operands[1], nzcvExprs, MODIFIER_UPDATE_FLAG, getShift());
                break;
            case TEQ:
                LogicInstruction.compute(ctx, Opcode.EOR, operands[0], operands[1], nzcvExprs, MODIFIER_UPDATE_FLAG, getShift());
                break;
            default:
                throw new RuntimeException("not implemented for " + opcode);
        }
    }
}
