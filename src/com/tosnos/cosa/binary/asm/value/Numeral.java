package com.tosnos.cosa.binary.asm.value;

import com.microsoft.z3.*;
import com.tosnos.cosa.binary.NativeLibraryHandler;
import com.tosnos.cosa.binary.asm.Instruction;
import com.tosnos.cosa.binary.asm.Modifier;
import com.tosnos.cosa.binary.asm.Opcode;
import com.tosnos.cosa.util.Misc;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by kevin on 10/25/15.
 */
public class Numeral implements Modifier {
    private final static Set<BoolExpr> emptyBoolExpr = new HashSet<BoolExpr>();
    private Set<ConstrainExpr> constrainExprSet = new HashSet<ConstrainExpr>();
    private Set<Numeral> relatedNumeral = new HashSet<Numeral>();
    private BitVecExpr value;
    private int hashcode;
    private Number constant; // long or int
    private int offset = 0; //  this offset
    private byte type = TYPE_INT; // default is object
    private Numeral[] halves = null;

    public Numeral() {
    }

    public Numeral add(int offset) {
        if(constant!=null) {
            return new Numeral(constant.intValue() + offset, type);
        } else {
            Numeral newVal = new Numeral();
            newVal.offset = this.offset + offset;
            newVal.value = this.value;
            newVal.type = this.type;
            newVal.halves = this.halves;
            return newVal;
        }
    }



    public static Numeral newValue(byte type) {
        return new Numeral().setType(type);
    }

    private Numeral setType(byte type) {
        this.type = type;
        return this;
    }

    public Numeral(Number constant, byte type) {
        if (constant != null) {
            if (constant instanceof Float) {
                this.constant = Float.floatToRawIntBits(constant.floatValue());
            } else if (constant instanceof Double) {
                this.constant = Double.doubleToRawLongBits(constant.doubleValue());
            } else {
                this.constant = constant;
            }
        }
        this.type = type;
    }

    public Numeral(Expr value, byte type) {
        if (value instanceof BitVecNum) {
            try {
                if (((BitVecNum)value).getSortSize() == BITS_DOUBLEWORD) {
                    this.constant = ((BitVecNum) value).getBigInteger().longValue();
                } else {
                    this.constant = ((BitVecNum) value).getBigInteger().intValue();
                }
            } catch (Z3Exception e) {
                e.printStackTrace();
            }
        }
        this.value = (BitVecExpr)value;
        this.type = type;
    }

    public Numeral(Numeral lower, Numeral upper, byte type) {
        halves = new Numeral[] {lower, upper};
        if(!lower.isUnknown() && !upper.isUnknown()) {
            constant = Misc.combineToLong(lower.intValue(), upper.intValue());
        }
        this.type = type;
    }

    public Numeral(int constant) {
        this(constant, TYPE_INT);
    }

    public Numeral(byte constant) {
        this(constant, TYPE_BYTE);
    }

    public Numeral(short constant) {
        this(constant, TYPE_HALF);
    }

    public Numeral(float constant) {
        this(constant, TYPE_FLOAT);
    }

    public Numeral(double constant) {
        this(constant, TYPE_DOUBLE);
    }

    public Numeral(long constant) {
        this(constant, TYPE_LONG);
    }


//    public Numeral(Numeral numeral) {
//        this.constrainExprSet.addAll(numeral.constrainExprSet);
//        this.value = numeral.value;
//        this.constant = numeral.constant;
//        this.nzcv = numeral.nzcv;
//        this.type = numeral.type;
//    }

    public byte getType() {
        return type;
    }

    public Double doubleValue() {
        if(constant!=null) {
            if(type==TYPE_DOUBLE) {
                return Double.longBitsToDouble(constant.longValue());
            } else if(type==TYPE_FLOAT){
                return floatValue().doubleValue();
            } else {
                return (double)constant.intValue();
            }
        }
        return null;
    }

    public Long longValue() {
        if(type!=TYPE_LONG) {
            throw new RuntimeException("not long");
        }

        if(constant!=null) {
            return constant.longValue();
        }
        return null;
    }

    public Float floatValue() {
        if(constant!=null) {
            return Float.intBitsToFloat(constant.intValue());
        }
        return null;
    }

    public Integer intValue() {
        if(constant!=null) {
            return constant.intValue();
        }
        return null;
    }

    public boolean isUnknown() {
        return constant==null;
    }


    // @Override
    synchronized public BitVecExpr getBitVecExpr(Context ctx) throws Z3Exception {
        return getBitVecExpr(ctx, BITS[type]);
    }

    synchronized public BitVecExpr getBitVecExpr(Context ctx, int bit) throws Z3Exception {
        if(value==null) {
            if(constant!=null) {
                if(bit == BITS_DOUBLEWORD) {
                    value = ctx.mkBV(constant.longValue(), bit);
                } else {
                    value = ctx.mkBV(constant.intValue(), bit);
                }
            } else {
                if(halves!=null) {
                    BitVecExpr upper = halves[Immediate.UPPER].getBitVecExpr(ctx);
                    BitVecExpr lower = halves[Immediate.LOWER].getBitVecExpr(ctx);
                    value = (BitVecExpr)ctx.mkConcat(upper, lower).simplify();
                    addRelatedNumeral(halves[Immediate.LOWER] );
                    halves[Immediate.LOWER] .addRelatedNumeral(this);
                    addRelatedNumeral(halves[Immediate.UPPER]);
                    halves[Immediate.UPPER].addRelatedNumeral(this);
                } else {
                    value = ctx.mkBVConst(ctx.mkSymbol(hashCode()), bit); // if an unknown value is not int type, it should be considered
                }
            }
        }
        if(offset>0) {
            value = (BitVecExpr)ctx.mkBVAdd(value, ctx.mkBV(offset, bit)).simplify();
            offset = 0;
        }
        return value;
    }

    public void addRelatedNumeral(Numeral numeral) {
        relatedNumeral.add(numeral);
    }

    private Numeral[] getHalves(Context ctx) throws Z3Exception {
        if(halves==null) {

            Numeral lower = new Numeral(ctx.mkExtract(31, 0, getBitVecExpr(ctx)).simplify(), TYPE_INT);
            Numeral upper = new Numeral(ctx.mkExtract(63, 32, getBitVecExpr(ctx)).simplify(), TYPE_INT);
            if (isUnknown()) {
                addRelatedNumeral(lower);
                lower .addRelatedNumeral(this);
                addRelatedNumeral(upper);
                upper.addRelatedNumeral(this);
            }
            halves = new Numeral[] { lower, upper};
        }
        return halves;
    }

    public Numeral getLower(Context ctx) throws Z3Exception {
        return getHalves(ctx)[Immediate.LOWER];
    }

    public Numeral getUpper(Context ctx)throws Z3Exception {
        return getHalves(ctx)[Immediate.UPPER];
    }

    public Set<BoolExpr> getConstrains() {
        if(constant!=null) {
            return emptyBoolExpr;
        }
        Set<BoolExpr> constrainSet = new HashSet<BoolExpr>();
        Set<Numeral> visited = new HashSet<Numeral>();
        constrainSet.addAll(getContrainsExprSet(visited));
        return constrainSet;
    }

    public Set<BoolExpr> getContrainsExprSet(Set<Numeral> visited) {
        if(constant!=null) {
            return emptyBoolExpr;
        }
        Set<BoolExpr> constrainSet = new HashSet<BoolExpr>();
        for(ConstrainExpr expr:constrainExprSet) {
            constrainSet.add(expr.getBoolExpr());
            for(Numeral numeral:expr.getRelatedNumeralSet()) {
                if(!visited.contains(numeral)) {
                    visited.add(numeral);
                    constrainSet.addAll(numeral.getContrainsExprSet(visited));
                }
            }
        }

        for(Numeral related:relatedNumeral) {
            if(!visited.contains(related)) {
                visited.add(related);
                constrainSet.addAll(related.getContrainsExprSet(visited));
            }
        }
        return constrainSet;
    }

//    public Numeral addConstrains(Context ctx, SIGN sign, Instruction inst, int value) throws Z3Exception {
//        constrainExprSet.add(new ConstrainExpr(makeConstrain(ctx, sign, inst, value)));
//        return this;
//    }
//
    public Numeral setPositive(Context ctx) throws Z3Exception { // positive
        addConstrains(new ConstrainExpr(ctx.mkBVSGE(getBitVecExpr(ctx), ctx.mkBV(0, getBitVecExpr(ctx).getSortSize()))));
        return this;
    }

    public boolean addConstrains(ConstrainExpr expr) {
        return constrainExprSet.add(expr);
    }

    public boolean removeConstrains(ConstrainExpr expr) {
        return constrainExprSet.remove(expr);
    }

//    public BoolExpr makeConstrain(Context ctx, SIGN sign, Instruction inst, int value) throws Z3Exception {
//        return makeConstrain(ctx, sign, inst, ctx.mkBV(value, BITS[type]));
//    }
//
//    public BoolExpr makeConstrain(Context ctx, SIGN sign, Instruction inst, BitVecExpr value) throws Z3Exception {
//        return inst.getConstrain(ctx, sign, getBitVecExpr(ctx), value);
//    }

    @Override
    public int hashCode() {
        if(hashcode==0) {
            if(constant!=null) {
                hashcode = constant.hashCode();
            } else {
                hashcode = super.hashCode();
            }
        }
        return hashcode;
    }

    public String toHexString() {
        if(constant!=null) {
            return "0x" + Integer.toHexString(constant.intValue());
        }
        return toString();
    }

    public String toString() {
        if(constant!=null) {
            return "0x" + Integer.toHexString(constant.intValue());
        }
        if(value!=null) {
            return value.toString();
        }
        if(halves!=null) {
            return halves[0].toString() + "|" + halves[1].toString();
        }

        return null;
    }


    public Number getNumber() {
        switch(type) {
            case TYPE_BYTE:
                return intValue().byteValue();
            case TYPE_HALF:
                return intValue().shortValue();
            case TYPE_INT:
                return intValue();
            case TYPE_FLOAT:
                return floatValue();
            case TYPE_DOUBLE:
                return doubleValue();
            case TYPE_LONG:
                return longValue();
            default:
                throw new RuntimeException("Wrong type");
        }
    }
}
