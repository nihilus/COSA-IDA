package com.tosnos.cosa.binary.asm;


import com.microsoft.z3.*;
import com.tosnos.cosa.binary.LibraryModule;
import com.tosnos.cosa.binary.NativeLibraryHandler;
import com.tosnos.cosa.binary.asm.value.AbstractValue;
import com.tosnos.cosa.binary.asm.value.ConstrainExpr;
import com.tosnos.cosa.binary.asm.value.Immediate;
import com.tosnos.cosa.binary.asm.value.Numeral;

import javax.print.attribute.standard.MediaSize;
import java.util.Arrays;

/**
 * Created by kevin on 7/4/14.
 */
public class ArithmeticInstruction extends Instruction {
    public ArithmeticInstruction(LibraryModule libraryModule, int address, boolean thumb, Opcode opcode, int modifier, byte condition, Operand operands) {
        super(libraryModule, address, thumb, opcode, modifier, condition, operands);
    }

    @Override
    public Operand[] getRequiredOperands() {
        return getArithmeticSources();
    }

    @Override
    public Operand[] getAffectedOperands() {
        return new Operand[]{getArithmeticDestination()};
    }

    public Operand[] getArithmeticSources() {
        if (operands.length > 2) {
            return Arrays.copyOfRange(operands, 1, operands.length);
        }
        return operands;
    }

    public Register getArithmeticDestination() {
        return (Register) operands[0];
    }

    @Override
    public boolean isArithmetic() {
        return true;
    }

    public static AbstractValue add(Operand o1, Operand o2) throws Z3Exception {
        return add(NativeLibraryHandler.getInstance().getContext(), o1, o2, null, 0);
    }

    public static AbstractValue add(Context ctx, Operand o1, Operand o2, NZCVExpr[] nzcvExprs, int modifier) throws Z3Exception {
        AbstractValue v1 = o1.getValue();
        AbstractValue v2 = o2.getValue();
        Numeral n1 = v1.getNumeral();
        Numeral n2 = v2.getNumeral();
        BitVecExpr b1 = n1.getBitVecExpr(ctx);
        BitVecExpr b2 = n2.getBitVecExpr(ctx);

        BitVecExpr expr = ctx.mkBVAdd(b1, b2);

        // update nzcv
        if ((modifier & MODIFIER_UPDATE_FLAG) > 0 && nzcvExprs != null) {

            // http://infocenter.arm.com/help/index.jsp?topic=/com.arm.doc.dui0801a/CIADCDHH.html
            BitVecNum zero = ctx.mkBV(0, 32);
            BoolExpr n = (BoolExpr) ctx.mkBVSLT(expr, zero).simplify();
            ConstrainExpr pN = new ConstrainExpr(n).setNumeral(n1, n2);
            ConstrainExpr nN = new ConstrainExpr((BoolExpr) ctx.mkNot(n).simplify()).setNumeral(n1, n2);
            nzcvExprs[N] = new NZCVExpr(pN, nN);

            BoolExpr z = (BoolExpr) ctx.mkEq(expr, zero).simplify();
            ConstrainExpr pZ = new ConstrainExpr(z).setNumeral(n1, n2);
            ConstrainExpr nZ = new ConstrainExpr((BoolExpr) ctx.mkNot(z).simplify()).setNumeral(n1, n2);
            nzcvExprs[Z] = new NZCVExpr(pZ, nZ);

            if((modifier & MODIFIER_UPDATE_FLAG_NZ)==0) {
                BoolExpr nc = (BoolExpr) ctx.mkBVAddNoOverflow(b1, b2, false).simplify();
                ConstrainExpr pC = new ConstrainExpr((BoolExpr) ctx.mkNot(nc).simplify()).setNumeral(n1, n2);
                ConstrainExpr nC = new ConstrainExpr(nc).setNumeral(n1, n2);
                nzcvExprs[C] = new NZCVExpr(pC, nC);

                BoolExpr nv = (BoolExpr) (ctx.mkBVAddNoOverflow(b1, b2, true).simplify());
                ConstrainExpr pV = new ConstrainExpr((BoolExpr) ctx.mkNot(nv).simplify()).setNumeral(n1, n2);
                ConstrainExpr nV = new ConstrainExpr(nv).setNumeral(n1, n2);
                nzcvExprs[V] = new NZCVExpr(pV, nV);
            }
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

        if(v1.isImmediate()||v2.isAllocatedMemory()) {
            return v2.getReplacement(result);
        } else {
            return v1.getReplacement(result);
        }
    }

    public static AbstractValue sub(Context ctx, Operand o1, Operand o2, NZCVExpr[] nzcvExprs, int modifier) throws Z3Exception {
        AbstractValue v1 = o1.getValue();
        AbstractValue v2 = o2.getValue();
        Numeral n1 = v1.getNumeral();
        Numeral n2 = v2.getNumeral();
        BitVecExpr b1 = n1.getBitVecExpr(ctx);
        BitVecExpr b2 = n2.getBitVecExpr(ctx);

        BitVecExpr expr = ctx.mkBVSub(b1, b2);
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

            if((modifier & MODIFIER_UPDATE_FLAG_NZ)==0) {
           //     BoolExpr nc = (BoolExpr) ctx.mkBVULT(b1, b2).simplify();
//                ConstrainExpr pC = new ConstrainExpr((BoolExpr) ctx.mkNot(nc).simplify()).setNumeral(n1, n2);
//                ConstrainExpr nC = new ConstrainExpr(nc).setNumeral(n1, n2);
                BoolExpr nc = (BoolExpr) ctx.mkBVSubNoUnderflow(b1,b2,false).simplify();//mkBVULT(b1, b2).simplify();
                ConstrainExpr pC = new ConstrainExpr(nc).setNumeral(n1, n2);
                ConstrainExpr nC = new ConstrainExpr((BoolExpr) ctx.mkNot(nc).simplify()).setNumeral(n1, n2);


                BoolExpr nv = (BoolExpr) ctx.mkAnd(ctx.mkBVSubNoOverflow(b1, b2), ctx.mkBVSubNoUnderflow(b1, b2, true)).simplify();
                ConstrainExpr pV = new ConstrainExpr((BoolExpr) ctx.mkNot(nv).simplify()).setNumeral(n1, n2);
                ConstrainExpr nV = new ConstrainExpr(nv).setNumeral(n1, n2);
                nzcvExprs[C] = new NZCVExpr(pC, nC);
                nzcvExprs[V] = new NZCVExpr(pV, nV);
            }
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

    public void compute(Context ctx, NZCVExpr[] nzcvExprs) throws Z3Exception {
        Operand[] sources = getArithmeticSources();
        Register rd = getArithmeticDestination();

        switch (opcode) {
            case ADD:
                rd.setValue(add(ctx, sources[0], sources[1], nzcvExprs, modifier));
                break;
            case SUB:
                rd.setValue(sub(ctx, sources[0], sources[1], nzcvExprs, modifier));
                break;
            case RSB:
                rd.setValue(sub(ctx, sources[1], sources[0], nzcvExprs, modifier));
                break;
            case SBC: // need to check the carry bit
                if(!isCarrySet()) {
                    rd.setValue(sub(ctx, sources[0], add(ctx, sources[1], Immediate.ONE, nzcvExprs, 0), nzcvExprs, modifier));
                } else {
                    rd.setValue(sub(ctx, sources[0], sources[1], nzcvExprs, modifier));
                }
                break;
            case ADC: // need to check the carry bit
                if(isCarrySet()) {
                    rd.setValue(add(ctx, sources[0], add(ctx, sources[1], Immediate.ONE, nzcvExprs, 0), nzcvExprs, modifier));
                } else {
                    rd.setValue(add(ctx, sources[0], sources[1], nzcvExprs, modifier));
                }
                break;
            case RSC: // need to check the carry bit
                if(!isCarrySet()) {
                    rd.setValue(sub(ctx, sources[1], add(ctx, sources[0], Immediate.ONE, nzcvExprs, 0), nzcvExprs, modifier));
                } else {
                    rd.setValue(sub(ctx, sources[1], sources[0], nzcvExprs, modifier));
                }
                break;
            default:
                throw new RuntimeException("not implemented for " + opcode);
        }
    }
}
