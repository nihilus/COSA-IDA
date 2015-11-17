package com.tosnos.cosa.binary.asm;

import com.tosnos.cosa.binary.asm.value.ConstrainExpr;

/**
 * Created by kevin on 11/3/15.
 */
public class NZCVExpr {
    private final ConstrainExpr positive;
    private final ConstrainExpr negative;

    public NZCVExpr(ConstrainExpr positive, ConstrainExpr negative) {
        this.positive = positive;
        this.negative = negative;
    }

    public ConstrainExpr getPositive() {
        return positive;
    }

    public ConstrainExpr getNegative() {
        return negative;
    }
}
