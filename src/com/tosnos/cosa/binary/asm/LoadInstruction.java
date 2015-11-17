package com.tosnos.cosa.binary.asm;

import com.tosnos.cosa.binary.LibraryModule;

import java.util.Arrays;

/**
 * Created by kevin on 7/4/14.
 */
public class LoadInstruction extends MemoryInstruction {
    Operand[] affectedOperands;
    Operand loadSource = null;
    Register[] loadDestinations = null;
    boolean lrRelative = false;
    boolean pcRelative = false;

    public LoadInstruction(LibraryModule libraryModule, int address, boolean thumb, Opcode opcode, int modifier, byte condition, Operand operand) {
        super(libraryModule, address, thumb, opcode, modifier, condition, operand);
        switch (opcode) {
            case LDR: {
                Operand o = operands[0];
                if(o.isRegister() && ((Register)o).isProgramCounter()) {
                    pcRelative = true;
                }
                break;
            }
            case LDM: {
                OperandSet os = (OperandSet) operands[1];
                for (Operand o : os.getOperands()) {
                    if (o.isRegister()) {
                        if (((Register) o).isProgramCounter()) {
                            pcRelative = true;
                            break;
                        } else if (((Register) o).isLinkRegister()) {
                            lrRelative = true;
                            break;
                        }
                    }
                }
                break;
            }
            case VPOP:
            case POP: {
                for (Operand o : operands) {
                    if (((Register) o).isProgramCounter()) {
                        pcRelative = true;
                        break;
                    } else if (((Register) o).isLinkRegister()) {
                        lrRelative = true;
                        break;
                    }
                }
                break;
            }
        }
    }

    @Override
    public Operand[] getRequiredOperands() {
        return new Operand[]{getLoadSource()};
    }

    @Override
    public Operand[] getAffectedOperands() {
        if (affectedOperands == null) {
            if (opcode == Opcode.POP) {
                affectedOperands = Arrays.copyOf(operands, operands.length + 1);
                affectedOperands[operands.length] = getLoadSource();
            } else {
                affectedOperands = operands;
            }
        }
        return affectedOperands;
    }


    public Operand getLoadSource() {
        if (loadSource == null) {
            switch (opcode) {
                case VPOP:
                case POP:
                    loadSource = new Register(Register.SP).setPreIndexed();
                    break;
                case LDM:
                    loadSource = operands[0];
                    break;
                case VLDR:
                case LDR:
                    loadSource = operands[operands.length-1];
                    if(!loadSource.isIndirectAddress() && !loadSource.isVariable() && !loadSource.isExprValue()) {
                        loadSource = operands[operands.length-2];
                    }
                    break;
            }
        }
        return loadSource;
    }

    public Register[] getLoadDestinations() {
        if (loadDestinations == null) {
            switch (opcode) {
                case VPOP:
                case POP:
                    loadDestinations = Arrays.copyOf(operands, operands.length, Register[].class);
                    break;
                case LDM:
                    loadDestinations = Arrays.copyOf(((OperandSet) operands[1]).getOperands(), ((OperandSet) operands[1]).size(), Register[].class);
                    break;
                case VLDR:
                case LDR:
                    if ((modifier & 0xf)== TYPE_DOUBLE && !((Register)operands[0]).isDoubleword()) {
                        if(operands[1].isIndirectAddress()) {
                            int regNo = ((Register) operands[0]).getNumber();
                            loadDestinations = new Register[]{(Register) operands[0], new Register(regNo + 1)};
                        } else {
                            loadDestinations = new Register[]{(Register) operands[0], (Register) operands[1]};
                        }
                    } else {
                        int length = operands.length-1;
                        if(!operands[length].isIndirectAddress() && !operands[length].isVariable() && !operands[length].isExprValue()) {
                            length--;
                        }
                        loadDestinations = Arrays.copyOf(operands, length, Register[].class);
                    }
                    break;
            }
        }
        return loadDestinations;
    }

    public boolean isLRRelative() {
        return lrRelative;
    }

    public boolean isPCRelative() {
        return pcRelative;
    }

    @Override
    public boolean isLoad() {
        return true;
    }
}
