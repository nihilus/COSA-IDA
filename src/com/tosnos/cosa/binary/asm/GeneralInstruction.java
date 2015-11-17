package com.tosnos.cosa.binary.asm;

import com.microsoft.z3.BitVecExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Z3Exception;
import com.tosnos.cosa.binary.LibraryModule;
import com.tosnos.cosa.binary.asm.value.AbstractValue;
import com.tosnos.cosa.binary.asm.value.Immediate;
import com.tosnos.cosa.binary.asm.value.Numeral;

/**
 * Created by kevin on 11/3/15.
 */
public class GeneralInstruction extends Instruction {
    public GeneralInstruction(LibraryModule libraryModule, int address, boolean thumb, Opcode opcode, int modifier, byte condition, Operand operand) {
        super(libraryModule, address, thumb, opcode, modifier, condition, operand);
    }

    @Override
    public boolean isGeneral() {
        return true;
    }

    public void compute(Context ctx, NZCVExpr[] nzcvExprs) throws Z3Exception {
        Register rd = (Register)operands[0];
        AbstractValue result;
        switch(opcode) {
            case CLZ:
                result = clz(operands[1]);
                break;
            case NEG:
                result = ArithmeticInstruction.sub(ctx, Immediate.ZERO, operands[1], nzcvExprs, modifier);
                break;
            case SBFX:
            case UBFX:
                result = bfx(ctx, operands[1], operands[2], operands[3], modifier);
                break;
            case SXT:
                modifier |= MODIFIER_SIGNED;
            case UXT:
                result = xt(ctx, operands[1], modifier);
                break;
            case UXTA:
            case SXTA:
            case REV:
            case RBIT:
            default:
                throw new RuntimeException("not imeple" +  opcode);
        }
        rd.setValue(result);
    }

    public AbstractValue clz(Operand o) throws Z3Exception {
        AbstractValue v = o.getValue();
        if(v.isUnknown()) {
           return Immediate.newValue(TYPE_INT);
        } else {
           return new Immediate(Integer.numberOfLeadingZeros(v.intValue()));
        }
    }

    public AbstractValue xt(Context ctx, Operand o1, int modifier) throws Z3Exception {
        AbstractValue v1 = o1.getValue();
        Numeral n1 = v1.getNumeral();
        BitVecExpr b1 = n1.getBitVecExpr(ctx);
        byte type = (byte)(modifier&0xF);
        int ext = 24;

        if(type==TYPE_BYTE) {
            b1 = ctx.mkExtract(7,0,b1);
        } else { // half
            b1 = ctx.mkExtract(15,0,b1);
            ext = 16;
        }

        if((modifier&MODIFIER_SIGNED)>0) {
            b1 = ctx.mkSignExt(ext, b1);
        } else {
            b1 = ctx.mkZeroExt(ext, b1);
        }

        Numeral n = new Numeral(b1, TYPE_INT);

        if(n.isUnknown()) {
            n.addRelatedNumeral(n1);
            n1.addRelatedNumeral(n);
        }

        return new Immediate(n);
    }

    public AbstractValue bfx(Context ctx, Operand o1, Operand o2, Operand o3, int modifier) throws Z3Exception {
        AbstractValue v1 = o1.getValue();
        AbstractValue v2 = o2.getValue();
        AbstractValue v3 = o3.getValue();

        if(v2.isUnknown() || v3.isUnknown()) {
            return Immediate.newValue(TYPE_INT);
        }
        Numeral n1 = v1.getNumeral();
        int lsb = v2.intValue();
        int width = v3.intValue();
        BitVecExpr b1 = n1.getBitVecExpr(ctx);

        b1 = ctx.mkExtract(lsb+width-1, lsb, b1);
        if((modifier&MODIFIER_SIGNED)>0) {
            b1 = (BitVecExpr)ctx.mkSignExt(BITS_WORD-width, b1).simplify();
        } else {
            b1 = (BitVecExpr)ctx.mkZeroExt(BITS_WORD-width, b1).simplify();
        }

        Numeral n = new Numeral(b1, TYPE_INT);

        if(n.isUnknown()) {
            n.addRelatedNumeral(n1);
            n1.addRelatedNumeral(n);
        }
        return v1.getReplacement(n);
    }
}
