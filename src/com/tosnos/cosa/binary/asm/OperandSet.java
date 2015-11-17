package com.tosnos.cosa.binary.asm;


import com.microsoft.z3.Z3Exception;
import com.tosnos.cosa.binary.asm.value.AbstractValue;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by kevin on 12/8/14.
 */
public class OperandSet extends Operand {
    protected Operand[] operands;

    public OperandSet(Operand[] operands) {
        this(operands, false);
    }

    public OperandSet(Operand[] operands, boolean range) {
        this.operands = operands;
        if (range && operands.length == 2) {
            int start = ((Register) operands[0]).getNumber();
            int end = ((Register) operands[1]).getNumber();
            operands = new Operand[end - start + 1];
            for (int i = start; i <= end; i++) {
                operands[i - start] = new Register(i);
            }
        }
    }

    public boolean isOperandSet() {
        return true;
    }

    public Operand get(int index) {
        if (index >= 0 && index < operands.length) {
            return operands[index];
        }
        return null;
    }

    public int size() {
        return operands.length;
    }

    public Operand[] getOperands() {
        return operands;
    }

    @Override
    public List<Register> getRegisterList() {
        List<Register> operandList = new ArrayList<Register>();
        for (Operand operand : operands) {
            if (operand.isOperandSet()) {
                operandList.addAll(operand.getRegisterList());
            } else if (operand.isRegister()) {
                operandList.add((Register) operand);
            }
        }
        return operandList;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < operands.length; i++) {
            if (i != 0) {
                sb.append(", ");
            }
            sb.append(operands[i].toString());
        }

        if (preIndexed) {
            sb.insert(0, "[");
            sb.append("]");
            sb.append("!");
        } else {
            sb.insert(0, "{");
            sb.append("}");
        }
        return sb.toString();
    }

    @Override
    public AbstractValue getValue() {
        if (operands.length == 0) {
            return null;
        }
        AbstractValue value = null;
        try {
            if (operands.length == 2 && operands[0].isOperandSet()) { // for post-indexed addressing
                value = operands[0].getValue();
                if (value != null) {
                    Register rd = (Register) ((OperandSet) operands[0]).get(0);

                        rd.setValue(ArithmeticInstruction.add(value, operands[1])); // operands[1].isAbstractValueContainer()?((IAbstractValueContainer)operands[1]).getValue():(AbstractValue)operands[1]));

                }
    //            System.out.println(Misc.ansi().fg(Misc.RED).a("OperandSet Post-Indexed").reset());
            } else {
                for (Operand operand : operands) {
                    AbstractValue v;
                    if (operand.isRegister()) {
                        v = operand.getValue();
                    } else {
                        v = (AbstractValue) operand; //operand.isAbstractValueContainer()?((IAbstractValueContainer)operand).getValue():(AbstractValue)operand;
                    }

                    if (value == null) {
                        value = v;
                    } else {
                        value = ArithmeticInstruction.add(value, v);
                    }
                }

                if (preIndexed) {
    //                System.out.println(Misc.ansi().fg(Misc.RED).a("OperandSet pre-Indexed").reset());
                    if (operands.length > 0 && operands[0] instanceof Register) {
                        ((Register) operands[0]).setValue(value);
                    }
                }
            }
        } catch (Z3Exception e) {
            e.printStackTrace();
        }
        return value;
    }

    public Operand getLastElement() {
        return operands[operands.length-1];
    }

    @Override
    public List<Operand> getElements() {
        List list = new ArrayList<Operand>();
        for(Operand operand:operands) {
            if(operand.isOperandSet()) {
                list.addAll(operand.getElements());
            } else {
                list.add(operand);
            }
        }
        return list;
    }
}
