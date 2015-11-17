package com.tosnos.cosa.binary.asm;

import com.microsoft.z3.Context;
import com.microsoft.z3.Z3Exception;
import com.tosnos.cosa.binary.LibraryModule;

/**

 * MOVS, MVNS - MoveInstruction
 * ANDS, ORRS, ORNS, EORS, BICS, LogicInstruction
 *TEQ or TST - CompareInstruction
 */
public class SecondShiftInstruction extends Instruction {
    private Register shift;

    public SecondShiftInstruction(LibraryModule libraryModule, int address, boolean thumb, Opcode opcode, int modifier, byte condition, Operand operand) {
        super(libraryModule, address, thumb, opcode, modifier, condition, operand);
        if(operands.length>0) {
            Operand lastRegister = operands[operands.length - 1];
            if (lastRegister.isOperandSet()) {
                lastRegister = ((OperandSet) lastRegister).getLastElement();
            }
            if (lastRegister.isRegister() && ((Register) lastRegister).hasShift()) {
                shift = (Register) lastRegister;
            }
        }
    }

    protected static NZCVExpr getCarryFlag(Context ctx, Register r) throws Z3Exception {
        if(r==null) {
            return null;
        }
        Register.Shift shift = r.getShift();
        return ShiftInstruction.getCarryExpr(ctx, shift.getOpcode(), r.getNumeral(), shift.shiftLength().getNumeral());
    }

    protected Register getShift() {
        return shift;
    }
}
