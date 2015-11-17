package com.tosnos.cosa.binary.asm;

import com.microsoft.z3.*;
import com.tosnos.cosa.binary.LibraryModule;
import com.tosnos.cosa.binary.asm.value.AbstractValue;
import com.tosnos.cosa.binary.asm.value.ConstrainExpr;
import com.tosnos.cosa.binary.asm.value.Immediate;
import com.tosnos.cosa.binary.asm.value.Numeral;

/**
 * Created by kevin on 7/6/14.
 * If S is specified, these instructions:
 * update the N and Z flags according to the result
 * can update the C flag during the calculation of Operand2
 * do not affect the V flag.
 *
 *
 * When the instruction is ASRS or when
 * ASR #n is used in Operand2 with the instructions
 * MOVS, MVNS, ANDS, ORRS, ORNS, EORS, BICS, TEQ or TST,
 * the carry flag is updated to the last bit shifted out,
 * bit[n-1], of the register Rm.
 *
 */
public class MoveInstruction extends SecondShiftInstruction {
    private final boolean pcRelated;
    private Register shift;


    public MoveInstruction(LibraryModule libraryModule, int address, boolean thumb, Opcode opcode, int modifier, byte condition, Operand operand) {
        super(libraryModule, address, thumb, opcode, modifier, condition, operand);
        this.pcRelated = ((Register)super.operands[0]).isProgramCounter();
    }

    public Operand getMoveSource() {
        return operands[1];
    }

    public Operand getMoveSecondSources() {
        return operands[2];
    }

    public Register getDestination() {
        return (Register) operands[0];
    }

    @Override
    public boolean isMove() {
        return true;
    }

    @Override
    public Operand[] getRequiredOperands() {
        return new Operand[]{getMoveSource()};
    }

    @Override
    public Operand[] getAffectedOperands() {
        return new Operand[]{getDestination()};
    }

    public boolean isPCRelated() {
        return pcRelated;
    }

    public void compute(Context ctx, NZCVExpr[] nzcvExprs) throws Z3Exception {
        // update nzcv
        if((modifier&MODIFIER_UPDATE_FLAG)>0 && nzcvExprs!=null) {
            NZCVExpr c = getCarryFlag(ctx, getShift());
            if(c!=null) {
                nzcvExprs[C] = c;
            }
        }

        Operand source = getMoveSource();
        Register rd = getDestination();
        AbstractValue value;

        switch (opcode) {
            case MOV: // nz(c)
            case ADR: // This instruction does not change the flags
                value = source.getValue();
                break;
            case MVN: {
                Numeral numeral = source.getNumeral();
                Numeral newNumeral = new Numeral(ctx.mkBVNot(numeral.getBitVecExpr(ctx)).simplify(), numeral.getType());
                if (newNumeral.isUnknown()) {
                    numeral.addRelatedNumeral(newNumeral);
                }
                value = source.getValue().getReplacement(new Numeral(ctx.mkBVNot(numeral.getBitVecExpr(ctx)).simplify(), numeral.getType()));
                break;
            }
            case MOVT: {
                BitVecExpr top = ctx.mkBV(source.getValue().intValue(), 16);
                AbstractValue v = rd.getValue();
                Numeral numeral = v.getNumeral();
                Numeral newNumeral = new Numeral(ctx.mkConcat(top, ctx.mkExtract(15,0, numeral.getBitVecExpr(ctx))).simplify(), TYPE_INT);
                if (newNumeral.isUnknown()) {
                    numeral.addRelatedNumeral(newNumeral);
                }
                value = v.getReplacement(newNumeral);
                break;
            }
            default:
                throw new RuntimeException("no" + opcode);

        }

        if((modifier&MODIFIER_UPDATE_FLAG)>0 && nzcvExprs!=null) {
            BitVecNum zero = ctx.mkBV(0, 32);
            Numeral n1 = value.getNumeral();
            BitVecExpr b1 = n1.getBitVecExpr(ctx);
            BoolExpr n = (BoolExpr) ctx.mkBVSLT(b1, zero).simplify();
            ConstrainExpr pN = new ConstrainExpr(n).setNumeral(n1);
            ConstrainExpr nN = new ConstrainExpr((BoolExpr) ctx.mkNot(n).simplify()).setNumeral(n1);

            BoolExpr z = (BoolExpr) ctx.mkEq(b1, zero).simplify();
            ConstrainExpr pZ = new ConstrainExpr(z).setNumeral(n1);
            ConstrainExpr nZ = new ConstrainExpr((BoolExpr) ctx.mkNot(z).simplify()).setNumeral(n1);
            nzcvExprs[N] = new NZCVExpr(pN, nN);
            nzcvExprs[Z] = new NZCVExpr(pZ, nZ);
        }

        rd.setValue(value);



//            case MOVT: // This instruction does not change the flags.
//                value = Instruction.operate(Opcode.ADD, (Instruction.operate(Opcode.ADD, rd.getValue(), new Immediate(0xFFFFFFFF))), Instruction.shift(Opcode.LSL, source.getValue(), new Immediate(16)));
//                break;
//            default:
//                throw new RuntimeException("not implemented yet for " + opcode);
//        }
//
//        rd.setValue(value);
//        switch(opcode) {
//            case ADR:
//            case MOV:
//            case MOVT:
//            case MVN:
//        }
    }


}
