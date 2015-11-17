package com.tosnos.cosa.binary.asm;

import com.tosnos.cosa.binary.LibraryModule;

/**
 * Created by kevin on 7/24/14.
 */
public class IfThenInstruction extends Instruction {
    boolean[] conditionSwitche = null;

    public IfThenInstruction(LibraryModule libraryModule, int address, boolean thumb, Opcode opcode, int modifier, byte condition, Operand operand) {
        super(libraryModule, address, thumb, opcode, modifier, condition, operand);
    }

    @Override
    public Operand[] getRequiredOperands() {
        return null;
    }

    @Override
    public Operand[] getAffectedOperands() {
        return null;
    }

    public boolean[] getConditionSwitch() {
        if (conditionSwitche == null) {
            conditionSwitche = new boolean[modifier & 0x3];
            for (int i = 0; i < conditionSwitche.length; i++) {
                if ((modifier & (1 << (2 + i))) > 0) {
                    conditionSwitche[i] = true;
                } else {
                    conditionSwitche[i] = false;
                }
            }
        }
        return conditionSwitche;
    }

    public boolean isIfThen() {
        return true;
    }
}
