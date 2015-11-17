package com.tosnos.cosa.binary.asm;

import com.microsoft.z3.*;
import com.tosnos.cosa.binary.LibraryModule;
import com.tosnos.cosa.binary.NativeLibraryHandler;
import com.tosnos.cosa.binary.asm.value.AbstractValue;
import com.tosnos.cosa.binary.asm.value.ConstrainExpr;
import com.tosnos.cosa.binary.asm.value.Immediate;
import com.tosnos.cosa.binary.asm.value.Numeral;

import java.util.Arrays;

public class ShiftInstruction extends Instruction {

    public ShiftInstruction(LibraryModule libraryModule, int address, boolean thumb, Opcode opcode, int modifier, byte condition, Operand operand) {
        super(libraryModule, address, thumb, opcode, modifier, condition, operand);
    }

    public Operand getShiftSource() {
        return operands.length == 2 ? operands[0] : operands[1];
    }

    public Operand getShiftLength() {
        return operands.length == 2 ? operands[1] : operands[2];
    }

    public Register getShiftDestination() {
        return (Register) operands[0];
    }

    public boolean isShift() {
        return true;
    }


    @Override
    public Operand[] getRequiredOperands() {
        return new Operand[]{getShiftSource()};
    }

    @Override
    public Operand[] getAffectedOperands() {
        return new Operand[]{getShiftDestination()};
    }

    public static NZCVExpr getCarryExpr(Context ctx, Opcode opcode, Numeral n1, Numeral n2) throws Z3Exception {
        BitVecExpr b1 = n1.getBitVecExpr(ctx);
        BitVecExpr b2 = n2.getBitVecExpr(ctx);
        BoolExpr nc;
        switch (opcode) {
            case LSL:
                nc = (BoolExpr) ctx.mkEq(ctx.mkBVAND(b1, ctx.mkBVSHL(ctx.mkBV(1,32), ctx.mkBVSub(ctx.mkBV(32,32), b2))), ctx.mkBV(0, 32)).simplify();
                break;
            case ASR:
            case LSR:
            case ROR:
                nc = (BoolExpr) ctx.mkEq(ctx.mkBVAND(b1, ctx.mkBVSHL(ctx.mkBV(1,32), ctx.mkBVSub(b2,ctx.mkBV(1,32)))), ctx.mkBV(0, 32)).simplify();
                break;
            default:
                throw new RuntimeException("not implemented "+opcode);
        }
        ConstrainExpr pC = new ConstrainExpr((BoolExpr) ctx.mkNot(nc).simplify()).setNumeral(n1, n2);
        ConstrainExpr nC = new ConstrainExpr(nc).setNumeral(n1, n2);
        return new NZCVExpr(pC, nC);
    }

    public void compute(Context ctx, NZCVExpr[] nzcvExprs) throws Z3Exception {
        ((Register) operands[0]).setValue(compute(ctx, opcode, getShiftSource(), getShiftLength(), nzcvExprs, modifier));
    }

    public static AbstractValue shift(Opcode opcode, Operand source, Operand shiftLength) throws Z3Exception {
        return compute(NativeLibraryHandler.getInstance().getContext(), opcode, source, shiftLength, null, 0);
    }


    private static AbstractValue compute(Context ctx, Opcode opcode, Operand source, Operand shiftLength, NZCVExpr[] nzcvExprs, int modifier) throws Z3Exception {

        AbstractValue v1 = source.getValue();
        AbstractValue v2 = shiftLength.getValue();
        Numeral n1 = v1.getNumeral();
        Numeral n2 = v2.getNumeral();
        BitVecExpr b1 = n1.getBitVecExpr(ctx);
        BitVecExpr b2 = n2.getBitVecExpr(ctx);

        if ((modifier & MODIFIER_UPDATE_FLAG) > 0 && nzcvExprs != null) {
            nzcvExprs[C] = getCarryExpr(ctx, opcode, n1, n2);
        }

        BitVecExpr expr;
        switch (opcode) {
            case ASR:
                expr = ctx.mkBVASHR(b1, b2);
                break;
            case LSL:
                expr = ctx.mkBVSHL(b1, b2);
                break;
            case LSR:
                expr = ctx.mkBVLSHR(b1, b2);
                break;
            case ROR:
                expr = ctx.mkBVRotateRight(b1, b2);
                break;
            case RRX:
            default:
                throw new RuntimeException("not implemented for " + opcode);
        }

        Numeral result = new Numeral(expr.simplify(), TYPE_INT);
        if(n1.isUnknown()) {
            n1.addRelatedNumeral(result);
            result.addRelatedNumeral(n1);
        }

        if(n2.isUnknown()) {
            n2.addRelatedNumeral(result);
            result.addRelatedNumeral(n2);
        }

        if((modifier&MODIFIER_UPDATE_FLAG)>0 && nzcvExprs!=null) {
            BitVecNum zero = ctx.mkBV(0, 32);
            BoolExpr n = (BoolExpr) ctx.mkBVSLT(expr, zero).simplify();
            ConstrainExpr pN = new ConstrainExpr(n).setNumeral(n1, n2);
            ConstrainExpr nN = new ConstrainExpr((BoolExpr) ctx.mkNot(n).simplify()).setNumeral(n1, n2);

            BoolExpr z = (BoolExpr) ctx.mkEq(expr, zero).simplify();
            ConstrainExpr pZ = new ConstrainExpr(z).setNumeral(n1, n2);
            ConstrainExpr nZ = new ConstrainExpr((BoolExpr) ctx.mkNot(z).simplify()).setNumeral(n1, n2);
            nzcvExprs[N] = new NZCVExpr(pN, nN);
            nzcvExprs[Z] = new NZCVExpr(pZ, nZ);
        }
        return v1.getReplacement(result);
    }
}


