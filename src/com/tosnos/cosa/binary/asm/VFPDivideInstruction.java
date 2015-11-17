package com.tosnos.cosa.binary.asm;

import com.microsoft.z3.Context;
import com.microsoft.z3.Z3Exception;
import com.tosnos.cosa.binary.LibraryModule;
import com.tosnos.cosa.binary.asm.value.AbstractValue;
import com.tosnos.cosa.binary.asm.value.Numeral;

import java.util.Arrays;

/**
 * Created by kevin on 10/30/15.
 */
public class VFPDivideInstruction extends Instruction {

    public VFPDivideInstruction(LibraryModule libraryModule, int address, boolean thumb, Opcode opcode, int modifier, byte condition, Operand operand) {
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
    public boolean isVFPDivide() {
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


    private AbstractValue vdiv(Context ctx, Operand o1, Operand o2, NZCVExpr[] nzcvExprs) throws Z3Exception {
        AbstractValue v1 = o1.getValue();
        AbstractValue v2 = o2.getValue();

        Numeral n1 = v1.getNumeral();
        Numeral n2 = v2.getNumeral();

        Numeral result = null;
        if(!n1.isUnknown() && !n2.isUnknown()) {
            double d1 = n1.doubleValue();
            double d2 = n2.doubleValue();
            double d3 = d1/d2;

            if((modifier&VFP_DATA_F)>0) {
                if((modifier&VFP_DATA_32)>0) {
                    result = new Numeral((float) d3);
                } else if((modifier&VFP_DATA_64)>0) {
                    result = new Numeral(d3);
                }
            }

            if(result==null) {
                throw new RuntimeException("no");
            }
        } else {
            result = Numeral.newValue(n1.getType());
        }

        if(v1.isImmediate()) {
            return v2.getReplacement(result);
        } else {
            return v1.getReplacement(result);
        }
    }


    public void compute(Context ctx, NZCVExpr[] nzcvExprs) throws Z3Exception {
        Operand[] sources = getDivideSources();
        Register rd = getDivideDestination();

        switch (opcode) {
            case VDIV:
                rd.setValue(vdiv(ctx, sources[0], sources[1], nzcvExprs));
                break;
            default:
                throw new RuntimeException("not implemented for " + opcode);
        }
    }
}