package com.tosnos.cosa.binary.asm;

import com.microsoft.z3.*;
import com.tosnos.cosa.binary.LibraryModule;
import com.tosnos.cosa.binary.asm.value.AbstractValue;
import com.tosnos.cosa.binary.asm.value.ConstrainExpr;
import com.tosnos.cosa.binary.asm.value.Immediate;
import com.tosnos.cosa.binary.asm.value.Numeral;

/**
 VMOV{cond} Dm, Rd, Rn
 VMOV{cond} Rd, Rn, Dm
 VMOV{cond} Sm, Sm1, Rd, Rn
 VMOV{cond} Rd, Rn, Sm, Sm1
 where:
 cond is an optional condition code.
 Dm is a 64-bit extension register.
 Sm is a VFP 32-bit register.
 Sm1 is the next consecutive VFP 32-bit register after Sm.
 Rd, Rn are the ARM registers. Rd and Rn must not be PC.
 Show/hideUsage
 VMOV Dm, Rd, Rn transfers the contents of Rd into the low half of Dm, and the contents of Rn into the high half of Dm.
 VMOV Rd, Rn, Dm transfers the contents of the low half of Dm into Rd, and the contents of the high half of Dm into Rn.
 VMOV Rd, Rn, Sm, Sm1 transfers the contents of Sm into Rd, and the contents of Sm1 into Rn.
 VMOV Sm, Sm1, Rd, Rn transfers the contents of Rd into Sm, and the contents of Rn into Sm1.
 */
public class VFPMoveInstruction extends Instruction {

    public VFPMoveInstruction(LibraryModule libraryModule, int address, boolean thumb, Opcode opcode, int modifier, byte condition, Operand operand) {
        super(libraryModule, address, thumb, opcode, modifier, condition, operand);
    }

    public Operand[] getMoveSource() {
        if(operands.length==2) {
            return new Operand[]{operands[1]};
        } else if(operands.length==3) {
            if(((Register)operands[0]).isDoubleword()) {
                return new Operand[]{operands[1],operands[2]};
            } else {
                return new Operand[]{operands[2]};
            }
        } else {
            return new Operand[]{operands[2],operands[3]};
        }
    }

/*
S32.F32
floating-point to signed integer or fixed-point

U32.F32
floating-point to unsigned integer or fixed-point

F32.S32
signed integer or fixed-point to floating-point

F32.U32
unsigned integer or fixed-point to floating-point.
 */

    public Operand[] getDestination() {
        if(operands.length==2) {
            return new Operand[]{operands[0]};
        } else if(operands.length==3) {
            if(((Register)operands[0]).isDoubleword()) {
                return new Operand[]{operands[0]};
            } else {
                return new Operand[]{operands[0],operands[1]};
            }
        } else {
            return new Operand[]{operands[0],operands[1]};
        }
    }

    @Override
    public boolean isVFPMove() {
        return true;
    }

    @Override
    public Operand[] getRequiredOperands() {
        return getMoveSource();
    }

    @Override
    public Operand[] getAffectedOperands() {
        return getDestination();
    }


    public void compute(Context ctx, NZCVExpr[] nzcvExprs) throws Z3Exception {
        Operand[] source = getMoveSource();
        Operand[] rd = getDestination();

        switch (opcode) {
            case VMOV:
                if (rd.length == 1) {
                    if (source.length == 1) {
                        ((Register)rd[0]).setValue(source[0].getValue());
                    } else {
                        ((Register)rd[0]).setValue(source[0].getValue().getReplacement(new Numeral(source[0].getNumeral(), source[1].getNumeral(), TYPE_DOUBLE)));
                    }
                } else {
                    if (source.length == 1) {
                        ((Register)rd[0]).setValue(source[0].getValue().getReplacement(source[0].getNumeral().getLower(ctx)));
                        ((Register)rd[1]).setValue(source[0].getValue().getReplacement(source[0].getNumeral().getUpper(ctx)));
                    } else {
                        ((Register)rd[0]).setValue(source[0].getValue());
                        ((Register)rd[1]).setValue(source[1].getValue());
                    }
                }
                break;
            case VCVT:
                if(source[0].getValue().isUnknown()) {
                    if ((modifier & VFP_DATA_64) > 0) {
                        ((Register)rd[0]).setValue(Immediate.newValue(TYPE_DOUBLE));
                        break;
                    } else if((modifier & VFP_DATA_32) > 0 && (modifier & VFP_DATA_F) > 0) {
                        ((Register)rd[0]).setValue(Immediate.newValue(TYPE_FLOAT));
                        break;
                    }
                    throw new RuntimeException("E");
                } else {
                    Number number = source[0].getValue().getNumeral().getNumber();
                    if ((modifier & VFP_DATA_64) > 0) {
                        ((Register)rd[0]).setValue(new Immediate(number.doubleValue()));
                        break;
                    } else if((modifier & VFP_DATA_32) > 0 && (modifier & VFP_DATA_F) > 0) {
                        ((Register)rd[0]).setValue(new Immediate((float)number.intValue()));
                    } else {
                        throw new RuntimeException("N");
                    }
                }
                break;
            default:
                throw new RuntimeException("no");

        }




    }


}