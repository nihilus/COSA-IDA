package com.tosnos.cosa.binary.asm;

import com.microsoft.z3.*;
import com.tosnos.cosa.binary.LibraryModule;
import com.tosnos.cosa.binary.NativeLibraryHandler;
import com.tosnos.cosa.binary.asm.value.AbstractValue;
import com.tosnos.cosa.binary.asm.value.ConstrainExpr;
import com.tosnos.cosa.binary.asm.value.Immediate;
import com.tosnos.cosa.binary.asm.value.Numeral;
import com.tosnos.cosa.binary.asm.value.memory.Address;

/**
 * Created by kevin on 12/7/14.
 */
public class Instruction extends AbstractValue {
    public static final Instruction empty = new IgnorableInstruction(null, 0, false, "S");
    public static final int THUMB_INSTRUCTION_SIZE = HALFWORD;
    public static final int THUMB_PC_RELATIVE = THUMB_INSTRUCTION_SIZE << 1;
    public static final int ARM_INSTRUCTION_SIZE = WORD;
    public static final int ARM_PC_RELATIVE = ARM_INSTRUCTION_SIZE << 1;
    protected final Opcode opcode;
    protected final Operand[] operands;
    protected final byte conditional;
    protected final boolean thumb;
    private final LibraryModule libraryModule;
    public String str;
    protected int modifier;
    private Instruction prev, next;
    protected int address;
    private boolean carry = false; // for sbc, abc to calculate ambiguous
    public void setCarry(boolean carry) {
        this.carry = carry;
    }

    public Instruction(LibraryModule libraryModule, int address, boolean thumb, Opcode opcode, int modifier, byte conditional, Operand operand) {
        this.libraryModule = libraryModule;
        this.address = address;
        this.thumb = thumb;
        this.opcode = opcode;
        this.modifier = modifier;
        this.conditional = conditional;
        if (operand != null) {
            if (operand.isOperandSet()) {
                this.operands = ((OperandSet) operand).getOperands();
            } else {
                this.operands = new Operand[]{operand};
            }
        } else {
            this.operands = new Operand[0];
        }
    }

    public boolean isCarrySet() {
        return carry;
    }

    @Override
    public Numeral getNumeral() {
        return new Numeral(address);
    }

    public Instruction(int address) {
        this.address = address;
        this.opcode = null;
        this.operands = null;
        this.conditional = 0;
        this.libraryModule = null;
        this.thumb = false;
    }


//    // update an abstract value to registers and stack memory
//    public static IAbstractValue updateValueOfParameter(int index, IAbstractValue value) {
//        IAbstractValue old = null;
//        if (index < Register.REG_PARAM_MAX) {
//            old = regs[index, value);
//        } else {
//            old = stack.put(regs[Register.SP).intValue() + (index - Register.REG_PARAM_MAX) * Instruction.WORD, value, 0);
//        }
//        return old;
//    }

//    public static void push(Map<Long, IAbstractValue> stack, Operand[] operands) {
//        Collections.reverse(Arrays.asList(operands));
//        for (Operand operand : operands) { // check memory section
//            push(stack, operand.getValue());
//        }
//    }


//    public static void pop(Map<Long, IAbstractValue> stack, Operand[] operands) {
//        for (Operand operand : operands) { // check memory section
//            pop(stack, (Register) operand);
//        }
//    }

//    public static void pop(Map<Long, IAbstractValue> stack, Register register) {
//        IAbstractValue offset = Register.getValue(Register.SP);
//        register.setValue(stack.get(offset.intValue()));
//        Register.setValue(Register.SP, ARMInstruction.add(offset, Instruction.WORD));
//    }


//    @Override
//    public static Address ubfx(Value lsb, Value width) {
//        if(isIndirect()) {
//            throw new RuntimeException("ubfx doesn't support indirect addressing");
//        }
//        return new Address((value >> lsb.intValue()) & ((1 << width.intValue()) - 1), this);
//    }
//
//    public static Address uxtb() {
//        return new Address(value & 0xFFFF, this);
//    }
//
//    public static Address uxth() {
//        return new Address(value & 0xFF, this);
//    }
//
////
//
//    //
//    @Override
//    public static Value smulbb(Value v) {
//        if(v.isIndirect()) {
//            throw new RuntimeException("smulbb doesn't support indirect addressing");
//        }
//        return new Address((value & 0xFFFF) * (v.intValue() & 0xFFFF), this);
//    }
    //    public static Immediate signExtend(int first, int last) {
//        int targetWidth = Math.max(bitWidth, last + 1);
//        long extension = value;
//        if ((value & BitRange.bitMask(bitWidth - 1,  bitWidth - 1)) > 0) {
//            extension = value | BitRange.bitMask(first, last);
//        }
//        return ValueFactory.createNumber(extension, targetWidth);
//    }
//
//    public static Immediate zeroFill(int first, int last) {
//        int targetWidth = Math.max(bitWidth, last + 1);
//        long filled = value & (
//                BitRange.bitMask(0, first - 1) |
//                        BitRange.bitMask(last + 1, targetWidth - 1));
//        return ValueFactory.createNumber(filled, targetWidth);
//    }

    /*

ASR
Arithmetic Shift Right.
LSL
Logical Shift Left.
LSR
Logical Shift Right.
ROR
Rotate Right.

ASR
shift length from 1 to 32
LSL
shift length from 0 to 31
LSR
shift length from 1 to 32
ROR
shift length from 1 to 31.


ASR provides the signed value of the contents of a register divided by a power of two. It copies the sign bit into vacated bit positions on the left.
LSL provides the value of a register multiplied by a power of two. LSR provides the unsigned value of a register divided by a variable power of two. Both instructions insert zeros into the vacated bit positions.
ROR provides the value of the contents of a register rotated by a value. The bits that are rotated off the right end are inserted into the vacated bit positions on the left.
RRX provides the value of the contents of a register shifted right one bit. The old carry flag is shifted into bit[31]. If the S suffix is present, the old bit[0] is placed in the carry flag.
    */
    
/*
    public static AbstractValue asr(Operand o1, Operand o2) {
        AbstractValue first = o1.getValue();
        AbstractValue second = o2.getValue();
        if (!first.isUnknown() && !second.isUnknown()) {
            Integer firstValue = first.intValue();
            Integer secondValue = second.intValue();
            if (firstValue != null && secondValue != null) {
                short nzcv = 0;
                if(secondValue>0) {
                    if((firstValue & (1 << (secondValue-1)))!=0) {
                        nzcv |= Modifier.FLAG_C;
                    }
                    firstValue >>= secondValue;
                }

                if(firstValue==0) {
                    nzcv |= Modifier.FLAG_Z;
                }

                AbstractValue resultValue = first.getReplacement(firstValue);
                resultValue.setNZCV(nzcv);
                return resultValue;
            }
        }
        return first.isUnknown()?first:second;
    }
*/


    /*
    public static AbstractValue lsl(Operand o1, Operand o2) {
        AbstractValue first = o1.getValue();
        AbstractValue second = o2.getValue();
        if (!first.isUnknown() && !second.isUnknown()) {
            Integer firstValue = first.intValue();
            Integer secondValue = second.intValue();
            if (firstValue != null && secondValue != null) {
                short nzcv = 0;
                if(secondValue>0) {
                    if((firstValue & (1 << (32-secondValue)))!=0) {
                        nzcv |= Modifier.FLAG_C;
                    }
                    firstValue <<= secondValue;
                }

                if(firstValue==0) {
                    nzcv |= Modifier.FLAG_Z;
                }

                AbstractValue resultValue = first.getReplacement(firstValue);
                resultValue.setNZCV(nzcv);
                return resultValue;
            }
        }
        return first.isUnknown()?first:second;
    }

*/

    /*
    public static AbstractValue ror(Operand o1, Operand o2) {
        AbstractValue first = o1.getValue();
        AbstractValue second = o2.getValue();
        if (!first.isUnknown() && !second.isUnknown()) {
            Integer firstValue = first.intValue();
            Integer secondValue = second.intValue();
            if (firstValue != null && secondValue != null) {
                short nzcv = 0;
                if(secondValue>0) {
                    if((firstValue & (1 << (secondValue-1)))!=0) {
                        nzcv |= Modifier.FLAG_C;
                    }
                    firstValue = Integer.rotateRight(firstValue, secondValue);
                }

                if(firstValue==0) {
                    nzcv |= Modifier.FLAG_Z;
                }

                AbstractValue resultValue = first.getReplacement(firstValue);
                resultValue.setNZCV(nzcv);
                return resultValue;
            }
        }
        return first.isUnknown()?first:second;
    }
*/



    /*
    public static AbstractValue rrx(Operand o1, short carry) {
        AbstractValue first = o1.getValue();
        if (!first.isUnknown()) {
            Integer firstValue = first.intValue();
            if (firstValue != null) {
                short nzcv = 0;
                if((firstValue & 1)!=0) {
                    nzcv |= Modifier.FLAG_C;
                }

                firstValue >>= 1;

                if(carry>0 && (carry&Modifier.FLAG_C)!=0) {
                    firstValue |= 0x80000000;
                }

                if(firstValue==0) {
                    nzcv |= Modifier.FLAG_Z;
                }

                AbstractValue resultValue = first.getReplacement(firstValue);
                resultValue.setNZCV(nzcv);
                return resultValue;
            }
            throw new RuntimeException("none empty");
        }
        return first;
    }
*/

/*
    public static AbstractValue and(Operand o1, Operand o2) {
        AbstractValue first = o1.getValue();
        AbstractValue second = o2.getValue();
        if (!first.isUnknown() && !second.isUnknown()) {
            Integer secondValue = second.intValue();
            if (secondValue != null) {
                return and(first, secondValue);
            }
        }
        return first.isUnknown()?first:second;
    }

    public static AbstractValue and(Operand o1, Integer secondValue) {
        AbstractValue first = o1.getValue();
        Integer firstValue = first.intValue();
        if (firstValue != null && secondValue != null) {
            short nzcv = 0;
            int result = firstValue & secondValue;
            if (result == 0) {
                nzcv |= Modifier.FLAG_Z;
            } else if (result < 0) {
                nzcv |= Modifier.FLAG_N;
            }
            AbstractValue resultValue = first.getReplacement(result);
            resultValue.setNZCV(nzcv);
            return resultValue;
        }
        return first;
    }

*/
/*
    public static AbstractValue orr(Operand o1, Operand o2) {
        AbstractValue first = o1.getValue();
        AbstractValue second = o2.getValue();
        if (!first.isUnknown() && !second.isUnknown()) {
            Integer firstValue = first.intValue();
            Integer secondValue = second.intValue();
            if (firstValue != null && secondValue != null) {
                short nzcv = 0;
                int result = firstValue | secondValue;
                if (result == 0) {
                    nzcv |= Modifier.FLAG_Z;
                } else if (result < 0) {
                    nzcv |= Modifier.FLAG_N;
                }
                AbstractValue resultValue = first.getReplacement(result);
                resultValue.setNZCV(nzcv);
                return resultValue;
            }
        }
        return first.isUnknown()?first:second;
    }

*/

    /*
    public static AbstractValue bic(Operand o1, Operand o2) {
        AbstractValue first = o1.getValue();
        AbstractValue second = o2.getValue();
        if (!first.isUnknown() && !second.isUnknown()) {
            Integer firstValue = first.intValue();
            Integer secondValue = second.intValue();
            if (firstValue != null && secondValue != null) {
                short nzcv = 0;
                int result = firstValue & ~secondValue;
                if (result == 0) {
                    nzcv |= Modifier.FLAG_Z;
                } else if (result < 0) {
                    nzcv |= Modifier.FLAG_N;
                }
                AbstractValue resultValue = first.getReplacement(result);
                resultValue.setNZCV(nzcv);
                return resultValue;
            }
        }
        return first.isUnknown()?first:second;
    }
*/

    /*
    public static AbstractValue orn(Operand o1, Operand o2) {
        AbstractValue first = o1.getValue();
        AbstractValue second = o2.getValue();
        if (!first.isUnknown() && !second.isUnknown()) {
            Integer firstValue = first.intValue();
            Integer secondValue = second.intValue();
            if (firstValue != null && secondValue != null) {
                short nzcv = 0;
                int result = firstValue | ~secondValue;
                if (result == 0) {
                    nzcv |= Modifier.FLAG_Z;
                } else if (result < 0) {
                    nzcv |= Modifier.FLAG_N;
                }
                AbstractValue resultValue = first.getReplacement(result);
                resultValue.setNZCV(nzcv);
                return resultValue;
            }
        }
        return first.isUnknown()?first:second;
    }
    */
//        public static AbstractValue neg(Operand o) {
//            return operate(Opcode.SUB, Immediate.ZERO, o);
//        }

/*
    public static AbstractValue neg(Operand o1) {
        AbstractValue first = o1.getValue();
        return sub(0, first);
    }
*/

/*
    public static AbstractValue mvn(Operand o1) {
        AbstractValue first = o1.getValue();
        return mvn(first);
    }

    public static AbstractValue mvn(AbstractValue first) {
        if(first.isUnknown()) {
            return UnknownImmediate.intType();
        }
        int value = first.intValue();
        value = ~value;
        return first.getReplacement(value);
    }
*/

    /*
    public static AbstractValue rev(Operand o1) {
        AbstractValue first = o1.getValue();
        if (!first.isUnknown()) {
            Integer firstValue = first.intValue();
            if (firstValue != null) {
                return first.getReplacement(Integer.reverseBytes(firstValue));
            }
            throw new RuntimeException("none empty");
        }
        return first;
    }
*/

    /*
    public static AbstractValue rbit(Operand o1) {
        AbstractValue first = o1.getValue();
        if (!first.isUnknown()) {
            Integer firstValue = first.intValue();
            if (firstValue != null) {
                return first.getReplacement(Integer.reverse(firstValue));
            }
            throw new RuntimeException("none empty");
        }
        return first;
    }



        public static AbstractValue xt(Operand o1, int modifier, byte type) {
            boolean signed = (modifier & MODIFIER_SIGNED) > 0;
            boolean h = (type == TYPE_HALF);

            AbstractValue first = o1.getValue();
            if (!first.isUnknown()) {
                Integer firstValue = first.intValue();
                if (firstValue != null) {
                    if (h) {
                        firstValue &= 0xFFFF;
                        if (signed && (firstValue & 0x8000) > 0) {
                            firstValue |= 0xFFFF0000;
                        }
                    } else {
                        firstValue &= 0xFF;
                        if (signed && (firstValue & 0x80) > 0) {
                            firstValue |= 0xFFFFFF00;
                        }
                    }
                    return first.getReplacement(firstValue);
                }
                throw new RuntimeException("none empty");
            }
            return first;
        }



    public static AbstractValue xta(Operand o1, Operand o2, int modifier, byte type) {
            return operate(Opcode.ADD, o1, xt(o2, modifier, type));
        }


        // TODO: signed??
        // Signed and Unsigned Bit Field Extract.
        public static AbstractValue bfx(Operand o1, Operand o2, Operand o3, int modifier) {
            throw new RuntimeException("not implemented ");

            boolean signed = (modifier & MODIFIER_SIGNED) > 0;
            AbstractValue first = o1.getValue();
            AbstractValue lsb = o2.getValue();
            AbstractValue width = o3.getValue();
            if (!first.isUnknown() && !lsb.isUnknown() && !width.isUnknown()) {
                return first.getReplacement((first.intValue() >> lsb.intValue()) & ((1 << width.intValue()) - 1));
            }
            return first;
        }

        public static short mlalxy(Register d1, Register d2, Register o1, Register o2, int modifier) {
            throw new RuntimeException("not implemented ");

            short nzcv = -1;
            boolean signed = (modifier & MODIFIER_SIGNED) > 0;
            AbstractValue rdLo = d1.getValue();
            AbstractValue rdHi = d2.getValue();
            AbstractValue rm = o1.getValue();
            AbstractValue rs = o2.getValue();
            if (!rdLo.isUnknown() && !rdHi.isUnknown()&& !rm.isUnknown() && !rs.isUnknown()) {
                long rmValue = rm.intValue();
                long rsValue = rs.intValue();
                if(!signed) {
                    rmValue = Integer.toUnsignedLong(rm.intValue());
                    rsValue = Integer.toUnsignedLong(rs.intValue());
                }

                if ((modifier & MODIFIER_MB) > 0) {
                    rmValue = rmValue & 0xFFFF;
                } else if ((modifier & MODIFIER_MT) > 0) {
                    rmValue = (rmValue >> 16) & 0xFFFFF;
                }
                if ((modifier & MODIFIER_SB) > 0) {
                    rsValue = rsValue & 0xFFFF;
                } else if ((modifier & MODIFIER_ST) > 0) {
                    rsValue = (rsValue >> 16) & 0xFFFFF;
                }

                long result = rmValue * rsValue;
                d1.setValue(add(rdLo, (int) (result & 0xFFFFFFFF)));
                d2.setValue(add(rdHi, (int) ((result >> 32) & 0xFFFFFFFF)));
                nzcv = 0;
                if (result == 0) {
                    nzcv |= Modifier.FLAG_Z;
                } else if (result < 0) {
                    nzcv |= Modifier.FLAG_N;
                }
            } else {
                d1.setValue(UnknownImmediate.doubleType());
                d2.setValue(UnknownImmediate.doubleType());
            }
            return nzcv;

        }

        public static short mlslxy(Register d1, Register d2, Register o1, Register o2, int modifier) {
            throw new RuntimeException("not implemented ");

            short nzcv = -1;
            boolean signed = (modifier & MODIFIER_SIGNED) > 0;
            AbstractValue rdLo = d1.getValue();
            AbstractValue rdHi = d2.getValue();
            AbstractValue rm = o1.getValue();
            AbstractValue rs = o2.getValue();
            if (!rdLo.isUnknown() && !rdHi.isUnknown() && !rm.isUnknown() && !rs.isUnknown()) {
                long rmValue = rm.intValue();
                long rsValue = rs.intValue();
                if(!signed) {
                    rmValue = Integer.toUnsignedLong(rm.intValue());
                    rsValue = Integer.toUnsignedLong(rs.intValue());
                }

                if ((modifier & MODIFIER_MB) > 0) {
                    rmValue = rmValue & 0xFFFF;
                } else if ((modifier & MODIFIER_MT) > 0) {
                    rmValue = (rmValue >> 16) & 0xFFFFF;
                }
                if ((modifier & MODIFIER_SB) > 0) {
                    rsValue = rsValue & 0xFFFF;
                } else if ((modifier & MODIFIER_ST) > 0) {
                    rsValue = (rsValue >> 16) & 0xFFFFF;
                }

                long result = rmValue * rsValue;
                d1.setValue(sub(rdLo, (int)(result & 0xFFFFFFFF)));
                d2.setValue(sub(rdHi, (int)((result >> 32) & 0xFFFFFFFF)));
                nzcv = 0;
                if (result == 0) {
                    nzcv |= Modifier.FLAG_Z;
                } else if (result < 0) {
                    nzcv |= Modifier.FLAG_N;
                }
            } else {
                d1.setValue(UnknownImmediate.doubleType());
                d2.setValue(UnknownImmediate.doubleType());
            }
            return nzcv;
        }

    public static AbstractValue mul(Context ctx, Register r1, Register r2) throws Z3Exception {
        Numeral n1 = r1.getNumeral();
        Numeral n2 = r2.getNumeral();
        BitVecExpr b1 = n1.getBitVecExpr(ctx);
        BitVecExpr b2 = n2.getBitVecExpr(ctx);

        if(n1.getType()!=TYPE_INT || n2.getType()!=TYPE_INT ) {
            throw new RuntimeException("not matched data type");
        }

        Numeral value = new Numeral(ctx.mkBVMul(b1, b2).simplify(),TYPE_INT);

        if (n1.isUnknown()) {
            n1.addRelatedNumeral(value);
            value.addRelatedNumeral(n1);
        }
        if(n2.isUnknown()) {
            n2.addRelatedNumeral(value);
            value.addRelatedNumeral(n2);
        }

        return new Immediate(value);
    }

    public static AbstractValue mulxy(Context ctx, Register r1, Register r2, int modifier) throws Z3Exception {
            boolean signed = (modifier & MODIFIER_SIGNED) > 0;
            BitVecExpr rnValue = r1.getValue().getNumeral().getBitVecExpr(ctx);
            BitVecExpr rmValue = r2.getValue().getNumeral().getBitVecExpr(ctx);

            if ((modifier & MODIFIER_MB) > 0) {
                rnValue = ctx.mkExtract(15, 0, rnValue);
            } else if ((modifier & MODIFIER_MT) > 0) {
                rnValue = ctx.mkExtract(31,16, rnValue);
            }
            if ((modifier & MODIFIER_SB) > 0) {
                rmValue = ctx.mkExtract(15,0, rmValue);
            } else if ((modifier & MODIFIER_ST) > 0) {
                rmValue = ctx.mkExtract(31,16, rmValue);
            }

            if(signed) {
                rnValue = ctx.mkSignExt(48, rnValue);
                rmValue = ctx.mkSignExt(48, rmValue);
            } else {
                rnValue = ctx.mkZeroExt(48, rnValue);
                rmValue = ctx.mkZeroExt(48, rmValue);
            }

            BitVecExpr rdValue = ctx.mkBVMul(rnValue, rmValue);

            throw new RuntimeException("N");


            if (!rn.isUnknown() && !rm.isUnknown()) {
                long rnValue = rn.intValue();
                long rmValue = rm.intValue();
                if(!signed) {
                    rnValue = Integer.toUnsignedLong(rn.intValue());
                    rmValue = Integer.toUnsignedLong(rm.intValue());
                }

                if ((modifier & MODIFIER_MB) > 0) {
                    rnValue = rnValue & 0xFFFF;
                } else if ((modifier & MODIFIER_MT) > 0) {
                    rnValue = (rnValue >> 16) & 0xFFFFF;
                }
                if ((modifier & MODIFIER_SB) > 0) {
                    rmValue = rmValue & 0xFFFF;
                } else if ((modifier & MODIFIER_ST) > 0) {
                    rmValue = (rmValue >> 16) & 0xFFFFF;
                }

                long result = rnValue * rmValue;
                short nzcv = 0;

                if (result == 0) {
                    nzcv |= Modifier.FLAG_Z;
                } else if (result < 0) {
                    nzcv |= Modifier.FLAG_N;
                }
                AbstractValue resultValue = rn.getReplacement((int)result);
                resultValue.setNZCV(nzcv);
                return resultValue;


        }


        public static short mullxy(Register d1, Register d2, Register o1, Register o2, int modifier) {
            throw new RuntimeException("not implemented ");
            /*
            short nzcv = -1;
            boolean signed = (modifier & MODIFIER_SIGNED) > 0;
            AbstractValue rm = o1.getValue();
            AbstractValue rs = o2.getValue();
            if (!rm.isUnknown() && !rs.isUnknown()) {
                long rmValue = rm.intValue();
                long rsValue = rs.intValue();
                if(!signed) {
                    rmValue = Integer.toUnsignedLong(rm.intValue());
                    rsValue = Integer.toUnsignedLong(rs.intValue());
                }

                if ((modifier & MODIFIER_MB) > 0) {
                    rmValue = rmValue & 0xFFFF;
                } else if ((modifier & MODIFIER_MT) > 0) {
                    rmValue = (rmValue >> 16) & 0xFFFFF;
                }
                if ((modifier & MODIFIER_SB) > 0) {
                    rsValue = rsValue & 0xFFFF;
                } else if ((modifier & MODIFIER_ST) > 0) {
                    rsValue = (rsValue >> 16) & 0xFFFFF;
                }

                long result = rmValue * rsValue;
                d1.setValue(new Immediate((int) (result & 0xFFFFFFFF)));
                d2.setValue(new Immediate((int) ((result >> 32) & 0xFFFFFFFF)));

                nzcv = 0;
                if (result == 0) {
                    nzcv |= Modifier.FLAG_Z;
                } else if (result < 0) {
                    nzcv |= Modifier.FLAG_N;
                }
            } else {
                d1.setValue(UnknownImmediate.doubleType());
                d2.setValue(UnknownImmediate.doubleType());
            }
            return nzcv;

        }

        public static short umaal(Register d1, Register d2, Register o1, Register o2, int modifier) {
            throw new RuntimeException("not implemented ");

            short nzcv = -1;
            AbstractValue rdLo = d1.getValue();
            AbstractValue rdHi = d2.getValue();
            AbstractValue rm = o1.getValue();
            AbstractValue rs = o2.getValue();
            if (!rdLo.isUnknown() && !rdHi.isUnknown() && !rm.isUnknown() && !rs.isUnknown()) {
                long rdLoValue = Integer.toUnsignedLong(rdLo.intValue());
                long rdHiValue = Integer.toUnsignedLong(rdHi.intValue());
                long rmValue = Integer.toUnsignedLong(rm.intValue());
                long rsValue = Integer.toUnsignedLong(rs.intValue());

                if ((modifier & MODIFIER_MB) > 0) {
                    rmValue = rmValue & 0xFFFF;
                } else if ((modifier & MODIFIER_MT) > 0) {
                    rmValue = (rmValue >> 16) & 0xFFFFF;
                }
                if ((modifier & MODIFIER_SB) > 0) {
                    rsValue = rsValue & 0xFFFF;
                } else if ((modifier & MODIFIER_ST) > 0) {
                    rsValue = (rsValue >> 16) & 0xFFFFF;
                }

                long result = rmValue * rsValue + rdLoValue + rdHiValue;
                d1.setValue(new Immediate((int) (result & 0xFFFFFFFF)));
                d2.setValue(new Immediate((int) ((result >> 32) & 0xFFFFFFFF)));

                nzcv = 0;
                if (result == 0) {
                    nzcv |= Modifier.FLAG_Z;
                } else if (result < 0) {
                    nzcv |= Modifier.FLAG_N;
                }
            } else {
                d1.setValue(UnknownImmediate.doubleType());
                d2.setValue(UnknownImmediate.doubleType());
            }
            return nzcv;

        }

        public static AbstractValue mlsxy(Register r1, Register r2, Register r3, int modifier) {
            throw new RuntimeException("not implemented ");

            boolean signed = (modifier & MODIFIER_SIGNED) > 0;
            AbstractValue rm = r1.getValue();
            AbstractValue rs = r2.getValue();
            AbstractValue ra = r3.getValue();
            if (!rm.isUnknown() && !rs.isUnknown() && !ra.isUnknown()) {
                long rmValue;
                long rsValue;
                long raValue;
                if(signed) {
                    rmValue = rm.intValue();
                    rsValue = rs.intValue();
                    raValue = ra.intValue();
                } else {
                    rmValue = Integer.toUnsignedLong(rm.intValue());
                    rsValue = Integer.toUnsignedLong(rs.intValue());
                    raValue = Integer.toUnsignedLong(ra.intValue());
                }

                if ((modifier & MODIFIER_MB) > 0) {
                    rmValue = rmValue & 0xFFFF;
                } else if ((modifier & MODIFIER_MT) > 0) {
                    rmValue = (rmValue >> 16) & 0xFFFFF;
                }
                if ((modifier & MODIFIER_SB) > 0) {
                    rsValue = rsValue & 0xFFFF;
                } else if ((modifier & MODIFIER_ST) > 0) {
                    rsValue = (rsValue >> 16) & 0xFFFFF;
                }
                long result = raValue - rmValue * rsValue;
                short nzcv = 0;
                if (result == 0) {
                    nzcv |= Modifier.FLAG_Z;
                } else if (result < 0) {
                    nzcv |= Modifier.FLAG_N;
                }

                AbstractValue resultValue = ra.getReplacement((int)result);
                resultValue.setNZCV(nzcv);
                return resultValue;
            }

            if(rm.isUnknown()) {
                return rm;
            } else if(rs.isUnknown()) {
                return rs;
            } else {
                return ra;
            }


        }

        public static AbstractValue mlaxy(Register r1, Register r2, Register r3, int modifier) {
            throw new RuntimeException("not implemented ");

            boolean signed = (modifier & MODIFIER_SIGNED) > 0;
            AbstractValue rm = r1.getValue();
            AbstractValue rs = r2.getValue();
            AbstractValue ra = r3.getValue();
            if (!rm.isUnknown() && !rs.isUnknown() && !ra.isUnknown()) {
                int rmValue = rm.intValue();
                int rsValue = rs.intValue();

                if ((modifier & MODIFIER_MB) > 0) {
                    rmValue = rmValue & 0xFFFF;
                } else if ((modifier & MODIFIER_MT) > 0) {
                    rmValue = (rmValue >> 16) & 0xFFFFF;
                }
                if ((modifier & MODIFIER_SB) > 0) {
                    rsValue = rsValue & 0xFFFF;
                } else if ((modifier & MODIFIER_ST) > 0) {
                    rsValue = (rsValue >> 16) & 0xFFFFF;
                }
                return (add(ra, rmValue * rsValue));
            }
            if(rm.isUnknown()) {
                return rm;
            } else if(rs.isUnknown()) {
                return rs;
            } else {
                return ra;
            }

        }
        */

    public AbstractValue getReplacement(Numeral value) {
        return new Address(libraryModule.getInternalMemory(), value.intValue());
    }

    public int getAddress() {
        return address;
    }

    public boolean isThumb() {
        return thumb;
    }

    public LibraryModule getLibraryModule() {
        return libraryModule;
    }

    public Operand[] getRequiredOperands() {
        throw new RuntimeException("not implemented " + this.getClass().toString());
    }

    public Operand[] getAffectedOperands() {
        throw new RuntimeException("not implemented " + this.getClass().toString());
    }

    public void setAffectedOperands(Operand[] operands) {
        throw new RuntimeException("not implemented " + this.getClass().toString());
    }

    public Opcode getOpcode() {
        return opcode;
    }

//    public static AbstractValue mul(Operand o1, Operand o2) {
//        AbstractValue first = o1.getValue();
//        AbstractValue second = o2.getValue();
//        if (o2 instanceof Register && ((Register) o2).getShift() != null) {
//            throw new RuntimeException("shift");
//        }
//        if (!first.isUnknown() && !second.isUnknown()) {
//            Integer firstValue = first.intValue();
//            Integer secondValue = second.intValue();
//            if (firstValue != null && secondValue != null) {
//
//                AbstractValue value = first.getReplacement(firstValue.intValue() * secondValue.intValue());
//                return value;
//            }
//        }
//
//        return first.isUnknown()?first:second;
//    }

    public Instruction getNext() {
        return next;
    }

    public Instruction getPrev() {
        return prev;
    }

    public void setPrev(Instruction prev) {
        this.prev = prev;
        prev.next = this;
    }

    @Override
    public boolean isInstruction() {
        return true;
    }

    public boolean isIndirect() {
        return false;
    }

    public Instruction addStr(String str) {
        this.str = str;
        return this;
    }

//    public static AbstractValue add(Operand o1, Integer secondValue) {
//        AbstractValue first = o1.getValue();
//        if (!first.isUnknown()) {
//            Integer firstValue = first.intValue();
//            if (firstValue != null) {
//                short nzcv = 0;
//                int result = firstValue + secondValue;
//                if (result == 0) {
//                    nzcv |= Modifier.FLAG_Z;
//                } else if (result < 0) {
//                    nzcv |= Modifier.FLAG_N;
//                }
//                if (((firstValue & secondValue & ~result) | (~firstValue & ~secondValue & result))  != 0) {
//                    nzcv |= Modifier.FLAG_V;
//                }
//
//                if(Integer.toUnsignedLong(firstValue) >= Integer.toUnsignedLong(secondValue)) {
//                    nzcv |= Modifier.FLAG_C;
//                }
//
//                AbstractValue resultValue = first.getReplacement(firstValue + secondValue);
//                resultValue.setNZCV(nzcv);
//                return resultValue;
//            }
//            throw new RuntimeException("none empty");
//        } else {
//            return Unknown.add(first, secondValue);
//        }
//    }

    public Operand getOperand(int i) {
        if (i >= 0 && i < operands.length) {
            return operands[i];
        }
        return null;
    }

    public int getOperandCount() {
        return operands.length;
    }
/*
    public static AbstractValue sub(Operand o1, Operand o2) {
        AbstractValue first = o1.getValue();
        AbstractValue second = o2.getValue();
        if (!first.isUnknown() && !second.isUnknown()) {
            if (first.isImmediate()) {
                return sub(first.intValue(), second);
            } else {
                return sub(first, second.intValue());
            }
        }
        return Unknown.sub(first, second);
    }

    public static AbstractValue sub(Operand o1, int secondValue) {
        AbstractValue first = o1.getValue();
        if (!first.isUnknown()) {
            Integer firstValue = first.intValue();
            if (firstValue != null) {
                short nzcv = 0;
                int result = firstValue - secondValue;
                if (result == 0) {
                    nzcv |= Modifier.FLAG_Z;
                } else if (result < 0) {
                    nzcv |= Modifier.FLAG_N;
                }
                if ((((~secondValue & firstValue & ~result) | (secondValue & ~firstValue & result)) & 0x80000000) != 0) {
                    nzcv |= Modifier.FLAG_V;
                }

                if(Integer.toUnsignedLong(firstValue) >= Integer.toUnsignedLong(secondValue)) {
                    nzcv |= Modifier.FLAG_C;
                }

                AbstractValue resultValue = first.getReplacement(result);
                resultValue.setNZCV(nzcv);
                return resultValue;
            }
            throw new RuntimeException("none empty");
        }
        return first;
    }


    public static AbstractValue sub(int firstValue, Operand o2) {
        AbstractValue second = o2.getValue();
        if (!second.isUnknown()) {
            Integer secondValue = second.intValue();
            if (secondValue != null) {
                short nzcv = 0;
                int result = firstValue - secondValue;
                if (result == 0) {
                    nzcv |= Modifier.FLAG_Z;
                } else if (result < 0) {
                    nzcv |= Modifier.FLAG_N;
                }
                if ((((~secondValue & firstValue & ~result) | (secondValue & ~firstValue & result)) & 0x80000000) != 0) {
                    nzcv |= Modifier.FLAG_V;
                }

                if(Integer.toUnsignedLong(firstValue) >= Integer.toUnsignedLong(secondValue)) {
                    nzcv |= Modifier.FLAG_C;
                }

                AbstractValue resultValue = second.getReplacement(result);
                resultValue.setNZCV(nzcv);
                return resultValue;
            }
            throw new RuntimeException("none empty");
        }
        return second;
    }
    */

    public boolean isConditional() {
        return conditional != CONDI_AL;
    }

    public byte getConditional() {
        return conditional;
    }

    public String toString() {
        if (opcode == Opcode.IGNORABLE) {
            return str + "(-)";
        }
        return Long.toHexString(address) + " : " + str;
    }

    public Operand[] getOperands() {
        return operands;
    }

    public int getModifier() {
        return modifier;
    }

    public byte getHandlingType() { return (byte) (modifier & 0xf); }

    public boolean isGeneral() {
        return false;
    }

    public byte getValueType() {
        return TYPE_INT; // an instruction should be handled as an address, so its size is the same as an address
    }


    /*
    public short getRandomPositive() {
        if(isCompare() && isBranch()) {
            if(conditional == CONDI_EQ) { // continue
                ((Register) operands[0]).setValue(Immediate.ZERO);
            }
            return -1;
        } else {
            short[] result = positiveConditions[conditional];
            if (conditional == CONDI_EQ) {
                if (prev.isCompare()) {
                    AbstractValue value = prev.getOperand(1).getValue();
                    if (value != null && !value.isUnknown()) {
                        ((Register) prev.getOperand(0)).setValue(value);
                    }
                }
            }
            int rnd = new Random().nextInt(result.length);
            return result[rnd];
        }
    }
*/
/*
    public short getRandomNegative() {
        orgValue = null;
        orgReg = -1;
        if(isCompare() && isBranch()) { // skip
            if(conditional == CONDI_NE) {
                ((Register) operands[0]).setValue(Immediate.ZERO);
                orgReg = ((Register) operands[0]).getNumber();
                orgValue = getOperand(0).getValue();
            }
            return -1;
        } else {
            short[] result = negativeConditions[conditional];
            if (conditional == CONDI_NE) {
                if (prev.isCompare()) {
                    AbstractValue value = prev.getOperand(1).getValue();
                    if (value != null && !value.isUnknown()) {
                        orgReg = ((Register) prev.getOperand(0)).getNumber();
                        orgValue = prev.getOperand(0).getValue();
                        ((Register) prev.getOperand(0)).setValue(value);
                    }
                }
            }
            int rnd = new Random().nextInt(result.length);
            return result[rnd];
        }
    }
*/

    public boolean isCompareBranch() {
        return false;
    }

    public boolean isBranch() {
        return false;
    }

    public boolean isCall() {
        return false;
    }

    public boolean isLoad() {
        return false;
    }

    public boolean isStore() {
        return false;
    }

    public boolean isMove() {
        return false;
    }

    public boolean isArithmetic() {
        return false;
    }

    public boolean isLogical() {
        return false;
    }

    public boolean isCompare() {
        return false;
    }

    public boolean isShift() {
        return false;
    }

    public boolean isMultiply() {
        return false;
    }

    public boolean isDivide() {
        return false;
    }

    public boolean isVFPMove() {
        return false;
    }

    public boolean isVFPArithmetic() {
        return false;
    }

    public boolean isVFPDivide() {
        return false;
    }

    public boolean isIfThen() {
        return false;
    }

    public boolean isIgnorable() {
        return false;
    }

    public boolean isReplaced() {
        return false;
    }

    public boolean isTableSwitchBranch() {
        return false;
    }

    public Integer intValue() {
        return address;
    }


    public void compute(Context ctx, NZCVExpr[] nzcvExprs) throws Z3Exception {
        throw new RuntimeException("not implemented yet "+getClass().getName());
    }

    public void compute(Context ctx) throws Z3Exception {
        compute(ctx, null);
    }

    public boolean isSkipForFalse() {
        return (modifier&MODIFIER_COND_DO_NOT_SKIP_FOR_FALSE)==0;
    }
}
