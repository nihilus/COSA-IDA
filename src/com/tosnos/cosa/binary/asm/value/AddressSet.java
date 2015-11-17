package com.tosnos.cosa.binary.asm.value;

import com.tosnos.cosa.binary.asm.Operand;
import com.tosnos.cosa.binary.asm.OperandSet;
import com.tosnos.cosa.binary.asm.Register;

/**
 * Created by kevin on 1/13/15.
 */
public class AddressSet extends OperandSet {
    public AddressSet(Operand[] operands) {
        super(operands);
    }

    @Override
    public boolean isAddressSet() {
        return true;
    }


    public Register getBaseRegister() {
        return (Register) operands[0];
    }

    public String toString() {
        StringBuffer stringBuffer = new StringBuffer();
        for (Operand operand : operands) {
            stringBuffer.append(operand.toString() + " ");
        }
        return stringBuffer.toString();
    }
}
