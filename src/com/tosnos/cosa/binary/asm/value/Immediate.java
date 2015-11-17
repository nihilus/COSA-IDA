/*
 * Copyright (c) 2002, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 *
 */

package com.tosnos.cosa.binary.asm.value;

// it's only for number variable

import com.tosnos.cosa.binary.asm.Instruction;

public final class Immediate extends AbstractValue {

    public static final int LOWER = 0; /// unsigned
    public static final int UPPER = 1;
    public static final Immediate EMPTY = Immediate.newValue(TYPE_INT);
    public static final Immediate ZERO = new Immediate(0);
    public static final Immediate ONE = new Immediate(1);
    public static final Immediate MINUS_ONE = new Immediate(-1);
    public static final Immediate WORD = new Immediate(Instruction.WORD);
    public String str;
    private Numeral numeral;

    public Immediate() {
        this.numeral = new Numeral();
    }

    public Immediate(Number constant, byte type) {
        this.numeral = new Numeral(constant, type);
    }

    public Immediate(Numeral numeral) {
        this.numeral = numeral;
    }

    public Immediate(String str) {
        if(str.contains(".")) {
            this.str = str;
        } else {
            this.numeral = new Numeral(Long.decode(str).intValue());
        }
    }

    public Immediate(int constant) {
        this(constant, TYPE_INT);
    }

    public Immediate(byte constant) {
        this(constant, TYPE_BYTE);
    }

    public Immediate(short constant) {
        this(constant, TYPE_HALF);
    }

    public Immediate(float constant) {
        this(constant, TYPE_FLOAT);
    }


    public Immediate(double constant) {
        this(constant, TYPE_DOUBLE);
    }

    public Immediate(long constant) {
        this(constant, TYPE_LONG);
    }

    public Immediate(AbstractValue lower, AbstractValue upper, byte type) { // for byte
        numeral = new Numeral(lower.getNumeral(), upper.getNumeral(), type);
    }

    public static Immediate newValue(byte type) {
        return new Immediate(null, type);
    }

    @Override
    public AbstractValue getReplacement(Numeral numeral) {
        return new Immediate(numeral);
    }

    @Override
    public boolean isUnknown() {
        return numeral.isUnknown();
    }

    @Override
    public boolean isImmediate() {
        return true;
    }

    /*
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null || obj.getClass() != this.getClass())
            return false;

        Immediate other = (Immediate)obj;
        return other.numeral.equals(other.numeral);}

    @Override
    public int hashCode() {
        return numeral.hashCode();
    }
    /*
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        if(numeral==null) {
            return "E";
        }
        return numeral.toString(); // value==null?"U":value.toString(); //+ bw;
    }

    @Override
    public Integer intValue() {
        return numeral.intValue();
    }

    @Override
    public Numeral getNumeral() {
        return numeral;
    }

    @Override
    public byte getType() {
        return numeral.getType();
    }

    @Override
    public AbstractValue DoublePrecision() {
        if(str!=null) {
            this.numeral = new Numeral(Double.parseDouble(str), TYPE_DOUBLE);
            this.str = null;
        }
        return this;
    }

    @Override
    public AbstractValue SinglePrecision() {
        if(str!=null) {
            this.numeral = new Numeral(Float.parseFloat(str), TYPE_FLOAT);
            this.str = null;
        }

        return this;
    }
}
