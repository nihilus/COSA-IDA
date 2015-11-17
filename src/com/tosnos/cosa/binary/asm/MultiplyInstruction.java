package com.tosnos.cosa.binary.asm;

import com.microsoft.z3.*;
import com.tosnos.cosa.binary.LibraryModule;
import com.tosnos.cosa.binary.asm.value.AbstractValue;
import com.tosnos.cosa.binary.asm.value.ConstrainExpr;
import com.tosnos.cosa.binary.asm.value.Immediate;
import com.tosnos.cosa.binary.asm.value.Numeral;

import java.util.Arrays;

/**
 * Created by kevin on 7/4/14.
 */
public class MultiplyInstruction extends Instruction {

    public MultiplyInstruction(LibraryModule libraryModule, int address, boolean thumb, Opcode opcode, int modifier, byte condition, Operand operand) {
        super(libraryModule, address, thumb, opcode, modifier, condition, operand);
    }

    public boolean isMultiply() {
        return true;
    }

    public Operand[] getMultiplySources() {
        if (operands.length > 2) {
            return Arrays.copyOfRange(operands, 1, operands.length);
        }
        return operands;
    }

    public Register getMultiplyDestination() {
        return (Register)operands[0];
    }

    @Override
    public Operand[] getRequiredOperands() {
        return getMultiplySources();
    }

    @Override
    public Operand[] getAffectedOperands() {
        return new Operand[]{getMultiplyDestination()};
    }


    public static AbstractValue mul(Context ctx, Operand o1, Operand o2) throws Z3Exception {
        return mul(ctx, o1, o2, null, 0);
    }

    public static AbstractValue mul(Context ctx, Operand o1, Operand o2, NZCVExpr[] nzcvExprs, int modifier) throws Z3Exception {
        AbstractValue v1 = o1.getValue();
        AbstractValue v2 = o2.getValue();
        Numeral n1 = v1.getNumeral();
        Numeral n2 = v2.getNumeral();
        BitVecExpr b1 = n1.getBitVecExpr(ctx);
        BitVecExpr b2 = n2.getBitVecExpr(ctx);

        BitVecExpr expr = ctx.mkBVMul(b1, b2);
        // update nzcv
        if ((modifier & MODIFIER_UPDATE_FLAG) > 0 && nzcvExprs != null) {
            BitVecNum zero = ctx.mkBV(0, b1.getSortSize());

            BoolExpr n = (BoolExpr) ctx.mkBVSLT(expr, zero).simplify();
            ConstrainExpr pN = new ConstrainExpr(n).setNumeral(n1, n2);
            ConstrainExpr nN = new ConstrainExpr((BoolExpr) ctx.mkNot(n).simplify()).setNumeral(n1, n2);

            BoolExpr z = (BoolExpr) ctx.mkEq(expr, zero).simplify();
            ConstrainExpr pZ = new ConstrainExpr(z).setNumeral(n1, n2);
            ConstrainExpr nZ = new ConstrainExpr((BoolExpr) ctx.mkNot(z).simplify()).setNumeral(n1, n2);

            nzcvExprs[N] = new NZCVExpr(pN, nN);
            nzcvExprs[Z] = new NZCVExpr(pZ, nZ);
        }

        Numeral result = new Numeral(expr.simplify(), n1.getType());

        if(n1.isUnknown()) {
            n1.addRelatedNumeral(result);
            result.addRelatedNumeral(n1);
        }

        if(n2.isUnknown()) {
            n2.addRelatedNumeral(result);
            result.addRelatedNumeral(n2);
        }

        if(v1.isImmediate()) {
            return v2.getReplacement(result);
        } else {
            return v1.getReplacement(result);
        }
    }

    public static AbstractValue smul(Context ctx, Operand o1, Operand o2, NZCVExpr[] nzcvExprs, int modifier) throws Z3Exception {
        AbstractValue v1 = o1.getValue();
        AbstractValue v2 = o2.getValue();
        Numeral n1 = v1.getNumeral();
        Numeral n2 = v2.getNumeral();
        BitVecExpr b1 = n1.getBitVecExpr(ctx);
        BitVecExpr b2 = n2.getBitVecExpr(ctx);

        // for b2 // tx.mkSignExt(0,ctx.mkExtract(15,0,b)).simplify());
        if((modifier&MODIFIER_SB)>0) {
            b2 = ctx.mkSignExt(16, ctx.mkExtract(15,0,b2));
        } else if((modifier&MODIFIER_ST)>0) {
            b2 = ctx.mkBVASHR(b2, ctx.mkBV(16,32));
        }

        // for b1
        if((modifier&MODIFIER_MB)>0) {
            b1 = ctx.mkSignExt(16, ctx.mkExtract(15,0,b1));
        } else if((modifier&MODIFIER_MT)>0) {
            b1 = ctx.mkBVASHR(b1, ctx.mkBV(16,32));
        } else { // for "W" Writes the signed most significant 32 bits of the 48-bit result in the destination register.
            b1 = ctx.mkSignExt(16, b1); // make 48bit
            b2 = ctx.mkSignExt(16, b2); // make 48bit
        }

        BitVecExpr expr = ctx.mkBVMul(b1, b2);

        if((modifier&MODIFIER_WIDE)>0) {
            expr = ctx.mkExtract(47,16, expr);
        }


        // update nzcv
        if ((modifier & MODIFIER_UPDATE_FLAG) > 0 && nzcvExprs != null) {
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

        Numeral result = new Numeral(expr.simplify(), n1.getType());

        if(n1.isUnknown()) {
            n1.addRelatedNumeral(result);
            result.addRelatedNumeral(n1);
        }

        if(n2.isUnknown()) {
            n2.addRelatedNumeral(result);
            result.addRelatedNumeral(n2);
        }

        if(v1.isImmediate()) {
            return v2.getReplacement(result);
        } else {
            return v1.getReplacement(result);
        }
    }



    public static void mull(Context ctx, Register lo, Register hi, Operand o1, Operand o2, NZCVExpr[] nzcvExprs, int modifier) throws Z3Exception {
        AbstractValue v1 = o1.getValue();
        AbstractValue v2 = o2.getValue();
        Numeral n1 = v1.getNumeral();
        Numeral n2 = v2.getNumeral();
        BitVecExpr b1 = n1.getBitVecExpr(ctx);
        BitVecExpr b2 = n2.getBitVecExpr(ctx);

        // for b2 // tx.mkSignExt(0,ctx.mkExtract(15,0,b)).simplify());
        if((modifier&MODIFIER_SIGNED)>0) {
            b1 = ctx.mkSignExt(32, b1);
            b2 = ctx.mkSignExt(32, b2);
        } else {
            b1 = ctx.mkZeroExt(32, b1);
            b2 = ctx.mkZeroExt(32, b2);
        }

        BitVecExpr expr = ctx.mkBVMul(b1, b2);

        // update nzcv
        if ((modifier & MODIFIER_UPDATE_FLAG) > 0 && nzcvExprs != null) {
            BitVecNum zero = ctx.mkBV(0, 64);

            BoolExpr n = (BoolExpr) ctx.mkBVSLT(expr, zero).simplify();
            ConstrainExpr pN = new ConstrainExpr(n).setNumeral(n1, n2);
            ConstrainExpr nN = new ConstrainExpr((BoolExpr) ctx.mkNot(n).simplify()).setNumeral(n1, n2);

            BoolExpr z = (BoolExpr) ctx.mkEq(expr, zero).simplify();
            ConstrainExpr pZ = new ConstrainExpr(z).setNumeral(n1, n2);
            ConstrainExpr nZ = new ConstrainExpr((BoolExpr) ctx.mkNot(z).simplify()).setNumeral(n1, n2);

            nzcvExprs[N] = new NZCVExpr(pN, nN);
            nzcvExprs[Z] = new NZCVExpr(pZ, nZ);
        }

        Numeral result1 = new Numeral(ctx.mkExtract(63,32,expr).simplify(), TYPE_INT);
        Numeral result2 = new Numeral(ctx.mkExtract(31,0,expr).simplify(), TYPE_INT);

        if(n1.isUnknown()) {
            n1.addRelatedNumeral(result1);
            result1.addRelatedNumeral(n1);
            n1.addRelatedNumeral(result2);
            result2.addRelatedNumeral(n1);
        }

        if(n2.isUnknown()) {
            n2.addRelatedNumeral(result1);
            result1.addRelatedNumeral(n2);
            n2.addRelatedNumeral(result2);
            result2.addRelatedNumeral(n2);
        }

        hi.setValue(new Immediate(result1));
        lo.setValue(new Immediate(result2));
    }

    public static void mlal(Context ctx, Register lo, Register hi, Operand o1, Operand o2, NZCVExpr[] nzcvExprs, int modifier) throws Z3Exception {
        AbstractValue loV = lo.getValue();
        AbstractValue hiV = hi.getValue();
        AbstractValue v1 = o1.getValue();
        AbstractValue v2 = o2.getValue();
        Numeral loN = loV.getNumeral();
        Numeral hiN = hiV.getNumeral();
        Numeral n1 = v1.getNumeral();
        Numeral n2 = v2.getNumeral();
        BitVecExpr loB = loN.getBitVecExpr(ctx);
        BitVecExpr hiB = hiN.getBitVecExpr(ctx);
        BitVecExpr b1 = n1.getBitVecExpr(ctx);
        BitVecExpr b2 = n2.getBitVecExpr(ctx);

        // for b2 // tx.mkSignExt(0,ctx.mkExtract(15,0,b)).simplify());
        if((modifier&MODIFIER_SIGNED)>0) {
            b1 = ctx.mkSignExt(32, b1);
            b2 = ctx.mkSignExt(32, b2);
        } else {
            b1 = ctx.mkZeroExt(32, b1);
            b2 = ctx.mkZeroExt(32, b2);
        }

        BitVecExpr expr = ctx.mkBVAdd(ctx.mkConcat(hiB,loB),ctx.mkBVMul(b1, b2));

        // update nzcv
        if ((modifier & MODIFIER_UPDATE_FLAG) > 0 && nzcvExprs != null) {
            BitVecNum zero = ctx.mkBV(0, 64);

            BoolExpr n = (BoolExpr) ctx.mkBVSLT(expr, zero).simplify();
            ConstrainExpr pN = new ConstrainExpr(n).setNumeral(n1, n2);
            ConstrainExpr nN = new ConstrainExpr((BoolExpr) ctx.mkNot(n).simplify()).setNumeral(n1, n2);

            BoolExpr z = (BoolExpr) ctx.mkEq(expr, zero).simplify();
            ConstrainExpr pZ = new ConstrainExpr(z).setNumeral(n1, n2);
            ConstrainExpr nZ = new ConstrainExpr((BoolExpr) ctx.mkNot(z).simplify()).setNumeral(n1, n2);

            nzcvExprs[N] = new NZCVExpr(pN, nN);
            nzcvExprs[Z] = new NZCVExpr(pZ, nZ);
        }

        Numeral result1 = new Numeral(ctx.mkExtract(63,32,expr).simplify(), TYPE_INT);
        Numeral result2 = new Numeral(ctx.mkExtract(31,0,expr).simplify(), TYPE_INT);

        if(n1.isUnknown()) {
            n1.addRelatedNumeral(result1);
            result1.addRelatedNumeral(n1);
            n1.addRelatedNumeral(result2);
            result2.addRelatedNumeral(n1);
        }

        if(n2.isUnknown()) {
            n2.addRelatedNumeral(result1);
            result1.addRelatedNumeral(n2);
            n2.addRelatedNumeral(result2);
            result2.addRelatedNumeral(n2);
        }

        hi.setValue(new Immediate(result1));
        lo.setValue(new Immediate(result2));
    }

    public void compute(Context ctx, NZCVExpr[] nzcvExprs) throws Z3Exception {
        Operand[] sources = getMultiplySources();
        Register rd = getMultiplyDestination();

        switch (opcode) {
            case MUL:
                rd.setValue(mul(ctx, sources[0], sources[1], nzcvExprs, modifier));
                break;
            case MLA:
                rd.setValue(ArithmeticInstruction.add(ctx, mul(ctx, sources[0], sources[1]), sources[2], nzcvExprs, modifier|MODIFIER_UPDATE_FLAG_NZ));
                break;
            case MLS:
                rd.setValue(ArithmeticInstruction.sub(ctx, mul(ctx, sources[0], sources[1]), sources[2], nzcvExprs, modifier|MODIFIER_UPDATE_FLAG_NZ));
                break;
            case SMUL:
                rd.setValue(smul(ctx, sources[0], sources[1], nzcvExprs, modifier));
                break;
            case SMULL:
                modifier |= MODIFIER_SIGNED;
            case UMULL:
                mull(ctx, (Register)operands[0], (Register)operands[1], operands[2], operands[3], nzcvExprs, modifier);
                break;
            case SMLAL:
                modifier |= MODIFIER_SIGNED;
            case UMLAL:
                mlal(ctx, (Register)operands[0], (Register)operands[1], operands[2], operands[3], nzcvExprs, modifier);
                break;
            default:
                throw new RuntimeException("not implemented for " + opcode);
        }
    }
}
