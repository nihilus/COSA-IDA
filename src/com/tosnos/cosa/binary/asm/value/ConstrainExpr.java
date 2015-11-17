package com.tosnos.cosa.binary.asm.value;

import com.microsoft.z3.*;
import com.tosnos.cosa.binary.asm.Instruction;
import soot.util.Cons;
import soot.util.Numberable;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by kevin on 10/26/15.
 */
public class ConstrainExpr {
    private Set<Numeral> relatedNumeralSet = new HashSet<Numeral>();
    private final BoolExpr expr;
    public ConstrainExpr(BoolExpr expr) {
        this.expr = expr;
    }

    public BoolExpr getBoolExpr() {
        return expr;
    }

    public ConstrainExpr setNumeral(Numeral numeral) {
        if(numeral.isUnknown()) {
            relatedNumeralSet.add(numeral);
        }
        return this;
    }

    public ConstrainExpr setNumeral(Numeral numeral1, Numeral numeral2) {
        if(numeral1.isUnknown()) {
            relatedNumeralSet.add(numeral1);
        }
        if(numeral2.isUnknown()) {
            relatedNumeralSet.add(numeral2);
        }
        return this;
    }

    public ConstrainExpr or(Context ctx, ConstrainExpr other) throws Z3Exception {
        ConstrainExpr e = new ConstrainExpr((BoolExpr) ctx.mkOr(expr, other.expr).simplify());
        Set<Numeral> ns = new HashSet<Numeral>();
        ns.addAll(relatedNumeralSet);
        ns.addAll(other.relatedNumeralSet);
        for(Numeral n:ns) {
            e.setNumeral(n);
        }
        return e;
    }

    public ConstrainExpr and(Context ctx, ConstrainExpr other) throws Z3Exception {
        ConstrainExpr e = new ConstrainExpr((BoolExpr) ctx.mkAnd(expr, other.expr).simplify());
        Set<Numeral> ns = new HashSet<Numeral>();
        ns.addAll(relatedNumeralSet);
        ns.addAll(other.relatedNumeralSet);
        for(Numeral n:ns) {
            e.setNumeral(n);
        }
        return e;
    }

    public ConstrainExpr eq(Context ctx, ConstrainExpr other) throws Z3Exception {
        ConstrainExpr e = new ConstrainExpr((BoolExpr) ctx.mkEq(expr, other.expr).simplify());
        Set<Numeral> ns = new HashSet<Numeral>();
        ns.addAll(relatedNumeralSet);
        ns.addAll(other.relatedNumeralSet);
        for(Numeral n:ns) {
            e.setNumeral(n);
        }
        return e;
    }

    public ConstrainExpr ne(Context ctx, ConstrainExpr other) throws Z3Exception {
        ConstrainExpr e = new ConstrainExpr((BoolExpr) ctx.mkNot(ctx.mkEq(expr, other.expr)).simplify());
        Set<Numeral> ns = new HashSet<Numeral>();
        ns.addAll(relatedNumeralSet);
        ns.addAll(other.relatedNumeralSet);
        for(Numeral n:ns) {
            e.setNumeral(n);
        }
        return e;
    }


    public Set<Numeral> getRelatedNumeralSet() {
        return relatedNumeralSet;
    }


    public Set<Numeral> setConstrain() {
        Set<Numeral> numerals = new HashSet<Numeral>();
        for(Numeral n:relatedNumeralSet) {
            if(n.addConstrains(this)) {
                numerals.add(n);
            }
        }
        return numerals;
    }

    public void releaseConstrain(Set<Numeral> numerals) {
        for(Numeral n:numerals) {
            n.removeConstrains(this);
        }
    }

    final public boolean checkCondition(Context ctx) throws Z3Exception {
        Solver solver = ctx.mkSolver();
        if(relatedNumeralSet.isEmpty()) {
            solver.add(expr);
        } else {
            for (BoolExpr e : relatedNumeralSet.iterator().next().getConstrains()) {
                solver.add(e);
            }
        }
        return solver.check() == Status.SATISFIABLE;
    }


}
