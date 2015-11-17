package com.tosnos.cosa.binary.function;

import com.tosnos.cosa.binary.asm.value.AbstractValue;
import com.tosnos.cosa.binary.asm.Register;

import java.util.Stack;

/**
 * Created by kevin on 2/14/15.
 */
public class JmpBuf extends AbstractValue {
    private AbstractValue[] registerBackup;
    private Stack<Subroutine> depth = new Stack<Subroutine>();

    public JmpBuf(Stack<Subroutine> depth) {
        this.registerBackup = new AbstractValue[Register.NUM_OF_REGISTERS - 1];
        for (int i = 1; i < Register.NUM_OF_REGISTERS; i++) {
            this.registerBackup[i - 1] = Register.getRegisterValue(i);
        }
        this.depth.addAll(depth);
    }

    public void restore(Stack<Subroutine> depth) {
        depth.clear();
        depth.addAll(this.depth);
        for (int i = 1; i < Register.NUM_OF_REGISTERS; i++) {
            Register.setValue(i, this.registerBackup[i - 1]);
        }
    }

    public byte getValueType() {
        throw new RuntimeException("N/A");
    }
}
