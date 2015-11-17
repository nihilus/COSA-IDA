package com.tosnos.cosa.binary.asm;

import com.microsoft.z3.*;
import com.tosnos.cosa.binary.LibraryModule;
import com.tosnos.cosa.binary.asm.value.AbstractValue;
import com.tosnos.cosa.binary.asm.value.ConstrainExpr;
import com.tosnos.cosa.binary.asm.value.Numeral;

import java.util.Arrays;

/**
 * Created by kevin on 7/9/14.
 */
public class LogicInstruction extends SecondShiftInstruction {
    public LogicInstruction(LibraryModule libraryModule, int address, boolean thumb, Opcode opcode, int modifier, byte condition, Operand operand) {
        super(libraryModule, address, thumb, opcode, modifier, condition, operand);
    }

    public Operand[] getLogicSources() {
        if (operands.length > 2) {
            return Arrays.copyOfRange(operands, 1, operands.length);
        }
        return operands;
    }

    public Register getLogicDestination() {
        return (Register)operands[0];
    }

    @Override
    public boolean isLogical() {
        return true;
    }

    @Override
    public Operand[] getRequiredOperands() {
        return getLogicSources();
    }

    @Override
    public Operand[] getAffectedOperands() {
        return new Operand[]{getLogicDestination()};
    }


    public static AbstractValue compute(Context ctx, Opcode opcode, Operand o1, Operand o2, NZCVExpr[] nzcvExprs, int modifier, Register shift) throws Z3Exception {
        if((modifier&MODIFIER_UPDATE_FLAG)>0 && nzcvExprs!=null) {
            NZCVExpr c = getCarryFlag(ctx, shift);
            if(c!=null) {
                nzcvExprs[C] = c;
            }
        }

        AbstractValue v1 = o1.getValue();
        AbstractValue v2 = o2.getValue();
        Numeral n1 = v1.getNumeral();
        Numeral n2 = v2.getNumeral();
        BitVecExpr b1 = n1.getBitVecExpr(ctx);
        BitVecExpr b2 = n2.getBitVecExpr(ctx);
        if(b1.getSortSize()<32) {
            b1 = ctx.mkZeroExt(32-b1.getSortSize(),b1);
        }
        if(b2.getSortSize()<32) {
            b2 = ctx.mkZeroExt(32-b2.getSortSize(),b2);
        }

        BitVecExpr expr;

        switch (opcode) {
            case ORR: // nz(c)
                expr = (BitVecExpr)ctx.mkBVOR(b1, b2).simplify();
                break;
            case AND: // nz(c)
                expr = (BitVecExpr)ctx.mkBVAND(b1, b2).simplify();
                break;
            case EOR: // nz(c)
                expr = (BitVecExpr)ctx.mkBVXOR(b1, b2).simplify();
                break;
            case BIC: // nz(c)
                expr = (BitVecExpr)ctx.mkBVAND(b1, ctx.mkBVNot(b2)).simplify();
                break;
            default:
                throw new RuntimeException("no");

        }

        Numeral result = new Numeral(expr, TYPE_INT);
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

    public void compute(Context ctx, NZCVExpr[] nzcvExprs) throws Z3Exception {
        Operand[] source = getLogicSources();
        Register rd = getLogicDestination();
        rd.setValue(compute(ctx, opcode, source[0], source[1], nzcvExprs, modifier, super.getShift()));
    }
}