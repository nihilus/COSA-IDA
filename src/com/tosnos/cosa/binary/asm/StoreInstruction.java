package com.tosnos.cosa.binary.asm;

import com.tosnos.cosa.binary.LibraryModule;

import java.util.Arrays;

/**
 * Created by kevin on 7/4/14.
 */
public class StoreInstruction extends MemoryInstruction {
    Operand storeDestination;
    Register[] storeSources;
    Operand[] affectedOperands;

    boolean lrRelative = false;

    public StoreInstruction(LibraryModule libraryModule, int address, boolean thumb, Opcode opcode, int modifier, byte condition, Operand operand) {
        super(libraryModule, address, thumb, opcode, modifier, condition, operand);

        if (opcode == Opcode.STM) {
            OperandSet os = (OperandSet) operands[1];
            for (Operand o : os.getOperands()) {
                if (o instanceof Register && ((Register) o).isLinkRegister()) {
                    lrRelative = true;
                    break;
                }
            }
        } else if (opcode == Opcode.PUSH) { // for push
            for (Operand o : operands) {
                if (o instanceof Register && ((Register) o).isLinkRegister()) {
                    lrRelative = true;
                    break;
                }
            }
        }
    }

    @Override
    public Operand[] getRequiredOperands() {
        return getStoreSources();
    }


    @Override
    public Operand[] getAffectedOperands() {
        if (affectedOperands == null) {
            if (opcode == Opcode.PUSH) {
                affectedOperands = Arrays.copyOf(operands, operands.length + 1);
                affectedOperands[operands.length] = getStoreDestination();
            } else {
                affectedOperands = operands;
            }
//            Operand source = getLoadSource();
//            if (source.isRegister()) {
//                Operand[] operands = Arrays.copyOf(loadDestinations, loadDestinations.length + 1);
//                operands[loadDestinations.length] = source;
//                affectedOperands = operands;
//            } else {
//                affectedOperands = loadDestinations;
//            }
        }
        return affectedOperands;
    }

    public Operand getStoreDestination() {
        if (storeDestination == null) {
            switch (opcode) {
                case VPUSH:
                case PUSH:
                    storeDestination = new Register(Register.SP).setPreIndexed();
                    break;
                case STM:
                    storeDestination = operands[0];
                    break;
                case VSTR:
                case STR:
                    storeDestination = operands[operands.length-1];
                    if(!storeDestination.isIndirectAddress() && !storeDestination.isVariable() && !storeDestination.isExprValue()) {
                        storeDestination = operands[operands.length-2];
                    }
                    break;
            }
        }
        return storeDestination;
    }

    public Register[] getStoreSources() {
        if (storeSources == null) {
            switch (opcode) {
                case VPUSH:
                case PUSH:
                    modifier |= MODIFIER_BEFORE | MODIFIER_DECREMENT;
                    storeSources = Arrays.copyOf(operands, operands.length, Register[].class); // reverse
                    break;

                case STM:
                    storeSources = Arrays.copyOf(((OperandSet) operands[1]).getOperands(), ((OperandSet) operands[1]).size(), Register[].class);
                    break;

                case STR:
                case VSTR:
                    if ((modifier & 0xf)== TYPE_DOUBLE && !((Register)operands[0]).isDoubleword()) {
                        int regNo = ((Register) operands[0]).getNumber();
                        storeSources = new Register[]{(Register) operands[0], new Register(regNo + 1)};
                    } else {
                        int length = operands.length-1;
                        if(!operands[length].isIndirectAddress() && !operands[length].isVariable() && !operands[length].isExprValue()) {
                            length--;
                        }
                        storeSources = Arrays.copyOf(operands, length, Register[].class);
                    }
                    break;
            }
        }
        return storeSources;
    }

    public boolean isLRRelative() {
        return lrRelative;
    }

    @Override
    public boolean isStore() {
        return true;
    }
}