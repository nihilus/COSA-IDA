package com.tosnos.cosa.binary.asm.value;

import com.tosnos.cosa.binary.LibraryModule;
import com.tosnos.cosa.binary.asm.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by kevin on 12/7/14.
 */
public class ExprValue extends AbstractValue {
    private final boolean assignedValue;
    private final String expr;
    private final LibraryModule libraryModule;
    private AbstractValue[] values;

    public ExprValue(LibraryModule libraryModule, String expr) {
        if (expr.charAt(0) == '=') {
            assignedValue = true;
            expr = expr.substring(1);
        } else {
            assignedValue = false;
        }

        if (expr.charAt(0) == '(') {
            expr = expr.substring(1, expr.length() - 1);
        }
        this.expr = expr;
        this.libraryModule = libraryModule;
    }

    @Override
    public AbstractValue getReplacement(Numeral numeral) {
        return new Immediate(numeral);
    }

    @Override
    public List<Register> getRegisterList() {
        if (values == null) {
            values = parse();
        }
        List<Register> operandList = new ArrayList<Register>();
        for (Operand operand : values) {
            if (operand.isOperandSet()) {
                operandList.addAll(operand.getRegisterList());
            } else if (operand.isRegister()) {
                operandList.add((Register) operand);
            }
        }
        return operandList;
    }

    @Override
    public AbstractValue getValue() {
        if (values == null) {
            values = parse();
        }
        if (values.length == 2) {
            return values[0].add(values[1]);
        } else {
            return values[0];
        }
    }

    //
    @Override
    public String toString() {
        return expr;
    }

    private AbstractValue[] parse() {
        String tokens[] = expr.split("\\s+"); // expr.split("(?<=[-+*/])|(?=[-+*/])");//
        if (tokens.length == 3) {
            return new AbstractValue[]{parse(libraryModule, tokens[0]), parse(libraryModule, tokens[1] + tokens[2])};
        } else if (tokens.length == 1) {
            tokens = expr.split("\\+");
            if (tokens.length == 2) {
                return new AbstractValue[]{parse(libraryModule, tokens[0]), parse(libraryModule, tokens[1])};
            } else {
                tokens = expr.split("\\-");
                if (tokens.length == 2) {
                    if(tokens[0].isEmpty()) {
                        return new AbstractValue[]{parse(libraryModule, "-" + tokens[1])};
                    } else {
                        return new AbstractValue[]{parse(libraryModule, tokens[0]), parse(libraryModule, "-" + tokens[1])};
                    }
                } else {
                    return new AbstractValue[]{parse(libraryModule, tokens[0])};
                }
            }
        } else {
            throw new RuntimeException(expr);
        }
    }

    @Override
    public boolean isExprValue() {
        return true;
    }

    public boolean isAssignedValue() {
        return assignedValue;
    }

    @Override
    public Numeral getNumeral() {
        return getValue().getNumeral();
    }
}
